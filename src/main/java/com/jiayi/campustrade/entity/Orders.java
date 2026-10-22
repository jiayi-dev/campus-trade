package com.jiayi.campustrade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Orders {

    private Integer orderId;

    private Integer goodsId;

    private Integer buyerId;

    private Integer sellerId;

    private Integer orderStatus;

    // 取消原因 / 取消人 / 取消时间（仅 order_status=3 时有值）
    private String cancelReason;

    private Integer cancelBy;

    private LocalDateTime cancelTime;

    private LocalDateTime createTime;
}
