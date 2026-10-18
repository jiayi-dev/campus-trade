package com.jiayi.campustrade.service;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.entity.AdminRecord;
import com.jiayi.campustrade.entity.SellerApplication;
import com.jiayi.campustrade.exception.BusinessException;
import com.jiayi.campustrade.mapper.AdminRecordMapper;
import com.jiayi.campustrade.mapper.SellerApplicationMapper;
import com.jiayi.campustrade.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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

    @Autowired
    private SellerApplicationMapper sellerApplicationMapper;

    @Autowired
    private UserMapper userMapper;

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
    // 卖家申请审核
    //
    // 事务保证：申请状态更新 + user.role 变更 + admin_record 留痕
    // 三步要么全部成功，要么整体回滚，杜绝“申请通过但用户仍是 student”。
    // =========================

    // 申请分页列表（JOIN 申请人 / 审核人信息）
    public List<Map<String, Object>> findApplications(
            Integer status, Integer pageNum, Integer pageSize) {

        Integer offset = (pageNum - 1) * pageSize;

        return sellerApplicationMapper.findAdminPage(status, offset, pageSize);
    }

    public int countApplications(Integer status) {
        return sellerApplicationMapper.countByStatus(status);
    }

    /**
     * 通过申请
     * 1. 条件更新 seller_application（仅 status=0 可改，防并发/重复审核）
     * 2. user.role 同步改为 seller
     * 3. 写 admin_record
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveApplication(Integer applicationId, String reviewComment) {

        Integer adminId = AuthContext.getUserId();

        SellerApplication application = loadPendingApplication(applicationId);

        int rows = sellerApplicationMapper.review(
                applicationId, 1, adminId, normalizeComment(reviewComment));

        if (rows == 0) {
            throw new BusinessException("该申请已被审核，不能重复操作");
        }

        // 同步提升用户角色
        userMapper.updateRole(application.getUserId(), "seller");

        log("审核卖家申请：通过，申请ID=" + applicationId
                + "，用户ID=" + application.getUserId());
    }

    /**
     * 拒绝申请
     * 1. 拒绝必须填写审核意见
     * 2. 条件更新 seller_application 为已拒绝
     * 3. user.role 保持 student 不变
     * 4. 写 admin_record
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectApplication(Integer applicationId, String reviewComment) {

        String comment = normalizeComment(reviewComment);

        if (comment.isEmpty()) {
            throw new BusinessException("拒绝申请时必须填写审核意见");
        }

        Integer adminId = AuthContext.getUserId();

        SellerApplication application = loadPendingApplication(applicationId);

        int rows = sellerApplicationMapper.review(
                applicationId, 2, adminId, comment);

        if (rows == 0) {
            throw new BusinessException("该申请已被审核，不能重复操作");
        }

        log("审核卖家申请：拒绝，申请ID=" + applicationId
                + "，用户ID=" + application.getUserId()
                + "，审核意见：" + comment);
    }

    // 加载申请并校验：存在 + 必须是待审核状态
    private SellerApplication loadPendingApplication(Integer applicationId) {

        SellerApplication application =
                sellerApplicationMapper.findById(applicationId);

        if (application == null) {
            throw new BusinessException("申请不存在");
        }

        if (application.getStatus() == null
                || application.getStatus() != 0) {
            throw new BusinessException("该申请已审核，不能重复操作");
        }

        return application;
    }

    // 审核意见归一化：null 转空串并去除首尾空白，长度限制与数据库 varchar(255) 对齐
    private String normalizeComment(String comment) {

        if (comment == null) {
            return "";
        }

        String trimmed = comment.trim();

        if (trimmed.length() > 255) {
            trimmed = trimmed.substring(0, 255);
        }

        return trimmed;
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
