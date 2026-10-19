package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.auth.AuthContext;
import com.jiayi.campustrade.entity.Goods;
import com.jiayi.campustrade.mapper.GoodsMapper;
import com.jiayi.campustrade.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private GoodsMapper goodsMapper;

    @PostMapping("/batch")
    public String batch(
            @RequestBody List<FavoriteService.FavoriteItem> list) {

        try {
            favoriteService.batchProcess(list);
            return "批量收藏处理成功";
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    // 查询当前用户是否已收藏某商品：GET /favorite/check/11
    @GetMapping("/check/{goodsId}")
    public Map<String, Object> check(@PathVariable Integer goodsId) {

        Integer userId = AuthContext.getUserId();

        Map<String, Object> result = new HashMap<>();
        result.put("favorite", favoriteService.isFavorite(userId, goodsId));

        return result;
    }

    // 批量查询收藏状态：GET /favorite/checkBatch?goodsIds=1,2,3
    // 商品列表页一次请求回显整页卡片的收藏状态，避免逐个卡片发 N 个请求
    @GetMapping("/checkBatch")
    public Map<String, Object> checkBatch(@RequestParam List<Integer> goodsIds) {

        Integer userId = AuthContext.getUserId();

        Map<String, Object> result = new HashMap<>();

        if (goodsIds == null || goodsIds.isEmpty()) {
            result.put("favoriteGoodsIds", List.of());
            return result;
        }

        result.put("favoriteGoodsIds",
                favoriteService.findFavoriteGoodsIds(userId, goodsIds));

        return result;
    }

    // 收藏商品：POST /favorite/11
    // userId 从登录上下文获取；商品必须存在，且不能收藏自己发布的商品
    @PostMapping("/{goodsId}")
    public String add(@PathVariable Integer goodsId) {

        Integer userId = AuthContext.getUserId();

        Goods goods = goodsMapper.findById(goodsId);

        if (goods == null) {
            return "商品不存在或已被删除";
        }

        if (userId.equals(goods.getUserId())) {
            return "不能收藏自己发布的商品";
        }

        // 与详情页规则保持一致：下架商品不允许新收藏（可能已被买走/撤下），
        // 已收藏过的仍可正常取消收藏
        if (goods.getStatus() == null || goods.getStatus() != 1) {
            return "商品已下架，不能收藏";
        }

        favoriteService.addFavorite(userId, goodsId);

        return "收藏成功";
    }

    // 取消收藏：DELETE /favorite/11
    @DeleteMapping("/{goodsId}")
    public String cancel(@PathVariable Integer goodsId) {

        Integer userId = AuthContext.getUserId();

        favoriteService.cancelFavorite(userId, goodsId);

        return "已取消收藏";
    }

    // 我的收藏商品列表（含商品展示字段与主图）：GET /favorite/my
    @GetMapping("/my")
    public List<Map<String, Object>> my() {
        return favoriteService.findMyFavorites();
    }

    // 批量取消收藏：DELETE /favorite/batch，请求体为商品ID数组 [1,2,3]
    // 静态段 batch 的匹配优先级高于动态段 {goodsId}，不会与 DELETE /favorite/{goodsId} 冲突
    @DeleteMapping("/batch")
    public String batchCancel(@RequestBody List<Integer> goodsIds) {
        try {
            int removed = favoriteService.cancelFavorites(goodsIds);
            return "已取消收藏 " + removed + " 件商品";
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }
}
