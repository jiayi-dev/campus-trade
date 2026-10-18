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

    // 统计某商品关联的订单数：删除商品前校验、判断商品能否重新上架
    int countByGoodsId(@Param("goodsId") Integer goodsId);

    // 个人中心数据概览：统计我作为买家的订单数
    // orderStatus 为 null 时统计全部状态，否则按状态过滤（0 待交易 / 2 已完成）
    int countByBuyerId(
            @Param("buyerId") Integer buyerId,
            @Param("orderStatus") Integer orderStatus
    );

    // 卖家信誉画像：统计我作为卖家的订单数
    // orderStatus 为 null 时统计全部状态，否则按状态过滤（2 已完成用于信用分计算）
    int countBySellerId(
            @Param("sellerId") Integer sellerId,
            @Param("orderStatus") Integer orderStatus
    );

    // 统一信用分数据源：统计用户作为买家或卖家参与的已完成订单数（status=2）
    // 用于 UserService 与 EvaluationService 的 creditScore，避免两处数据源不一致
    int countCompletedByUserId(@Param("userId") Integer userId);

    // 个人中心交易轨迹：我作为买家的首单/末单时间（一条 SQL 同时取 MIN/MAX，无订单时 value 为 null）
    Map<String, Object> findOrderTimeRange(@Param("buyerId") Integer buyerId);

    // 管理员：全量订单查询（复用 orderDetailColumns JOIN 片段，可选状态筛选）
    List<Map<String, Object>> findAllForAdmin(@Param("orderStatus") Integer orderStatus);

}
