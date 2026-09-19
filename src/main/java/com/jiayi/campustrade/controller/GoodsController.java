package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.entity.Goods;
import com.jiayi.campustrade.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;


    // =========================
    // 实施六：商品分页查询
    // =========================

    @GetMapping("/page")
    public Map<String, Object> findPage(

            @RequestParam(required = false)
            String goodsName,

            @RequestParam(required = false)
            Integer categoryId,

            @RequestParam(required = false)
            BigDecimal minPrice,

            @RequestParam(required = false)
            BigDecimal maxPrice,

            @RequestParam(defaultValue = "1")
            Integer pageNum,

            @RequestParam(defaultValue = "5")
            Integer pageSize) {


        List<Goods> list = goodsService.findPage(
                goodsName,
                categoryId,
                minPrice,
                maxPrice,
                pageNum,
                pageSize
        );


        int total = goodsService.count(
                goodsName,
                categoryId,
                minPrice,
                maxPrice
        );


        Map<String, Object> result = new HashMap<>();

        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);


        return result;
    }


    // =========================
    // 商品详情
    // =========================

    @GetMapping("/{id}")
    public Goods findById(
            @PathVariable Integer id) {

        return goodsService.findById(id);

    }


    // =========================
    // 新增商品
    // =========================

    @PostMapping
    public String insert(
            @RequestBody Goods goods) {

        try {

            goodsService.insert(goods);

            return "商品发布成功，商品ID：" + goods.getGoodsId();

        } catch (RuntimeException e) {

            return e.getMessage();

        }

    }


    // =========================
    // 修改商品（仅商品发布者本人或管理员）
    // =========================

    @PutMapping
    public String update(
            @RequestBody Goods goods) {

        try {

            goodsService.update(goods);

            return "商品修改成功";

        } catch (RuntimeException e) {

            return e.getMessage();

        }

    }


    // =========================
    // 删除商品（仅商品发布者本人或管理员；已产生交易记录的商品禁止删除）
    // =========================

    @DeleteMapping("/{id}")
    public String deleteById(
            @PathVariable Integer id) {

        try {

            goodsService.deleteById(id);

            return "商品删除成功";

        } catch (RuntimeException e) {

            return e.getMessage();

        }

    }


    // =========================
    // 商品状态流转：PUT /goods/{id}/status，请求体 { status: 0 下架 / 1 上架 }
    // 编辑接口不允许改动状态，上下架走独立接口以承载业务规则
    //（已产生交易记录的商品不能重新上架）
    // =========================

    @PutMapping("/{id}/status")
    public String updateStatus(
            @PathVariable("id") Integer goodsId,
            @RequestBody Map<String, Object> body) {

        Object statusObj = body.get("status");

        if (statusObj == null) {
            return "缺少商品状态";
        }

        Integer targetStatus;

        try {
            targetStatus = Integer.valueOf(String.valueOf(statusObj));
        } catch (NumberFormatException e) {
            return "商品状态格式不正确";
        }

        try {

            goodsService.updateStatus(goodsId, targetStatus);

            return targetStatus == 1 ? "商品已上架" : "商品已下架";

        } catch (RuntimeException e) {

            return e.getMessage();

        }

    }


    // =========================
    // 实施八：批量新增商品
    // =========================

    @PostMapping("/batch")
    public String batchInsert(
            @RequestBody List<Goods> goodsList) {

        try {

            goodsService.batchInsert(goodsList);

            return "批量新增商品成功";

        } catch (RuntimeException e) {

            return e.getMessage();

        }

    }


    /*
     * ============================
     * 实施九：统计接口
     * ============================
     */


    // 商品首页统计
    @GetMapping("/statistics")
    public Map<String, Object> statistics() {

        return goodsService.statistics();

    }


    // 各分类平均价格
    @GetMapping("/categoryAvg")
    public List<Map<String, Object>> categoryAvg() {

        return goodsService.categoryAvg();

    }


    // 商品价格区间统计
    @GetMapping("/priceLevelCount")
    public List<Map<String, Object>> priceLevelCount() {

        return goodsService.priceLevelCount();

    }

    @GetMapping("/my")
    public List<Goods> findMyGoods() {
        return goodsService.findMyGoods();
    }

}