-- =============================================================
-- V1.2 · 为无图商品补齐演示主图
--
-- 背景：早期测试商品是直接插入数据库的，goods_image 没有对应记录，
--       前端只能显示占位图。本脚本按「品类」为所有缺图商品生成一张主图，
--       不需要逐个手工编辑上传。
--
-- 用法：在 MySQL 8.0 中手动执行
--   USE campus_trade;
--   SOURCE v1.2-goods-demo-images.sql;
--
-- 规则：
--   * 只处理「当前完全没有图片」的商品，已有真实上传图的一律不动；
--   * 每个商品补 1 张主图（is_primary=1, sort_order=0）；
--   * 图片由文生图接口按品类生成，URL 直接存 goods_image.image_url。
-- 可重复执行：已有图片的商品不会重复插入。
-- =============================================================

INSERT INTO goods_image (goods_id, image_url, sort_order, is_primary, create_time)
SELECT
    g.goods_id,
    CONCAT(
        'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=product%20photo%20of%20',
        CASE g.category_id
            WHEN 1 THEN 'a%20used%20smartphone%20or%20laptop'
            WHEN 2 THEN 'a%20stack%20of%20used%20textbooks'
            WHEN 3 THEN 'a%20small%20desk%20lamp'
            WHEN 4 THEN 'a%20basketball%20and%20sports%20gear'
            WHEN 5 THEN 'casual%20clothes%20and%20shoes'
            WHEN 6 THEN 'study%20notes%20and%20exam%20papers'
            WHEN 7 THEN 'dormitory%20storage%20items'
            WHEN 8 THEN 'an%20acoustic%20guitar'
            WHEN 9 THEN 'office%20stationery'
            ELSE 'a%20second-hand%20campus%20item'
        END,
        '%2C%20used%20item%20on%20cream%20background%2C%20soft%20light%2C%20realistic',
        '&image_size=square_hd'
    ),
    0,
    1,
    NOW()
FROM goods g
WHERE NOT EXISTS (
    SELECT 1 FROM goods_image gi WHERE gi.goods_id = g.goods_id
);

-- =============================================================
-- 验证：
--   SELECT g.goods_id, g.goods_name, gi.image_url
--   FROM goods g
--   LEFT JOIN goods_image gi ON gi.goods_id = g.goods_id
--   ORDER BY g.goods_id;
--   每一行都应有 image_url（不再有 NULL）。
-- =============================================================
