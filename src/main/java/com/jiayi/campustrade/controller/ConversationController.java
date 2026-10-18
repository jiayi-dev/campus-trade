package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.entity.Conversation;
import com.jiayi.campustrade.entity.ConversationMessage;
import com.jiayi.campustrade.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * AI 对话历史管理接口（V1.2 Day2.5）
 *
 * 挂在 /api/ai/conversations 下，AuthInterceptor 对 /api/ai/** 中
 * 非 chat/stream 的路径走默认登录校验（游客 401）。
 *
 * 接口：
 *   GET    /api/ai/conversations            — 列出当前登录用户的全部对话
 *   POST   /api/ai/conversations            — 新建对话
 *   GET    /api/ai/conversations/{id}/messages — 加载某对话的消息列表
 *   DELETE /api/ai/conversations/{id}       — 删除对话
 */
@RestController
@RequestMapping("/ai/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    /**
     * 列出当前登录用户的全部对话，按最近活跃时间降序。
     */
    @GetMapping
    public Map<String, Object> list() {
        Integer userId = AuthContext.getUserId();
        // 理论上不会走到这里（拦截器已拦截游客），防御式校验
        if (userId == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("code", 401);
            err.put("message", "请先登录");
            err.put("data", Collections.emptyList());
            return err;
        }

        List<Conversation> conversations = conversationService.listConversations(userId);
        List<Map<String, Object>> data = new ArrayList<>();
        for (Conversation conv : conversations) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", conv.getId());
            item.put("title", conv.getTitle());
            item.put("createTime", conv.getCreateTime());
            item.put("updateTime", conv.getUpdateTime());
            data.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("conversations", data);
        return result;
    }

    /**
     * 新建对话。请求体可选 {title}，为空时默认「新对话」。
     */
    @PostMapping
    public Map<String, Object> create(@RequestBody(required = false) Map<String, Object> body) {
        Integer userId = AuthContext.getUserId();
        if (userId == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("code", 401);
            err.put("message", "请先登录");
            return err;
        }

        String title = null;
        if (body != null) {
            Object titleObj = body.get("title");
            if (titleObj != null) {
                title = String.valueOf(titleObj).trim();
            }
        }

        Conversation conv = conversationService.createConversation(userId, title);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", conv.getId());
        result.put("title", conv.getTitle());
        result.put("createTime", conv.getCreateTime());
        result.put("updateTime", conv.getUpdateTime());
        return result;
    }

    /**
     * 加载某对话的全部消息。
     * 带所有权校验：对话不存在或不属于当前用户时返回 404。
     */
    @GetMapping("/{id}/messages")
    public Map<String, Object> messages(@PathVariable Integer id) {
        Integer userId = AuthContext.getUserId();
        if (userId == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("code", 401);
            err.put("message", "请先登录");
            return err;
        }

        List<ConversationMessage> msgs = conversationService.getMessages(id, userId);
        if (msgs == null) {
            Map<String, Object> notFound = new LinkedHashMap<>();
            notFound.put("code", 404);
            notFound.put("message", "对话不存在或无权访问");
            notFound.put("messages", Collections.emptyList());
            return notFound;
        }

        List<Map<String, Object>> data = new ArrayList<>();
        for (ConversationMessage msg : msgs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", msg.getId());
            item.put("role", msg.getRole());
            item.put("content", msg.getContent());
            item.put("extraJson", msg.getExtraJson());
            item.put("createTime", msg.getCreateTime());
            data.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("messages", data);
        return result;
    }

    /**
     * 删除对话（先删消息再删对话，带所有权校验）。
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Integer id) {
        Integer userId = AuthContext.getUserId();
        if (userId == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("code", 401);
            err.put("message", "请先登录");
            return err;
        }

        boolean ok = conversationService.deleteConversation(id, userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", ok);
        if (!ok) {
            result.put("message", "对话不存在或无权删除");
        }
        return result;
    }
}
