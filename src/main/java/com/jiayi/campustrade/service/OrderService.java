package com.jiayi.campustrade.service;

import com.jiayi.campustrade.entity.Goods;
import com.jiayi.campustrade.entity.Orders;
import com.jiayi.campustrade.mapper.GoodsMapper;
import com.jiayi.campustrade.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    // V1.0 全量查询保留
    public List<Orders> findAll() {
        return orderMapper.findAll();
    }

    /**
     * 下单（核心交易逻辑）
     *
     * buyerId 来自登录上下文，sellerId 来自商品本身，前端均不可指定。
     * 整个方法在同一个事务中：商品下架 + 创建订单要么全部成功，要么全部回滚，
     * 不会出现“订单建了但商品还在售”的脏数据。
     *
     * @return 新订单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Integer createOrder(Integer buyerId, Integer goodsId) {

        // 1. 商品必须存在
        Goods goods = goodsMapper.findById(goodsId);

        if (goods == null) {
            throw new RuntimeException("商品不存在或已被删除");
        }

        // 2. 商品必须在售（前端按钮禁用不可信，这里是真正的业务闸门）
        if (goods.getStatus() == null || goods.getStatus() != 1) {
            throw new RuntimeException("商品已下架或已被购买");
        }

        // 3. 不能购买自己发布的商品
        if (buyerId.equals(goods.getUserId())) {
            throw new RuntimeException("不能购买自己发布的商品");
        }

        // 4. 并发抢占：条件更新只在 status=1 时生效
        //    两人同时点购买，数据库行锁保证只有一个 UPDATE 影响 1 行，
        //    另一个返回 0 行，即被告知“已被买走”，不会产生重复订单
        int rows = goodsMapper.updateStatusIfOnSale(goodsId, 0);

        if (rows == 0) {
            throw new RuntimeException("手慢了，商品刚刚被其他人买走");
        }

        // 5. 抢占成功，创建待交易订单（卖家取商品发布人，不取前端参数）
        Orders order = new Orders();
        order.setGoodsId(goodsId);
        order.setBuyerId(buyerId);
        order.setSellerId(goods.getUserId());
        order.setOrderStatus(0);

        orderMapper.insert(order);

        // 插入失败会抛异常，@Transactional 使第 4 步的商品下架一并回滚
        return order.getOrderId();
    }

    // 当前用户买到的订单
    public List<Map<String, Object>> findBuyerOrders(Integer userId) {
        return orderMapper.findByBuyerId(userId);
    }

    // 当前用户卖出的订单
    public List<Map<String, Object>> findSellerOrders(Integer userId) {
        return orderMapper.findBySellerId(userId);
    }

    /**
     * 确认订单完成：待交易(0) → 已完成(1)
     *
     * 只有订单的买家或卖家本人可以操作，且状态必须是待交易，
     * 不允许从任意状态跳到任意状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public void finishOrder(Integer orderId, Integer userId) {

        Orders order = orderMapper.findById(orderId);

        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        boolean isBuyer = userId.equals(order.getBuyerId());
        boolean isSeller = userId.equals(order.getSellerId());

        if (!isBuyer && !isSeller) {
            throw new RuntimeException("只能操作与自己相关的订单");
        }

        if (order.getOrderStatus() == null || order.getOrderStatus() != 0) {
            throw new RuntimeException("当前订单状态不允许该操作");
        }

        orderMapper.updateStatus(orderId, 1);
    }
}
