package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.entity.Category;
import com.jiayi.campustrade.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // 商品分类列表（登录用户均可查询，用于商品筛选与发布表单）
    @GetMapping
    public List<Category> findAll() {
        return categoryService.findAll();
    }
}
