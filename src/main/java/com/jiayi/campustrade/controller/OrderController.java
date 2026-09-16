package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.entity.Orders;
import com.jiayi.campustrade.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public List<Orders> findAll() {
        return orderService.findAll();
    }
}