package com.jiayi.campustrade.service;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.entity.AdminRecord;
import com.jiayi.campustrade.mapper.AdminRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 管理员操作编排 Service
 *
 * 职责：把“业务操作”和“操作记录”放在同一个事务里，
 * 保证操作成功才留痕，留痕失败则业务操作一并回滚。
 *
 * 业务规则本身不在这里重写，而是复用 UserService / GoodsService
 * 已有的规则（归属校验、订单保护等），管理员只是权限更高的调用者。
 */
@Service
public class AdminService {

    @Autowired
    private UserService userService;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private AdminRecordMapper adminRecordMapper;

    // =========================
    // 启用 / 禁用用户
    // =========================

    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Integer targetUserId, Integer status) {

        userService.updateStatus(targetUserId, status);

        log((status == 1 ? "启用" : "禁用") + "用户：用户ID=" + targetUserId);
    }

    // =========================
    // 下架商品
    // 复用 GoodsService.updateStatus：管理员通过归属校验，
    // 仍受“已产生交易记录的商品不能重新上架”等规则约束
    // =========================

    @Transactional(rollbackFor = Exception.class)
    public void offSaleGoods(Integer goodsId) {

        goodsService.updateStatus(goodsId, 0);

        log("下架商品：商品ID=" + goodsId);
    }

    // =========================
    // 删除商品
    // 复用 GoodsService.deleteById：有订单的商品会被拒绝
    // =========================

    @Transactional(rollbackFor = Exception.class)
    public void deleteGoods(Integer goodsId) {

        goodsService.deleteById(goodsId);

        log("删除商品：商品ID=" + goodsId);
    }

    // =========================
    // 操作记录查询
    // =========================

    public List<AdminRecord> findRecords() {
        return adminRecordMapper.findAll();
    }

    // =========================
    // 写入操作记录
    // adminId 从登录上下文获取，不信任前端传入
    // =========================

    private void log(String operation) {

        Integer adminId = AuthContext.getUserId();

        AdminRecord record = new AdminRecord();
        record.setAdminId(adminId);
        record.setOperation(operation);

        adminRecordMapper.insert(record);
    }

}
