package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.exception.BusinessException;
import com.jiayi.campustrade.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/evaluations")
public class EvaluationController {

    @Autowired
    private EvaluationService evaluationService;

    @PostMapping
    public String create(@RequestBody Map<String, Object> body) {

        Integer buyerId = AuthContext.getUserId();

        if (buyerId == null) {
            throw new BusinessException("请先登录后再评价");
        }

        Integer orderId = null;
        Integer rating = null;
        String content = null;

        if (body != null) {
            Object oidObj = body.get("orderId");
            if (oidObj != null) {
                try {
                    orderId = Integer.valueOf(String.valueOf(oidObj));
                } catch (NumberFormatException e) {
                    throw new BusinessException("订单ID格式不正确");
                }
            }

            Object ratingObj = body.get("rating");
            if (ratingObj != null) {
                try {
                    rating = Integer.valueOf(String.valueOf(ratingObj));
                } catch (NumberFormatException e) {
                    throw new BusinessException("评分格式不正确");
                }
            }

            Object contentObj = body.get("content");
            if (contentObj != null) {
                content = String.valueOf(contentObj);
            }
        }

        try {
            return evaluationService.create(orderId, rating, content);
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    @GetMapping("/order/{orderId}")
    public Map<String, Object> findByOrder(@PathVariable("orderId") Integer orderId) {
        return evaluationService.findByOrderId(orderId);
    }

    @GetMapping("/seller/{sellerId}")
    public Map<String, Object> findSellerProfile(@PathVariable("sellerId") Integer sellerId) {
        return evaluationService.findSellerProfile(sellerId);
    }

    @GetMapping("/goods/{goodsId}")
    public List<Map<String, Object>> findByGoods(@PathVariable("goodsId") Integer goodsId) {
        return evaluationService.findByGoodsId(goodsId);
    }

    @PostMapping("/status")
    public Map<Integer, Map<String, Object>> findStatus(@RequestBody Map<String, List<Integer>> body) {
        List<Integer> orderIds = body == null ? null : body.get("orderIds");
        return evaluationService.findStatusByOrderIds(orderIds);
    }
}