package com.jiayi.campustrade.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品图片（对应 goods_image 表）
 * 图片文件保存在服务器本地上传目录，数据库只存 URL，
 * 通过 Spring Boot 静态资源映射对外提供访问
 */
@Data
public class GoodsImage {

    // 图片ID
    private Integer imageId;

    // 所属商品ID
    private Integer goodsId;

    // 图片访问 URL（相对路径，如 /api/files/goods/xxx.jpg）
    private String imageUrl;

    // 排序号（越小越靠前）
    private Integer sortOrder;

    // 是否主图：1=是 0=否（列表页封面取主图）
    private Integer isPrimary;

    // 创建时间
    private LocalDateTime createTime;
}
