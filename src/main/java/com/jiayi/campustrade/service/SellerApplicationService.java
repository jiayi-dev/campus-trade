package com.jiayi.campustrade.service;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.entity.SellerApplication;
import com.jiayi.campustrade.entity.User;
import com.jiayi.campustrade.mapper.SellerApplicationMapper;
import com.jiayi.campustrade.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 学生侧卖家申请 Service
 *
 * 业务规则：
 * 1. userId 只从 AuthContext 获取，不信前端参数
 * 2. 只有 student 可以申请；seller 重复申请拒绝；admin 不允许申请
 * 3. 存在 pending 申请时不能重复提交
 * 4. 已拒绝后允许重新申请（新插入一条记录，保留历史轨迹）
 * 5. reason 必填，长度 10-500
 *
 * 管理员审核逻辑在 AdminService 中，与 admin_record 留痕放在同一事务。
 */
@Service
public class SellerApplicationService {

    // 申请原因长度限制（与数据库 varchar(500) 对齐）
    private static final int REASON_MIN = 10;
    private static final int REASON_MAX = 500;

    @Autowired
    private SellerApplicationMapper sellerApplicationMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 学生提交卖家申请
     */
    public void apply(String reason) {

        Integer userId = AuthContext.getUserId();

        // ① 原因校验
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("申请原因不能为空");
        }

        String trimmedReason = reason.trim();

        if (trimmedReason.length() < REASON_MIN) {
            throw new RuntimeException("申请原因至少填写 " + REASON_MIN + " 个字");
        }

        if (trimmedReason.length() > REASON_MAX) {
            throw new RuntimeException("申请原因不能超过 " + REASON_MAX + " 个字");
        }

        // ② 查当前登录用户真实角色
        User user = userMapper.findById(userId);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        String role = user.getRole();

        if ("admin".equals(role)) {
            throw new RuntimeException("管理员账号无需申请卖家身份");
        }

        if ("seller".equals(role)) {
            throw new RuntimeException("你已经是卖家，无需重复申请");
        }

        // ③ 存在 pending 申请时阻止重复提交
        SellerApplication pending =
                sellerApplicationMapper.findPendingByUserId(userId);

        if (pending != null) {
            throw new RuntimeException("你已有一条待审核的申请，请耐心等待管理员审核");
        }

        // ④ 插入新申请（status 由数据库默认 0）
        SellerApplication application = new SellerApplication();
        application.setUserId(userId);
        application.setReason(trimmedReason);

        sellerApplicationMapper.insert(application);
    }

    /**
     * 查询当前登录用户最新一条申请（无申请返回 null）
     */
    public SellerApplication findMyLatest() {

        Integer userId = AuthContext.getUserId();

        return sellerApplicationMapper.findLatestByUserId(userId);
    }

}
