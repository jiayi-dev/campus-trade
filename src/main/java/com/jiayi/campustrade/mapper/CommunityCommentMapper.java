package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.CommunityComment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommunityCommentMapper {

    // 查询某条动态的全部评论（LEFT JOIN user 带出评论者昵称，按时间正序）
    List<CommunityComment> findByPostId(Integer postId);

    // 新增评论
    int insert(CommunityComment comment);
}
