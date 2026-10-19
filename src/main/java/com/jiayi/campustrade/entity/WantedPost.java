package com.jiayi.campustrade.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WantedPost {

    // 求购ID
    private Integer wantedId;

    // 发布者用户ID
    private Integer userId;

    // 求购标题
    private String title;

    // 期望分类ID
    private Integer categoryId;

    // 预算下限
    private BigDecimal budgetMin;

    // 预算上限
    private BigDecimal budgetMax;

    // 期望校区
    private String campus;

    // 需求描述
    private String description;

    // 状态：1=进行中 2=已关闭
    private Integer status;

    // 响应数（冗余字段：新增响应时按 wanted_response 真实行数刷新）
    private Integer responseCount;

    // 发布时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;

    // 发布者昵称（非持久化字段：查询时 LEFT JOIN user 带出，不参与 INSERT/UPDATE）
    private String username;
}
