package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.WantedResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WantedResponseMapper {

    // 查询某条求购的全部报价（JOIN user 带出响应者昵称，按时间倒序）
    List<WantedResponse> findByWantedId(Integer wantedId);

    // 查询单条报价（选定报价时校验归属）
    WantedResponse findById(Integer responseId);

    // 新增报价：每次都 INSERT，同一人可对同一求购多次报价
    int insert(WantedResponse response);

    // 选定报价：目标报价置 1，同求购下其余报价置 2
    int accept(@Param("wantedId") Integer wantedId,
               @Param("responseId") Integer responseId);
}
