package com.jiayi.campustrade.service;

import com.jiayi.campustrade.mapper.HomeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HomeService {

    @Autowired
    private HomeMapper homeMapper;

    /**
     * 首页平台概览
     *
     * 返回字段：
     *   onSaleCount        在售商品数
     *   totalTradeCount    已完成交易数（orders.order_status=1）
     *   sellerCount        校园卖家数（role=seller）
     *   totalFavoriteCount 累计收藏数
     *   avgTradeDays       平均流转天数（商品发布→订单创建，仅统计已完成订单）
     *   campuses           真实有在售商品的校区列表（动态生成校园专区）
     *
     * 口径说明：orders 表未独立保存成交价，因此不提供"累计流转金额"，
     *          改用更可靠的"已完成交易数"等指标，避免用 goods.price 近似产生歧义。
     */
    public Map<String, Object> overview() {

        Map<String, Object> stats = homeMapper.overview();
        List<String> campuses = homeMapper.findOnSaleCampuses();

        Map<String, Object> result = new HashMap<>();

        // 子查询 COUNT 总会返回一行，防御性处理 null
        if (stats == null) {
            stats = new HashMap<>();
        }

        result.put("onSaleCount", stats.get("onSaleCount"));
        result.put("totalTradeCount", stats.get("totalTradeCount"));
        result.put("sellerCount", stats.get("sellerCount"));
        result.put("totalFavoriteCount", stats.get("totalFavoriteCount"));
        result.put("avgTradeDays", stats.get("avgTradeDays"));
        result.put("campuses", campuses);

        return result;
    }
}
