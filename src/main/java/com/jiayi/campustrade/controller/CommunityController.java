package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.entity.CommunityComment;
import com.jiayi.campustrade.entity.CommunityPost;
import com.jiayi.campustrade.service.CommunityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 校园社区动态
 *
 * 挂在 /community 下，AuthInterceptor 默认拦截（白名单未放行），因此
 * 全部接口都要求登录态，符合「登录后才可使用」的产品要求。
 */
@RestController
@RequestMapping("/community")
public class CommunityController {

    @Autowired
    private CommunityService communityService;


    // =========================
    // 动态分页查询：GET /community/page
    // =========================

    @GetMapping("/page")
    public Map<String, Object> findPage(

            // 标题模糊
            @RequestParam(required = false)
            String keyword,

            // 主题筛选（闲置分享/使用体验/交易经验/环保循环）
            @RequestParam(required = false)
            String topic,

            // 排序：latest 最新 / hot 最热
            @RequestParam(defaultValue = "latest")
            String sortBy,

            @RequestParam(defaultValue = "1")
            Integer pageNum,

            @RequestParam(defaultValue = "8")
            Integer pageSize) {

        return communityService.findPage(keyword, topic, sortBy, pageNum, pageSize);
    }


    // =========================
    // 我的动态：GET /community/mine
    // （静态段 mine 的匹配优先级高于动态段 {id}）
    // =========================

    @GetMapping("/mine")
    public List<CommunityPost> findMine() {

        return communityService.findMine();
    }


    // =========================
    // 动态详情（不存在返回 null）：GET /community/{id}
    // =========================

    @GetMapping("/{id}")
    public CommunityPost findById(
            @PathVariable("id") Integer id) {

        return communityService.findById(id);
    }


    // =========================
    // 发布动态：POST /community
    // =========================

    @PostMapping
    public String publish(
            @RequestBody CommunityPost post) {

        try {

            Integer postId = communityService.publish(post);

            return "动态发布成功，动态ID：" + postId;

        } catch (RuntimeException e) {

            // 业务失败（校验不通过等）以文本返回，与项目现有接口风格一致
            return e.getMessage();
        }
    }


    // =========================
    // 编辑动态（仅发布者本人）：PUT /community
    // =========================

    @PutMapping
    public String update(
            @RequestBody CommunityPost post) {

        try {

            communityService.update(post);

            return "动态更新成功";

        } catch (RuntimeException e) {

            return e.getMessage();
        }
    }


    // =========================
    // 删除动态（仅发布者本人）：DELETE /community/{id}
    // =========================

    @DeleteMapping("/{id}")
    public String delete(
            @PathVariable("id") Integer id) {

        try {

            communityService.delete(id);

            return "动态已删除";

        } catch (RuntimeException e) {

            return e.getMessage();
        }
    }


    // =========================
    // 评论列表：GET /community/{id}/comments
    // =========================

    @GetMapping("/{id}/comments")
    public List<CommunityComment> findComments(
            @PathVariable("id") Integer id) {

        return communityService.findComments(id);
    }


    // =========================
    // 发表评论：POST /community/{id}/comments，请求体 { content }
    // =========================

    @PostMapping("/{id}/comments")
    public String addComment(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, Object> body) {

        Object contentObj = body.get("content");

        String content = contentObj == null ? null : String.valueOf(contentObj);

        try {

            communityService.addComment(id, content);

            return "评论已发布";

        } catch (RuntimeException e) {

            return e.getMessage();
        }
    }


    // =========================
    // 点赞 / 取消点赞切换：POST /community/{id}/like
    // 返回 { liked, likeCount }（真实按 community_like 行数维护）
    // 业务异常交由 GlobalExceptionHandler 统一转 400 + {code,message}
    // =========================

    @PostMapping("/{id}/like")
    public Map<String, Object> toggleLike(
            @PathVariable("id") Integer id) {

        return communityService.toggleLike(id);
    }
}
