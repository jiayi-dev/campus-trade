package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.entity.User;
import com.jiayi.campustrade.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    // 注入 UserService
    // Spring 启动时会创建 UserService 对象，
    // 然后把它交给这里的 userService 使用
    @Autowired
    private UserService userService;

    // 根据ID查询用户
    @GetMapping("/{id}")
    public User findById(@PathVariable Integer id) {
        return userService.findById(id);
    }

    // 用户注册
    @PostMapping("/register")
    public String register(@RequestBody User user) {

        try {
            userService.register(user);
            return "注册成功";
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }
}