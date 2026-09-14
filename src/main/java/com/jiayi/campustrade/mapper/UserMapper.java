package com.jiayi.campustrade.mapper;


import com.jiayi.campustrade.entity.User;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface UserMapper {

    // 根据手机号查询用户
    // 给 Service 用来判断：这个手机号是否已经注册
    User findByPhone(String phone);

    // 保存新用户
    // 返回受影响的行数，正常插入成功一般为 1
    int insert(User user);

    // 根据用户ID查询用户
    User findById(Integer id);

    // 根据用户名查询用户
// 登录时使用，用于获取用户的账号、密码、角色、状态等信息
    User findByUsername(String username);
}