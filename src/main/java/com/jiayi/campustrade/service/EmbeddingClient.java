package com.jiayi.campustrade.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiayi.campustrade.config.AiProperties;
import com.jiayi.campustrade.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-Compatible Embedding 客户端（B12 RAG）
 *
 * 仿照 {@link LlmClient} 的 JDK HttpClient 实现，调用
 * POST {llm.base-url}/embeddings（阿里云百炼 DashScope 兼容模式），
 * 与对话模型共用同一个 base-url 与 api-key，不新增供应商/账号。
 * 默认模型 text-embedding-v3（1024 维，中文优化），由 llm.embedding-model 配置。
 *
 * 不修改 LlmClient 的任何 B9 Tool Calling 逻辑。
 */
@Component
public class EmbeddingClient {

    /** 兼容模式 /embeddings 单次批量上限（百炼 text-embedding-v3 批量上限 10 条文本） */
    private static final int BATCH_SIZE = 10;

    private final AiProperties props;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public EmbeddingClient(AiProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getEmbeddingTimeout()))
                .build();
    }

    /** 当前配置的 Embedding 模型名（写入 kb_chunk.model，便于追溯向量来源） */
    public String currentModel() {
        return props.getEmbeddingModel();
    }

    /**
     * 单文本向量化。
     *
     * @return 1024 维（由模型决定）浮点向量
     */
    public double[] embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException("Embedding 输入文本不能为空");
        }
        List<double[]> result = embedAll(List.of(text));
        return result.get(0);
    }

    /**
     * 批量向量化：内部按 BATCH_SIZE 分批调用，输入顺序与输出顺序一一对应。
     *
     * @return 与入参等长的向量列表
     */
    public List<double[]> embedAll(List<String> texts) {
        if (!props.isConfigured()) {
            throw new BusinessException("AI 模型未配置 API Key，无法调用 Embedding 接口");
        }
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        List<double[]> all = new ArrayList<>(texts.size());
        for (int from = 0; from < texts.size(); from += BATCH_SIZE) {
            List<String> batch = texts.subList(from, Math.min(from + BATCH_SIZE, texts.size()));
            all.addAll(callEmbeddings(batch));
        }
        return all;
    }

    /** 调用 /embeddings 并解析一批向量（按返回的 index 排序，保证与输入顺序一致） */
    private List<double[]> callEmbeddings(List<String> batch) {
        String url = props.getBaseUrl().replaceAll("/+$", "") + "/embeddings";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.getEmbeddingModel());
        body.put("input", batch);

        HttpResponse<String> response;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(props.getEmbeddingTimeout()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException e) {
            throw new BusinessException("Embedding 接口请求超时（" + props.getEmbeddingTimeout() + "ms）");
        } catch (Exception e) {
            throw new BusinessException("Embedding 接口网络异常：" + e.getMessage());
        }

        // HTTP 非 2xx：明确抛出可识别业务异常（后续接入 AiService 时据此降级）
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String detail = response.body() == null ? "" : response.body();
            if (detail.length() > 300) {
                detail = detail.substring(0, 300) + "...";
            }
            throw new BusinessException("Embedding 接口调用失败 HTTP "
                    + response.statusCode() + "：" + detail);
        }

        // 解析 OpenAI 兼容格式：{ data: [ { index, embedding: [...] } ], model, usage }
        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) {
                throw new BusinessException("Embedding 接口返回缺少 data 数组");
            }
            // 按 index 归位，防御个别服务乱序返回
            double[][] ordered = new double[batch.size()][];
            for (JsonNode item : data) {
                int index = item.path("index").asInt(-1);
                JsonNode vec = item.path("embedding");
                if (index < 0 || index >= batch.size() || !vec.isArray() || vec.isEmpty()) {
                    throw new BusinessException("Embedding 接口返回格式异常（index/embedding 缺失）");
                }
                double[] v = new double[vec.size()];
                for (int i = 0; i < vec.size(); i++) {
                    v[i] = vec.get(i).asDouble();
                }
                ordered[index] = v;
            }
            List<double[]> result = new ArrayList<>(batch.size());
            for (double[] v : ordered) {
                if (v == null) {
                    throw new BusinessException("Embedding 接口返回缺少部分向量");
                }
                result.add(v);
            }
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Embedding 响应解析失败：" + e.getMessage());
        }
    }
}
