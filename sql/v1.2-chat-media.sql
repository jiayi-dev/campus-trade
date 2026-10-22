-- =============================================================
-- V1.2 · 消息中心增强：支持图片消息与订单卡片
--
-- 前置条件：
--   已执行 campus-trade/sql/v1.2-chat.sql（chat_message 表已存在）
--
-- 用法：
--   在 MySQL 8.0 中手动执行（本工程不会自动执行任何 SQL）
--     USE campus_trade;
--     SOURCE v1.2-chat-media.sql;
--
-- 说明：
--   1) 仅新增 2 个字段，不改动任何既有字段，历史消息自动归类为 text；
--   2) message_type：text 文本 / image 图片 / order 订单卡片；
--   3) related_id：order 消息存 order_id；image 消息的 URL 存在 content 中，
--      related_id 为空（图片地址同时用于会话列表的 [图片] 预览判定）；
--   4) 未执行本脚本时，后端仍可运行，但发送图片/订单会因字段缺失报错，
--      前端会明确提示，不会伪造发送成功。
-- =============================================================

ALTER TABLE chat_message
    ADD COLUMN message_type VARCHAR(16) NOT NULL DEFAULT 'text'
        COMMENT '消息类型：text 文本 / image 图片 / order 订单卡片' AFTER content,
    ADD COLUMN related_id INT NULL
        COMMENT '关联ID：order=订单ID；image 消息的 URL 存在 content' AFTER message_type;

-- 历史消息统一为文本消息（DEFAULT 已覆盖，这里显式兜底一次）
UPDATE chat_message
SET message_type = 'text'
WHERE message_type IS NULL OR message_type = '';

-- 验证
-- SELECT message_type, COUNT(*) FROM chat_message GROUP BY message_type;
-- SELECT chat_id, message_type, related_id, content FROM chat_message ORDER BY chat_id DESC LIMIT 20;
