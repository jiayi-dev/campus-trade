package com.jiayi.campustrade.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户间交易私聊消息（对应 chat_message 表）
 *
 * 与 Message（商品留言板）无关：
 * - Message 只有 goods_id / user_id / content，是公开留言；
 * - 本实体是「发送者 ↔ 接收者」的私聊消息，带接收人、已读状态与会话键。
 */
@Data
public class ChatMessage {

    // 消息主键（自增）
    private Integer chatId;

    // 发送者用户ID
    private Integer fromUserId;

    // 接收者用户ID
    private Integer toUserId;

    // 关联商品ID（可空：不针对具体商品的沟通）
    private Integer goodsId;

    // 消息正文
    private String content;

    // 接收者是否已读：0 未读 / 1 已读
    private Integer isRead;

    // 发送时间
    private LocalDateTime createTime;

    // 非持久化字段：会话键 min-max-gid，仅用于 Service 层传参与查询，不入库
    private String sessionKey;
}
