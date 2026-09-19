package com.jiayi.campustrade.service;

import com.jiayi.campustrade.entity.Category;
import com.jiayi.campustrade.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    // 查询全部分类
    public List<Category> findAll() {
        return categoryMapper.findAll();
    }
}
