-- =============================================================
-- V1.2 Day2.5 · AI 对话历史持久化建表脚本
--
-- 用法：在 MySQL 8.0 中手动执行
--   USE campus_trade;
--   SOURCE v1.2-conversation.sql;
--
-- 不会影响任何已有表，仅新增 2 张表。
-- =============================================================

-- 对话表：一个登录用户可以有多个对话
CREATE TABLE IF NOT EXISTS conversation (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL COMMENT '所属用户ID',
    title       VARCHAR(100) NOT NULL DEFAULT '新对话' COMMENT '对话标题（取首条消息前20字）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近活跃时间',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话会话';

-- 对话消息表：每轮对话的 user 消息和 assistant 消息
CREATE TABLE IF NOT EXISTS conversation_message (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    conversation_id INT NOT NULL COMMENT '所属对话ID',
    role            VARCHAR(10) NOT NULL COMMENT '消息角色：user / assistant',
    content         TEXT NOT NULL COMMENT '消息正文',
    extra_json      TEXT COMMENT '结构化附加数据（assistant 消息的 goods 商品卡片白名单 / sources 知识来源），JSON 格式',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    INDEX idx_conversation_id (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话消息';
