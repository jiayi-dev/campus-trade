package com.jiayi.campustrade.service;

import com.jiayi.campustrade.entity.Goods;
import com.jiayi.campustrade.mapper.GoodsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;


    // =========================
    // 实施六：分页查询
    // =========================

    public List<Goods> findPage(
            String goodsName,
            Integer categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer pageNum,
            Integer pageSize) {

        Integer offset = (pageNum - 1) * pageSize;

        return goodsMapper.findPage(
                goodsName,
                categoryId,
                minPrice,
                maxPrice,
                offset,
                pageSize
        );
    }


    // =========================
    // 查询商品总数
    // =========================

    public int count(
            String goodsName,
            Integer categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice) {

        return goodsMapper.count(
                goodsName,
                categoryId,
                minPrice,
                maxPrice
        );
    }


    // =========================
    // 商品详情
    // =========================

    public Goods findById(Integer goodsId) {

        return goodsMapper.findById(goodsId);

    }


    // =========================
    // 新增商品
    // =========================

    public void insert(Goods goods) {

        goodsMapper.insert(goods);

    }


    // =========================
    // 修改商品
    // =========================

    public void update(Goods goods) {

        goodsMapper.update(goods);

    }


    // =========================
    // 删除商品
    // =========================

    public void deleteById(Integer goodsId) {

        goodsMapper.deleteById(goodsId);

    }


    // =========================
    // 实施八：批量新增商品
    // =========================

    @Transactional(rollbackFor = Exception.class)
    public void batchInsert(List<Goods> goodsList) {

        if (goodsList == null || goodsList.isEmpty()) {
            throw new RuntimeException("商品数据不能为空");
        }

        for (int i = 0; i < goodsList.size(); i++) {

            Goods goods = goodsList.get(i);

            int rowNum = i + 1;


            // 校验发布用户
            if (goods.getUserId() == null) {
                throw new RuntimeException(
                        "第" + rowNum + "行：用户ID不能为空"
                );
            }


            // 校验商品名称
            if (goods.getGoodsName() == null
                    || goods.getGoodsName().trim().isEmpty()) {

                throw new RuntimeException(
                        "第" + rowNum + "行：商品名称不能为空"
                );
            }


            // 校验价格
            if (goods.getPrice() == null) {

                throw new RuntimeException(
                        "第" + rowNum + "行：商品价格不能为空"
                );
            }


            // 价格不能小于0
            if (goods.getPrice().compareTo(BigDecimal.ZERO) < 0) {

                throw new RuntimeException(
                        "第" + rowNum + "行：商品价格不能小于0"
                );
            }


            // 执行新增
            goodsMapper.insert(goods);

        }

    }


    /*
     * ============================
     * 实施九：统计功能
     * ============================
     */


    // 商品首页统计
    public Map<String, Object> statistics() {

        return goodsMapper.statistics();

    }


    // 各分类平均价格
    public List<Map<String, Object>> categoryAvg() {

        return goodsMapper.categoryAvg();

    }


    // 商品价格区间统计
    public List<Map<String, Object>> priceLevelCount() {

        return goodsMapper.priceLevelCount();

    }

}