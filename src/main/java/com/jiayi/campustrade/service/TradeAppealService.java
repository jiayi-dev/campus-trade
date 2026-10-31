package com.jiayi.campustrade.service;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.entity.Orders;
import com.jiayi.campustrade.entity.TradeAppeal;
import com.jiayi.campustrade.entity.User;
import com.jiayi.campustrade.exception.BusinessException;
import com.jiayi.campustrade.mapper.OrderMapper;
import com.jiayi.campustrade.mapper.TradeAppealMapper;
import com.jiayi.campustrade.mapper.UserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TradeAppealService {

    @Autowired
    private TradeAppealMapper tradeAppealMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    private final ObjectMapper om = new ObjectMapper();

    // 证据数量上限（业务约束）
    private static final int MAX_EVIDENCES = 5;


    /**
     * 提交交易申诉：只有该订单的买家或卖家可以申诉
     *
     * evidences：证据图片 URL 数组（可空）。规则：
     *   1) 最多 5 张；
     *   2) 每张 URL 必须以 /api/files/（本地上传）或 http(s):// 开头；
     *   3) 不允许重复；
     *   4) 转 JSON 存库，查询时前端再 parse。
     */
    @Transactional(rollbackFor = Exception.class)
    public Integer create(Integer orderId, String reason, String detail, List<String> evidences) {

        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new BusinessException("当前用户未登录");
        }

        Orders order = orderMapper.findById(orderId);

        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        boolean isBuyer = userId.equals(order.getBuyerId());
        boolean isSeller = userId.equals(order.getSellerId());

        if (!isBuyer && !isSeller) {
            throw new BusinessException("只能申诉自己参与的订单");
        }

        if (order.getOrderStatus() != null && order.getOrderStatus() == 3) {
            throw new BusinessException("该订单已取消，无需申诉");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException("请选择申诉类型");
        }

        if (reason.trim().length() > 50) {
            throw new BusinessException("申诉类型过长");
        }

        if (detail != null && detail.length() > 500) {
            throw new BusinessException("申诉说明不能超过500字");
        }

        if (evidences != null && !evidences.isEmpty()) {

            if (evidences.size() > MAX_EVIDENCES) {
                throw new BusinessException("证据图片最多 " + MAX_EVIDENCES + " 张");
            }

            for (String url : evidences) {

                if (url == null || url.trim().isEmpty()) {
                    throw new BusinessException("存在空的证据图片地址");
                }

                boolean ok = url.startsWith("/api/files/")
                        || url.startsWith("http://")
                        || url.startsWith("https://");

                if (!ok) {
                    throw new BusinessException("非法的证据图片地址（必须是 /api/files/ 或 http(s)://）");
                }

            }

            // 去重：相同 URL 只留一张
            List<String> dedup = evidences.stream().distinct().toList();
            if (dedup.size() != evidences.size()) {
                throw new BusinessException("证据图片地址存在重复");
            }

        }

        if (tradeAppealMapper.countPending(orderId, userId) > 0) {
            throw new BusinessException("该订单已有待处理的申诉，请等待管理员处理");
        }

        TradeAppeal appeal = new TradeAppeal();
        appeal.setOrderId(orderId);
        appeal.setUserId(userId);
        appeal.setReason(reason.trim());
        appeal.setDetail(detail == null ? null : detail.trim());

        if (evidences != null && !evidences.isEmpty()) {
            try {
                appeal.setEvidences(om.writeValueAsString(evidences));
            } catch (JsonProcessingException e) {
                throw new BusinessException("证据序列化失败");
            }
        } else {
            appeal.setEvidences(null);
        }

        tradeAppealMapper.insert(appeal);

        return appeal.getAppealId();
    }


    // 我提交的申诉
    public List<TradeAppeal> findMine() {

        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new BusinessException("当前用户未登录");
        }

        return tradeAppealMapper.findByUserId(userId);
    }


    // 管理员：全部申诉
    public List<TradeAppeal> findAllForAdmin(Integer status) {

        checkAdmin();

        return tradeAppealMapper.findAll(status);
    }


    /**
     * 管理员处理申诉：status 只能是 1 已受理 / 2 已驳回，且只能处理待处理申诉
     */
    @Transactional(rollbackFor = Exception.class)
    public void handle(Integer appealId, Integer status, String comment) {

        Integer adminId = checkAdmin();

        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException("不支持的处理结果");
        }

        TradeAppeal appeal = tradeAppealMapper.findById(appealId);

        if (appeal == null) {
            throw new BusinessException("申诉不存在");
        }

        if (appeal.getStatus() != null && appeal.getStatus() != 0) {
            throw new BusinessException("该申诉已处理，请勿重复操作");
        }

        tradeAppealMapper.handle(appealId, status, adminId, comment);
    }


    // 管理员校验：角色来自数据库，不信任前端
    private Integer checkAdmin() {

        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new BusinessException("当前用户未登录");
        }

        User user = userMapper.findById(userId);

        if (user == null || !"admin".equals(user.getRole())) {
            throw new BusinessException("仅管理员可操作");
        }

        return userId;
    }
}
