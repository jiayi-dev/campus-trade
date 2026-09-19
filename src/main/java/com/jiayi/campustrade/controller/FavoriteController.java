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
}
