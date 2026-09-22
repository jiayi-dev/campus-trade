package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    // 当前登录用户信息（角色以数据库实时值为准）
    @GetMapping("/me")
    public Map<String, Object> me() {
        return authService.currentUser();
    }

    // 管理员登录
    @PostMapping("/admin/login")
    public Map<String, Object> adminLogin(
            @RequestBody Map<String, String> request) {

        String username = request.get("username");
        String password = request.get("password");

        return authService.adminLogin(username, password);
    }

    // 学生登录
    @PostMapping("/student/login")
    public Map<String, Object> studentLogin(
            @RequestBody Map<String, String> request) {

        String username = request.get("username");
        String password = request.get("password");

        return authService.studentLogin(username, password);
    }

    // 卖家登录
    @PostMapping("/seller/login")
    public Map<String, Object> sellerLogin(
            @RequestBody Map<String, String> request) {

        String username = request.get("username");
        String password = request.get("password");

        return authService.sellerLogin(username, password);
    }
}