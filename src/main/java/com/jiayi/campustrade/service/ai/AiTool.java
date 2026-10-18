package com.jiayi.campustrade.service.ai;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * AI 助手可调用的业务工具抽象（B9：Tool Calling）
 *
 * 设计原则：
 * 1. 一个实现 = 一个只读业务查询，直接复用现有 Service/Mapper，不另起查询体系；
 *2. 工具不接收「当前用户ID」入参——currentUserId 由编排层从登录 Token 解析后显式传入，
 *    模型与用户在对话中提到的任何 userId 都不能作为身份依据，从结构上杜绝越权；
 * 3. 参数契约用 OpenAI function calling 的 JSON Schema 描述，供 LlmClient 原样下发给模型；
 * 4. 后续新增工具只需实现本接口并交给 Spring 托管，ToolRegistry 自动收集，无需改动编排逻辑。
 */
public interface AiTool {

    /** 工具名（下发给模型的 function.name），全局唯一，仅允许字母/数字/下划线 */
    String name();

    /** 工具用途说明（下发给模型的 function.description），决定模型何时选择该工具 */
    String description();

    /** 入参 JSON Schema（function.parameters），无入参时返回 {"type":"object","properties":{}} */
    Map<String, Object> parametersSchema();

    /**
     * 是否需要登录态。
     * 返回 true 的工具只会在请求携带有效 Token 时注册给模型（游客看不到也调不到）；
     * execute 内仍会做一次防御式校验。
     */
    default boolean requiresLogin() {
        return false;
    }

    /** 工具开始执行时前端展示的真实状态文案，如「正在查询商品…」 */
    String runningLabel(JsonNode args);

    /**
     * 执行工具（只读）。
     *
     * @param args          模型给出的参数 JSON（编排层保证非 null，但可能是空对象）
     * @param currentUserId 当前登录用户ID，来源是登录 Token（游客为 null），永不取自 args
     * @return 结构化执行结果（contentJson 回灌模型，doneLabel 用于前端完成态）
     */
    ToolResult execute(JsonNode args, Integer currentUserId);

    /**
     * 从执行结果 contentJson 中提取「可安全下发浏览器」的展示数据（白名单）。
     *
     * 默认返回 null：该工具没有附加展示数据，前端只渲染 doneLabel。
     * 有展示需求的工具（如 searchGoods）重写本方法，且必须重新挑选字段，
     * 不得原样透传 contentJson——其中可能包含 sellerId、location 等
     * 对模型有用但不必暴露给浏览器的字段。
     * 返回 Map 的键不得与工具事件保留键冲突（key/tool/phase/label/ok）。
     *
     * 注意：实现必须无状态（工具为 Spring 单例，禁止把结果暂存成员变量）。
     */
    default Map<String, Object> frontendPayload(String contentJson) {
        return null;
    }
}
