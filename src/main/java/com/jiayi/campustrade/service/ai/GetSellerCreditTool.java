package com.jiayi.campustrade.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiayi.campustrade.entity.User;
import com.jiayi.campustrade.mapper.UserMapper;
import com.jiayi.campustrade.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * getSellerCredit：查询卖家公开信用/信誉画像（B9）
 *
 * 数据完全复用 B2 既有实现 EvaluationService.findSellerProfile：
 *   信用分 = 100 + 已完成订单数 × 20（OrderMapper.countCompletedByUserId），
 *   信誉等级、平均评分、评价总数等口径与个人中心、商品详情页卖家画像完全一致，
 *   本工具不重新计算任何信用规则。
 *
 * 与 getOrderStatus 的权限区分：
 *   卖家信誉是公开信息（GET /api/evaluations/seller/{id} 对游客开放），本工具游客可用，
 *   但只返回画像白名单字段，手机号/邮箱等隐私字段一律不输出。
 */
@Component
public class GetSellerCreditTool implements AiTool {

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String name() {
        return "getSellerCredit";
    }

    @Override
    public String description() {
        return "查询某位卖家的公开信用信息：信用分、信用等级、已完成交易数、平均评分、评价数。"
                + "用户询问「这个卖家靠谱吗」「卖家15信用多少」「某用户信誉怎么样」时调用。"
                + "可通过 sellerId（数字）或 sellerName（卖家用户名）指定卖家。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> sellerId = new LinkedHashMap<>();
        sellerId.put("type", "integer");
        sellerId.put("description", "卖家用户ID（数字）。商品搜索结果中的 sellerId 可直接使用；与 sellerName 二选一");

        Map<String, Object> sellerName = new LinkedHashMap<>();
        sellerName.put("type", "string");
        sellerName.put("description", "卖家用户名；不知道ID时可用用户名查询，与 sellerId 二选一");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("sellerId", sellerId);
        properties.put("sellerName", sellerName);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new String[0]);
        return schema;
    }

    @Override
    public String runningLabel(JsonNode args) {
        return "正在查询卖家信用…";
    }

    @Override
    public ToolResult execute(JsonNode args, Integer currentUserId) {

        User seller = null;

        JsonNode idNode = args.get("sellerId");
        if (idNode != null && idNode.isNumber()) {
            seller = userMapper.findById(idNode.intValue());
        } else {
            JsonNode nameNode = args.get("sellerName");
            if (nameNode != null && nameNode.isTextual() && !nameNode.asText().trim().isEmpty()) {
                seller = userMapper.findByUsername(nameNode.asText().trim());
            }
        }

        if (seller == null) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", "SELLER_NOT_FOUND");
            body.put("message", "没有找到该卖家，请让用户确认卖家ID或用户名是否正确");
            return ToolResult.fail(toJson(body), "未找到该卖家");
        }

        // 直接复用 B2 公开画像（与商品详情页卖家信誉同一数据源、同一公式）
        Map<String, Object> profile = evaluationService.findSellerProfile(seller.getUserId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sellerId", seller.getUserId());
        result.put("username", seller.getUsername());
        result.put("role", seller.getRole());
        result.put("creditScore", profile.get("creditScore"));
        result.put("creditLevel", profile.get("creditLevel"));
        result.put("completedCount", profile.get("completedCount"));
        result.put("avgScore", profile.get("avgScore"));
        result.put("evaluationCount", profile.get("totalCount"));

        String label = "已获取「" + seller.getUsername() + "」的信用信息";
        return ToolResult.ok(toJson(result), label);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{\"error\":\"SERIALIZE_FAILED\"}";
        }
    }
}
