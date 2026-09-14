package com.jiayi.campustrade.service;

import com.jiayi.campustrade.entity.User;
import com.jiayi.campustrade.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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
}