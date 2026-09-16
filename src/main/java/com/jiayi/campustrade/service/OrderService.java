package com.jiayi.campustrade.service;

import com.jiayi.campustrade.entity.Orders;
import com.jiayi.campustrade.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    public List<Orders> findAll() {
        return orderMapper.findAll();
    }
}