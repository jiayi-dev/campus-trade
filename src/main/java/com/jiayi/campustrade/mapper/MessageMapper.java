package com.jiayi.campustrade.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface MessageMapper {

    // 查询某个商品的全部留言（JOIN user 带出留言人昵称）
    List<Map<String, Object>> findByGoodsId(@Param("goodsId") Integer goodsId);

    // 发布留言
    int insert(
            @Param("goodsId") Integer goodsId,
            @Param("userId") Integer userId,
            @Param("content") String content
    );
}
