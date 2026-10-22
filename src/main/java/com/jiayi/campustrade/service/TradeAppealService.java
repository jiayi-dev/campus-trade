package com.jiayi.campustrade.service;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.entity.Orders;
import com.jiayi.campustrade.entity.TradeAppeal;
import com.jiayi.campustrade.entity.User;
import com.jiayi.campustrade.exception.BusinessException;
import com.jiayi.campustrade.mapper.OrderMapper;
import com.jiayi.campustrade.mapper.TradeAppealMapper;
import com.jiayi.campustrade.mapper.UserMapper;
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


    /**
     * 提交交易申诉：只有该订单的买家或卖家可以申诉
     * 同一人对同一订单只允许一条待处理申诉，避免刷单式重复提交。
     */
    @Transactional(rollbackFor = Exception.class)
    public Integer create(Integer orderId, String reason, String detail) {

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

        if (tradeAppealMapper.countPending(orderId, userId) > 0) {
            throw new BusinessException("该订单已有待处理的申诉，请等待管理员处理");
        }

        TradeAppeal appeal = new TradeAppeal();
        appeal.setOrderId(orderId);
        appeal.setUserId(userId);
        appeal.setReason(reason.trim());
        appeal.setDetail(detail == null ? null : detail.trim());

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
