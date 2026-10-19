package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户间交易私聊接口
 *
 * 统一挂在 /chat 下（/messages 已被 MessageController 的商品留言功能占用，不能冲突）。
 * 当前用户一律从 AuthContext 取，不信任前端传 userId。
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;


    // 会话列表：GET /chat/sessions
    @GetMapping("/sessions")
    public List<Map<String, Object>> sessions() {

        return chatService.getSessions();
    }


    // 消息历史：GET /chat/history?otherUserId=&goodsId=（goodsId 可为空）
    @GetMapping("/history")
    public List<Map<String, Object>> history(
            @RequestParam Integer otherUserId,
            @RequestParam(required = false) Integer goodsId) {

        return chatService.getHistory(otherUserId, goodsId);
    }


    // 发送消息：POST /chat/send，请求体 { toUserId, goodsId, content }
    // 业务失败以文本返回，与项目现有 Controller 风格一致
    @PostMapping("/send")
    public String send(@RequestBody Map<String, Object> body) {

        Object toUserIdObj = body.get("toUserId");
        Object goodsIdObj = body.get("goodsId");

        String content = body.get("content") == null
                ? null
                : String.valueOf(body.get("content"));

        if (toUserIdObj == null) {
            return "缺少接收人ID";
        }

        Integer toUserId;
        Integer goodsId = null;

        try {
            toUserId = Integer.valueOf(String.valueOf(toUserIdObj));

            if (goodsIdObj != null && !"".equals(String.valueOf(goodsIdObj))) {
                goodsId = Integer.valueOf(String.valueOf(goodsIdObj));
            }
        } catch (NumberFormatException e) {
            return "参数格式不正确";
        }

        try {
            return chatService.send(toUserId, goodsId, content);
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }


    // 未读总数：GET /chat/unread → { total: n }
    @GetMapping("/unread")
    public Map<String, Object> unread() {

        Map<String, Object> result = new HashMap<>();
        result.put("total", chatService.countUnread());

        return result;
    }


    // 联系人候选（发起新会话）：GET /chat/contacts
    @GetMapping("/contacts")
    public List<Map<String, Object>> contacts() {

        return chatService.getContacts();
    }
}
