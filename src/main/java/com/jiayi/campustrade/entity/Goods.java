package com.jiayi.campustrade.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Goods {

    // 商品ID
    private Integer goodsId;

    // 发布用户ID
    private Integer userId;

    // 商品分类ID
    private Integer categoryId;

    // 商品名称
    private String goodsName;

    // 商品描述
    private String description;

    // 商品价格
    private BigDecimal price;

    // 商品状态
    private Integer status;

    // 浏览数（列表页展示热度，详情页被访问时 +1）
    private Integer viewCount;

    // 收藏数（冗余字段：在 favorite 表新增/删除时事务同步，避免每次列表查询都 JOIN favorite 表 COUNT）
    private Integer favoriteCount;

    // 成色：1=全新 2=99新 3=95新 4=9成新 5=8成新及以下
    private Integer conditionLevel;

    // 交易校区/区域（东区/西区/南区/北区等，发布商品页选择）
    private String campus;

    // 面交地点（自由文本，如"三食堂门口"）
    private String location;

    // 发布时间
    private LocalDateTime createTime;

    // 主图 URL（非持久化字段：列表/详情页 SQL LEFT JOIN goods_image 取主图时填充）
    // 不参与 INSERT/UPDATE，发布商品页改造时由独立的图片接口写入 goods_image 表
    private String imageUrl;

    // 是否已售出（非持久化字段：仅详情接口填充）
    // goods.status 只有 在售=1 / 下架=0 两种；"已售出" = status=0 且存在关联订单，
    // 由详情查询时通过 orderMapper.countByGoodsId 判断，列表页为性能不计
    private Boolean sold;
}
