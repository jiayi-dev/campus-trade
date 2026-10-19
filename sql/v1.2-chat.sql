-- =============================================================
-- V1.2 · 用户间交易私聊（Chat）建表脚本
--
-- 用法：在 MySQL 8.0 中手动执行（本工程不会自动执行任何 SQL）
--   USE campus_trade;
--   SOURCE v1.2-chat.sql;
--
-- 说明（重要）：
--   1) 不会影响任何已有表，仅新增 1 张表 chat_message；
--   2) 与 message 表（商品留言板，只有 goods_id/user_id/content）语义无关，
--      不改动其结构，商品留言功能完全不受影响；
--   3) 与 conversation / conversation_message（AI 对话）无关，亦不复用。
-- =============================================================

CREATE TABLE IF NOT EXISTS chat_message (
    chat_id      INT AUTO_INCREMENT PRIMARY KEY,
    session_key  VARCHAR(64)  NOT NULL COMMENT '会话键：min(from,to)-max(from,to)-goodsId，goodsId 为空时用 0',
    from_user_id INT          NOT NULL COMMENT '发送者用户ID',
    to_user_id   INT          NOT NULL COMMENT '接收者用户ID',
    goods_id     INT          NULL COMMENT '关联商品ID（可空：不针对具体商品的沟通）',
    content      VARCHAR(500) NOT NULL COMMENT '消息正文',
    is_read      TINYINT      NOT NULL DEFAULT 0 COMMENT '接收者是否已读：0 未读 / 1 已读',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    INDEX idx_session (session_key, chat_id),
    INDEX idx_to_read (to_user_id, is_read),
    INDEX idx_from (from_user_id, chat_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户间交易私聊消息';
