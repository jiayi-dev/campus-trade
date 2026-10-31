-- ============================================================
-- V1.2 商品图片修复：统一改为「本地上传目录 + 服务端相对 URL」
--
-- 背景：
--   历史数据里的图片 URL 指向 trae-api-cn.mchost.guru（IDE 内部文生图接口），
--   浏览器无法访问，导致列表/详情/Top1 全部空白。
--   上一版曾尝试 picsum.photos，属于随机图，不能代表真实商品，已废弃。
--
-- 本轮方案：
--   1) 12 张按品类生成的商品种子图已放到服务器上传目录：
--      D:/campus-trade-uploads/goods/seed-<品类>.jpg
--   2) 数据库只保存相对 URL：/api/files/goods/seed-<品类>.jpg
--      不写任何机器绝对路径，部署到别的电脑只要把 seed-images/web 下的文件
--      复制到该机器上传目录，即可通过 http://<服务器IP>:8080/api/files/... 访问。
--   3) 只更新无效 URL（trae-api-cn / picsum）的行；
--      用户自己上传的真实图片（/api/files/...）一律不动。
--
-- 幂等性：可重复执行，重复执行不会产生新变化。
-- 影响范围：仅 goods_image.image_url 字段，不删除任何商品/图片记录。
-- 回滚：执行前在 Navicat 备份 goods_image 表即可（右键 → 备份表）。
-- ============================================================

-- 【执行前检查 1】当前无效图片数量（预期 21 行左右）
SELECT COUNT(*) AS invalid_image_rows
FROM goods_image
WHERE image_url LIKE '%trae-api-cn%'
   OR image_url LIKE '%picsum.photos%';

-- 【执行前检查 2】确认种子图可以直接访问（应返回一行 /api/files/... 路径）
SELECT '通过浏览器访问 http://localhost:8080/api/files/goods/seed-phone.jpg 验证图片是否可显示' AS manual_check;

-- 【正式更新】
UPDATE goods_image gi
JOIN goods g ON g.goods_id = gi.goods_id
SET gi.image_url = CASE g.goods_id
    WHEN 1, 13, 14, 22 THEN '/api/files/goods/seed-phone.jpg'
    WHEN 2, 15, 30, 31 THEN '/api/files/goods/seed-textbook.jpg'
    WHEN 3             THEN '/api/files/goods/seed-lamp.jpg'
    WHEN 4             THEN '/api/files/goods/seed-basketball.jpg'
    WHEN 5             THEN '/api/files/goods/seed-sneakers.jpg'
    WHEN 6             THEN '/api/files/goods/seed-notes.jpg'
    WHEN 7             THEN '/api/files/goods/seed-storage.jpg'
    WHEN 8             THEN '/api/files/goods/seed-guitar.jpg'
    WHEN 9             THEN '/api/files/goods/seed-calculator.jpg'
    WHEN 10            THEN '/api/files/goods/seed-accessories.jpg'
    WHEN 12, 18        THEN '/api/files/goods/seed-keyboard.jpg'
    WHEN 19, 20        THEN '/api/files/goods/seed-accessories.jpg'
    WHEN 21            THEN '/api/files/goods/seed-camera.jpg'
    ELSE '/api/files/goods/seed-accessories.jpg'
END
WHERE gi.image_url LIKE '%trae-api-cn%'
   OR gi.image_url LIKE '%picsum.photos%';

-- 【执行后验证 1】不应再有 trae-api-cn / picsum 残留
SELECT COUNT(*) AS remaining_invalid_rows
FROM goods_image
WHERE image_url LIKE '%trae-api-cn%'
   OR image_url LIKE '%picsum.photos%';

-- 【执行后验证 2】逐条核对（应看到每个商品都指向本地 /api/files/goods/seed-*.jpg）
SELECT gi.image_id, gi.goods_id, g.goods_name, gi.is_primary, gi.image_url
FROM goods_image gi
JOIN goods g ON g.goods_id = gi.goods_id
ORDER BY gi.goods_id, gi.image_id;
