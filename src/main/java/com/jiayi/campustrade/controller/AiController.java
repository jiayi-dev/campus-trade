package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.config.AiProperties;
import com.jiayi.campustrade.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 校园 AI 助手入口
 *
 * 两个接口：
 *   POST /api/ai/chat         —— 非流式（旧契约，降级/调试用），返回 { reply, suggestions }
 *   POST /api/ai/chat/stream  —— SSE 流式（B3 主链路），逐段返回文本增量
 *
 * SSE 事件：
 *   delta: 文本增量片段
 *   done:  结束标记 [DONE]
 *   error: 错误信息
 *
 * 接口在 AuthInterceptor 中对游客开放（页面公开、基础问答无用户数据）。
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    private static final int MAX_MESSAGE_LENGTH = 500;

    @Autowired
    private AiService aiService;

    @Autowired
    private AiProperties aiProperties;

    /**
     * 非流式对话（保留旧契约）
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> body) {

        String message = parseMessage(body);
        String conversationId = parseConversationId(body);

        return aiService.chat(message, conversationId);
    }

    /**
     * SSE 流式对话（B3）
     *
     * 使用 POST 而非 GET：需要在请求体中携带 message + conversationId，
     * 前端通过 fetch + ReadableStream 读取 SSE 流（浏览器 EventSource 仅支持 GET，
     * 此处用 fetch 实现流式 POST，更灵活且与现有 axios 鉴权头一致）。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> body) {

        String message = parseMessage(body);
        String conversationId = parseConversationId(body);

        // 在请求线程内捕获登录身份：SSE 处理被放到独立线程执行，
        // AuthContext 的 ThreadLocal 不会向新线程传递，必须在此显式取值并传入。
        // 该 ID 是 AI 查询个人订单时唯一信任的身份来源（游客为 null）。
        Integer currentUserId = AuthContext.getUserId();

        SseEmitter emitter = new SseEmitter(aiProperties.getSseTimeout());

        // 前端断开时释放资源，避免后台线程无限运行
        emitter.onCompletion(() -> {});
        emitter.onTimeout(() -> emitter.complete());
        emitter.onError(t -> emitter.complete());

        // 异步执行流式调用，避免阻塞 Tomcat 请求线程
        new Thread(() -> aiService.chatStream(message, conversationId, currentUserId, emitter),
                "ai-stream-" + conversationId).start();

        return emitter;
    }

    // ---------- 参数解析 ----------

    private String parseMessage(Map<String, Object> body) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "消息内容不能为空");
        }
        Object messageObj = body.get("message");
        String message = messageObj == null ? "" : String.valueOf(messageObj).trim();
        if (message.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "消息内容不能为空");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "消息内容过长，请控制在 " + MAX_MESSAGE_LENGTH + " 字以内");
        }
        return message;
    }

    private String parseConversationId(Map<String, Object> body) {
        if (body == null) return null;
        Object idObj = body.get("conversationId");
        return idObj == null ? null : String.valueOf(idObj);
    }
}
