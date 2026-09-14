package com.jiayi.campustrade.service;

import com.jiayi.campustrade.mapper.FavoriteMapper;
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

    @Transactional(rollbackFor = Exception.class)
    public void batchProcess(List<FavoriteItem> list) {

        if (list == null || list.isEmpty()) {
            throw new RuntimeException("收藏数据不能为空");
        }

        Set<String> set = new HashSet<>();

        for (int i = 0; i < list.size(); i++) {

            FavoriteItem item = list.get(i);

            if (item.getUserId() == null) {
                throw new RuntimeException(
                        "第" + (i + 1) + "条收藏的用户ID不能为空");
            }

            if (item.getGoodsId() == null) {
                throw new RuntimeException(
                        "第" + (i + 1) + "条收藏的商品ID不能为空");
            }

            String key = item.getUserId() + "_" + item.getGoodsId();

            if (!set.add(key)) {
                throw new RuntimeException(
                        "第" + (i + 1) + "条收藏与前面的数据重复");
            }

            int count = favoriteMapper.findByUserIdAndGoodsId(
                    item.getUserId(),
                    item.getGoodsId()
            );

            if (count > 0) {
                favoriteMapper.updateTime(
                        item.getUserId(),
                        item.getGoodsId()
                );
            } else {
                favoriteMapper.insert(
                        item.getUserId(),
                        item.getGoodsId()
                );
            }
        }
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