package com.jiayi.campustrade.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiayi.campustrade.config.AiProperties;
import com.jiayi.campustrade.entity.KbChunk;
import com.jiayi.campustrade.entity.KbDoc;
import com.jiayi.campustrade.exception.BusinessException;
import com.jiayi.campustrade.mapper.KbChunkMapper;
import com.jiayi.campustrade.mapper.KbDocMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 平台知识库服务（B12 RAG 基础层）
 *
 * 职责（本轮仅基础层，暂不接入 AiService）：
 *   1. rebuildFromSeed()：读取 classpath:knowledge/*.md 种子文档，
 *      按标题 upsert kb_doc → 删除旧块 → 分块 → 调 EmbeddingClient 向量化 → 写入 kb_chunk；
 *      仅由管理端手动触发，绝不自动修改数据库（无 CommandLineRunner/@PostConstruct）。
 *   2. reload()：从 kb_chunk 加载「启用文档 + 已向量化」知识块，在 JVM 内存建立检索索引。
 *   3. search(query)：查询文本向量化 → 与内存向量逐块余弦相似度 → Top-K + 阈值过滤，
 *      未命中时返回 hit=false（调用方可据此决定是否注入提示词）。
 *
 * 规模假设：语料为百级块（1024 维），内存占用约 MB 级，暴力余弦毫秒级，无需向量数据库。
 */
@Service
public class KnowledgeService {

    /** 分块目标长度（字符）：对齐提示词注入粒度，中文 FAQ 段落约 200~300 字 */
    private static final int CHUNK_TARGET = 300;

    /** 单块硬上限：超长段落强制再切，防止单块过大稀释向量语义 */
    private static final int CHUNK_MAX = 600;

    /** 种子知识文件所在 classpath 目录 */
    private static final String SEED_DIR = "classpath:knowledge/*.md";

    private final KbDocMapper kbDocMapper;
    private final KbChunkMapper kbChunkMapper;
    private final EmbeddingClient embeddingClient;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;

    @Autowired
    public KnowledgeService(KbDocMapper kbDocMapper,
                            KbChunkMapper kbChunkMapper,
                            EmbeddingClient embeddingClient,
                            ObjectMapper objectMapper,
                            AiProperties aiProperties) {
        this.kbDocMapper = kbDocMapper;
        this.kbChunkMapper = kbChunkMapper;
        this.embeddingClient = embeddingClient;
        this.objectMapper = objectMapper;
        this.aiProperties = aiProperties;
    }

    // =========================
    // 内存检索索引
    // =========================

    /** 内存索引条目：向量与范数在建索引时预计算，检索时只做点积 */
    private static final class IndexedChunk {
        final int chunkId;
        final int docId;
        final String docTitle;
        final int seq;
        final String content;
        final double[] vector;
        final double norm;

        IndexedChunk(int chunkId, int docId, String docTitle, int seq, String content, double[] vector) {
            this.chunkId = chunkId;
            this.docId = docId;
            this.docTitle = docTitle;
            this.seq = seq;
            this.content = content;
            this.vector = vector;
            this.norm = norm(vector);
        }
    }

    /** 不可变索引引用：reload 整体替换，search 无锁读取 */
    private volatile List<IndexedChunk> index = List.of();
    private volatile boolean loaded = false;

    /** 检索命中结果 */
    public static final class SearchHit {
        private final int docId;
        private final String docTitle;
        private final int seq;
        private final String content;
        private final double similarity;

        SearchHit(int docId, String docTitle, int seq, String content, double similarity) {
            this.docId = docId;
            this.docTitle = docTitle;
            this.seq = seq;
            this.content = content;
            this.similarity = similarity;
        }

        public int getDocId() { return docId; }
        public String getDocTitle() { return docTitle; }
        public int getSeq() { return seq; }
        public String getContent() { return content; }
        public double getSimilarity() { return similarity; }
    }

    /** 检索结果：hit=false 表示未命中（低于阈值/索引为空），调用方不应注入提示词 */
    public static final class SearchResult {
        private final boolean hit;
        private final List<SearchHit> hits;

        SearchResult(List<SearchHit> hits) {
            this.hit = !hits.isEmpty();
            this.hits = hits;
        }

        public boolean isHit() { return hit; }
        public List<SearchHit> getHits() { return hits; }
    }

    /** 重建索引报告 */
    public static final class RebuildReport {
        public final int docCount;
        public final int chunkCount;
        public final int embedCount;
        public final long elapsedMs;

        RebuildReport(int docCount, int chunkCount, int embedCount, long elapsedMs) {
            this.docCount = docCount;
            this.chunkCount = chunkCount;
            this.embedCount = embedCount;
            this.elapsedMs = elapsedMs;
        }
    }

    /**
     * 知识库检索（B12 基础层对外入口，后续接入 AiService 时调用）。
     * 索引为空或查询为空 → 未命中；Embedding 调用失败会抛 BusinessException，
     * 由上层决定是否降级为无 RAG 回答。
     */
    public SearchResult search(String query) {
        ensureLoaded();
        if (query == null || query.trim().isEmpty() || index.isEmpty()) {
            return new SearchResult(List.of());
        }
        double[] queryVector = embeddingClient.embed(query);
        double queryNorm = norm(queryVector);
        if (queryNorm == 0) {
            return new SearchResult(List.of());
        }

        List<SearchHit> candidates = new ArrayList<>();
        double threshold = aiProperties.getRag().getThreshold();
        for (IndexedChunk chunk : index) {
            double similarity = cosine(queryVector, queryNorm, chunk);
            if (similarity >= threshold) {
                candidates.add(new SearchHit(chunk.docId, chunk.docTitle, chunk.seq, chunk.content, similarity));
            }
        }
        // 相似度降序取 Top-K（K 由 llm.rag.top-k 配置）
        int topK = Math.max(1, aiProperties.getRag().getTopK());
        candidates.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        return new SearchResult(candidates.subList(0, Math.min(topK, candidates.size())));
    }

    /** 从数据库重建内存索引（可重复调用） */
    public synchronized void reload() {
        List<KbChunk> rows = kbChunkMapper.findEnabled();
        List<IndexedChunk> newIndex = new ArrayList<>(rows.size());
        for (KbChunk row : rows) {
            if (row.getEmbedding() == null || row.getEmbedding().isBlank()) {
                continue;
            }
            try {
                double[] vector = objectMapper.readValue(row.getEmbedding(), double[].class);
                newIndex.add(new IndexedChunk(row.getChunkId(), row.getDocId(),
                        row.getDocTitle(), row.getSeq() == null ? 0 : row.getSeq(),
                        row.getContent(), vector));
            } catch (Exception e) {
                // 单条向量损坏不拖垮整个索引，跳过并留痕
                System.out.println("[KnowledgeService] 跳过向量解析失败的 chunkId=" + row.getChunkId());
            }
        }
        this.index = List.copyOf(newIndex);
        this.loaded = true;
        System.out.println("[KnowledgeService] 内存检索索引已加载：" + index.size() + " 个知识块");
    }

    /** 懒加载：首次 search 时才读库；库为空也视为已加载（避免反复查空表） */
    private void ensureLoaded() {
        if (!loaded) {
            reload();
        }
    }

    // =========================
    // 种子知识重建索引（手动触发）
    // =========================

    /**
     * 从 classpath:knowledge/*.md 重建知识库（仅管理端手动触发，不自动执行）。
     * 流程：解析种子文档 → 按标题 upsert kb_doc → 删除该文档旧块 →
     * 分块 → 批量向量化 → 写入 kb_chunk（向量 JSON 文本 + SHA-256 哈希）→ 重载内存索引。
     */
    public synchronized RebuildReport rebuildFromSeed() {
        long start = System.currentTimeMillis();
        List<SeedDoc> seeds = loadSeedDocs();
        if (seeds.isEmpty()) {
            throw new BusinessException("未找到种子知识文件（classpath:knowledge/*.md）");
        }

        int docCount = 0;
        int chunkCount = 0;
        int embedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (SeedDoc seed : seeds) {
            // 1. 按标题 upsert 文档：存在则更新元信息并 version+1，不存在则插入
            KbDoc doc = kbDocMapper.findByTitle(seed.title);
            if (doc != null) {
                doc.setCategory(seed.category);
                doc.setSource(seed.source);
                doc.setVersion(doc.getVersion() == null ? 2 : doc.getVersion() + 1);
                doc.setEnabled(1);
                doc.setUpdateTime(now);
                kbDocMapper.updateMeta(doc);
            } else {
                doc = new KbDoc();
                doc.setTitle(seed.title);
                doc.setCategory(seed.category);
                doc.setSource(seed.source);
                doc.setVersion(1);
                doc.setEnabled(1);
                doc.setCreateTime(now);
                doc.setUpdateTime(now);
                kbDocMapper.insert(doc);
            }

            // 2. 重建该文档知识块：先删后插（外键 ON DELETE CASCADE 不影响，手动删保证幂等）
            kbChunkMapper.deleteByDocId(doc.getDocId());
            List<String> chunks = splitIntoChunks(seed.body);
            if (chunks.isEmpty()) {
                continue;
            }

            // 3. 批量向量化（embedAll 内部按 10 条一批调用兼容接口）
            List<double[]> vectors = embeddingClient.embedAll(chunks);
            embedCount += vectors.size();

            // 4. 写入 kb_chunk：向量转 JSON 文本持久化
            for (int i = 0; i < chunks.size(); i++) {
                KbChunk chunk = new KbChunk();
                chunk.setDocId(doc.getDocId());
                chunk.setSeq(i);
                chunk.setContent(chunks.get(i));
                chunk.setEmbedding(toJson(vectors.get(i)));
                chunk.setContentHash(sha256(chunks.get(i)));
                chunk.setModel(embeddingClient.currentModel());
                chunk.setCreateTime(now);
                chunk.setUpdateTime(now);
                kbChunkMapper.insert(chunk);
            }

            docCount++;
            chunkCount += chunks.size();
        }

        // 5. 立即重载内存索引，重建完成即可被检索
        reload();

        return new RebuildReport(docCount, chunkCount, embedCount, System.currentTimeMillis() - start);
    }

    // =========================
    // 种子文档解析（resources/knowledge/*.md）
    // =========================

    /** 种子文档：YAML 风格 front matter（--- 包裹） + 正文 */
    private static final class SeedDoc {
        final String title;
        final String category;
        final String source;
        final String body;

        SeedDoc(String title, String category, String source, String body) {
            this.title = title;
            this.category = category;
            this.source = source;
            this.body = body;
        }
    }

    private List<SeedDoc> loadSeedDocs() {
        List<SeedDoc> seeds = new ArrayList<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources(SEED_DIR);
            for (Resource resource : resources) {
                String text = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                String fileName = resource.getFilename() == null ? "untitled" : resource.getFilename();
                seeds.add(parseSeedDoc(text, fileName));
            }
        } catch (IOException e) {
            throw new BusinessException("读取种子知识文件失败：" + e.getMessage());
        }
        return seeds;
    }

    /** 解析 front matter（title/category/source 三项，均可缺省：title 回退文件名） */
    private static SeedDoc parseSeedDoc(String text, String fileName) {
        String title = fileName.replaceAll("\\.md$", "");
        String category = "rule";
        String source = "平台运营规范";
        String body = text;

        String trimmed = text.stripLeading();
        if (trimmed.startsWith("---")) {
            int end = trimmed.indexOf("\n---", 3);
            if (end > 0) {
                String frontMatter = trimmed.substring(3, end);
                body = trimmed.substring(trimmed.indexOf('\n', end + 1) + 1);
                for (String line : frontMatter.split("\n")) {
                    int colon = line.indexOf(':');
                    if (colon <= 0) {
                        continue;
                    }
                    String key = line.substring(0, colon).trim();
                    String value = line.substring(colon + 1).trim();
                    switch (key) {
                        case "title" -> title = value;
                        case "category" -> category = value;
                        case "source" -> source = value;
                        default -> { }
                    }
                }
            }
        }
        return new SeedDoc(title, category, source, body);
    }

    // =========================
    // 分块 / 相似度 / 工具方法
    // =========================

    /**
     * 文本分块：按空行分段 → 合并至目标长度；单段超过硬上限时按行强切。
     * 确定性输出，同一文档重复重建得到相同块序列。
     */
    static List<String> splitIntoChunks(String body) {
        List<String> chunks = new ArrayList<>();
        if (body == null || body.isBlank()) {
            return chunks;
        }
        StringBuilder current = new StringBuilder();
        for (String rawPara : body.trim().split("\\n\\s*\\n+")) {
            String para = rawPara.trim();
            if (para.isEmpty()) {
                continue;
            }
            // 去掉 Markdown 标题符号但保留标题文字（标题本身承载语义）
            para = para.replaceAll("(?m)^#{1,6}\\s*", "");
            if (para.length() > CHUNK_MAX) {
                flush(current, chunks);
                hardSplit(para, chunks);
                continue;
            }
            if (current.length() > 0 && current.length() + para.length() + 1 > CHUNK_TARGET) {
                flush(current, chunks);
            }
            if (current.length() > 0) {
                current.append('\n');
            }
            current.append(para);
        }
        flush(current, chunks);
        return chunks;
    }

    private static void flush(StringBuilder current, List<String> chunks) {
        if (current.length() > 0) {
            chunks.add(current.toString());
            current.setLength(0);
        }
    }

    /** 超长段落按行累积强切，每块不超过 CHUNK_TARGET */
    private static void hardSplit(String para, List<String> chunks) {
        StringBuilder piece = new StringBuilder();
        for (String line : para.split("\n")) {
            if (piece.length() > 0 && piece.length() + line.length() + 1 > CHUNK_TARGET) {
                chunks.add(piece.toString());
                piece.setLength(0);
            }
            if (piece.length() > 0) {
                piece.append('\n');
            }
            piece.append(line.trim());
        }
        flush(piece, chunks);
    }

    /** 余弦相似度：预计算范数后只做点积（向量已归一存储原值，不重复开方） */
    private static double cosine(double[] queryVector, double queryNorm, IndexedChunk chunk) {
        double dot = 0;
        for (int i = 0; i < queryVector.length && i < chunk.vector.length; i++) {
            dot += queryVector[i] * chunk.vector[i];
        }
        if (chunk.norm == 0 || queryNorm == 0) {
            return 0;
        }
        return dot / (queryNorm * chunk.norm);
    }

    private static double norm(double[] vector) {
        double sum = 0;
        for (double v : vector) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }

    /** 向量 → JSON 数组文本（如 [0.12,-0.33,...]）持久化到 LONGTEXT */
    private String toJson(double[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            throw new BusinessException("向量序列化失败：" + e.getMessage());
        }
    }

    /** 片段正文 SHA-256 摘要（64 位十六进制），供后续增量重建比对 */
    static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new BusinessException("内容摘要计算失败：" + e.getMessage());
        }
    }
}
