package com.jiayi.campustrade.service;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.auth.TokenManager;
import com.jiayi.campustrade.entity.User;
import com.jiayi.campustrade.exception.BusinessException;
import com.jiayi.campustrade.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
    // 严格匹配 admin 角色，防止 seller / student 通过管理员入口进入后台
    public Map<String, Object> adminLogin(String username, String password) {

        User user = userMapper.findByUsername(username);

        if (user == null
                || user.getStatus() == null
                || user.getStatus() != 1
                || !"admin".equals(user.getRole())
                || !passwordEncoder.matches(password, user.getPassword())) {
            // 认证失败统一映射为 HTTP 401（原来是裸 RuntimeException → 500，前端只能提示"请求失败"）
            // 不区分"账号不存在/密码错误/账号停用"，避免泄露用户名是否存在
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        return buildLoginResult(user);
    }

    // 学生登录
    // 学生 / 卖家共用同一登录入口：拒绝 admin 角色，但不强制区分 student / seller
    // 因为 student 审核通过后数据库 role 会变成 seller，强制匹配会让其无法登录
    // 真实身份以数据库 role 为准，登录成功后返回真实 role，由前端展示与控制权限
    public Map<String, Object> studentLogin(String username, String password) {
        return userLogin(username, password);
    }

    // 卖家登录
    // 与学生登录共用同一登录逻辑（兼容旧端点）
    public Map<String, Object> sellerLogin(String username, String password) {
        return userLogin(username, password);
    }

    // 通用用户登录：学生 / 卖家共用
    // 仅拒绝 admin（管理员只能走 adminLogin），其余只要账号正常 + 密码正确即放行
    private Map<String, Object> userLogin(String username, String password) {

        User user = userMapper.findByUsername(username);

        if (user == null
                || user.getStatus() == null
                || user.getStatus() != 1
                || "admin".equals(user.getRole())
                || !passwordEncoder.matches(password, user.getPassword())) {
            // 认证失败统一映射为 HTTP 401，前端据此提示"账号或密码错误"而非"请求失败"
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        return buildLoginResult(user);
    }

    // 当前登录用户信息（从登录上下文取 userId，再查库拿最新角色）
    // 用途：学生卖家申请被通过后，前端可据此感知真实 role，不依赖登录时缓存的旧角色
    public Map<String, Object> currentUser() {

        Integer userId = AuthContext.getUserId();

        User user = userMapper.findById(userId);

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("phone", user.getPhone());
        result.put("status", user.getStatus());

        return result;
    }

    // 组装登录成功返回结果
    // 注意：这里故意不返回 password
    private Map<String, Object> buildLoginResult(User user) {

        String token = tokenManager.createToken(user.getUserId());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getUserId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());

        return result;
    }
}