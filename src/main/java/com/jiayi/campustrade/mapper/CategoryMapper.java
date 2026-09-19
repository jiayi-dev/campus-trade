package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.Category;

import java.util.List;

public interface CategoryMapper {

    // 查询全部分类（商品筛选、发布/编辑商品表单使用）
    List<Category> findAll();
}
