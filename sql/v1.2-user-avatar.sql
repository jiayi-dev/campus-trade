-- ============================================================
-- V1.2 用户头像字段
--
-- 目的：支持「登录后上传头像，刷新 / 重新登录后保持」。
-- 用法：图片先通过 POST /api/upload/image 上传，得到 /api/files/goods/xxx.jpg，
--       再把该相对 URL 通过 PUT /api/user/avatar 保存到这里。
-- 说明：只存相对 URL，不存任何机器绝对路径。
-- 幂等性：MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS，
--         重复执行会报 Duplicate column name，可忽略（说明字段已存在）。
-- 回滚：ALTER TABLE `user` DROP COLUMN avatar;
-- ============================================================

ALTER TABLE `user`
    ADD COLUMN avatar VARCHAR(255) NULL COMMENT '头像URL（服务端相对路径，如 /api/files/goods/xxx.jpg）' AFTER email;

-- 验证
SELECT user_id, username, avatar FROM `user` ORDER BY user_id LIMIT 5;
