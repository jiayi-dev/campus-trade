package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.entity.Orders;
import com.jiayi.campustrade.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // V1.0 全量订单（保留；管理员后台批次会收紧为仅 admin 可访问）
    @GetMapping
    public List<Orders> findAll() {
        return orderService.findAll();
    }

    // 下单：POST /orders，请求体只接收 goodsId
    // 买家ID一律取自登录上下文，前端传 buyerId 也不会被使用
    @PostMapping
    public String create(@RequestBody Map<String, Object> body) {

        Integer buyerId = AuthContext.getUserId();

        Object goodsIdObj = body.get("goodsId");

        if (goodsIdObj == null) {
            return "缺少商品ID";
        }

        Integer goodsId;

        try {
            goodsId = Integer.valueOf(String.valueOf(goodsIdObj));
        } catch (NumberFormatException e) {
            return "商品ID格式不正确";
        }

        try {
            Integer orderId = orderService.createOrder(buyerId, goodsId);
            return "下单成功，订单ID：" + orderId;
        } catch (RuntimeException e) {
            // 业务失败（已下架/自购/被抢）以文本返回，与项目现有接口风格一致
            return e.getMessage();
        }
    }

    // 我的购买
    @GetMapping("/buyer")
    public List<Map<String, Object>> buyerOrders() {
        return orderService.findBuyerOrders(AuthContext.getUserId());
    }

    // 我的出售
    @GetMapping("/seller")
    public List<Map<String, Object>> sellerOrders() {
        return orderService.findSellerOrders(AuthContext.getUserId());
    }

    // 状态流转：PUT /orders/{id}/status，请求体 { orderStatus: 1 }
    // 目前只允许 待交易(0) → 已完成(1)，操作者必须是订单参与者
    @PutMapping("/{id}/status")
    public String updateStatus(
            @PathVariable("id") Integer orderId,
            @RequestBody Map<String, Object> body) {

        Integer userId = AuthContext.getUserId();

        Object statusObj = body.get("orderStatus");

        if (statusObj == null) {
            return "缺少订单状态";
        }

        Integer targetStatus;

        try {
            targetStatus = Integer.valueOf(String.valueOf(statusObj));
        } catch (NumberFormatException e) {
            return "订单状态格式不正确";
        }

        // 只开放“确认完成”这一条流转，拒绝任意状态跳转
        if (targetStatus != 1) {
            return "不支持的订单状态操作";
        }

        try {
            orderService.finishOrder(orderId, userId);
            return "订单已完成";
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }
}
