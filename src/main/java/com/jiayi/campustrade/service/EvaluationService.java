package com.jiayi.campustrade.service;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.entity.Evaluation;
import com.jiayi.campustrade.entity.Orders;
import com.jiayi.campustrade.exception.BusinessException;
import com.jiayi.campustrade.mapper.EvaluationMapper;
import com.jiayi.campustrade.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EvaluationService {

    @Autowired
    private EvaluationMapper evaluationMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Transactional(rollbackFor = Exception.class)
    public String create(Integer orderId, Integer rating, String content) {

        Integer buyerId = AuthContext.getUserId();

        if (buyerId == null) {
            throw new BusinessException("请先登录后再评价");
        }

        if (orderId == null) {
            throw new BusinessException("订单ID不能为空");
        }

        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException("评分必须在 1～5 分之间");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("评价内容不能为空");
        }

        String trimmed = content.trim();
        if (trimmed.length() < 5) {
            throw new BusinessException("评价内容至少 5 个字");
        }
        if (trimmed.length() > 300) {
            throw new BusinessException("评价内容不能超过 300 字");
        }

        Orders order = orderMapper.findById(orderId);

        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (!buyerId.equals(order.getBuyerId())) {
            throw new BusinessException("无权评价该订单");
        }

        if (order.getOrderStatus() == null || order.getOrderStatus() != 2) {
            throw new BusinessException("只有已完成的订单才能评价");
        }

        Map<String, Object> exist = evaluationMapper.findByOrderId(orderId);
        if (exist != null) {
            throw new BusinessException("该订单已经评价过");
        }

        Evaluation evaluation = new Evaluation();
        evaluation.setOrderId(orderId);
        evaluation.setUserId(buyerId);
        evaluation.setContent(trimmed);
        evaluation.setScore(rating);

        try {
            evaluationMapper.insert(evaluation);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("该订单已经评价过");
        }

        return "评价提交成功";
    }

    public Map<String, Object> findByOrderId(Integer orderId) {
        if (orderId == null) {
            return null;
        }
        return evaluationMapper.findByOrderId(orderId);
    }

    public Map<Integer, Map<String, Object>> findStatusByOrderIds(List<Integer> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Map<String, Object>> list = evaluationMapper.findStatusByOrderIds(orderIds);

        Map<Integer, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> row : list) {
            Object oidObj = row.get("orderId");
            if (oidObj != null) {
                Integer oid = ((Number) oidObj).intValue();
                result.put(oid, row);
            }
        }
        return result;
    }

    public Map<String, Object> findSellerProfile(Integer sellerId) {
        Map<String, Object> summary = evaluationMapper.findSummaryBySellerId(sellerId);
        if (summary == null) {
            summary = new LinkedHashMap<>();
        }

        // 信用分数据源：用户作为买家或卖家参与的已完成订单总数（与 UserService 统一）
        int completedCount = orderMapper.countCompletedByUserId(sellerId);

        int creditScore = 100 + completedCount * 20;
        String creditLevel = creditLevelOf(completedCount);

        List<Map<String, Object>> recentList = evaluationMapper.findRecentBySellerId(sellerId, 5);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sellerId", sellerId);
        result.put("totalCount", summary.getOrDefault("totalCount", 0));
        result.put("avgScore", summary.getOrDefault("avgScore", 0));
        result.put("star5", summary.getOrDefault("star5", 0));
        result.put("star4", summary.getOrDefault("star4", 0));
        result.put("star3", summary.getOrDefault("star3", 0));
        result.put("star2", summary.getOrDefault("star2", 0));
        result.put("star1", summary.getOrDefault("star1", 0));
        result.put("completedCount", completedCount);
        result.put("creditScore", creditScore);
        result.put("creditLevel", creditLevel);
        result.put("recentList", recentList);

        return result;
    }

    public List<Map<String, Object>> findByGoodsId(Integer goodsId) {
        if (goodsId == null) {
            return Collections.emptyList();
        }
        return evaluationMapper.findByGoodsId(goodsId);
    }

    private String creditLevelOf(int completedCount) {
        if (completedCount <= 0) return "校园新生";
        if (completedCount <= 2) return "信用良好";
        if (completedCount <= 5) return "交易达人";
        return "信誉之星";
    }
}