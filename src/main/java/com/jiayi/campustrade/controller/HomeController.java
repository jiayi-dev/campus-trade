package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.service.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/home")
public class HomeController {

    @Autowired
    private HomeService homeService;

    // 首页平台概览：公开接口（无需登录），供首页展示平台数据与动态校区列表
    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return homeService.overview();
    }
}
