package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.Evaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface EvaluationMapper {

    int insert(Evaluation evaluation);

    Map<String, Object> findByOrderId(@Param("orderId") Integer orderId);

    List<Map<String, Object>> findStatusByOrderIds(@Param("orderIds") List<Integer> orderIds);

    Map<String, Object> findSummaryBySellerId(@Param("sellerId") Integer sellerId);

    List<Map<String, Object>> findRecentBySellerId(
            @Param("sellerId") Integer sellerId,
            @Param("limit") Integer limit
    );

    List<Map<String, Object>> findByGoodsId(@Param("goodsId") Integer goodsId);
}