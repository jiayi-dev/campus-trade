package com.jiayi.campustrade.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WantedResponse {

    // 响应ID
    private Integer responseId;

    // 关联求购ID
    private Integer wantedId;

    // 响应者用户ID
    private Integer userId;

    // 响应内容
    private String message;

    // 报价金额（null 表示面议）
    private BigDecimal price;

    // 报价状态：0 待选 / 1 已选定 / 2 未入选
    private Integer status;

    // 响应时间
    private LocalDateTime createTime;

    // 响应者昵称（非持久化字段：查询时 LEFT JOIN user 带出）
    private String username;

    // 求购标题（非持久化字段：查询时 LEFT JOIN wanted_post 带出）
    private String title;
}
