package com.jiayi.campustrade.service;

import com.jiayi.campustrade.entity.Conversation;
import com.jiayi.campustrade.entity.ConversationMessage;
import com.jiayi.campustrade.mapper.ConversationMapper;
import com.jiayi.campustrade.mapper.ConversationMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 对话历史服务（V1.2 Day2.5）
 *
 * 职责：
 *   1. 会话 CRUD（登录用户，带所有权校验）；
 *   2. 消息保存（AiService 在 SSE 流式结束后调用，不影响流式输出）；
 *   3. 消息加载（重新进入页面时恢复历史）。
 *
 * 安全：
 *   - 所有查询/删除都校验 userId，防止越权访问他人对话；
 *   - 消息保存不做越权校验（由 AiController 保证 currentUserId 来自 Token）。
 */
@Service
public class ConversationService {

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private ConversationMessageMapper conversationMessageMapper;

    /**
     * 创建新对话。
     * @param userId 登录用户ID
     * @param title  对话标题，为空时默认「新对话」
     * @return 带自增 id 的 Conversation 对象
     */
    public Conversation createConversation(Integer userId, String title) {
        Conversation conv = new Conversation();
        conv.setUserId(userId);
        conv.setTitle(title == null || title.trim().isEmpty() ? "新对话" : title.trim());
        LocalDateTime now = LocalDateTime.now();
        conv.setCreateTime(now);
        conv.setUpdateTime(now);
        conversationMapper.insert(conv);
        return conv;
    }

    /**
     * 查询用户全部对话，按最近活跃时间降序。
     */
    public List<Conversation> listConversations(Integer userId) {
        return conversationMapper.findByUserId(userId);
    }

    /**
     * 查询某对话的全部消息（带所有权校验）。
     * @return 消息列表（时间升序）；对话不存在或不属于该用户时返回 null
     */
    public List<ConversationMessage> getMessages(Integer conversationId, Integer userId) {
        Conversation conv = conversationMapper.findByIdAndUser(conversationId, userId);
        if (conv == null) {
            return null;
        }
        return conversationMessageMapper.findByConversationId(conversationId);
    }

    /**
     * 删除对话（先删消息再删对话，带所有权校验）。
     * @return true=删除成功；false=对话不存在或不属于该用户
     */
    public boolean deleteConversation(Integer conversationId, Integer userId) {
        Conversation conv = conversationMapper.findByIdAndUser(conversationId, userId);
        if (conv == null) {
            return false;
        }
        conversationMessageMapper.deleteByConversationId(conversationId);
        conversationMapper.deleteById(conversationId);
        return true;
    }

    /**
     * 保存消息（由 AiService 在 SSE 流式结束后调用）。
     * 同时更新对话标题（首次对话时取首条用户消息前 20 字）和活跃时间。
     * extraJson 携带 assistant 消息的结构化附加数据（goods 商品卡片 / sources 知识来源），
     * 仅 assistant 消息需要，user 消息传 null。
     * 异常被吞掉，不影响 SSE 已发出的内容。
     */
    public void saveMessage(Integer conversationId, String role, String content, String extraJson) {
        try {
            ConversationMessage msg = new ConversationMessage();
            msg.setConversationId(conversationId);
            msg.setRole(role);
            msg.setContent(content);
            msg.setExtraJson(extraJson);
            msg.setCreateTime(LocalDateTime.now());
            conversationMessageMapper.insert(msg);

            // 首条用户消息：更新对话标题
            if ("user".equals(role)) {
                String title = content.length() > 20 ? content.substring(0, 20) + "…" : content;
                conversationMapper.updateTitle(conversationId, title);
            }
            // 刷新活跃时间（排序用）
            conversationMapper.touchUpdateTime(conversationId);
        } catch (Exception e) {
            // 保存失败不影响已发出的 SSE 内容，只记日志
            System.out.println("[ConversationService] 消息保存失败 conv=" + conversationId
                    + " role=" + role + " err=" + e.getMessage());
        }
    }

    /**
     * 判断 conversationId 是否可持久化（数字 + 非空）。
     * AiService 中仅当返回 true 且 currentUserId 非空时才调 saveMessage。
     */
    public static boolean isPersistable(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(conversationId);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
