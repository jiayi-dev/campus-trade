package com.jiayi.campustrade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Favorite {

    private Integer favoriteId;

    private Integer userId;

    private Integer goodsId;

    private LocalDateTime createTime;
}
