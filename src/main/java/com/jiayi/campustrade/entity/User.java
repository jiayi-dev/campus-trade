package com.jiayi.campustrade.entity;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class User {

    private Integer userId;

    private String username;

    // 密码只能在注册/登录时写入（反序列化），绝不允许通过接口返回（序列化）
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String phone;

    private String email;

    private String role;

    private Integer status;

    private LocalDateTime createTime;

}