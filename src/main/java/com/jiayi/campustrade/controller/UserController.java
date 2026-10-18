package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.entity.User;
import com.jiayi.campustrade.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    // =========================
    // 个人中心（B1）
    //
    // 安全约定：
    // 1. 以下接口的 userId 一律取自 Token → AuthInterceptor → AuthContext 链路，
    //    前端不传 userId，后端也不读 —— 从链路上杜绝水平越权
    // 2. 业务校验失败直接抛 BusinessException，
    //    由 GlobalExceptionHandler 统一转为 400 + {code, message, data}
    // 3. /user/profile 与 /user/profile/overview 是精确路径，
    //    Spring MVC 优先匹配精确映射，不会被上方 /user/{id} 通配吞掉
    // =========================

    /**
     * 当前登录用户的个人档案（含最新卖家申请，不含密码）
     * GET /api/user/profile
     */
    @GetMapping("/profile")
    public Map<String, Object> profile() {
        return userService.getProfile();
    }

    /**
     * 修改个人资料（仅手机号 / 邮箱）
     * PUT /api/user/profile   body: { "phone": "...", "email": "..." }
     * 返回更新后的最新档案
     */
    @PutMapping("/profile")
    public Map<String, Object> updateProfile(@RequestBody Map<String, String> body) {

        return userService.updateProfile(
                body == null ? null : body.get("phone"),
                body == null ? null : body.get("email")
        );
    }

    /**
     * 修改密码（成功后该用户所有登录会话失效，需用新密码重新登录）
     * PUT /api/user/password
     * body: { "oldPassword": "...", "newPassword": "...", "confirmPassword": "..." }
     */
    @PutMapping("/password")
    public String changePassword(@RequestBody Map<String, String> body) {

        userService.changePassword(
                body == null ? null : body.get("oldPassword"),
                body == null ? null : body.get("newPassword"),
                body == null ? null : body.get("confirmPassword")
        );

        return "密码修改成功，请重新登录";
    }

    /**
     * 个人中心数据概览：商品 / 收藏 / 订单聚合统计
     * GET /api/user/profile/overview
     */
    @GetMapping("/profile/overview")
    public Map<String, Object> overview() {
        return userService.getOverview();
    }
}
