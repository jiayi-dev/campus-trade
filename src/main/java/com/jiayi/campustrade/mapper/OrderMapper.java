package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderMapper {

    List<Orders> findAll();

}