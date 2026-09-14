package com.jiayi.campustrade.mapper;


import com.jiayi.campustrade.entity.Goods;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


public interface GoodsMapper {


    // 动态条件分页查询
    List<Goods> findPage(
            @Param("goodsName") String goodsName,
            @Param("categoryId") Integer categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );


    // 查询符合条件的商品总数
    int count(
            @Param("goodsName") String goodsName,
            @Param("categoryId") Integer categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );


    // 根据ID查询商品
    Goods findById(Integer goodsId);


    // 新增商品
    int insert(Goods goods);


    // 修改商品
    int update(Goods goods);


    // 删除商品
    int deleteById(Integer goodsId);



    // 批量新增商品
    int batchInsert(List<Goods> goodsList);



    /*
     * ============================
     * 实施九：统计功能
     * ============================
     */


    // 首页统计
    Map<String,Object> statistics();



    // 商品分类平均价格统计
    List<Map<String,Object>> categoryAvg();



    // 商品价格区间统计
    List<Map<String,Object>> priceLevelCount();


}