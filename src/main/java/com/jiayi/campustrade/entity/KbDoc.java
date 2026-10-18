package com.jiayi.campustrade.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识库文档（对应 kb_doc 表，B12 RAG）
 *
 * 一篇文档 = 一个平台知识主题（FAQ / 交易规则 / 流程 / 禁售等），
 * 由 KnowledgeService 切分为多个 kb_chunk 并向量化。
 */
@Data
public class KbDoc {

    // 文档ID
    private Integer docId;

    // 文档标题（唯一，重建索引按标题 upsert）
    private String title;

    // 文档分类：faq / trading / rule / forbidden
    private String category;

    // 内容来源说明
    private String source;

    // 内容版本号，重建索引更新内容时 +1
    private Integer version;

    // 是否参与检索：1=启用 0=停用
    private Integer enabled;

    // 创建时间
    private LocalDateTime createTime;

    // 最近更新时间
    private LocalDateTime updateTime;
}
