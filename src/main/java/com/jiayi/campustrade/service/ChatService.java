package com.jiayi.campustrade.service;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.entity.ChatMessage;
import com.jiayi.campustrade.entity.Orders;
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

    /**
     * 发送消息
     *
     * @param messageType text 文本 / image 图片 / order 订单卡片；为空按 text 处理
     * @param relatedId   order 消息必填，必须是当前用户作为买家或卖家参与的订单
     */
    public String send(Integer toUserId, Integer goodsId, String content,
                       String messageType, Integer relatedId) {

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

        String type = (messageType == null || messageType.trim().isEmpty())
                ? "text"
                : messageType.trim();

        if (!"text".equals(type) && !"image".equals(type) && !"order".equals(type)) {
            throw new BusinessException("不支持的消息类型");
        }

        String text = content == null ? "" : content.trim();

        if ("text".equals(type) && text.isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }

        if ("image".equals(type)) {
            // 只接受本地上传接口返回的地址或 http(s) 图片地址，
            // 避免把任意字符串塞进 <img src>
            boolean allowed = text.startsWith("/api/files/")
                    || text.startsWith("http://")
                    || text.startsWith("https://");

            if (!allowed) {
                throw new BusinessException("图片地址不合法");
            }

            relatedId = null;
        }

        if ("order".equals(type)) {
            if (relatedId == null) {
                throw new BusinessException("缺少关联订单ID");
            }

            Orders order = orderMapper.findById(relatedId);

            if (order == null) {
                throw new BusinessException("订单不存在");
            }

            boolean isBuyer = userId.equals(order.getBuyerId());
            boolean isSeller = userId.equals(order.getSellerId());

            if (!isBuyer && !isSeller) {
                throw new BusinessException("只能发送与自己相关的订单");
            }

            Integer counterparty = isBuyer ? order.getSellerId() : order.getBuyerId();

            if (!toUserId.equals(counterparty)) {
                throw new BusinessException("订单双方与当前会话不一致");
            }

            // 订单卡片正文只做会话列表预览，真实展示走 relatedId + 订单联查
            text = "订单 #" + relatedId;
        }

        if (text.length() > 500) {
            throw new BusinessException("消息内容不能超过 500 字");
        }

        ChatMessage message = new ChatMessage();
        message.setFromUserId(userId);
        message.setToUserId(toUserId);
        message.setGoodsId(goodsId);
        message.setContent(text);
        message.setMessageType(type);
        message.setRelatedId(relatedId);
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
