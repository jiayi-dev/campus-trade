package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 校园 AI 助手入口
 *
 * 当前只暴露一个对话接口，前端只与该接口对话：
 *   POST /api/ai/chat  { "message": "...", "conversationId": "可选" }
 *   -> { "reply": "...", "suggestions": ["..."] }
 *
 * 该接口在 AuthInterceptor 中对游客开放（页面公开、基础问答无用户数据）。
 * conversationId 当前仅透传给 Service，为未来多轮对话/会话持久化预留，本批不落库。
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    // 单条消息最大长度：防止异常超长输入，与课程项目阶段的文本咨询场景匹配
    private static final int MAX_MESSAGE_LENGTH = 500;

    @Autowired
    private AiService aiService;

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> body) {

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

        Object conversationIdObj = body.get("conversationId");
        String conversationId = conversationIdObj == null
                ? null
                : String.valueOf(conversationIdObj);

        Map<String, Object> reply = aiService.chat(message, conversationId);

        // 统一再包一层稳定字段顺序，便于前端直接消费
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reply", reply.get("reply"));
        result.put("suggestions", reply.get("suggestions"));
        return result;
    }
}
