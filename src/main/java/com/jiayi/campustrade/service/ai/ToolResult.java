package com.jiayi.campustrade.service.ai;

/**
 * 工具执行结果（B9）
 *
 * - contentJson：回灌给 LLM 的结构化结果（JSON 字符串），模型据此组织自然语言回答；
 * - doneLabel：  工具执行结束后前端展示的真实状态文案，如「已找到 3 件商品」。
 *
 * 工具失败（未登录/参数非法/查无数据）也返回 ToolResult 而非抛异常：
 * 把错误以结构化 JSON 回灌模型，由模型用友好语言转述，避免整段 SSE 中断。
 */
public class ToolResult {

    private final boolean success;
    private final String contentJson;
    private final String doneLabel;

    private ToolResult(boolean success, String contentJson, String doneLabel) {
        this.success = success;
        this.contentJson = contentJson;
        this.doneLabel = doneLabel;
    }

    public static ToolResult ok(String contentJson, String doneLabel) {
        return new ToolResult(true, contentJson, doneLabel);
    }

    public static ToolResult fail(String contentJson, String doneLabel) {
        return new ToolResult(false, contentJson, doneLabel);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getContentJson() {
        return contentJson;
    }

    public String getDoneLabel() {
        return doneLabel;
    }
}
