package com.jiayi.campustrade.service;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.entity.User;
import com.jiayi.campustrade.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    // BCrypt密码加密工具
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    // 根据用户ID查询
    public User findById(Integer id) {
        return userMapper.findById(id);
    }

    // 用户注册
    public void register(User user) {

        // ① 查询手机号是否已经注册
        User existUser = userMapper.findByPhone(user.getPhone());

        // ② 如果已经存在，则注册失败
        if (existUser != null) {
            throw new RuntimeException("手机号已经注册");
        }

        // ③ 对用户输入的密码进行加密
        String encodedPassword =
                passwordEncoder.encode(user.getPassword());

        user.setPassword(encodedPassword);

        // ④ 设置默认角色
        user.setRole("student");

        // ⑤ 设置账号状态：正常
        user.setStatus(1);

        // ⑥ 保存用户
        userMapper.insert(user);
    }

    // =========================
    // 管理员：用户分页查询
    // =========================

    public List<User> findPage(
            String username,
            String phone,
            String role,
            Integer status,
            Integer pageNum,
            Integer pageSize) {

        Integer offset = (pageNum - 1) * pageSize;

        return userMapper.findPage(username, phone, role, status, offset, pageSize);
    }

    public int count(
            String username,
            String phone,
            String role,
            Integer status) {

        return userMapper.count(username, phone, role, status);
    }

    // =========================
    // 启用/禁用用户
    //
    // 业务规则：
    // 1. 管理员不能禁用/启用自己（避免把自己锁死）
    // 2. 只允许 0（禁用）和 1（启用）
    // 3. 被禁用的用户在登录时会被 AuthService 拒绝
    // =========================

    public void updateStatus(Integer userId, Integer status) {

        if (status == null || (status != 0 && status != 1)) {
            throw new RuntimeException("不支持的用户状态");
        }

        Integer currentUserId = AuthContext.getUserId();

        if (currentUserId != null && currentUserId.equals(userId)) {
            throw new RuntimeException("不能禁用或启用自己的账号");
        }

        userMapper.updateStatus(userId, status);
    }
}