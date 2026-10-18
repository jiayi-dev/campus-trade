package com.jiayi.campustrade.service.ai;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 工具注册中心（B9）
 *
 * Spring 自动注入所有 {@link AiTool} 实现，按工具名索引；
 * 同时负责按「当前登录身份」裁剪可见工具：需要登录态的工具对游客不注册，
 * 模型在游客会话中根本拿不到 getOrderStatus 的定义，无法产生越权调用。
 */
@Component
public class ToolRegistry {

    private final Map<String, AiTool> toolsByName = new ConcurrentHashMap<>();

    public ToolRegistry(List<AiTool> tools) {
        for (AiTool tool : tools) {
            toolsByName.put(tool.name(), tool);
        }
    }

    /**
     * 生成下发给模型的 tools 数组（OpenAI function calling 格式）。
     *
     * @param currentUserId 当前登录用户ID（游客为 null）
     */
    public List<Map<String, Object>> specsForUser(Integer currentUserId) {
        List<Map<String, Object>> specs = new ArrayList<>();
        for (AiTool tool : toolsByName.values()) {
            if (tool.requiresLogin() && currentUserId == null) {
                continue;
            }
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", tool.name());
            function.put("description", tool.description());
            function.put("parameters", tool.parametersSchema());

            Map<String, Object> spec = new LinkedHashMap<>();
            spec.put("type", "function");
            spec.put("function", function);
            specs.add(spec);
        }
        return specs;
    }

    /** 按模型返回的函数名查找工具；未注册（幻觉工具名）时返回 null */
    public AiTool find(String name) {
        return name == null ? null : toolsByName.get(name);
    }
}
