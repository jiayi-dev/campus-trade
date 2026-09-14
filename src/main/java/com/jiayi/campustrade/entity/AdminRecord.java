package com.jiayi.campustrade.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminRecord {

    private Integer recordId;

    private Integer adminId;

    private String operation;

    private LocalDateTime createTime;
}
