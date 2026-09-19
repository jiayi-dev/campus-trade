package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    // 查询某个商品的留言列表：GET /messages/goods/11
    @GetMapping("/goods/{goodsId}")
    public List<Map<String, Object>> findByGoodsId(
            @PathVariable Integer goodsId) {

        return messageService.findByGoodsId(goodsId);
    }

    // 发布留言：POST /messages，请求体 { goodsId, content }
    // 留言人 userId 从登录上下文获取，不信任前端传参
    @PostMapping
    public String add(@RequestBody Map<String, Object> body) {

        Integer userId = AuthContext.getUserId();

        Object goodsIdObj = body.get("goodsId");
        String content = body.get("content") == null
                ? null
                : String.valueOf(body.get("content"));

        if (goodsIdObj == null) {
            return "缺少商品ID";
        }

        Integer goodsId;

        try {
            goodsId = Integer.valueOf(String.valueOf(goodsIdObj));
        } catch (NumberFormatException e) {
            return "商品ID格式不正确";
        }

        String error = messageService.addMessage(goodsId, userId, content);

        return error == null ? "留言成功" : error;
    }
}
