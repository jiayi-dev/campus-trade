package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.Conversation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * AI 对话会话 Mapper（V1.2 Day2.5）
 */
public interface ConversationMapper {

    // 新建对话（返回自增 id）
    int insert(Conversation conversation);

    // 按主键查询（带 user_id 校验，防止越权）
    Conversation findByIdAndUser(@Param("id") Integer id, @Param("userId") Integer userId);

    // 查询用户的全部对话，按最近活跃时间降序
    List<Conversation> findByUserId(@Param("userId") Integer userId);

    // 更新对话标题
    int updateTitle(@Param("id") Integer id, @Param("title") String title);

    // 更新最近活跃时间
    int touchUpdateTime(@Param("id") Integer id);

    // 按主键删除（物理删除，消息表 ON DELETE 或手动删）
    int deleteById(@Param("id") Integer id);
}
