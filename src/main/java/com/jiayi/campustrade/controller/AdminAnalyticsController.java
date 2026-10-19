package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.service.AdminAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 平台运营数据中心接口
 *
 * 路径挂在 /admin/analytics/** 下，由 AuthInterceptor 统一校验：
 * 请求前缀为 /api/admin/ 且用户 role != admin 时直接返回 403。
 * 因此普通学生 / 卖家无法通过前端路由或直接调用接口获取管理员数据。
 *
 * 统计口径全部来自真实数据库表，无任何硬编码或模拟数据。
 */
@RestController
@RequestMapping("/admin/analytics")
public class AdminAnalyticsController {

    @Autowired
    private AdminAnalyticsService adminAnalyticsService;

    // 平台运营总览：总量 + 分布 + 趋势 + 近期活动 + 异常提示
    // days 为趋势天数，收敛到 [7, 90]，默认 14
    @GetMapping("/overview")
    public Map<String, Object> overview(
            @RequestParam(required = false, defaultValue = "14") Integer days) {

        return adminAnalyticsService.overview(days);
    }
}
