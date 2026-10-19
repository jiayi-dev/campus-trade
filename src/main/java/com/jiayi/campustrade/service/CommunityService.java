package com.jiayi.campustrade.service;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.entity.CommunityComment;
import com.jiayi.campustrade.entity.CommunityPost;
import com.jiayi.campustrade.exception.BusinessException;
import com.jiayi.campustrade.mapper.CommunityCommentMapper;
import com.jiayi.campustrade.mapper.CommunityLikeMapper;
import com.jiayi.campustrade.mapper.CommunityPostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CommunityService {

    @Autowired
    private CommunityPostMapper communityPostMapper;

    @Autowired
    private CommunityCommentMapper communityCommentMapper;

    @Autowired
    private CommunityLikeMapper communityLikeMapper;

    // 允许的主题标签集合（与产品定义 / 前端筛选项保持一致）
    private static final Set<String> TOPICS = new HashSet<>(Arrays.asList(
            "闲置分享", "使用体验", "交易经验", "环保循环"
    ));


    // =========================
    // 分页查询动态（筛选 + 排序）
    // =========================

    public Map<String, Object> findPage(
            String keyword,
            String topic,
            String sortBy,
            Integer pageNum,
            Integer pageSize) {

        Integer currentUserId = AuthContext.getUserId();

        Integer offset = (pageNum - 1) * pageSize;

        List<CommunityPost> list = communityPostMapper.findPage(
                keyword,
                topic,
                sortBy,
                currentUserId,
                offset,
                pageSize
        );

        int total = communityPostMapper.count(keyword, topic);

        Map<String, Object> result = new HashMap<>();

        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        return result;
    }


    // 动态详情（不存在返回 null，与项目既有接口风格一致）；存在则浏览量原子 +1
    @Transactional(rollbackFor = Exception.class)
    public CommunityPost findById(Integer postId) {

        Integer currentUserId = AuthContext.getUserId();

        CommunityPost post = communityPostMapper.findById(postId, currentUserId);

        if (post == null) {
            return null;
        }

        communityPostMapper.incrementViewCount(postId);

        // 同步返回自增后的浏览量，避免前端再次查询
        post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);

        return post;
    }


    // 我发布的动态（用于「我的动态」）
    public List<CommunityPost> findMine() {

        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new BusinessException("当前用户未登录");
        }

        return communityPostMapper.findByUserId(userId, userId);
    }


    // =========================
    // 发布动态
    // =========================

    /**
     * 发布动态（发布人一律取登录上下文，前端传 userId 不生效）
     *
     * @return 新动态ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Integer publish(CommunityPost post) {

        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new BusinessException("当前用户未登录");
        }

        validate(post);

        post.setUserId(userId);

        communityPostMapper.insert(post);

        return post.getPostId();
    }


    // =========================
    // 编辑动态（仅发布者本人）
    // =========================

    @Transactional(rollbackFor = Exception.class)
    public void update(CommunityPost post) {

        if (post.getPostId() == null) {
            throw new BusinessException("缺少动态ID");
        }

        // 归属校验：不是自己的动态直接拒绝
        checkOwnership(post.getPostId());

        validate(post);

        communityPostMapper.update(post);
    }


    // =========================
    // 删除动态（仅发布者本人；评论 / 点赞由外键级联清理）
    // =========================

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer postId) {

        checkOwnership(postId);

        communityPostMapper.deleteById(postId);
    }


    // =========================
    // 评论
    // =========================

    // 查询某条动态的全部评论（含评论者昵称，按时间正序）
    public List<CommunityComment> findComments(Integer postId) {

        return communityCommentMapper.findByPostId(postId);
    }


    /**
     * 发表评论（任意登录用户）
     *
     * 提交后按 community_comment 的真实行数刷新 community_post.comment_count，
     * 保证计数不做假（参照 WantedService.refreshResponseCount 的做法）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void addComment(Integer postId, String content) {

        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new BusinessException("当前用户未登录");
        }

        CommunityPost post = communityPostMapper.findById(postId, userId);

        if (post == null) {
            throw new BusinessException("动态不存在或已被删除");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("评论内容不能为空");
        }

        if (content.trim().length() > 500) {
            throw new BusinessException("评论内容不能超过500字");
        }

        CommunityComment comment = new CommunityComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content.trim());

        communityCommentMapper.insert(comment);

        // 真实维护评论数：与 community_comment 的实际行数保持一致
        communityPostMapper.refreshCommentCount(postId);
    }


    // =========================
    // 点赞 / 取消点赞（切换）
    // =========================

    /**
     * 点赞 / 取消点赞切换（唯一键 uk_post_user 保证一人一赞）
     *
     * 切换后按 community_like 的真实行数刷新 community_post.like_count，
     * 返回切换后的点赞状态与真实点赞数。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleLike(Integer postId) {

        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new BusinessException("当前用户未登录");
        }

        CommunityPost post = communityPostMapper.findById(postId, userId);

        if (post == null) {
            throw new BusinessException("动态不存在或已被删除");
        }

        boolean liked;

        if (communityLikeMapper.countByPostAndUser(postId, userId) > 0) {
            communityLikeMapper.delete(postId, userId);
            liked = false;
        } else {
            communityLikeMapper.insert(postId, userId);
            liked = true;
        }

        // 真实维护点赞数：与 community_like 的实际行数保持一致
        communityPostMapper.refreshLikeCount(postId);

        CommunityPost latest = communityPostMapper.findById(postId, userId);

        Map<String, Object> result = new HashMap<>();

        result.put("liked", liked);
        result.put("likeCount",
                latest == null || latest.getLikeCount() == null ? 0 : latest.getLikeCount());

        return result;
    }


    // =========================
    // 内部方法
    // =========================

    // 归属校验：校验登录态 + 动态存在 + 是否为发布者本人
    private CommunityPost checkOwnership(Integer postId) {

        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new BusinessException("当前用户未登录");
        }

        CommunityPost post = communityPostMapper.findById(postId, userId);

        if (post == null) {
            throw new BusinessException("动态不存在或已被删除");
        }

        if (!userId.equals(post.getUserId())) {
            throw new BusinessException("无权操作他人的动态");
        }

        return post;
    }


    // 发布 / 编辑共用的字段校验
    private void validate(CommunityPost post) {

        if (post.getTitle() == null || post.getTitle().trim().isEmpty()) {
            throw new BusinessException("动态标题不能为空");
        }

        if (post.getTitle().trim().length() > 60) {
            throw new BusinessException("动态标题不能超过60字");
        }

        if (post.getContent() == null || post.getContent().trim().isEmpty()) {
            throw new BusinessException("动态正文不能为空");
        }

        if (post.getContent().trim().length() > 2000) {
            throw new BusinessException("动态正文不能超过2000字");
        }

        if (post.getTopic() == null || !TOPICS.contains(post.getTopic())) {
            throw new BusinessException("请选择有效的主题标签");
        }
    }
}
