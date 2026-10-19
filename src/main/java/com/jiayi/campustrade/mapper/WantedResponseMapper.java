package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.WantedResponse;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WantedResponseMapper {

    // 查询某条求购的全部响应（JOIN user 带出响应者昵称，按响应时间倒序）
    List<WantedResponse> findByWantedId(Integer wantedId);

    // 新增响应；同一人对同一条求购重复响应时走 UPDATE（唯一键 uk_wanted_user）
    int upsert(WantedResponse response);
}
