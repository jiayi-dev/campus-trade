package com.jiayi.campustrade.service;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.auth.TokenManager;
import com.jiayi.campustrade.entity.SellerApplication;
import com.jiayi.campustrade.entity.User;
import com.jiayi.campustrade.exception.BusinessException;
import com.jiayi.campustrade.mapper.FavoriteMapper;
import com.jiayi.campustrade.mapper.GoodsMapper;
import com.jiayi.campustrade.mapper.OrderMapper;
import com.jiayi.campustrade.mapper.SellerApplicationMapper;
import com.jiayi.campustrade.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    // 修改密码后需要使该用户的登录会话全部失效
    @Autowired
    private TokenManager tokenManager;

    // 个人中心数据概览：聚合统计依赖
    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private OrderMapper orderMapper;

    // 个人中心档案：嵌入当前用户最新一条卖家申请（未申请为 null）
    @Autowired
    private SellerApplicationMapper sellerApplicationMapper;

    // BCrypt密码加密工具
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    // 根据用户ID查询
    public User findById(Integer id) {
        return userMapper.findById(id);
    }

    // 用户注册
    public void register(User user) {

        // ① 查询手机号是否已经注册
        User existUser = userMapper.findByPhone(user.getPhone());

        // ② 如果已经存在，则注册失败
        if (existUser != null) {
            throw new BusinessException("手机号已经注册");
        }

        // ③ 对用户输入的密码进行加密
        String encodedPassword =
                passwordEncoder.encode(user.getPassword());

        user.setPassword(encodedPassword);

        // ④ 设置默认角色
        user.setRole("student");

        // ⑤ 设置账号状态：正常
        user.setStatus(1);

        // ⑥ 保存用户
        userMapper.insert(user);
    }

    // =========================
    // 管理员：用户分页查询
    // =========================

    public List<User> findPage(
            String username,
            String phone,
            String role,
            Integer status,
            Integer pageNum,
            Integer pageSize) {

        Integer offset = (pageNum - 1) * pageSize;

        return userMapper.findPage(username, phone, role, status, offset, pageSize);
    }

    public int count(
            String username,
            String phone,
            String role,
            Integer status) {

        return userMapper.count(username, phone, role, status);
    }

    // =========================
    // 启用/禁用用户
    //
    // 业务规则：
    // 1. 管理员不能禁用/启用自己（避免把自己锁死）
    // 2. 只允许 0（禁用）和 1（启用）
    // 3. 被禁用的用户在登录时会被 AuthService 拒绝
    // =========================

    public void updateStatus(Integer userId, Integer status) {

        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("不支持的用户状态");
        }

        Integer currentUserId = AuthContext.getUserId();

        if (currentUserId != null && currentUserId.equals(userId)) {
            throw new BusinessException("不能禁用或启用自己的账号");
        }

        userMapper.updateStatus(userId, status);
    }

    // =========================
    // 个人中心（B1）：资料 / 密码 / 数据概览
    //
    // 安全约定：本区块所有方法的 userId 一律取自 AuthContext
    // （由 Token → AuthInterceptor → AuthContext 链路写入），
    // Controller 层不接收 userId 参数 —— 从链路上杜绝
    // "前端传谁的 userId 就操作谁"的水平越权
    // =========================

    /**
     * 当前登录用户的个人档案
     *
     * 返回专用档案结构（Map 组装），而不是直接返回 User 实体：
     * 1. 显式挑选字段，password 根本不放入，序列化层面不可能泄漏；
     * 2. 嵌入 sellerApplication（最新一条卖家申请，未申请为 null），
     *    前端一次请求即可渲染"身份档案 + 卖家认证卡"，不必再串行调第二次；
     * 3. 不污染 User 实体（findById 被认证拦截器等多处复用，
     *    在实体上加非数据库字段会让其它接口多出 null 字段）。
     */
    public Map<String, Object> getProfile() {

        Integer userId = AuthContext.getUserId();

        User user = userMapper.findById(userId);

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // userId 同样来自 AuthContext，只查当前登录用户自己的申请
        SellerApplication application =
                sellerApplicationMapper.findLatestByUserId(userId);

        return buildProfile(user, application);
    }

    /**
     * 组装个人档案响应（显式字段白名单，绝不放 password）
     */
    private Map<String, Object> buildProfile(User user,
                                             SellerApplication application) {

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userId", user.getUserId());
        profile.put("username", user.getUsername());
        profile.put("role", user.getRole());
        profile.put("phone", user.getPhone());
        profile.put("email", user.getEmail());
        profile.put("status", user.getStatus());
        profile.put("createTime", user.getCreateTime());
        // 卖家认证档案：null = 从未申请；否则含 status/reason/审核意见等全部字段
        profile.put("sellerApplication", application);

        return profile;
    }

    /**
     * 修改个人资料：仅允许手机号 / 邮箱
     *
     * role / status / username / password 根本不在本方法的入参与 SQL 中，
     * 在代码结构上就不存在被修改的可能（而非"运行时校验后再放行"）
     */
    public Map<String, Object> updateProfile(String phone, String email) {

        Integer userId = AuthContext.getUserId();

        // 手机号：必填 + 11 位数字（与数据库 varchar + 唯一约束对齐）
        if (phone == null || phone.trim().isEmpty()) {
            throw new BusinessException("手机号不能为空");
        }

        String trimmedPhone = phone.trim();

        if (!trimmedPhone.matches("\\d{11}")) {
            throw new BusinessException("手机号格式不正确，应为 11 位数字");
        }

        // 邮箱：选填，仅做长度兜底（格式由前端表单校验）
        String trimmedEmail = email == null ? "" : email.trim();

        if (trimmedEmail.length() > 100) {
            throw new BusinessException("邮箱长度不能超过 100 个字符");
        }

        // 手机号唯一性校验（排除自己）：
        // user.phone 有唯一约束，先在业务层给出可读提示，
        // 避免直接触发数据库唯一约束变成 500
        User existUser = userMapper.findByPhone(trimmedPhone);

        if (existUser != null && !existUser.getUserId().equals(userId)) {
            throw new BusinessException("该手机号已被其他账号使用");
        }

        // 只更新 phone / email 两个字段
        userMapper.updateProfile(userId, trimmedPhone, trimmedEmail);

        // 返回更新后的最新资料
        return getProfile();
    }

    /**
     * 修改密码：
     * BCrypt 校验旧密码 → 校验新密码 → BCrypt 加密入库 → 使该用户全部 Token 失效
     */
    public void changePassword(String oldPassword,
                               String newPassword,
                               String confirmPassword) {

        Integer userId = AuthContext.getUserId();

        // ① 基础校验
        if (oldPassword == null || oldPassword.isEmpty()) {
            throw new BusinessException("请输入原密码");
        }

        if (newPassword == null || newPassword.isEmpty()) {
            throw new BusinessException("请输入新密码");
        }

        if (newPassword.length() < 6 || newPassword.length() > 32) {
            throw new BusinessException("新密码长度需在 6-32 位之间");
        }

        // 两次新密码一致性：前端已校验，这里兜底（防止绕过前端直接调接口）
        if (confirmPassword != null && !confirmPassword.equals(newPassword)) {
            throw new BusinessException("两次输入的新密码不一致");
        }

        if (newPassword.equals(oldPassword)) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        // ② 查询当前用户
        User user = userMapper.findById(userId);

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // ③ BCrypt 校验旧密码：matches(明文输入, 库中摘要)。
        //    数据库里存的永远是 BCrypt 摘要，系统任何环节都不接触明文
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }

        // ④ BCrypt 加密新密码后更新（每次 encode 自动生成新盐值，
        //    即使两个用户密码相同，库中摘要也不同）
        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));

        // ⑤ 密码已变更：使该用户所有登录会话失效（包括当前 Token）。
        //    旧 Token 即使泄露也无法继续使用，必须用新密码重新登录
        tokenManager.removeByUserId(userId);
    }

    /**
     * 个人中心数据概览：一次请求聚合「交易成长面板 + 交易轨迹里程碑」
     * 全部来自数据库真实 COUNT/MIN/MAX 查询，不写死数字
     *
     * 信誉分规则（透明、可解释，不伪造）：
     *   creditScore = 100 + 已完成订单数 × 20
     *   0 单=校园新生 / 1-2 单=信用良好 / 3-5 单=交易达人 / ≥6 单=信誉之星
     * 已完成订单数 = 用户作为买家或卖家参与的已完成订单总数（与 EvaluationService 统一数据源）
     */
    public Map<String, Object> getOverview() {

        Integer userId = AuthContext.getUserId();

        // 信用分数据源：用户作为买家或卖家参与的已完成订单总数（与 EvaluationService 统一）
        int completedCount = orderMapper.countCompletedByUserId(userId);

        Map<String, Object> overview = new LinkedHashMap<>();

        // ---- 交易成长面板 ----
        overview.put("goodsCount", goodsMapper.countByUserId(userId));
        overview.put("favoriteCount", favoriteMapper.countByUserId(userId));
        overview.put("orderCount", orderMapper.countByBuyerId(userId, null));
        overview.put("pendingOrderCount", orderMapper.countByBuyerId(userId, 0));
        overview.put("completedOrderCount", completedCount);

        // 信誉分与等级：由真实完成订单数推导
        overview.put("creditScore", 100 + completedCount * 20);
        overview.put("creditLevel", creditLevelOf(completedCount));

        // ---- 交易轨迹里程碑（无数据时对应字段为 null，前端渲染空状态） ----
        Map<String, Object> milestones = new LinkedHashMap<>();

        LocalDateTime firstGoodsTime = goodsMapper.findFirstPublishTime(userId);
        milestones.put("firstGoodsTime", firstGoodsTime);

        // 首单/末单一条 SQL 取回
        Map<String, Object> orderRange = orderMapper.findOrderTimeRange(userId);
        milestones.put("firstOrderTime", orderRange == null ? null : orderRange.get("firstOrderTime"));
        milestones.put("lastOrderTime", orderRange == null ? null : orderRange.get("lastOrderTime"));

        overview.put("milestones", milestones);

        return overview;
    }

    /**
     * 按已完成订单数映射信誉等级
     */
    private String creditLevelOf(int completedCount) {

        if (completedCount <= 0) {
            return "校园新生";
        }
        if (completedCount <= 2) {
            return "信用良好";
        }
        if (completedCount <= 5) {
            return "交易达人";
        }
        return "信誉之星";
    }
}
