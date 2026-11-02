-- =============================================================
-- 校园二手交易平台 · 独立演示实例建库脚本（00-create-demo-database.sql）
--
-- ⚠️ 本文件只用于「独立演示 MySQL 实例」：请在新实例（Docker 容器 / 云数据库 /
--    另一台机器）上执行，绝不要在本地开发实例 localhost:3306 执行后继续后面的步骤。
--    本机 localhost:3306 上的 campus_trade 是原开发库，含真实风格数据，二者必须物理隔离。
--
-- 作用：只创建空库，不建表、不写入任何账号/商品/业务数据。
-- 字符集与排序规则取自项目原始 schema.sql：
--   DEFAULT CHARACTER SET utf8mb4  COLLATE utf8mb4_unicode_ci
--   （表级统一使用 utf8mb4_0900_ai_ci，见 01-create-demo-schema.sql）
--
-- 执行前提：已用「新实例」的连接参数登录（主机、端口、账号由部署者自行提供）。
-- 执行方式：
--   mysql -h <新实例地址> -P <新实例端口> -u <账号> -p < 00-create-demo-database.sql
--
-- 安全性说明：
--   · 使用 CREATE DATABASE IF NOT EXISTS，库已存在时不报错、不改动任何已有数据；
--   · 不含 DROP DATABASE、DROP TABLE、TRUNCATE、DELETE；
--   · 不含任何用户名、密码、连接串等敏感配置；
--   · 不含业务数据，也不切换当前连接数据库（不写 USE）。
--
-- 执行后请立即核对实例身份（应显示新实例的 hostname/port）：
--   SELECT @@hostname, @@port, @@version;
-- =============================================================

CREATE DATABASE IF NOT EXISTS `campus_trade`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 只读验证：确认库已创建、字符集与排序规则正确
SELECT SCHEMA_NAME AS db_name,
       DEFAULT_CHARACTER_SET_NAME AS charset,
       DEFAULT_COLLATION_NAME AS collation
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME = 'campus_trade';
