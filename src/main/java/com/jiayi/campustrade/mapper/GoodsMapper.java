package com.jiayi.campustrade.mapper;


import com.jiayi.campustrade.entity.Goods;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


public interface GoodsMapper {


    // 动态条件分页查询（JOIN goods_image 取主图 + 成色/校区筛选 + 多种排序）
    // sortBy 取值：latest 最新发布 / price_asc 价格升序 / price_desc 价格降序
    //              / hot 热度（收藏数+浏览数）/ treasure 基于浏览量和收藏量的业务加权排序
    List<Goods> findPage(
            @Param("goodsName") String goodsName,
            @Param("categoryId") Integer categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("conditionLevel") Integer conditionLevel,
            @Param("campus") String campus,
            @Param("sortBy") String sortBy,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );


    // 查询符合条件的商品总数
    int count(
            @Param("goodsName") String goodsName,
            @Param("categoryId") Integer categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("conditionLevel") Integer conditionLevel,
            @Param("campus") String campus
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

    List<Goods> findMyGoods(Integer userId);


    // 仅当商品仍在售（status=1）时才更新状态
    // 下单并发控制：抢占成功返回 1，商品已被别人买走则返回 0
    int updateStatusIfOnSale(
            @Param("goodsId") Integer goodsId,
            @Param("status") Integer status
    );

    // 商品状态流转（下架/上架），归属与状态合法性由 Service 层校验
    int updateStatus(
            @Param("goodsId") Integer goodsId,
            @Param("status") Integer status
    );

    // 收藏数同步：新增收藏时 +1（FavoriteService 在事务内调用）
    int incrementFavoriteCount(@Param("goodsId") Integer goodsId);

    // 收藏数同步：取消收藏时 -1（带下限保护，避免减成负数）
    int decrementFavoriteCount(@Param("goodsId") Integer goodsId);

    // 浏览数 +1：数据库端原子自增（并发浏览不丢计数），商品不存在时影响 0 行
    int incrementViewCount(@Param("goodsId") Integer goodsId);

    // 管理员：商品分页查询（JOIN 带出卖家名与分类名）
    List<Map<String, Object>> findAdminPage(
            @Param("goodsName") String goodsName,
            @Param("categoryId") Integer categoryId,
            @Param("status") Integer status,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    // 管理员：商品总数
    int countAdmin(
            @Param("goodsName") String goodsName,
            @Param("categoryId") Integer categoryId,
            @Param("status") Integer status
    );

}