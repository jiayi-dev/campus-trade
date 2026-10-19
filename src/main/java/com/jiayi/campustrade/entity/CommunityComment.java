package com.jiayi.campustrade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommunityComment {

    // 评论ID
    private Integer commentId;

    // 关联动态ID
    private Integer postId;

    // 评论者用户ID
    private Integer userId;

    // 评论内容
    private String content;

    // 评论时间
    private LocalDateTime createTime;

    // 评论者昵称（非持久化字段：查询时 LEFT JOIN user 带出）
    private String authorName;
}
