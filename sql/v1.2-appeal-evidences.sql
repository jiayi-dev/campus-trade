-- =============================================================
-- V1.2 · 申诉证据上传（trade_appeal 增加 evidences 字段）
--
-- 背景：原 trade_appeal 只支持文字申诉，真实交易纠纷需要提交
--       商品破损照、描述不符照、聊天截图等图片证据。
--
-- 改动：
--   给 trade_appeal 表加 evidences TEXT 字段，存 JSON 数组：
--     ["/api/files/appeal/xxx.jpg", "/api/files/appeal/yyy.jpg"]
--   文件通过已有的 POST /api/upload/image 上传后把 URL 拼入。
--
-- 为什么不用独立关联表：
--   申诉证据只在提交时一次性写入、列表展示时整体读出、
--   数量有限（最多 5 张）、业务查询不需要按单张图片维度检索，
--   用 JSON 字段比新建 trade_appeal_evidence 表更简单、查询效率更高、
--   不引入额外的 JOIN、不改变现有 Service 的调用链。
--
-- 约束（由后端 Service 强制校验）：
--   - evidences 最多 5 张；
--   - 单张不超 5MB；
--   - URL 必须以 /api/files/ 或 http(s):// 开头；
--   - 用户必须是该订单的买家或卖家；
--   - 同一用户对同一订单只能有一条待处理申诉。
--
-- 执行方式：
--   USE campus_trade;
--   SOURCE v1.2-appeal-evidences.sql;
--
-- 幂等：ADD COLUMN 带 IF NOT EXISTS 逻辑（MySQL 8.0 不支持此语法，
--       如执行时报 Duplicate column，说明之前已执行过本脚本，可忽略）。
-- =============================================================

ALTER TABLE trade_appeal
    ADD COLUMN evidences TEXT NULL COMMENT '证据图片URL数组(JSON)，如 ["url1","url2"]' AFTER detail;

-- 验证
-- SHOW COLUMNS FROM trade_appeal LIKE 'evidences';
