package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.CommunityPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommunityPostMapper {

    // 动态条件分页查询（LEFT JOIN user 带出作者昵称、LEFT JOIN community_like 带出当前用户点赞状态）
    // keyword 标题模糊 / topic 主题筛选 / sortBy：latest 最新 / hot 最热
    List<CommunityPost> findPage(
            @Param("keyword") String keyword,
            @Param("topic") String topic,
            @Param("sortBy") String sortBy,
            @Param("currentUserId") Integer currentUserId,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    // 查询符合条件的动态总数（与 findPage 同筛选条件，不 JOIN 提升性能）
    int count(
            @Param("keyword") String keyword,
            @Param("topic") String topic
    );

    // 根据ID查询动态详情（含作者昵称 / 点赞状态 / 关联商品名）
    CommunityPost findById(
            @Param("postId") Integer postId,
            @Param("currentUserId") Integer currentUserId
    );

    // 查询某用户发布的全部动态（用于「我的动态」）
    List<CommunityPost> findByUserId(
            @Param("userId") Integer userId,
            @Param("currentUserId") Integer currentUserId
    );

    // 新增动态，自增主键回填到 postId
    int insert(CommunityPost post);

    // 编辑动态（仅可编辑字段；like_count / comment_count / view_count 由系统维护）
    int update(CommunityPost post);

    // 删除动态（评论 / 点赞由外键 ON DELETE CASCADE 级联清理）
    int deleteById(@Param("postId") Integer postId);

    // 浏览量原子 +1（避免读改写造成并发丢失）
    int incrementViewCount(@Param("postId") Integer postId);

    // 按 community_like 的真实行数刷新冗余字段 like_count
    int refreshLikeCount(@Param("postId") Integer postId);

    // 按 community_comment 的真实行数刷新冗余字段 comment_count
    int refreshCommentCount(@Param("postId") Integer postId);
}
