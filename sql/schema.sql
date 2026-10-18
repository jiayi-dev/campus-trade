-- =============================================================
-- 校园二手交易平台 · 数据库结构初始化脚本（schema.sql）
--
-- 生成方式：mysqldump --no-data 从真实运行库（MySQL 8.0.22）导出，
-- 表 / 字段 / 类型 / 索引 / 外键 / 注释与正在运行的代码库完全一致。
--
-- 用法（MySQL 8.0+，字符集 utf8mb4_0900_ai_ci）：
--   mysql -u root -p < schema.sql
--   mysql -u root -p < data.sql
--
-- ⚠️ 警告：本脚本会 DROP 重建全部 10 张表（含 DROP TABLE IF EXISTS），
--    仅用于全新环境初始化，切勿对存有真实数据的库执行！
-- =============================================================
-- MySQL dump 10.13  Distrib 8.0.22, for Win64 (x86_64)
--
-- Host: localhost    Database: campus_trade
-- ------------------------------------------------------
-- Server version	8.0.22

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `campus_trade`
--

-- 出于安全考虑已注释 DROP DATABASE（防止误删现有库）；如需彻底重建，请手动执行：
-- DROP DATABASE IF EXISTS `campus_trade`;
-- /*!40000 DROP DATABASE IF EXISTS `campus_trade`*/;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `campus_trade` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `campus_trade`;

--
-- Table structure for table `admin_record`
--

DROP TABLE IF EXISTS `admin_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_record` (
  `record_id` int NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `admin_id` int NOT NULL COMMENT '管理员ID',
  `operation` varchar(255) NOT NULL COMMENT '操作内容',
  `create_time` datetime NOT NULL COMMENT '操作时间',
  PRIMARY KEY (`record_id`),
  KEY `fk_record_admin` (`admin_id`),
  CONSTRAINT `fk_record_admin` FOREIGN KEY (`admin_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员操作记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `category_id` int NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `category_name` varchar(50) NOT NULL COMMENT '分类名称',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `category_name` (`category_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `evaluation`
--

DROP TABLE IF EXISTS `evaluation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `favorite`
--

DROP TABLE IF EXISTS `favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `goods`
--

DROP TABLE IF EXISTS `goods`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `goods_image`
--

DROP TABLE IF EXISTS `goods_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `message`
--

DROP TABLE IF EXISTS `message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `order_id` int NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `goods_id` int NOT NULL COMMENT '商品ID',
  `buyer_id` int NOT NULL COMMENT '买家ID',
  `seller_id` int NOT NULL COMMENT '卖家ID',
  `order_status` int DEFAULT '0' COMMENT '订单状态',
  `create_time` datetime NOT NULL COMMENT '订单创建时间',
  PRIMARY KEY (`order_id`),
  KEY `fk_order_goods` (`goods_id`),
  KEY `fk_order_buyer` (`buyer_id`),
  KEY `fk_order_seller` (`seller_id`),
  CONSTRAINT `fk_order_buyer` FOREIGN KEY (`buyer_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_order_goods` FOREIGN KEY (`goods_id`) REFERENCES `goods` (`goods_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_order_seller` FOREIGN KEY (`seller_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `seller_application`
--

DROP TABLE IF EXISTS `seller_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `user_id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '用户密码',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `role` varchar(20) NOT NULL COMMENT '角色：admin管理员 student学生 seller卖家',
  `status` int DEFAULT '1' COMMENT '账号状态',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed
