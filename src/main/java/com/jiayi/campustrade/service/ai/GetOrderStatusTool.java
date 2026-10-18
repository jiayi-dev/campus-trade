package com.jiayi.campustrade.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiayi.campustrade.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * getOrderStatus：查询「当前登录用户自己」的订单（B9）
 *
 * 安全模型（本批次重点）：
 * 1. requiresLogin=true：游客会话中该工具不会被注册给模型，无法触发；
 * 2. 工具参数 schema 中【没有 userId 参数】，模型只能传视角(买家/卖家)与状态过滤，
 *    用户在对话里说「查用户5的订单」时，5 无法进入查询条件；
 * 3. userId 唯一来源是编排层从 Authorization Token 解析出的 currentUserId，
 *    最终复用 OrderService.findBuyerOrders / findSellerOrders（与 /orders/buyer、
 *    /orders/seller 页面接口同一条数据链），天然只能查到本人参与的订单；
 * 4. 只读：不提供任何状态流转能力。
 */
@Component
public class GetOrderStatusTool implements AiTool {

    /** 返回给模型的最大订单条数（底层 SQL 已按时间倒序） */
    private static final int LIMIT = 10;

    private static final Map<Integer, String> STATUS_TEXT = new LinkedHashMap<>();
    static {
        STATUS_TEXT.put(0, "待交易（等待卖家线下面交交付）");
        STATUS_TEXT.put(1, "待买家确认（卖家已交付，等待买家确认收货）");
        STATUS_TEXT.put(2, "已完成");
        STATUS_TEXT.put(3, "已取消");
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String name() {
        return "getOrderStatus";
    }

    @Override
    public String description() {
        return "查询当前登录用户自己的订单（我买到的 / 我卖出的），可按订单状态过滤。"
                + "仅当用户询问「我的订单」「我买的东西到哪了」「有人拍了我的商品吗」等本人订单问题时调用。"
                + "该工具只能查当前登录用户本人，无法查询其他用户的订单。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> role = new LinkedHashMap<>();
        role.put("type", "string");
        role.put("enum", List.of("buyer", "seller"));
        role.put("description", "订单视角：buyer=我买到的（默认），seller=我卖出的");

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("type", "integer");
        status.put("enum", List.of(0, 1, 2));
        status.put("description", "订单状态过滤：0=待交易，1=待买家确认，2=已完成；省略则返回全部状态");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("role", role);
        properties.put("orderStatus", status);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new ArrayList<>());
        return schema;
    }

    @Override
    public boolean requiresLogin() {
        return true;
    }

    @Override
    public String runningLabel(JsonNode args) {
        return "正在查询我的订单…";
    }

    @Override
    public ToolResult execute(JsonNode args, Integer currentUserId) {

        // 防御式二次校验：即使编排/注册出现疏漏，没有登录身份一律拒绝
        if (currentUserId == null) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", "NOT_LOGGED_IN");
            body.put("message", "查询订单需要登录，请引导用户先登录后再询问订单问题");
            return ToolResult.fail(toJson(body), "未登录，无法查询订单");
        }

        String role = "buyer";
        JsonNode roleNode = args.get("role");
        if (roleNode != null && roleNode.isTextual() && "seller".equalsIgnoreCase(roleNode.asText())) {
            role = "seller";
        }

        Integer statusFilter = null;
        JsonNode statusNode = args.get("orderStatus");
        if (statusNode != null && statusNode.isNumber()) {
            int s = statusNode.intValue();
            if (s >= 0 && s <= 2) {
                statusFilter = s;
            }
        }

        // userId 只取自 Token 链路的 currentUserId，args 中的任何身份字段都不参与查询
        List<Map<String, Object>> rows = "seller".equals(role)
                ? orderService.findSellerOrders(currentUserId)
                : orderService.findBuyerOrders(currentUserId);

        List<Map<String, Object>> orders = new ArrayList<>();
        int matched = 0;
        for (Map<String, Object> row : rows) {
            Integer orderStatus = toInt(row.get("orderStatus"));
            if (statusFilter != null && !statusFilter.equals(orderStatus)) {
                continue;
            }
            matched++;
            if (orders.size() >= LIMIT) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("orderId", toInt(row.get("orderId")));
            item.put("goodsId", toInt(row.get("goodsId")));
            item.put("goodsName", String.valueOf(row.get("goodsName")));
            item.put("price", row.get("price"));
            item.put("orderStatus", orderStatus);
            item.put("orderStatusText", STATUS_TEXT.getOrDefault(orderStatus, "未知状态"));
            item.put("createTime", String.valueOf(row.get("createTime")));
            item.put("campus", row.get("campus"));
            item.put("location", row.get("location"));
            // 买家视角展示卖家名，卖家视角展示买家名，不暴露多余身份信息
            item.put("counterpartName",
                    "seller".equals(role) ? row.get("buyerName") : row.get("sellerName"));
            orders.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", "seller".equals(role) ? "我卖出的" : "我买到的");
        result.put("total", matched);
        result.put("shown", orders.size());
        result.put("orders", orders);
        if (matched > LIMIT) {
            result.put("hint", "订单较多，仅展示最近 " + LIMIT + " 笔，可按状态进一步筛选");
        }

        String label = "查询完成，共 " + matched + " 笔订单";
        return ToolResult.ok(toJson(result), label);
    }

    private Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{\"error\":\"SERIALIZE_FAILED\"}";
        }
    }
}
