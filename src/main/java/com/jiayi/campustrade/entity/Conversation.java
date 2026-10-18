package com.jiayi.campustrade.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 对话会话（对应 conversation 表，V1.2 Day2.5）
 *
 * 一个登录用户可以有多个对话，每个对话包含多条消息（user + assistant 交替）。
 * 游客不落库，使用 localStorage 兜底。
 */
@Data
public class Conversation {

    // 对话ID
    private Integer id;

    // 所属用户ID
    private Integer userId;

    // 对话标题（取首条用户消息前 20 字）
    private String title;

    // 创建时间
    private LocalDateTime createTime;

    // 最近活跃时间
    private LocalDateTime updateTime;
}
