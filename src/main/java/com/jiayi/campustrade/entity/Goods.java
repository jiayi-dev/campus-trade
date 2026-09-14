package com.jiayi.campustrade.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Goods {

    // 商品ID
    private Integer goodsId;

    // 发布用户ID
    private Integer userId;

    // 商品分类ID
    private Integer categoryId;

    // 商品名称
    private String goodsName;

    // 商品描述
    private String description;

    // 商品价格
    private BigDecimal price;

    // 商品状态
    private Integer status;

    // 发布时间
    private LocalDateTime createTime;
}