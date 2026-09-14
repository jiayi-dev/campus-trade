package com.jiayi.campustrade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Message {

    private Integer messageId;

    private Integer goodsId;

    private Integer userId;

    private String content;

    private LocalDateTime createTime;
}
