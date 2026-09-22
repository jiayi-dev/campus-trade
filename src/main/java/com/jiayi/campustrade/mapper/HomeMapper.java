package com.jiayi.campustrade.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface HomeMapper {

    // 首页平台概览：在售商品数 / 已完成订单数 / 校园卖家数 / 累计收藏数 / 平均流转天数
    // 说明：orders 表无独立成交价字段，故不提供"累计流转金额"口径，
    //      避免用 goods.price 近似统计产生歧义
    Map<String, Object> overview();

    // 真实有在售商品的校区列表（首页动态生成校园专区，不前端写死）
    List<String> findOnSaleCampuses();
}
