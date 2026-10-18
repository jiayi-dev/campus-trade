package com.jiayi.campustrade.mapper;


import com.jiayi.campustrade.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface UserMapper {

    // 根据手机号查询用户
    User findByPhone(String phone);

    // 保存新用户
    int insert(User user);

    // 根据用户ID查询用户
    User findById(Integer id);

    // 根据用户名查询用户
    User findByUsername(String username);

    // 管理员：用户分页查询（用户名/手机号模糊、角色、状态筛选）
    List<User> findPage(
            @Param("username") String username,
            @Param("phone") String phone,
            @Param("role") String role,
            @Param("status") Integer status,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    // 管理员：用户总数（与 findPage 同条件）
    int count(
            @Param("username") String username,
            @Param("phone") String phone,
            @Param("role") String role,
            @Param("status") Integer status
    );

    // 启用/禁用用户：仅更新 status 字段
    int updateStatus(
            @Param("userId") Integer userId,
            @Param("status") Integer status
    );

    // =========================
    // 个人中心（B1）：userId 一律来自 AuthContext，不接受前端传入
    // =========================

    // 更新个人资料：仅允许 phone / email 两个字段，其他字段不可能通过本语句变更
    int updateProfile(
            @Param("userId") Integer userId,
            @Param("phone") String phone,
            @Param("email") String email
    );

    // 修改密码：仅更新 password 字段（Service 层已用 BCrypt 加密）
    int updatePassword(
            @Param("userId") Integer userId,
            @Param("password") String password
    );

    // 卖家申请审核通过：仅更新 role 字段（student → seller）
    int updateRole(
            @Param("userId") Integer userId,
            @Param("role") String role
    );

}