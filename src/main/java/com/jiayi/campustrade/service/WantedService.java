package com.jiayi.campustrade.service;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.entity.WantedPost;
import com.jiayi.campustrade.entity.WantedResponse;
import com.jiayi.campustrade.exception.BusinessException;
import com.jiayi.campustrade.mapper.WantedPostMapper;
import com.jiayi.campustrade.mapper.WantedResponseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WantedService {

    @Autowired
    private WantedPostMapper wantedPostMapper;

    @Autowired
    private WantedResponseMapper wantedResponseMapper;


    // =========================
    // 分页查询求购（动态筛选 + 多种排序）
    // =========================

    public Map<String, Object> findPage(
            String keyword,
            Integer categoryId,
            String campus,
            BigDecimal budgetMax,
            Integer status,
            String sortBy,
            Integer pageNum,
            Integer pageSize) {

        Integer offset = (pageNum - 1) * pageSize;

        List<WantedPost> list = wantedPostMapper.findPage(
                keyword,
                categoryId,
                campus,
                budgetMax,
                status,
                sortBy,
                offset,
                pageSize
        );

        int total = wantedPostMapper.count(
                keyword,
                categoryId,
                campus,
                budgetMax,
                status
        );

        Map<String, Object> result = new HashMap<>();

        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        return result;
    }


    // 求购详情（不存在返回 null，与项目既有接口风格一致）
    public WantedPost findById(Integer wantedId) {

        return wantedPostMapper.findById(wantedId);

    }


    // =========================
    // 发布求购
    // =========================

    /**
     * 发布求购（发布人一律取登录上下文，前端传 userId 不生效）
     *
     * @return 新求购ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Integer publish(WantedPost post) {

        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new BusinessException("当前用户未登录");
        }

        validate(post);

        post.setUserId(userId);

        wantedPostMapper.insert(post);

        return post.getWantedId();

    }


    // =========================
    // 修改求购（仅发布者本人）
    // =========================

    @Transactional(rollbackFor = Exception.class)
    public void update(WantedPost post) {

        if (post.getWantedId() == null) {
            throw new BusinessException("缺少求购ID");
        }

        // 归属校验：不是自己的求购直接拒绝
        checkOwnership(post.getWantedId());

        validate(post);

        wantedPostMapper.update(post);

    }


    // =========================
    // 求购状态流转：仅发布者本人可关闭（只允许 1 → 2）
    // =========================

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer wantedId, Integer targetStatus) {

        WantedPost post = checkOwnership(wantedId);

        // 唯一合法流转：进行中(1) → 已关闭(2)，其余一律拒绝
        if (targetStatus == null || targetStatus != 2) {
            throw new BusinessException("不支持的求购状态操作");
        }

        if (post.getStatus() == null || post.getStatus() != 1) {
            throw new BusinessException("不支持的求购状态操作");
        }

        wantedPostMapper.updateStatus(wantedId, 2);

    }


    // =========================
    // 需求响应
    // =========================

    // 查询某条求购的全部响应（按时间倒序，含响应者昵称）
    public List<WantedResponse> findResponses(Integer wantedId) {

        return wantedResponseMapper.findByWantedId(wantedId);

    }


    /**
     * 提交报价（任意登录用户，不能响应自己的求购）
     *
     * 同一人可以对同一条求购提交多条报价，每条独立成行、独立状态（0待选），
     * 需求方在详情页择优选定其中一条，选定后其余报价自动标记为未入选。
     * 提交后按真实响应行数刷新 wanted_post.response_count，保证计数不做假。
     */
    @Transactional(rollbackFor = Exception.class)
    public void respond(Integer wantedId, String message, BigDecimal price) {

        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new BusinessException("当前用户未登录");
        }

        WantedPost post = wantedPostMapper.findById(wantedId);

        if (post == null) {
            throw new BusinessException("求购不存在或已被删除");
        }

        // 已关闭的求购不再接受新报价
        if (post.getStatus() != null && post.getStatus() != 1) {
            throw new BusinessException("该求购已关闭，无法再提交报价");
        }

        // 不能响应自己发布的求购
        if (userId.equals(post.getUserId())) {
            throw new BusinessException("不能响应自己发布的求购");
        }

        if (message == null || message.trim().isEmpty()) {
            throw new BusinessException("响应内容不能为空");
        }

        if (message.trim().length() > 300) {
            throw new BusinessException("响应内容不能超过300字");
        }

        if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("报价金额不能小于0");
        }

        if (price != null && price.compareTo(new BigDecimal("999999")) > 0) {
            throw new BusinessException("报价金额超出合理范围");
        }

        WantedResponse response = new WantedResponse();
        response.setWantedId(wantedId);
        response.setUserId(userId);
        response.setMessage(message.trim());
        response.setPrice(price);

        wantedResponseMapper.insert(response);

        // 真实维护响应数：与 wanted_response 的实际行数保持一致
        wantedPostMapper.refreshResponseCount(wantedId);

    }


    /**
     * 选定报价（仅求购发布者本人）
     *
     * 1. 目标报价必须属于这条求购；
     * 2. 该报价在当前求购下只能是 0 待选；
     * 3. 选定后：目标报价 1 已选定、其余 2 未入选、求购状态置 2 已关闭。
     */
    @Transactional(rollbackFor = Exception.class)
    public void acceptQuote(Integer wantedId, Integer responseId) {

        WantedPost post = checkOwnership(wantedId);

        if (responseId == null) {
            throw new BusinessException("缺少报价ID");
        }

        if (post.getStatus() == null || post.getStatus() != 1) {
            throw new BusinessException("该求购已关闭，无法再选定报价");
        }

        WantedResponse quote = wantedResponseMapper.findById(responseId);

        if (quote == null || !wantedId.equals(quote.getWantedId())) {
            throw new BusinessException("报价不存在或不属于该求购");
        }

        if (quote.getStatus() != null && quote.getStatus() != 0) {
            throw new BusinessException("该报价已处理，请勿重复选定");
        }

        wantedResponseMapper.accept(wantedId, responseId);

        // 选定即成交意向达成：求购同步关闭，不再接受新报价
        wantedPostMapper.updateStatus(wantedId, 2);

    }


    // =========================
    // 内部方法
    // =========================

    // 归属校验：校验登录态 + 求购存在 + 是否为发布者本人
    private WantedPost checkOwnership(Integer wantedId) {

        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new BusinessException("当前用户未登录");
        }

        WantedPost post = wantedPostMapper.findById(wantedId);

        if (post == null) {
            throw new BusinessException("求购不存在或已被删除");
        }

        if (!userId.equals(post.getUserId())) {
            throw new BusinessException("无权修改他人的求购");
        }

        return post;

    }


    // 发布/编辑共用的字段校验
    private void validate(WantedPost post) {

        if (post.getTitle() == null || post.getTitle().trim().isEmpty()) {
            throw new BusinessException("求购标题不能为空");
        }

        if (post.getTitle().trim().length() > 60) {
            throw new BusinessException("求购标题不能超过60字");
        }

        if (post.getDescription() != null && post.getDescription().length() > 500) {
            throw new BusinessException("需求描述不能超过500字");
        }

        BigDecimal min = post.getBudgetMin();
        BigDecimal max = post.getBudgetMax();

        if (min != null && min.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("预算下限不能小于0");
        }

        if (max != null && max.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("预算上限不能小于0");
        }

        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BusinessException("预算下限不能大于预算上限");
        }

    }
}
