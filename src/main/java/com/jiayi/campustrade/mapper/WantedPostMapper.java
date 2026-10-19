package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.WantedPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface WantedPostMapper {

    // 动态条件分页查询求购（JOIN user 带出发布者昵称）
    // keyword 标题模糊 / categoryId 分类 / campus 校区 / budgetMax 预算上限上限阈值
    // status 状态筛选（null 时全部返回）
    // sortBy：latest 最新发布 / budget_desc 预算高→低 / budget_asc 预算低→高
    List<WantedPost> findPage(
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            @Param("campus") String campus,
            @Param("budgetMax") BigDecimal budgetMax,
            @Param("status") Integer status,
            @Param("sortBy") String sortBy,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    // 查询符合条件的求购总数（与 findPage 同筛选条件）
    int count(
            @Param("keyword") String keyword,
            @Param("categoryId") Integer categoryId,
            @Param("campus") String campus,
            @Param("budgetMax") BigDecimal budgetMax,
            @Param("status") Integer status
    );

    // 根据ID查询求购详情（含发布者昵称）
    WantedPost findById(Integer wantedId);

    // 新增求购，自增主键回填到 wantedId
    int insert(WantedPost post);

    // 修改求购（动态 SET；status / response_count 不允许经编辑改动）
    int update(WantedPost post);

    // 求购状态流转（关闭），归属与状态合法性由 Service 层校验
    int updateStatus(
            @Param("wantedId") Integer wantedId,
            @Param("status") Integer status
    );

    // 按 wanted_response 的真实行数刷新冗余字段 response_count，保证计数与真实响应一致
    int refreshResponseCount(@Param("wantedId") Integer wantedId);
}
