package com.jiayi.campustrade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Evaluation {

    private Integer evaluationId;

    private Integer orderId;

    private Integer userId;

    private String content;

    private Integer score;

    private LocalDateTime createTime;
}
