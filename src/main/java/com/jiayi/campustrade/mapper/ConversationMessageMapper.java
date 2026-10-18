package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.ConversationMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI 对话消息 Mapper（V1.2 Day2.5）
 */
public interface ConversationMessageMapper {

    // 新增消息
    int insert(ConversationMessage message);

    // 查询某对话全部消息，按时间升序
    List<ConversationMessage> findByConversationId(@Param("conversationId") Integer conversationId);

    // 删除某对话全部消息（删对话前先清消息）
    int deleteByConversationId(@Param("conversationId") Integer conversationId);
}
