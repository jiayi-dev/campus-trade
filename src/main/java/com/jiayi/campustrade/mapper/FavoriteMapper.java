package com.jiayi.campustrade.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

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

    // 批量查询当前用户已收藏的商品ID（列表页一次请求回显收藏状态，避免 N+1）
    List<Integer> findFavoriteGoodsIds(
            @Param("userId") Integer userId,
            @Param("goodsIds") List<Integer> goodsIds
    );
}