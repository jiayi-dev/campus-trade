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

    private LocalDateTime createTime;
}
