package com.jiayi.campustrade.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 对话消息（对应 conversation_message 表，V1.2 Day2.5）
 *
 * 一条消息 = user 发送的问题 或 assistant 返回的回答。
 * role=user / assistant，content 为纯文本（不含 SSE 元数据）。
 */
@Data
public class ConversationMessage {

    // 消息ID
    private Integer id;

    // 所属对话ID
    private Integer conversationId;

    // 消息角色：user / assistant
    private String role;

    // 消息正文
    private String content;

    // 结构化附加数据（assistant 消息的 goods 商品卡片 / sources 知识来源），JSON 格式
    private String extraJson;

    // 发送时间
    private LocalDateTime createTime;
}
