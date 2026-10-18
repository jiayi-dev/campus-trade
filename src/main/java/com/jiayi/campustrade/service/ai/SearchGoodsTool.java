package com.jiayi.campustrade.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiayi.campustrade.entity.Category;
import com.jiayi.campustrade.entity.Goods;
import com.jiayi.campustrade.service.CategoryService;
import com.jiayi.campustrade.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * searchGoods：按自然语言条件查询平台在售商品（B9）
 *
 * 完全复用 B1 商品业务链：GoodsService.findPage / count → GoodsMapper.findPage，
 * 底层 SQL 已固定 g.status = 1（只返回在售商品），与商品中心页面同一套筛选逻辑，
 * 不新建查询、不绕过任何既有过滤条件。
 *
 * 公开工具：商品列表对游客同样开放（与 GET /api/goods/page 的公开口径一致）。
 */
@Component
public class SearchGoodsTool implements AiTool {

    /** 单次喂给模型的商品条数（控制 token 预算），总数仍如实返回 */
    private static final int PAGE_SIZE = 8;

    private static final Map<Integer, String> CONDITION_TEXT = new LinkedHashMap<>();
    static {
        CONDITION_TEXT.put(1, "全新");
        CONDITION_TEXT.put(2, "99新");
        CONDITION_TEXT.put(3, "95新");
        CONDITION_TEXT.put(4, "9成新");
        CONDITION_TEXT.put(5, "8成新及以下");
    }

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String name() {
        return "searchGoods";
    }

    @Override
    public String description() {
        return "按关键词、分类、价格区间、成色、校区等条件查询校园二手平台上当前在售的商品。"
                + "用户想找/买/推荐具体商品（如「有没有二手自行车」「100元以内的篮球」）时调用。"
                + "只能查询在售商品，不包含已下架、已售出商品。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> keyword = new LinkedHashMap<>();
        keyword.put("type", "string");
        keyword.put("description", "商品关键词，如 自行车、篮球、高数教材；用户没有明确关键词时省略");

        Map<String, Object> category = new LinkedHashMap<>();
        category.put("type", "string");
        category.put("description", "商品分类名称，如 数码、教材、生活用品；无法判断分类时省略，不要猜测分类ID");

        Map<String, Object> minPrice = new LinkedHashMap<>();
        minPrice.put("type", "number");
        minPrice.put("description", "最低价格（元，含），可选");

        Map<String, Object> maxPrice = new LinkedHashMap<>();
        maxPrice.put("type", "number");
        maxPrice.put("description", "最高价格（元，含），可选");

        Map<String, Object> condition = new LinkedHashMap<>();
        condition.put("type", "integer");
        condition.put("enum", List.of(1, 2, 3, 4, 5));
        condition.put("description", "成色：1=全新，2=99新，3=95新，4=9成新，5=8成新及以下；可选");

        Map<String, Object> campus = new LinkedHashMap<>();
        campus.put("type", "string");
        campus.put("description", "交易校区，如 东区、西区；用户未指定时省略");

        Map<String, Object> sortBy = new LinkedHashMap<>();
        sortBy.put("type", "string");
        sortBy.put("enum", List.of("latest", "price_asc", "price_desc", "hot"));
        sortBy.put("description", "排序：latest最新发布(默认)、price_asc价格从低到高、price_desc价格从高到低、hot热度优先");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("keyword", keyword);
        properties.put("category", category);
        properties.put("minPrice", minPrice);
        properties.put("maxPrice", maxPrice);
        properties.put("conditionLevel", condition);
        properties.put("campus", campus);
        properties.put("sortBy", sortBy);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new ArrayList<>());
        return schema;
    }

    @Override
    public String runningLabel(JsonNode args) {
        return "正在查询商品…";
    }

    @Override
    public ToolResult execute(JsonNode args, Integer currentUserId) {
        String keyword = textArg(args, "keyword");
        String categoryName = textArg(args, "category");
        String campus = textArg(args, "campus");
        String sortBy = textArg(args, "sortBy");
        if (sortBy == null) {
            sortBy = "latest";
        }

        BigDecimal minPrice = decimalArg(args, "minPrice");
        BigDecimal maxPrice = decimalArg(args, "maxPrice");
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            minPrice = BigDecimal.ZERO;
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            return fail("价格区间不能为负数");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            return fail("最低价格高于最高价格，价格区间不合法");
        }

        Integer conditionLevel = intArg(args, "conditionLevel");
        if (conditionLevel != null && (conditionLevel < 1 || conditionLevel > 5)) {
            conditionLevel = null;
        }

        // 分类名 → 分类ID：复用现有分类数据，不新增查询
        Integer categoryId = null;
        String categoryWarning = null;
        if (categoryName != null) {
            Category matched = matchCategory(categoryName);
            if (matched != null) {
                categoryId = matched.getCategoryId();
            } else {
                categoryWarning = "平台没有名为「" + categoryName + "」的分类，已忽略分类条件按其他条件搜索";
            }
        }

        List<Goods> page = goodsService.findPage(
                keyword, categoryId, minPrice, maxPrice, conditionLevel, campus, null, sortBy, 1, PAGE_SIZE);
        int total = goodsService.count(
                keyword, categoryId, minPrice, maxPrice, conditionLevel, campus, null);

        List<Map<String, Object>> goodsList = new ArrayList<>();
        for (Goods g : page) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("goodsId", g.getGoodsId());
            item.put("name", g.getGoodsName());
            item.put("price", g.getPrice());
            item.put("conditionLevel", g.getConditionLevel());
            item.put("conditionText", CONDITION_TEXT.getOrDefault(g.getConditionLevel(), "未知成色"));
            // 主图地址：由 GoodsMapper.findPage 的 LEFT JOIN goods_image 填充，供前端商品卡片展示
            item.put("imageUrl", g.getImageUrl());
            item.put("campus", g.getCampus());
            item.put("location", g.getLocation());
            item.put("sellerId", g.getUserId());
            item.put("favoriteCount", g.getFavoriteCount());
            goodsList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("shown", goodsList.size());
        result.put("keyword", keyword);
        result.put("goods", goodsList);
        if (categoryWarning != null) {
            result.put("warning", categoryWarning);
        }
        if (total > goodsList.size()) {
            result.put("hint", "符合条件的商品较多，仅给出最新的 " + PAGE_SIZE
                    + " 件，可让用户补充关键词或价格区间缩小范围");
        }

        String label = total > 0 ? "已找到 " + total + " 件商品" : "没有找到符合条件的商品";
        return ToolResult.ok(toJson(result), label);
    }

    /**
     * 提取前端商品卡片所需的白名单字段（AI 导购卡片数据源）。
     *
     * 只保留渲染必需字段：goodsId / name / price / conditionLevel / conditionText /
     * imageUrl / campus；sellerId、location、favoriteCount 等不下发浏览器。
     * 未命中（goods 为空）或解析失败时返回 null，前端不渲染卡片，绝不伪造商品。
     * 数值字段直接复用 JsonNode，保证 Long/BigDecimal 类型精度不丢失。
     */
    @Override
    public Map<String, Object> frontendPayload(String contentJson) {
        if (contentJson == null || contentJson.isEmpty()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(contentJson);
            JsonNode goods = root.get("goods");
            if (goods == null || !goods.isArray() || goods.isEmpty()) {
                return null;
            }
            List<Map<String, Object>> cards = new ArrayList<>();
            for (JsonNode node : goods) {
                Map<String, Object> card = new LinkedHashMap<>();
                card.put("goodsId", node.path("goodsId"));
                card.put("name", node.path("name").asText(""));
                card.put("price", node.path("price"));
                card.put("conditionLevel", node.path("conditionLevel"));
                card.put("conditionText", node.path("conditionText").asText(""));
                // 无图商品不下发该字段，前端渲染占位色块
                if (!node.path("imageUrl").isNull() && !node.path("imageUrl").asText().isEmpty()) {
                    card.put("imageUrl", node.path("imageUrl").asText());
                }
                if (!node.path("campus").isNull() && !node.path("campus").asText().isEmpty()) {
                    card.put("campus", node.path("campus").asText());
                }
                cards.add(card);
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("goods", cards);
            // 命中总数（可能多于卡片数），供前端如实提示「共 N 件，展示前 M 件」
            payload.put("total", root.path("total"));
            return payload;
        } catch (Exception e) {
            // 展示数据提取失败不影响主链路：返回 null 即不下发，文字回答照常
            return null;
        }
    }

    /**
     * 分类名匹配：先精确匹配，再双向包含（用户说「数码类」也能匹配「数码」）。
     * 匹配不到返回 null，由调用方决定忽略还是提示。
     */
    private Category matchCategory(String input) {
        List<Category> all = categoryService.findAll();
        if (all == null) {
            return null;
        }
        Category fallback = null;
        for (Category c : all) {
            String name = c.getCategoryName();
            if (name == null) {
                continue;
            }
            if (name.equals(input)) {
                return c;
            }
            if (fallback == null
                    && (name.contains(input) || input.contains(name))) {
                fallback = c;
            }
        }
        return fallback;
    }

    private String textArg(JsonNode args, String field) {
        JsonNode node = args.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText().trim();
        return value.isEmpty() ? null : value;
    }

    private Integer intArg(JsonNode args, String field) {
        JsonNode node = args.get(field);
        if (node == null || node.isNull() || !node.isNumber()) {
            return null;
        }
        return node.intValue();
    }

    private BigDecimal decimalArg(JsonNode args, String field) {
        JsonNode node = args.get(field);
        if (node == null || node.isNull() || !node.isNumber()) {
            return null;
        }
        return node.decimalValue();
    }

    private ToolResult fail(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "INVALID_ARGUMENT");
        body.put("message", message);
        return ToolResult.fail(toJson(body), "查询条件有误");
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{\"error\":\"SERIALIZE_FAILED\"}";
        }
    }
}
