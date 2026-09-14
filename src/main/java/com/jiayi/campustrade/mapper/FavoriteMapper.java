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
}