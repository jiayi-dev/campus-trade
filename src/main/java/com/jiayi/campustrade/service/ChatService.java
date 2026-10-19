package com.jiayi.campustrade.service;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.entity.ChatMessage;
import com.jiayi.campustrade.exception.BusinessException;
import com.jiayi.campustrade.mapper.ChatMessageMapper;
import com.jiayi.campustrade.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户间交易私聊服务
 *
 * 当前用户一律取 AuthContext，不接受前端传入的 userId，
 * 避免越权查看/发送他人会话。
 */
@Service
public class ChatService {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    // 用于「发起会话」的联系人候选：只从真实订单数据聚合，不造假
    @Autowired
    private OrderMapper orderMapper;


    // =========================
    // 会话键工具
    // =========================

    /**
     * 会话键：小 id 在前、大 id 在后，格式 min-max-gid；
     * goodsId 为空时用 0 占位（表示不针对具体商品的沟通）。
     */
    public String sessionKeyOf(Integer a, Integer b, Integer goodsId) {

        int min = Math.min(a, b);
        int max = Math.max(a, b);
        int gid = goodsId == null ? 0 : goodsId;

        return min + "-" + max + "-" + gid;
    }


    // =========================
    // 会话列表
    // =========================

    public List<Map<String, Object>> getSessions() {

        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new BusinessException("请先登录");
        }

        List<Map<String, Object>> sessions = chatMessageMapper.findSessions(userId);

        if (sessions == null) {
            sessions = new ArrayList<>();
        }

        // MyBatis 的 SUM 可能返回 BigDecimal / Long，统一兜底转成 int
        for (Map<String, Object> session : sessions) {
            session.put("unread", toInt(session.get("unread")));
        }

        return sessions;
    }


    // =========================
    // 会话历史
    // =========================

    /**
     * 取某会话的全部消息（时间正序）。
     * 顺序很重要：先 markRead（只影响发给我的消息）再查历史，
     * 这样返回的消息已带上最新已读状态。
     * 每条消息补 mine 字段（fromUserId == 当前用户），方便前端渲染气泡。
     */
    public List<Map<String, Object>> getHistory(Integer otherUserId, Integer goodsId) {

        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new BusinessException("请先登录");
        }

        if (otherUserId == null) {
            throw new BusinessException("缺少对方用户ID");
        }

        if (otherUserId.equals(userId)) {
            throw new BusinessException("不能与自己聊天");
        }

        String sessionKey = sessionKeyOf(userId, otherUserId, goodsId);

        chatMessageMapper.markRead(sessionKey, userId);

        List<Map<String, Object>> rows = chatMessageMapper.findBySession(sessionKey);

        List<Map<String, Object>> result = new ArrayList<>();

        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new LinkedHashMap<>(row);
                item.put("mine", toInt(row.get("fromUserId")) == userId);
                result.add(item);
            }
        }

        return result;
    }


    // =========================
    // 发送消息
    // =========================

    public String send(Integer toUserId, Integer goodsId, String content) {

        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new BusinessException("请先登录");
        }

        if (toUserId == null) {
            throw new BusinessException("缺少接收人ID");
        }

        if (toUserId.equals(userId)) {
            throw new BusinessException("不能给自己发消息");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }

        String text = content.trim();

        if (text.length() > 500) {
            throw new BusinessException("消息内容不能超过 500 字");
        }

        ChatMessage message = new ChatMessage();
        message.setFromUserId(userId);
        message.setToUserId(toUserId);
        message.setGoodsId(goodsId);
        message.setContent(text);
        message.setIsRead(0);
        message.setSessionKey(sessionKeyOf(userId, toUserId, goodsId));

        chatMessageMapper.insert(message);

        return "发送成功";
    }


    // =========================
    // 未读总数
    // =========================

    public int countUnread() {

        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            return 0;
        }

        return chatMessageMapper.countUnread(userId);
    }


    // =========================
    // 联系人候选（发起新会话用）
    // =========================

    /**
     * 只用现有真实订单数据聚合联系人：
     * - 我买到的 → 对方是卖家；
     * - 我卖出的 → 对方是买家；
     * 按 goodsId + otherUserId 去重，按 goodsId 倒序（新的在前）。
     */
    public List<Map<String, Object>> getContacts() {

        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new BusinessException("请先登录");
        }

        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        List<Map<String, Object>> bought = orderMapper.findByBuyerId(userId);

        if (bought != null) {
            for (Map<String, Object> order : bought) {
                addContact(result, seen, order, "sellerId", "sellerName", "我是买家");
            }
        }

        List<Map<String, Object>> sold = orderMapper.findBySellerId(userId);

        if (sold != null) {
            for (Map<String, Object> order : sold) {
                addContact(result, seen, order, "buyerId", "buyerName", "我是卖家");
            }
        }

        // 新的商品在前
        result.sort((x, y) -> toInt(y.get("goodsId")) - toInt(x.get("goodsId")));

        return result;
    }


    // 把一条订单映射为联系人候选，goodsId + otherUserId 去重
    private void addContact(
            List<Map<String, Object>> result,
            Set<String> seen,
            Map<String, Object> order,
            String idKey,
            String nameKey,
            String role) {

        Object otherId = order.get(idKey);
        Object goodsId = order.get("goodsId");

        String dedupKey = toInt(goodsId) + "-" + toInt(otherId);

        if (seen.contains(dedupKey)) {
            return;
        }

        seen.add(dedupKey);

        Map<String, Object> contact = new LinkedHashMap<>();
        contact.put("otherUserId", otherId == null ? null : toInt(otherId));
        contact.put("otherUserName", order.get(nameKey));
        contact.put("goodsId", goodsId == null ? null : toInt(goodsId));
        contact.put("goodsName", order.get("goodsName"));
        contact.put("price", order.get("price"));
        contact.put("imageUrl", order.get("imageUrl"));
        contact.put("role", role);

        result.add(contact);
    }


    // 数值兜底转换：null → 0，Number 直接取 intValue，其余尝试 parse
    private int toInt(Object value) {

        if (value == null) {
            return 0;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
