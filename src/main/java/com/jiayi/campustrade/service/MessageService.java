package com.jiayi.campustrade.service;

import com.jiayi.campustrade.entity.Goods;
import com.jiayi.campustrade.mapper.GoodsMapper;
import com.jiayi.campustrade.mapper.MessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    // 查询某个商品的留言列表
    public List<Map<String, Object>> findByGoodsId(Integer goodsId) {
        return messageMapper.findByGoodsId(goodsId);
    }

    // 发布留言：校验商品存在与内容非空
    // 返回 null 表示成功；返回字符串表示失败原因（与项目内注册/批量接口风格一致）
    public String addMessage(Integer goodsId, Integer userId, String content) {

        if (content == null || content.trim().isEmpty()) {
            return "留言内容不能为空";
        }

        if (content.trim().length() > 200) {
            return "留言内容不能超过200字";
        }

        Goods goods = goodsMapper.findById(goodsId);

        if (goods == null) {
            return "商品不存在或已被删除";
        }

        messageMapper.insert(goodsId, userId, content.trim());

        return null;
    }
}
