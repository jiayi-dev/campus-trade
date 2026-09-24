package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    // 登录前产品首页（/landing）精选商品：在售 + 主图，按收藏/浏览热度排序
    // 公开只读数据，不含任何用户敏感信息
    List<Goods> findShowcase(@Param("limit") int limit);
}
