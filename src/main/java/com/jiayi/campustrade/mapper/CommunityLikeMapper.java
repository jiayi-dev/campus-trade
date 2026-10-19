package com.jiayi.campustrade.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommunityLikeMapper {

    // 查询某用户对某动态是否已点赞（>0 表示已点赞）
    int countByPostAndUser(
            @Param("postId") Integer postId,
            @Param("userId") Integer userId
    );

    // 新增点赞（唯一键 uk_post_user 保证一人一赞）
    int insert(
            @Param("postId") Integer postId,
            @Param("userId") Integer userId
    );

    // 取消点赞
    int delete(
            @Param("postId") Integer postId,
            @Param("userId") Integer userId
    );
}
