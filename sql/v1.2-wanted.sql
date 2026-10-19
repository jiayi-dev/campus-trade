-- =============================================================
-- V1.2 · 校园求购（Wanted）建表脚本
--
-- 用法：在 MySQL 8.0 中手动执行
--   USE campus_trade;
--   SOURCE v1.2-wanted.sql;
--
-- 不会影响任何已有表，仅新增 2 张表。
-- =============================================================

-- 求购需求表：一个登录用户可以发布多条求购
CREATE TABLE IF NOT EXISTS wanted_post (
    wanted_id      INT AUTO_INCREMENT PRIMARY KEY COMMENT '求购ID',
    user_id        INT NOT NULL COMMENT '发布者用户ID',
    title          VARCHAR(60) NOT NULL COMMENT '求购标题',
    category_id    INT NULL COMMENT '期望分类（来自 category 表）',
    budget_min     DECIMAL(10,2) NULL COMMENT '预算下限',
    budget_max     DECIMAL(10,2) NULL COMMENT '预算上限',
    campus         VARCHAR(30) NULL COMMENT '期望校区',
    description    VARCHAR(500) NULL COMMENT '需求描述',
    status         TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1进行中 2已关闭',
    response_count INT NOT NULL DEFAULT 0 COMMENT '响应数（真实维护，与 wanted_response 行数一致）',
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    update_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='校园求购需求表';

-- 求购响应表：卖家/同学对某条求购的响应
CREATE TABLE IF NOT EXISTS wanted_response (
    response_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '响应ID',
    wanted_id   INT NOT NULL COMMENT '关联求购ID',
    user_id     INT NOT NULL COMMENT '响应者用户ID',
    message     VARCHAR(300) NOT NULL COMMENT '响应内容（如"我有这本，9成新，可以面交"）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '响应时间',
    UNIQUE KEY uk_wanted_user (wanted_id, user_id),
    INDEX idx_wanted_id (wanted_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='校园求购响应表';
