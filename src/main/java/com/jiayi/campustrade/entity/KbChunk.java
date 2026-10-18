package com.jiayi.campustrade.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识库分块（对应 kb_chunk 表，B12 RAG）
 *
 * 向量以 1024 维浮点 JSON 数组文本存于 embedding（LONGTEXT），
 * MySQL 8.0 无原生向量类型，检索时由 KnowledgeService 加载进内存做余弦计算。
 */
@Data
public class KbChunk {

    // 分块ID
    private Integer chunkId;

    // 所属文档ID
    private Integer docId;

    // 片段在文档内的顺序号，从 0 开始
    private Integer seq;

    // 片段正文（Embedding 输入与提示词注入内容）
    private String content;

    // Embedding 向量 JSON 文本；NULL=尚未向量化（不参与检索）
    private String embedding;

    // 片段正文 SHA-256 摘要（后续增量重建用）
    private String contentHash;

    // 生成该向量的 Embedding 模型名
    private String model;

    // 创建时间
    private LocalDateTime createTime;

    // 最近更新时间
    private LocalDateTime updateTime;

    // ---------- 查询辅助字段（无对应列，来自 kb_doc 联表） ----------

    // 所属文档标题（findEnabled 联表查询返回，注入提示词时用于溯源）
    private String docTitle;
}
