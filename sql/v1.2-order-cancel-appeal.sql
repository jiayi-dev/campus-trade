-- =============================================================
-- V1.2 · 订单取消 + 交易申诉
--
-- 1. orders 增加取消信息（状态 3 = 已取消，原 schema 注释已预留该状态位）
-- 2. 新增 trade_appeal 交易申诉表
--
-- 用法：在 MySQL 8.0 中手动执行
--   USE campus_trade;
--   SOURCE v1.2-order-cancel-appeal.sql;
--
-- 仅新增字段与一张新表，不删除任何已有的订单或商品数据。
-- =============================================================

-- 1. 订单取消信息
ALTER TABLE orders
    ADD COLUMN cancel_reason VARCHAR(200) NULL COMMENT '取消原因' AFTER order_status,
    ADD COLUMN cancel_by     INT NULL          COMMENT '取消操作人用户ID' AFTER cancel_reason,
    ADD COLUMN cancel_time   DATETIME NULL     COMMENT '取消时间' AFTER cancel_by;


-- 2. 交易申诉表
CREATE TABLE IF NOT EXISTS trade_appeal (
    appeal_id      INT AUTO_INCREMENT PRIMARY KEY COMMENT '申诉ID',
    order_id       INT NOT NULL COMMENT '关联订单ID',
    user_id        INT NOT NULL COMMENT '申诉人用户ID（买家或卖家）',
    reason         VARCHAR(50)  NOT NULL COMMENT '申诉类型，如 商品与描述不符 / 未收到商品 / 卖家拒绝取消',
    detail         VARCHAR(500) NULL     COMMENT '申诉说明',
    status         TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0待处理 1已受理 2已驳回',
    handle_by      INT NULL COMMENT '处理人（管理员）用户ID',
    handle_comment VARCHAR(300) NULL COMMENT '处理意见',
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申诉时间',
    handle_time    DATETIME NULL COMMENT '处理时间',
    INDEX idx_order_id (order_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易申诉表';

-- =============================================================
-- 验证：
--   SHOW COLUMNS FROM orders LIKE 'cancel%';
--   DESC trade_appeal;
-- =============================================================
