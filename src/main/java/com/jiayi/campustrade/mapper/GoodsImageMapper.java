package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.GoodsImage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GoodsImageMapper {

    // 新增图片记录（sortOrder 从 0 开始，第一条即主图）
    int insert(
            @Param("goodsId") Integer goodsId,
            @Param("imageUrl") String imageUrl,
            @Param("sortOrder") Integer sortOrder,
            @Param("isPrimary") Integer isPrimary
    );

    // 查询商品全部图片（主图在前）
    List<GoodsImage> findByGoodsId(@Param("goodsId") Integer goodsId);

    // 查询商品全部图片 URL（主图在前，详情页多图展示用）
    List<String> findUrlsByGoodsId(@Param("goodsId") Integer goodsId);

    // 删除商品全部图片记录（编辑替换/删除商品时调用）
    int deleteByGoodsId(@Param("goodsId") Integer goodsId);
}
