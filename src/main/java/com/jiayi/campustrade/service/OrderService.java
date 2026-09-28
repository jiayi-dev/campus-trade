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

    // 管理员：查询平台全部订单（可选状态筛选）
    public List<Map<String, Object>> findAllForAdmin(Integer orderStatus) {
        return orderMapper.findAllForAdmin(orderStatus);
    }

    /**
     * 订单状态流转（校园面交模型：交付 → 买家确认 → 完成）
     *
     * 状态定义：
     *   0 待交易      —— 买家已下单、商品已下架，等待卖家线下交付
     *   1 待买家确认  —— 卖家已交付，等待买家确认收货
     *   2 已完成      —— 买家确认收货，交易结束
     *
     * 唯一合法的两条流转：
     *   卖家：0 → 1（确认已交付）
     *   买家：1 → 2（确认收货）
     *
     * 其它一切跳转（0→2、买家 0→1、卖家 1→2、非参与者操作、重复流转等）
     * 全部在 Service 层拒绝，前端隐藏按钮只是体验层，不能替代这里的校验。
     *
     * @return 本次流转成功后的提示文案
     */
    @Transactional(rollbackFor = Exception.class)
    public String updateOrderStatus(Integer orderId, Integer userId, Integer targetStatus) {

        Orders order = orderMapper.findById(orderId);

        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        Integer currentStatus = order.getOrderStatus();

        if (currentStatus == null) {
            throw new RuntimeException("订单状态异常，请联系管理员");
        }

        boolean isBuyer = userId.equals(order.getBuyerId());
        boolean isSeller = userId.equals(order.getSellerId());

        // 流转一：卖家确认已交付  0 → 1
        if (currentStatus == 0 && targetStatus != null && targetStatus == 1) {
            if (!isSeller) {
                // 买家或无关用户不能替卖家确认交付
                throw new RuntimeException("只有卖家可以确认商品已交付");
            }
            orderMapper.updateStatus(orderId, 1);
            return "已确认交付，等待买家确认收货";
        }

        // 流转二：买家确认收货  1 → 2
        if (currentStatus == 1 && targetStatus != null && targetStatus == 2) {
            if (!isBuyer) {
                // 卖家或无关用户不能替买家确认收货
                throw new RuntimeException("只有买家可以确认收货");
            }
            orderMapper.updateStatus(orderId, 2);
            return "交易已完成";
        }

        // 其余所有组合（0→2、跨级、对已完成订单再操作、目标状态非法等）一律拒绝
        throw new RuntimeException("当前订单状态不允许该操作");
    }
}
