package com.jiayi.campustrade.entity;


import lombok.Data;

import java.time.LocalDateTime;


@Data
public class User {

    private Integer userId;

    private String username;

    private String password;

    private String phone;

    private String email;

    private String role;

    private Integer status;

    private LocalDateTime createTime;

}