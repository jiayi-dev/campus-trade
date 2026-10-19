package com.jiayi.campustrade.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface FavoriteMapper {

    int findByUserIdAndGoodsId(
            @Param("userId") Integer userId,
            @Param("goodsId") Integer goodsId
    );

    int insert(
            @Param("userId") Integer userId,
            @Param("goodsId") Integer goodsId
    );

    int updateTime(
            @Param("userId") Integer userId,
            @Param("goodsId") Integer goodsId
    );

    // 取消收藏
    int delete(
            @Param("userId") Integer userId,
            @Param("goodsId") Integer goodsId
    );

    // 个人中心数据概览：统计我的收藏总数
    int countByUserId(@Param("userId") Integer userId);

    // 批量查询当前用户已收藏的商品ID（列表页一次请求回显收藏状态，避免 N+1）
    List<Integer> findFavoriteGoodsIds(
            @Param("userId") Integer userId,
            @Param("goodsIds") List<Integer> goodsIds
    );

    // 我的收藏商品（含商品展示字段与主图），按收藏时间倒序
    List<Map<String, Object>> findMyFavoriteGoods(@Param("userId") Integer userId);
}