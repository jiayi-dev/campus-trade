-- =============================================================
-- V1.2 · 校园社区动态（Community）建表脚本
--
-- 用法：在 MySQL 8.0 中手动执行（本工程不会自动执行任何 SQL）
--   USE campus_trade;
--   SOURCE v1.2-community.sql;
--
-- 说明（重要）：
--   1) 不会影响任何已有表，仅新增 3 张表：
--      community_post / community_comment / community_like；
--   2) 与 wanted_post（校园求购）、chat_message（交易私聊）等 V1.2 表无关，
--      互不复用、互不影响；
--   3) community_like 用唯一键 uk_post_user(post_id, user_id) 保证「一人一赞」，
--      点赞接口据此做点赞/取消切换；
--   4) 删除动态时，评论与点赞由外键 ON DELETE CASCADE 级联清理；
--      community_post 的冗余计数 like_count / comment_count 由服务端
--      按真实行数刷新，不做假的累加。
-- =============================================================

-- 1. 社区动态主表
CREATE TABLE IF NOT EXISTS community_post (
    post_id       INT AUTO_INCREMENT PRIMARY KEY COMMENT '动态ID',
    user_id       INT NOT NULL COMMENT '发布者用户ID',
    title         VARCHAR(60) NOT NULL COMMENT '动态标题',
    topic         VARCHAR(20) NOT NULL COMMENT '主题标签：闲置分享/使用体验/交易经验/环保循环',
    content       TEXT NOT NULL COMMENT '正文内容',
    image_url     VARCHAR(255) NULL COMMENT '配图URL（可选，允许为空）',
    goods_id      INT NULL COMMENT '关联商品ID（可选，可为 NULL）',
    like_count    INT NOT NULL DEFAULT 0 COMMENT '点赞数（冗余字段：按 community_like 真实行数刷新）',
    comment_count INT NOT NULL DEFAULT 0 COMMENT '评论数（冗余字段：按 community_comment 真实行数刷新）',
    view_count    INT NOT NULL DEFAULT 0 COMMENT '浏览数（详情访问时原子 +1）',
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_topic (topic),
    INDEX idx_create_time (create_time),
    CONSTRAINT fk_community_post_user  FOREIGN KEY (user_id)  REFERENCES `user` (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_community_post_goods FOREIGN KEY (goods_id) REFERENCES goods (goods_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='校园社区动态表';

-- 2. 动态评论表
CREATE TABLE IF NOT EXISTS community_comment (
    comment_id  INT AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
    post_id     INT NOT NULL COMMENT '关联动态ID',
    user_id     INT NOT NULL COMMENT '评论者用户ID',
    content     VARCHAR(500) NOT NULL COMMENT '评论内容',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    INDEX idx_post_id (post_id),
    INDEX idx_comment_user (user_id),
    CONSTRAINT fk_community_comment_post FOREIGN KEY (post_id) REFERENCES community_post (post_id) ON DELETE CASCADE,
    CONSTRAINT fk_community_comment_user FOREIGN KEY (user_id) REFERENCES `user` (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='校园社区动态评论表';

-- 3. 动态点赞表（唯一键保证一人一赞）
CREATE TABLE IF NOT EXISTS community_like (
    like_id     INT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    post_id     INT NOT NULL COMMENT '关联动态ID',
    user_id     INT NOT NULL COMMENT '点赞用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    UNIQUE KEY uk_post_user (post_id, user_id),
    INDEX idx_like_user (user_id),
    CONSTRAINT fk_community_like_post FOREIGN KEY (post_id) REFERENCES community_post (post_id) ON DELETE CASCADE,
    CONSTRAINT fk_community_like_user FOREIGN KEY (user_id) REFERENCES `user` (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='校园社区动态点赞表';
