package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.TradeAppeal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TradeAppealMapper {

    // 新增申诉
    int insert(TradeAppeal appeal);

    // 单条申诉（处理前校验存在与状态）
    TradeAppeal findById(Integer appealId);

    // 我提交的申诉（含订单与商品信息）
    List<TradeAppeal> findByUserId(Integer userId);

    // 管理员：全部申诉，可按状态筛选
    List<TradeAppeal> findAll(@Param("status") Integer status);

    // 同一用户对同一订单是否已有待处理申诉（防止重复提交）
    int countPending(@Param("orderId") Integer orderId,
                     @Param("userId") Integer userId);

    // 管理员处理申诉
    int handle(@Param("appealId") Integer appealId,
               @Param("status") Integer status,
               @Param("handleBy") Integer handleBy,
               @Param("handleComment") String handleComment);
}
