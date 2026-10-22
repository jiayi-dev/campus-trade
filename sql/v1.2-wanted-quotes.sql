-- =============================================================
-- V1.2 · 求购响应升级为「多条报价」
--
-- 背景：原设计一个用户对一条求购只能有一条响应（唯一键 uk_wanted_user），
--       重复提交会覆盖旧内容，无法形成"多家报价、需求方择优"的真实流程。
-- 改造：去掉唯一键，允许同一人多次报价；新增报价金额与报价状态。
--
-- 用法：在 MySQL 8.0 中手动执行（先执行过 v1.2-wanted.sql 再执行本脚本）
--   USE campus_trade;
--   SOURCE v1.2-wanted-quotes.sql;
--
-- 仅修改 wanted_response 一张表，不删除任何数据。
-- =============================================================

-- 1. 去掉"一人一条"唯一键，替换为普通索引（仍然按 求购 + 用户 加速查询）
ALTER TABLE wanted_response
    DROP INDEX uk_wanted_user,
    ADD INDEX idx_wanted_user (wanted_id, user_id);

-- 2. 报价金额（可空表示面议）
ALTER TABLE wanted_response
    ADD COLUMN price DECIMAL(10,2) NULL COMMENT '报价金额，NULL 表示面议' AFTER message;

-- 3. 报价状态：0 待选 / 1 已选定 / 2 未入选
ALTER TABLE wanted_response
    ADD COLUMN status TINYINT NOT NULL DEFAULT 0 COMMENT '报价状态：0待选 1已选定 2未入选' AFTER price;

-- 4. 历史数据：全部置为待选
UPDATE wanted_response SET status = 0 WHERE status IS NULL;

-- =============================================================
-- 验证：
--   SHOW INDEX FROM wanted_response;            -- 不应再出现 uk_wanted_user
--   SELECT response_id, wanted_id, user_id, price, status FROM wanted_response LIMIT 20;
-- =============================================================
