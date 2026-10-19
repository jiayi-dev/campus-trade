package com.jiayi.campustrade.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 平台运营数据统计 Mapper（仅管理员使用）
 *
 * 全部为纯统计查询，数据来源为已存在的真实业务表：
 * user / goods / orders / category / favorite / evaluation。
 * 不依赖 / 不联查 V1.2 新增的可选表（wanted_post、community_* 等），
 * 避免未执行建表脚本时统计接口报错。
 */
@Mapper
public interface AdminAnalyticsMapper {

    // 平台总量指标（用户/商品/订单/收藏/评价/分类等，单行多列）
    Map<String, Object> countTotals();

    // 商品分类分布（LEFT JOIN category，按商品数倒序）
    List<Map<String, Object>> categoryDistribution();

    // 订单状态分布（orders.order_status：0待交易 1待买家确认 2已完成）
    List<Map<String, Object>> orderStatusDistribution();

    // 近 N 天新增商品（按日期分组，可能缺日期，由 Service 补齐为连续序列）
    List<Map<String, Object>> goodsTrend(@Param("days") Integer days);

    // 近 N 天新增订单
    List<Map<String, Object>> orderTrend(@Param("days") Integer days);

    // 近 N 天新增用户
    List<Map<String, Object>> userTrend(@Param("days") Integer days);

    // 在售商品的校区分布
    List<Map<String, Object>> campusDistribution();

    // 最近 N 条订单（JOIN 商品名与买卖双方昵称）
    List<Map<String, Object>> recentOrders(@Param("limit") Integer limit);

    // 最近 N 条发布商品（JOIN 卖家昵称）
    List<Map<String, Object>> recentGoods(@Param("limit") Integer limit);

    // 异常数据：价格为 0 或负数的商品数（数据质量问题提示）
    int countZeroPriceGoods();

    // 异常数据：被禁用（status=0）的用户数
    int countDisabledUsers();

    // 异常数据：长期下架（status=0）的商品数
    int countOffSaleGoods();
}
