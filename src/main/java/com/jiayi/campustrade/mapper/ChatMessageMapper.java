package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper {

    // 会话列表：一次查询拿到每个会话的最后一条消息、对方信息、商品信息与未读数
    List<Map<String, Object>> findSessions(@Param("userId") Integer userId);

    // 某会话的全部消息（按 chat_id 升序，即时间正序）
    List<Map<String, Object>> findBySession(@Param("sessionKey") String sessionKey);

    // 发送消息，自增主键回填到 chatId
    int insert(ChatMessage chatMessage);

    // 把「发给我的未读消息」标记为已读（只影响 to_user_id = 当前用户的行）
    int markRead(
            @Param("sessionKey") String sessionKey,
            @Param("userId") Integer userId
    );

    // 当前用户的未读总数
    int countUnread(@Param("userId") Integer userId);
}
