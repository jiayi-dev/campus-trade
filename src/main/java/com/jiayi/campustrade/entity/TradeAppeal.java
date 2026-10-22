package com.jiayi.campustrade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TradeAppeal {

    private Integer appealId;

    private Integer orderId;

    // 申诉人（买家或卖家）
    private Integer userId;

    // 申诉类型
    private String reason;

    // 申诉说明
    private String detail;

    // 0 待处理 / 1 已受理 / 2 已驳回
    private Integer status;

    private Integer handleBy;

    private String handleComment;

    private LocalDateTime createTime;

    private LocalDateTime handleTime;

    // ===== 非持久化字段：查询时联表带出 =====
    private String username;

    private String goodsName;

    private Integer orderStatus;

    private String buyerName;

    private String sellerName;

    private String handlerName;
}
