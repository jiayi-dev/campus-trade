package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    // V1.0 全量查询（保留，供后续管理员后台使用）
    List<Orders> findAll();

    // 创建订单，自增主键回填到 orderId
    int insert(Orders order);

    // 根据ID查询订单（状态流转前的归属与状态校验）
    Orders findById(Integer orderId);

    // 买家视角：当前用户作为 buyerId 参与的订单（JOIN 商品与买卖家信息）
    List<Map<String, Object>> findByBuyerId(@Param("buyerId") Integer buyerId);

    // 卖家视角：当前用户作为 sellerId 参与的订单
    List<Map<String, Object>> findBySellerId(@Param("sellerId") Integer sellerId);

    // 订单状态流转
    int updateStatus(
            @Param("orderId") Integer orderId,
            @Param("orderStatus") Integer orderStatus
    );

}
