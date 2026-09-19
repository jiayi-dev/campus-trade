package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.AdminRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AdminRecordMapper {

    // 写入一条管理员操作记录
    int insert(AdminRecord record);

    // 查询全部操作记录（按时间倒序）
    List<AdminRecord> findAll();

}
