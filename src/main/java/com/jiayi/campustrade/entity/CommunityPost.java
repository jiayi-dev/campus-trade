package com.jiayi.campustrade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommunityPost {

    // 动态ID
    private Integer postId;

    // 发布者用户ID
    private Integer userId;

    // 动态标题
    private String title;

    // 主题标签：闲置分享/使用体验/交易经验/环保循环
    private String topic;

    // 正文内容
    private String content;

    // 配图URL（可选，允许为空）
    private String imageUrl;

    // 关联商品ID（可选，可为 NULL）
    private Integer goodsId;

    // 点赞数（冗余字段：按 community_like 真实行数刷新）
    private Integer likeCount;

    // 评论数（冗余字段：按 community_comment 真实行数刷新）
    private Integer commentCount;

    // 浏览数（详情访问时原子 +1）
    private Integer viewCount;

    // 发布时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;

    // ===== 以下为非持久化字段：查询时 LEFT JOIN 带出，不参与 INSERT/UPDATE =====

    // 作者昵称（LEFT JOIN user）
    private String authorName;

    // 关联商品名称（LEFT JOIN goods，可为空）
    private String goodsName;

    // 当前登录用户是否已点赞（LEFT JOIN community_like 判定）
    private Boolean liked;
}
