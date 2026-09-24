package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.entity.Goods;
import com.jiayi.campustrade.service.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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

    // 登录前产品首页（/landing）精选商品：公开只读，在售商品按热度取前 N 件
    // limit 收敛到 [1, 12]，防止被当成分页接口拉全量商品
    @GetMapping("/showcase")
    public List<Goods> showcase(@RequestParam(defaultValue = "8") Integer limit) {
        int safeLimit = Math.min(Math.max(limit == null ? 8 : limit, 1), 12);
        return homeService.showcase(safeLimit);
    }
}
