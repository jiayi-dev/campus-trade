-- =============================================================
-- 校园二手交易平台 · 独立演示库完整建表脚本（01-create-demo-schema.sql）
--
-- 适用：全新、空的独立演示实例（库名 campus_trade，由 00-create-demo-database.sql 创建）
-- 内容：21 张表的完整结构（原始 schema.sql 的 10 张 + v1.2 系列结构变更的 11 张）
-- 生成方式：以项目真实结构为准，将 schema.sql 与 8 个 v1.2-*.sql 的结构变更合并为
--           一次性 CREATE TABLE（不做 ALTER），不修改任何原始脚本。
--
-- 与原始 schema.sql 的差异（已逐项审查后刻意排除）：
--   · 去掉 CREATE DATABASE / USE campus_trade  —— 由 00 脚本负责建库，本脚本不切库
--   · 去掉 DROP TABLE IF EXISTS                —— 演示库为空，且脚本必须在任何情况下都不破坏数据
--   · 去掉 mysqldump 的 FOREIGN_KEY_CHECKS=0 / UNIQUE_CHECKS=0 等会话级开关
--   · 去掉 data.sql 中的分类、账号、商品等初始数据 —— 业务数据由 demo-data.sql 负责
--
-- 合并进来的结构变更（字段均已确认存在）：
--   v1.2-user-avatar.sql          user.avatar
--   v1.2-order-cancel-appeal.sql  orders.cancel_reason / cancel_by / cancel_time + trade_appeal
--   v1.2-appeal-evidences.sql     trade_appeal.evidences
--   v1.2-chat.sql                 chat_message
--   v1.2-chat-media.sql           chat_message.message_type / related_id
--   v1.2-conversation.sql         conversation / conversation_message
--   v1.2-community.sql            community_post / community_comment / community_like
--   v1.2-wanted.sql               wanted_post / wanted_response
--   v1.2-wanted-quotes.sql        wanted_response.price / status（去掉旧唯一键，改为普通索引）
--
-- 说明：kb_doc / kb_chunk 在仓库中没有对应 SQL 文件（历史库中直接建表），
--       本脚本按实体类 KbDoc / KbChunk 与 KbDocMapper.xml / KbChunkMapper.xml
--       的真实列名还原，字段类型按实体注释（embedding 为 LONGTEXT 的 1024 维 JSON 文本）
--       与代码用途推导。若后续拿到原始 DDL，请以原始 DDL 为准并同步本文件。
--
-- 执行前提：
--   1. 已在新实例执行 00-create-demo-database.sql；
--   2. 已确认当前连接指向新实例（SELECT @@hostname, @@port;）；
--   3. 目标库为空（无同名表），否则 CREATE TABLE 会报 1050 并整体停止（不会写坏数据）。
--
-- 执行方式（必须显式指定库名，本脚本不含 USE）：
--   mysql -h <新实例地址> -P <新实例端口> -u <账号> -p campus_trade < 01-create-demo-schema.sql
--
-- 顺序：按外键依赖排列（父表在前），可整文件顺序执行。
-- 本脚本不含任何业务数据、账号、密码、数据库口令。
-- =============================================================

SET NAMES utf8mb4;

-- 执行前自检：确认实例与目标库（应显示新实例 hostname/port 与 campus_trade）
SELECT @@hostname AS host, @@port AS port, DATABASE() AS current_db;


-- =============================================================
-- 1. user 用户信息表（父表：被 goods / orders / evaluation / favorite 等引用）
-- =============================================================
CREATE TABLE `user` (
  `user_id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '用户密码',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) NULL COMMENT '头像URL（服务端相对路径，如 /api/files/goods/xxx.jpg）',
  `role` varchar(20) NOT NULL COMMENT '角色：admin管理员 student学生 seller卖家',
  `status` int DEFAULT '1' COMMENT '账号状态',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信息表';


-- =============================================================
-- 2. category 商品分类表
-- =============================================================
CREATE TABLE `category` (
  `category_id` int NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `category_name` varchar(50) NOT NULL COMMENT '分类名称',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `category_name` (`category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品分类表';


-- =============================================================
-- 3. goods 商品信息表（FK: user / category）
-- =============================================================
CREATE TABLE `goods` (
  `goods_id` int NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `user_id` int NOT NULL COMMENT '发布用户ID',
  `category_id` int DEFAULT NULL COMMENT '商品分类ID',
  `goods_name` varchar(100) NOT NULL COMMENT '商品名称',
  `description` text COMMENT '商品描述',
  `price` decimal(10,2) NOT NULL COMMENT '商品价格',
  `status` int DEFAULT '1' COMMENT '商品状态',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览数',
  `favorite_count` int NOT NULL DEFAULT '0' COMMENT '收藏数（冗余字段，事务同步 favorite 表）',
  `condition_level` tinyint DEFAULT NULL COMMENT '成色：1=全新 2=99新 3=95新 4=9成新 5=8成新及以下',
  `campus` varchar(50) DEFAULT NULL COMMENT '交易校区/区域',
  `location` varchar(100) DEFAULT NULL COMMENT '面交地点',
  `create_time` datetime NOT NULL COMMENT '发布时间',
  PRIMARY KEY (`goods_id`),
  KEY `fk_goods_user` (`user_id`),
  KEY `fk_goods_category` (`category_id`),
  KEY `idx_condition` (`condition_level`),
  KEY `idx_campus` (`campus`),
  CONSTRAINT `fk_goods_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`category_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_goods_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品信息表';


-- =============================================================
-- 4. goods_image 商品图片表（多图；历史结构无外键，仅普通索引）
-- =============================================================
CREATE TABLE `goods_image` (
  `image_id` int NOT NULL AUTO_INCREMENT COMMENT '图片主键',
  `goods_id` int NOT NULL COMMENT '关联商品ID',
  `image_url` varchar(255) NOT NULL COMMENT '图片URL',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号，0为主图',
  `is_primary` tinyint NOT NULL DEFAULT '0' COMMENT '是否主图：1=是 0=否',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`image_id`),
  KEY `idx_goods_id` (`goods_id`),
  KEY `idx_primary` (`goods_id`,`is_primary`),
  KEY `idx_sort` (`goods_id`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品图片表（多图）';


-- =============================================================
-- 5. message 商品留言表（FK: goods / user）
-- =============================================================
CREATE TABLE `message` (
  `message_id` int NOT NULL AUTO_INCREMENT COMMENT '留言ID',
  `goods_id` int NOT NULL COMMENT '商品ID',
  `user_id` int NOT NULL COMMENT '留言用户ID',
  `content` text NOT NULL COMMENT '留言内容',
  `create_time` datetime NOT NULL COMMENT '留言时间',
  PRIMARY KEY (`message_id`),
  KEY `fk_message_goods` (`goods_id`),
  KEY `fk_message_user` (`user_id`),
  CONSTRAINT `fk_message_goods` FOREIGN KEY (`goods_id`) REFERENCES `goods` (`goods_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_message_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品留言表';


-- =============================================================
-- 6. orders 交易订单表（FK: goods / user；含 v1.2 取消三字段）
-- =============================================================
CREATE TABLE `orders` (
  `order_id` int NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `goods_id` int NOT NULL COMMENT '商品ID',
  `buyer_id` int NOT NULL COMMENT '买家ID',
  `seller_id` int NOT NULL COMMENT '卖家ID',
  `order_status` int DEFAULT '0' COMMENT '订单状态',
  `cancel_reason` varchar(200) NULL COMMENT '取消原因',
  `cancel_by` int NULL COMMENT '取消操作人用户ID',
  `cancel_time` datetime NULL COMMENT '取消时间',
  `create_time` datetime NOT NULL COMMENT '订单创建时间',
  PRIMARY KEY (`order_id`),
  KEY `fk_order_goods` (`goods_id`),
  KEY `fk_order_buyer` (`buyer_id`),
  KEY `fk_order_seller` (`seller_id`),
  CONSTRAINT `fk_order_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_order_goods` FOREIGN KEY (`goods_id`) REFERENCES `goods` (`goods_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_order_seller` FOREIGN KEY (`seller_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易订单表';


-- =============================================================
-- 7. evaluation 交易评价表（FK: orders / user；order_id 唯一=一单一评）
-- =============================================================
CREATE TABLE `evaluation` (
  `evaluation_id` int NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `order_id` int NOT NULL COMMENT '订单ID',
  `user_id` int NOT NULL COMMENT '评价用户ID',
  `content` text COMMENT '评价内容',
  `score` int DEFAULT NULL COMMENT '评分',
  `create_time` datetime NOT NULL COMMENT '评价时间',
  PRIMARY KEY (`evaluation_id`),
  UNIQUE KEY `order_id` (`order_id`),
  KEY `fk_evaluation_user` (`user_id`),
  CONSTRAINT `fk_evaluation_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_evaluation_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易评价表';


-- =============================================================
-- 8. favorite 商品收藏表（FK: goods / user；(user_id, goods_id) 唯一）
-- =============================================================
CREATE TABLE `favorite` (
  `favorite_id` int NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `goods_id` int NOT NULL COMMENT '商品ID',
  `create_time` datetime NOT NULL COMMENT '收藏时间',
  PRIMARY KEY (`favorite_id`),
  UNIQUE KEY `uk_user_goods` (`user_id`,`goods_id`),
  KEY `fk_favorite_goods` (`goods_id`),
  CONSTRAINT `fk_favorite_goods` FOREIGN KEY (`goods_id`) REFERENCES `goods` (`goods_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品收藏表';


-- =============================================================
-- 9. seller_application 卖家申请表（FK: user / reviewer→user）
-- =============================================================
CREATE TABLE `seller_application` (
  `application_id` int NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `user_id` int NOT NULL COMMENT '申请人用户ID',
  `reason` varchar(500) NOT NULL COMMENT '申请原因（学生填写，10-500字）',
  `status` int NOT NULL DEFAULT '0' COMMENT '审核状态：0待审核 1已通过 2已拒绝',
  `reviewer_id` int DEFAULT NULL COMMENT '审核管理员ID',
  `review_comment` varchar(255) DEFAULT NULL COMMENT '审核意见（管理员填写，拒绝时必填）',
  `create_time` datetime NOT NULL COMMENT '申请提交时间',
  `review_time` datetime DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (`application_id`),
  KEY `idx_application_user` (`user_id`),
  KEY `idx_application_status` (`status`),
  KEY `idx_application_reviewer` (`reviewer_id`),
  CONSTRAINT `fk_application_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_application_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='卖家申请表';


-- =============================================================
-- 10. admin_record 管理员操作记录表（FK: admin→user）
-- =============================================================
CREATE TABLE `admin_record` (
  `record_id` int NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `admin_id` int NOT NULL COMMENT '管理员ID',
  `operation` varchar(255) NOT NULL COMMENT '操作内容',
  `create_time` datetime NOT NULL COMMENT '操作时间',
  PRIMARY KEY (`record_id`),
  KEY `fk_record_admin` (`admin_id`),
  CONSTRAINT `fk_record_admin` FOREIGN KEY (`admin_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员操作记录表';


-- =============================================================
-- 11. chat_message 用户间交易私聊消息（含 v1.2 图片/订单卡片字段）
--     历史结构无外键，仅普通索引
-- =============================================================
CREATE TABLE `chat_message` (
  `chat_id` int NOT NULL AUTO_INCREMENT,
  `session_key` varchar(64) NOT NULL COMMENT '会话键：min(from,to)-max(from,to)-goodsId，goodsId 为空时用 0',
  `from_user_id` int NOT NULL COMMENT '发送者用户ID',
  `to_user_id` int NOT NULL COMMENT '接收者用户ID',
  `goods_id` int NULL COMMENT '关联商品ID（可空：不针对具体商品的沟通）',
  `content` varchar(500) NOT NULL COMMENT '消息正文',
  `message_type` varchar(16) NOT NULL DEFAULT 'text' COMMENT '消息类型：text 文本 / image 图片 / order 订单卡片',
  `related_id` int NULL COMMENT '关联ID：order=订单ID；image 消息的 URL 存在 content',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '接收者是否已读：0 未读 / 1 已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`chat_id`),
  INDEX `idx_session` (`session_key`, `chat_id`),
  INDEX `idx_to_read` (`to_user_id`, `is_read`),
  INDEX `idx_from` (`from_user_id`, `chat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户间交易私聊消息';


-- =============================================================
-- 12. conversation AI 对话会话（无外键）
-- =============================================================
CREATE TABLE `conversation` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL COMMENT '所属用户ID',
  `title` varchar(100) NOT NULL DEFAULT '新对话' COMMENT '对话标题（取首条消息前20字）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近活跃时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话会话';


-- =============================================================
-- 13. conversation_message AI 对话消息（无外键）
-- =============================================================
CREATE TABLE `conversation_message` (
  `id` int NOT NULL AUTO_INCREMENT,
  `conversation_id` int NOT NULL COMMENT '所属对话ID',
  `role` varchar(10) NOT NULL COMMENT '消息角色：user / assistant',
  `content` text NOT NULL COMMENT '消息正文',
  `extra_json` text COMMENT '结构化附加数据（assistant 消息的 goods 商品卡片白名单 / sources 知识来源），JSON 格式',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  INDEX `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI对话消息';


-- =============================================================
-- 14. community_post 校园社区动态（FK: user / goods）
-- =============================================================
CREATE TABLE `community_post` (
  `post_id` int NOT NULL AUTO_INCREMENT COMMENT '动态ID',
  `user_id` int NOT NULL COMMENT '发布者用户ID',
  `title` varchar(60) NOT NULL COMMENT '动态标题',
  `topic` varchar(20) NOT NULL COMMENT '主题标签：闲置分享/使用体验/交易经验/环保循环',
  `content` text NOT NULL COMMENT '正文内容',
  `image_url` varchar(255) NULL COMMENT '配图URL（可选，允许为空）',
  `goods_id` int NULL COMMENT '关联商品ID（可选，可为 NULL）',
  `like_count` int NOT NULL DEFAULT 0 COMMENT '点赞数（冗余字段：按 community_like 真实行数刷新）',
  `comment_count` int NOT NULL DEFAULT 0 COMMENT '评论数（冗余字段：按 community_comment 真实行数刷新）',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '浏览数（详情访问时原子 +1）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`post_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_topic` (`topic`),
  INDEX `idx_create_time` (`create_time`),
  CONSTRAINT `fk_community_post_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_community_post_goods` FOREIGN KEY (`goods_id`) REFERENCES `goods` (`goods_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='校园社区动态表';


-- =============================================================
-- 15. community_comment 社区动态评论（FK: community_post / user）
-- =============================================================
CREATE TABLE `community_comment` (
  `comment_id` int NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `post_id` int NOT NULL COMMENT '关联动态ID',
  `user_id` int NOT NULL COMMENT '评论者用户ID',
  `content` varchar(500) NOT NULL COMMENT '评论内容',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  PRIMARY KEY (`comment_id`),
  INDEX `idx_post_id` (`post_id`),
  INDEX `idx_comment_user` (`user_id`),
  CONSTRAINT `fk_community_comment_post` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`post_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_community_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='校园社区动态评论表';


-- =============================================================
-- 16. community_like 社区点赞（FK: community_post / user；(post_id, user_id) 唯一）
-- =============================================================
CREATE TABLE `community_like` (
  `like_id` int NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `post_id` int NOT NULL COMMENT '关联动态ID',
  `user_id` int NOT NULL COMMENT '点赞用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`like_id`),
  UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
  INDEX `idx_like_user` (`user_id`),
  CONSTRAINT `fk_community_like_post` FOREIGN KEY (`post_id`) REFERENCES `community_post` (`post_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_community_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='校园社区动态点赞表';


-- =============================================================
-- 17. wanted_post 校园求购需求（无外键）
-- =============================================================
CREATE TABLE `wanted_post` (
  `wanted_id` int NOT NULL AUTO_INCREMENT COMMENT '求购ID',
  `user_id` int NOT NULL COMMENT '发布者用户ID',
  `title` varchar(60) NOT NULL COMMENT '求购标题',
  `category_id` int NULL COMMENT '期望分类（来自 category 表）',
  `budget_min` decimal(10,2) NULL COMMENT '预算下限',
  `budget_max` decimal(10,2) NULL COMMENT '预算上限',
  `campus` varchar(30) NULL COMMENT '期望校区',
  `description` varchar(500) NULL COMMENT '需求描述',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1进行中 2已关闭',
  `response_count` int NOT NULL DEFAULT 0 COMMENT '响应数（真实维护，与 wanted_response 行数一致）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`wanted_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='校园求购需求表';


-- =============================================================
-- 18. wanted_response 求购报价（支持多条报价：已去掉旧唯一键 uk_wanted_user）
-- =============================================================
CREATE TABLE `wanted_response` (
  `response_id` int NOT NULL AUTO_INCREMENT COMMENT '响应ID',
  `wanted_id` int NOT NULL COMMENT '关联求购ID',
  `user_id` int NOT NULL COMMENT '响应者用户ID',
  `message` varchar(300) NOT NULL COMMENT '响应内容（如"我有这本，9成新，可以面交"）',
  `price` decimal(10,2) NULL COMMENT '报价金额，NULL 表示面议',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '报价状态：0待选 1已选定 2未入选',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '响应时间',
  PRIMARY KEY (`response_id`),
  INDEX `idx_wanted_user` (`wanted_id`, `user_id`),
  INDEX `idx_wanted_id` (`wanted_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='校园求购响应表';


-- =============================================================
-- 19. trade_appeal 交易申诉（含 v1.2 证据字段；历史结构无外键，仅普通索引）
-- =============================================================
CREATE TABLE `trade_appeal` (
  `appeal_id` int NOT NULL AUTO_INCREMENT COMMENT '申诉ID',
  `order_id` int NOT NULL COMMENT '关联订单ID',
  `user_id` int NOT NULL COMMENT '申诉人用户ID（买家或卖家）',
  `reason` varchar(50) NOT NULL COMMENT '申诉类型，如 商品与描述不符 / 未收到商品 / 卖家拒绝取消',
  `detail` varchar(500) NULL COMMENT '申诉说明',
  `evidences` text NULL COMMENT '证据图片URL数组(JSON)，如 ["url1","url2"]',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '处理状态：0待处理 1已受理 2已驳回',
  `handle_by` int NULL COMMENT '处理人（管理员）用户ID',
  `handle_comment` varchar(300) NULL COMMENT '处理意见',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申诉时间',
  `handle_time` datetime NULL COMMENT '处理时间',
  PRIMARY KEY (`appeal_id`),
  INDEX `idx_order_id` (`order_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易申诉表';


-- =============================================================
-- 20. kb_doc AI 知识库文档（无 SQL 源文件，按实体/Mapper 还原；标题唯一）
-- =============================================================
CREATE TABLE `kb_doc` (
  `doc_id` int NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  `title` varchar(100) NOT NULL COMMENT '文档标题（唯一，重建索引按标题 upsert）',
  `category` varchar(50) NOT NULL COMMENT '文档分类：faq / trading / rule / forbidden',
  `source` varchar(100) DEFAULT NULL COMMENT '内容来源说明',
  `version` int NOT NULL DEFAULT '1' COMMENT '内容版本号，重建索引更新内容时 +1',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否参与检索：1=启用 0=停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  PRIMARY KEY (`doc_id`),
  UNIQUE KEY `uk_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI知识库文档';


-- =============================================================
-- 21. kb_chunk AI 知识库分块（无 SQL 源文件，按实体/Mapper 还原）
--     embedding 存 1024 维浮点 JSON 文本（MySQL 8 无原生向量类型）
-- =============================================================
CREATE TABLE `kb_chunk` (
  `chunk_id` int NOT NULL AUTO_INCREMENT COMMENT '分块ID',
  `doc_id` int NOT NULL COMMENT '所属文档ID',
  `seq` int NOT NULL DEFAULT '0' COMMENT '片段在文档内的顺序号，从 0 开始',
  `content` text NOT NULL COMMENT '片段正文（Embedding 输入与提示词注入内容）',
  `embedding` longtext NULL COMMENT 'Embedding 向量 JSON 文本；NULL=尚未向量化（不参与检索）',
  `content_hash` varchar(64) DEFAULT NULL COMMENT '片段正文 SHA-256 摘要（增量重建用）',
  `model` varchar(50) DEFAULT NULL COMMENT '生成该向量的 Embedding 模型名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  PRIMARY KEY (`chunk_id`),
  INDEX `idx_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI知识库分块';


-- =============================================================
-- 执行后只读验收：应为 21 张表，且全部 InnoDB / utf8mb4_0900_ai_ci
-- =============================================================
SELECT COUNT(*) AS table_count
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE();

SELECT TABLE_NAME, ENGINE, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME;
