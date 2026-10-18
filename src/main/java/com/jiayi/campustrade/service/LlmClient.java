package com.jiayi.campustrade.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiayi.campustrade.config.AiProperties;
import com.jiayi.campustrade.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * OpenAI-Compatible LLM 客户端（B3 流式问答 / B9 Tool Calling）
 *
 * 职责：
 *   1. 拼装 /chat/completions 请求并解析 SSE 响应（JDK HttpClient + Jackson，无重型 AI 框架）
 *   2. {@link #chatStream}：B3 纯文本流式问答（旧契约，保留不动）
 *   3. {@link #chatDecisionStream}：B9 第一轮——流式直答内容实时透传；
 *      若模型决定调用工具，在 finish_reason=tool_calls 时收集增量 tool_calls 片段并返回
 *   4. {@link #chatFinalStream}：B9 第二轮——携带工具执行结果，流式生成最终自然语言回答
 *
 * 工具调用协议以 DeepSeek/OpenAI Chat Completion 为准：
 *   请求 tools=[{type:function,function:{name,description,parameters}}]、tool_choice=auto；
 *   流式响应 delta.tool_calls 按 index 分片（首片带 id/function.name，后续片追加 function.arguments）。
 */
@Component
public class LlmClient {

    private final AiProperties props;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public LlmClient(AiProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.getTimeout()))
                .build();
    }

    // =========================
    // B3：纯文本流式问答（契约保持不变）
    // =========================

    /**
     * 流式调用大模型
     *
     * @param systemPrompt   系统提示词
     * @param userMessage    用户本轮输入
     * @param history        历史对话（role/content），可为空
     * @param chunkConsumer  每个内容片段的回调（已去掉 SSE 协议层，纯文本增量）
     * @return 完整回答文本（所有片段拼接）
     */
    public String chatStream(String systemPrompt,
                             String userMessage,
                             List<Map<String, String>> history,
                             Consumer<String> chunkConsumer) {

        if (!props.isConfigured()) {
            throw new BusinessException("AI 模型未配置 API Key");
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (history != null) {
            for (Map<String, String> entry : history) {
                Map<String, Object> copy = new LinkedHashMap<>();
                copy.putAll(entry);
                messages.add(copy);
            }
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> body = baseBody(messages);
        return executeStream(body, chunkConsumer, null);
    }

    // =========================
    // B9：Tool Calling 两轮编排
    // =========================

    /** 模型要求执行的一次函数调用（id 用于第二轮 tool 消息配对） */
    public static class ToolCall {
        private final String id;
        private final String name;
        private final String arguments;

        public ToolCall(String id, String name, String arguments) {
            this.id = id;
            this.name = name;
            this.arguments = arguments;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getArguments() { return arguments; }
    }

    /** 第一轮决策结果：toolCalls 非空表示模型要求调用工具；否则 content 为直答全文 */
    public static class Decision {
        private final List<ToolCall> toolCalls;
        private final String content;

        public Decision(List<ToolCall> toolCalls, String content) {
            this.toolCalls = toolCalls;
            this.content = content;
        }

        public List<ToolCall> getToolCalls() { return toolCalls; }
        public String getContent() { return content; }
    }

    /**
     * B9 第一轮：带工具定义的流式决策。
     *
     * 直答内容在到达时通过 liveContent 实时透传给用户（保留 B3 逐段流式体验）；
     * 若最终 finish_reason=tool_calls，则返回收集到的工具调用（content 为可能存在的前导文本，
     * 通常为空）。
     *
     * @param history 本轮之前的历史消息（role/content 文本消息，由调用方裁剪）
     * @param toolsSpec 下发给模型的工具定义；空列表表示本轮无可用工具（退化为纯流式问答）
     */
    public Decision chatDecisionStream(String systemPrompt,
                                       String userMessage,
                                       List<Map<String, Object>> history,
                                       List<Map<String, Object>> toolsSpec,
                                       Consumer<String> liveContent) {

        if (!props.isConfigured()) {
            throw new BusinessException("AI 模型未配置 API Key");
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (history != null) {
            messages.addAll(history);
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> body = baseBody(messages);
        if (toolsSpec != null && !toolsSpec.isEmpty()) {
            body.put("tools", toolsSpec);
            body.put("tool_choice", "auto");
        }

        StringBuilder contentBuffer = new StringBuilder();
        Map<Integer, ToolCallAccumulator> toolAccumulator = new TreeMap<>();

        executeStream(body, delta -> {
            contentBuffer.append(delta);
            liveContent.accept(delta);
        }, toolAccumulator);

        if (!toolAccumulator.isEmpty()) {
            List<ToolCall> calls = new ArrayList<>();
            for (ToolCallAccumulator acc : toolAccumulator.values()) {
                String callId = acc.id != null ? acc.id : ("call_" + System.nanoTime());
                String callName = acc.name != null ? acc.name : "";
                calls.add(new ToolCall(callId, callName, acc.arguments.toString()));
            }
            return new Decision(calls, contentBuffer.toString());
        }

        // 理论上 finishReason 应为 stop；兜底：没有工具调用即视为直答
        return new Decision(null, contentBuffer.toString());
    }

    /**
     * B9 第二轮：携带工具结果流式生成最终回答（不再下发 tools，避免模型循环调用）。
     *
     * @param turnMessages 本轮完整消息：user + assistant(tool_calls) + tool(结果)，不含 system
     * @return 最终回答全文
     */
    public String chatFinalStream(String systemPrompt,
                                  List<Map<String, Object>> turnMessages,
                                  Consumer<String> chunkConsumer) {

        if (!props.isConfigured()) {
            throw new BusinessException("AI 模型未配置 API Key");
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(turnMessages);

        return executeStream(baseBody(messages), chunkConsumer, null);
    }

    // =========================
    // 内部：HTTP + SSE 解析
    // =========================

    private Map<String, Object> baseBody(List<Map<String, Object>> messages) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.getModel());
        body.put("messages", messages);
        body.put("stream", true);
        body.put("max_tokens", props.getMaxTokens());
        body.put("temperature", props.getTemperature());
        return body;
    }

    /** 工具调用分片累加器（流式 delta.tool_calls 按 index 分片到达） */
    private static class ToolCallAccumulator {
        String id;
        String name;
        final StringBuilder arguments = new StringBuilder();
    }

    /**
     * 执行一次流式请求并解析 SSE。
     *
     * @param body             请求体
     * @param contentConsumer  文本增量回调
     * @param toolAccumulator  非 null 时收集 tool_calls 分片；为 null 表示本轮不涉及工具
     */
    private String executeStream(Map<String, Object> body,
                                 Consumer<String> contentConsumer,
                                 Map<Integer, ToolCallAccumulator> toolAccumulator) {

        try {
            String requestBody = objectMapper.writeValueAsString(body);
            String url = props.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(props.getTimeout()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new BusinessException("模型接口返回错误 (" + response.statusCode() + ")：" + errBody);
            }

            // 累积实际内容文本（finishReason 只是 LLM 的结束信号，不是回答内容）
            StringBuilder contentBuilder = new StringBuilder();
            String finishReason = null;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(response.body(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || !line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    finishReason = parseChunk(data,
                            text -> {
                                contentBuilder.append(text);
                                contentConsumer.accept(text);
                            },
                            toolAccumulator, finishReason);
                }
            }
            // 返回累积的文本内容，而非 finishReason（后者是 "stop"/"tool_calls" 等信号）
            return contentBuilder.toString();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("调用 AI 模型失败：" + e.getMessage());
        }
    }

    /**
     * 解析单个 SSE data JSON：
     * 提取 choices[0].finish_reason / delta.content / delta.tool_calls 增量。
     * 单个 chunk 解析失败不中断整段流。
     */
    private String parseChunk(String data,
                              Consumer<String> contentConsumer,
                              Map<Integer, ToolCallAccumulator> toolAccumulator,
                              String previousFinishReason) {
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.size() == 0) {
                return previousFinishReason;
            }
            JsonNode choice = choices.get(0);

            JsonNode finishNode = choice.get("finish_reason");
            String finishReason = finishNode != null && !finishNode.isNull()
                    ? finishNode.asText() : previousFinishReason;

            JsonNode delta = choice.get("delta");
            if (delta == null) {
                return finishReason;
            }

            JsonNode content = delta.get("content");
            if (content != null && !content.isNull()) {
                String text = content.asText();
                if (!text.isEmpty()) {
                    contentConsumer.accept(text);
                }
            }

            if (toolAccumulator != null) {
                JsonNode toolCalls = delta.get("tool_calls");
                if (toolCalls != null && toolCalls.isArray()) {
                    for (JsonNode tc : toolCalls) {
                        int index = tc.path("index").asInt(0);
                        ToolCallAccumulator acc =
                                toolAccumulator.computeIfAbsent(index, k -> new ToolCallAccumulator());
                        if (tc.has("id") && !tc.get("id").isNull()) {
                            acc.id = tc.get("id").asText();
                        }
                        JsonNode function = tc.get("function");
                        if (function != null) {
                            JsonNode fnName = function.get("name");
                            if (fnName != null && !fnName.isNull()) {
                                acc.name = fnName.asText();
                            }
                            JsonNode args = function.get("arguments");
                            if (args != null && !args.isNull()) {
                                acc.arguments.append(args.asText());
                            }
                        }
                    }
                }
            }

            return finishReason;
        } catch (Exception e) {
            return previousFinishReason;
        }
    }
}
