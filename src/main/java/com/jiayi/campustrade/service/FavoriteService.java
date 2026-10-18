package com.jiayi.campustrade.service;

import com.jiayi.campustrade.exception.BusinessException;
import com.jiayi.campustrade.mapper.FavoriteMapper;
import com.jiayi.campustrade.mapper.GoodsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    // 注入 GoodsMapper 用于同步 goods.favorite_count 冗余字段
    // 冗余字段的目的：列表页展示收藏数时避免对每条商品 JOIN favorite 表 COUNT，性能更好
    @Autowired
    private GoodsMapper goodsMapper;

    @Transactional(rollbackFor = Exception.class)
    public void batchProcess(List<FavoriteItem> list) {

        if (list == null || list.isEmpty()) {
            throw new BusinessException("收藏数据不能为空");
        }

        Set<String> set = new HashSet<>();

        for (int i = 0; i < list.size(); i++) {

            FavoriteItem item = list.get(i);

            if (item.getUserId() == null) {
                throw new BusinessException(
                        "第" + (i + 1) + "条收藏的用户ID不能为空");
            }

            if (item.getGoodsId() == null) {
                throw new BusinessException(
                        "第" + (i + 1) + "条收藏的商品ID不能为空");
            }

            String key = item.getUserId() + "_" + item.getGoodsId();

            if (!set.add(key)) {
                throw new BusinessException(
                        "第" + (i + 1) + "条收藏与前面的数据重复");
            }

            // 复用单个收藏逻辑：已存在则刷新时间，不存在才插入，天然防重复
            addFavorite(item.getUserId(), item.getGoodsId());
        }
    }

    /**
     * 单个收藏（幂等：已收藏时只更新收藏时间，不会产生重复记录）
     * 同时事务内同步 goods.favorite_count +1，保证冗余字段与 favorite 表一致
     * 注意：幂等场景下重复收藏不会让 favorite_count 重复 +1（只在真正新增时才 +1）
     */
    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(Integer userId, Integer goodsId) {

        int count = favoriteMapper.findByUserIdAndGoodsId(userId, goodsId);

        if (count > 0) {
            // 已收藏：仅刷新时间，不重复 +1 favorite_count
            favoriteMapper.updateTime(userId, goodsId);
        } else {
            // 新收藏：先插入 favorite 表，再同步 goods.favorite_count
            favoriteMapper.insert(userId, goodsId);
            goodsMapper.incrementFavoriteCount(goodsId);
        }
    }

    /**
     * 取消收藏（未收藏时删除 0 行，也视为成功）
     * 同步事务内 goods.favorite_count -1，GREATEST 函数兜底防止减成负数
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelFavorite(Integer userId, Integer goodsId) {

        // 先确认是否真的收藏过，避免未收藏时 favorite_count 误减
        int count = favoriteMapper.findByUserIdAndGoodsId(userId, goodsId);

        favoriteMapper.delete(userId, goodsId);

        if (count > 0) {
            goodsMapper.decrementFavoriteCount(goodsId);
        }
    }

    // 当前用户是否已收藏该商品
    public boolean isFavorite(Integer userId, Integer goodsId) {
        return favoriteMapper.findByUserIdAndGoodsId(userId, goodsId) > 0;
    }

    // 批量查询当前用户已收藏的商品ID（列表页回显收藏状态，避免 N+1 请求）
    public List<Integer> findFavoriteGoodsIds(Integer userId, List<Integer> goodsIds) {
        return favoriteMapper.findFavoriteGoodsIds(userId, goodsIds);
    }

    public static class FavoriteItem {

        private Integer userId;
        private Integer goodsId;

        public Integer getUserId() {
            return userId;
        }

        public void setUserId(Integer userId) {
            this.userId = userId;
        }

        public Integer getGoodsId() {
            return goodsId;
        }

        public void setGoodsId(Integer goodsId) {
            this.goodsId = goodsId;
        }
    }
}
