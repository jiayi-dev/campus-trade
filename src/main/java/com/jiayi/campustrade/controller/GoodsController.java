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

            // 成色：1=全新 2=99新 3=95新 4=9成新 5=8成新及以下
            @RequestParam(required = false)
            Integer conditionLevel,

            // 校区：东区/西区/南区/北区等
            @RequestParam(required = false)
            String campus,

            // 排序：latest 最新发布 / price_asc 价格升序 / price_desc 价格降序 / hot 热度
            @RequestParam(defaultValue = "latest")
            String sortBy,

            @RequestParam(defaultValue = "1")
            Integer pageNum,

            @RequestParam(defaultValue = "12")
            Integer pageSize) {


        List<Goods> list = goodsService.findPage(
                goodsName,
                categoryId,
                minPrice,
                maxPrice,
                conditionLevel,
                campus,
                sortBy,
                pageNum,
                pageSize
        );


        int total = goodsService.count(
                goodsName,
                categoryId,
                minPrice,
                maxPrice,
                conditionLevel,
                campus
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
    // 浏览量 +1：POST /goods/11/view
    // 进入商品详情页时由前端调用；编辑商品拉详情走 GET /goods/{id} 纯查询，
    // 两个入口分离，避免编辑动作被误计为浏览。
    // 返回自增后的最新浏览量，前端直接更新显示；商品不存在时 viewCount=-1
    // =========================

    @PostMapping("/{id}/view")
    public Map<String, Object> addView(
            @PathVariable("id") Integer goodsId) {

        Map<String, Object> result = new HashMap<>();

        result.put("viewCount", goodsService.increaseView(goodsId));

        return result;

    }


    // =========================
    // 商品图片查询：GET /goods/11/images
    // 返回按 主图优先 → sort_order 排序的 URL 列表，详情页多图展示用
    // =========================

    @GetMapping("/{id}/images")
    public List<String> findImages(
            @PathVariable("id") Integer goodsId) {

        return goodsService.findImageUrls(goodsId);

    }


    // =========================
    // 商品图片保存/替换：POST 或 PUT /goods/11/images
    // 请求体 { "imageUrls": ["...", "..."] }，第一张自动为主图
    // 发布商品成功后首次保存、编辑商品保存时全量替换共用此接口（先删后插幂等）
    // =========================

    @RequestMapping(value = "/{id}/images", method = {RequestMethod.POST, RequestMethod.PUT})
    public String saveImages(
            @PathVariable("id") Integer goodsId,
            @RequestBody Map<String, List<String>> body) {

        try {

            goodsService.replaceImages(goodsId, body.get("imageUrls"));

            return "商品图片保存成功";

        } catch (RuntimeException e) {

            return e.getMessage();

        }

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