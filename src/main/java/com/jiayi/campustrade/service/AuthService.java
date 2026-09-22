package com.jiayi.campustrade.service;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.auth.TokenManager;
import com.jiayi.campustrade.entity.User;
import com.jiayi.campustrade.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TokenManager tokenManager;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    // 管理员登录
    public Map<String, Object> adminLogin(String username, String password) {
        return login(username, password, "admin");
    }

    // 学生登录
    public Map<String, Object> studentLogin(String username, String password) {
        return login(username, password, "student");
    }

    // 卖家登录
    public Map<String, Object> sellerLogin(String username, String password) {
        return login(username, password, "seller");
    }

    // 当前登录用户信息（从登录上下文取 userId，再查库拿最新角色）
    // 用途：学生卖家申请被通过后，前端可据此感知真实 role，不依赖登录时缓存的旧角色
    public Map<String, Object> currentUser() {

        Integer userId = AuthContext.getUserId();

        User user = userMapper.findById(userId);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("phone", user.getPhone());
        result.put("status", user.getStatus());

        return result;
    }

    // 真正执行登录业务的核心方法
    private Map<String, Object> login(
            String username,
            String password,
            String expectedRole) {

        // 1. 根据用户名查询用户
        User user = userMapper.findByUsername(username);

        // 2. 用户不存在、账号被禁用、角色不匹配、密码错误
        //    都统一提示，避免暴露账号信息
        if (user == null
                || user.getStatus() == null
                || user.getStatus() != 1
                || !expectedRole.equals(user.getRole())
                || !passwordEncoder.matches(password, user.getPassword())) {

            throw new RuntimeException("账号或密码错误");
        }

        // 3. 登录成功，创建 Token
        String token = tokenManager.createToken(user.getUserId());

        // 4. 手工组装返回结果
        //    注意：这里故意不返回 password
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getUserId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());

        return result;
    }
}