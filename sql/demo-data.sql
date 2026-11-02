-- =============================================================
-- 校园二手交易平台 · 完整演示数据脚本（demo-data.sql）
--
-- 目标数据库：campus_trade（独立演示 MySQL 实例，禁止在本地开发实例执行）
-- 适用版本：campus-trade v1.1（MySQL 8.0.22，utf8mb4）
--
-- 前置条件（缺一不可）：
--   1. 已连接「独立演示实例」，不是本机开发实例 localhost:3306；
--   2. 已执行 sql/00-create-demo-database.sql（建空库 campus_trade）；
--   3. 已执行 sql/01-create-demo-schema.sql（建 21 张表，含全部 v1.2 结构变更）；
--   4. 不要执行 data.sql（固定主键、旧演示账号，与本脚本账号体系冲突）。
--
-- 执行方式：
--   mysql -h <新实例地址> -P <新实例端口> -u <账号> -p campus_trade < sql/demo-data.sql
--
-- 脚本行为：
--   · 单事务写入，末尾 COMMIT；
--   · 不使用 TRUNCATE / DELETE / DROP / UPDATE 全表；
--   · 不写死任何自增主键，全部通过自然键或 LAST_INSERT_ID() 解析；
--   · 重复执行会在第 3 步账号插入处因 username 唯一键冲突中止，
--     未提交事务随连接关闭整体回滚，不会留下半套数据；
--   · 商品描述末尾统一带【DEMO-V1】标记，用于识别与清理。
--
-- 本轮仅生成脚本，未执行、未创建任何数据库。
-- =============================================================

SET NAMES utf8mb4;

-- 【关键确认】本脚本只允许在独立演示实例的 campus_trade 库执行
USE campus_trade;

-- 执行前自检：确认当前库与实例（如果不是演示实例的 campus_trade，立刻停止）
SELECT DATABASE() AS current_db,
       @@hostname AS host,
       @@port     AS port,
       NOW()      AS now;

START TRANSACTION;

-- =============================================================
-- 1. 商品分类（10 类，与前端筛选、现库一致）
-- =============================================================

INSERT INTO `category` (category_name, create_time)
SELECT '数码产品', '2026-10-01 09:00:00' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE category_name = '数码产品');

INSERT INTO `category` (category_name, create_time)
SELECT '图书教材', '2026-10-01 09:00:00' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE category_name = '图书教材');

INSERT INTO `category` (category_name, create_time)
SELECT '生活用品', '2026-10-01 09:00:00' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE category_name = '生活用品');

INSERT INTO `category` (category_name, create_time)
SELECT '运动用品', '2026-10-01 09:00:00' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE category_name = '运动用品');

INSERT INTO `category` (category_name, create_time)
SELECT '服装鞋帽', '2026-10-01 09:00:00' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE category_name = '服装鞋帽');

INSERT INTO `category` (category_name, create_time)
SELECT '学习资料', '2026-10-01 09:00:00' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE category_name = '学习资料');

INSERT INTO `category` (category_name, create_time)
SELECT '宿舍用品', '2026-10-01 09:00:00' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE category_name = '宿舍用品');

INSERT INTO `category` (category_name, create_time)
SELECT '乐器设备', '2026-10-01 09:00:00' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE category_name = '乐器设备');

INSERT INTO `category` (category_name, create_time)
SELECT '办公用品', '2026-10-01 09:00:00' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE category_name = '办公用品');

INSERT INTO `category` (category_name, create_time)
SELECT '其他', '2026-10-01 09:00:00' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE category_name = '其他');

SET @cat_digital  := (SELECT category_id FROM `category` WHERE category_name = '数码产品');
SET @cat_book     := (SELECT category_id FROM `category` WHERE category_name = '图书教材');
SET @cat_life     := (SELECT category_id FROM `category` WHERE category_name = '生活用品');
SET @cat_sport    := (SELECT category_id FROM `category` WHERE category_name = '运动用品');
SET @cat_clothes  := (SELECT category_id FROM `category` WHERE category_name = '服装鞋帽');
SET @cat_study    := (SELECT category_id FROM `category` WHERE category_name = '学习资料');
SET @cat_dorm     := (SELECT category_id FROM `category` WHERE category_name = '宿舍用品');
SET @cat_music    := (SELECT category_id FROM `category` WHERE category_name = '乐器设备');
SET @cat_office   := (SELECT category_id FROM `category` WHERE category_name = '办公用品');
SET @cat_other    := (SELECT category_id FROM `category` WHERE category_name = '其他');


-- =============================================================
-- 2. 演示账号（6 个，全部虚构）
--
-- 公开账号（可写入 README / 登录页）：
--   demo_buyer   123456   student  买家视角
--   demo_seller  123456   seller   卖家视角
--   demo_admin   123456   admin    管理视角（学生/卖家入口会被拒绝）
-- 剧情账号（不公开，仅用于数据自洽）：
--   demo_seller2 123456   seller   第二卖家
--   demo_student 123456   student  社区/求购/订单互动
--   demo_buyer2  123456   student  另一买家
--
-- 密码哈希：BCrypt（强度 10），与 AuthService 的 BCryptPasswordEncoder.matches
--          校验逻辑一致；已用 spring-security-crypto 5.6.8（项目实际依赖版本）验证通过。
--          6 个哈希均为本轮新生成（各自独立随机盐），未复制项目中任何既有账号的哈希。
-- 手机号：19900000001~19900000006，均为虚构号段，不对应真实用户。
-- =============================================================

INSERT INTO `user` (username, password, phone, email, role, status, create_time) VALUES
('demo_buyer',   '$2a$10$m1QSmD5Fzuzvn8EDOckSCeVFwuM443eEMbKntF81I/pOL2isPdEnW', '19900000001', 'demo_buyer@campus-demo.local',   'student', 1, '2026-10-01 10:00:00'),
('demo_seller',  '$2a$10$27h8rcCcyEF1Y/9UXGHX/.o9ZYdJakbOR.p.Xf5UG.e9YCOXcHQhK', '19900000002', 'demo_seller@campus-demo.local',  'seller',  1, '2026-10-01 10:30:00'),
('demo_admin',   '$2a$10$0HYKDszg7gFDb0ySdiA4WewmuOuJW32DXXsjDdJMMmsE2d5R9rAFG', '19900000003', 'demo_admin@campus-demo.local',   'admin',   1, '2026-10-01 11:00:00'),
('demo_seller2', '$2a$10$aUvtiLxYwKbIaId3qgS2VeAg2Y9URQmDdXo82y54adrA29eTNcq5e', '19900000004', 'demo_seller2@campus-demo.local', 'seller',  1, '2026-10-02 09:00:00'),
('demo_student', '$2a$10$qgjFsl4Ke8dZUScYOp0houMa5jNLie4fESLIXown.rybfgZiTPe6W', '19900000005', 'demo_student@campus-demo.local', 'student', 1, '2026-10-02 10:00:00'),
('demo_buyer2',  '$2a$10$ydGTHg0dI2ga/bjvKT5UNurPj/kmgE3XzuDBCevizBUOwS8/KGpt2', '19900000006', 'demo_buyer2@campus-demo.local',  'student', 1, '2026-10-02 11:00:00');

SET @demo_buyer   := (SELECT user_id FROM `user` WHERE username = 'demo_buyer');
SET @demo_seller  := (SELECT user_id FROM `user` WHERE username = 'demo_seller');
SET @demo_admin   := (SELECT user_id FROM `user` WHERE username = 'demo_admin');
SET @demo_seller2 := (SELECT user_id FROM `user` WHERE username = 'demo_seller2');
SET @demo_student := (SELECT user_id FROM `user` WHERE username = 'demo_student');
SET @demo_buyer2  := (SELECT user_id FROM `user` WHERE username = 'demo_buyer2');


-- =============================================================
-- 3. 商品（24 件：15 在售 + 9 已下单锁定）
--    状态规则：已被未取消订单锁定的商品 status=0；订单已取消或从未下单的 status=1。
--    favorite_count 与第 7 步 favorite 表行数严格一致。
-- =============================================================

-- G01 已被 O1 完成 → status=0
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller, @cat_digital, 'iPhone 12 128G 95新',
        '自用两年，电池健康 88%，边框有轻微使用痕迹，无维修无拆修，含原装充电线，可当面验机。【DEMO-V1】',
        2200.00, 0, 126, 0, 3, '东区', '三食堂门口', '2026-10-05 10:00:00');
SET @g01 := LAST_INSERT_ID();

-- G02 已被 O2 完成 → status=0
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller2, @cat_book, '高等数学教材全套（同济第七版）',
        '上下册加习题解析，大一用过一学期，重点标注清晰，笔记整洁，适合提前预习。【DEMO-V1】',
        45.00, 0, 88, 0, 4, '北区', '图书馆正门', '2026-10-06 16:00:00');
SET @g02 := LAST_INSERT_ID();

-- G03 已被 O3 完成 → status=0
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller, @cat_sport, '室内篮球 7 号（皮面完好）',
        '打了一学期，皮面完好气足，毕业清宿舍出，可约球场试球。【DEMO-V1】',
        60.00, 0, 64, 0, 3, '西区', '体育馆侧门', '2026-10-07 09:00:00');
SET @g03 := LAST_INSERT_ID();

-- G04 已被 O4 完成 → status=0
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller, @cat_life, '宿舍折叠台灯（三档调光）',
        '可充电，夹床沿不占地方，续航约 5 小时，搬宿舍用不上了。【DEMO-V1】',
        18.00, 0, 71, 0, 2, '北区', '5号楼下', '2026-10-08 11:00:00');
SET @g04 := LAST_INSERT_ID();

-- G05 已被 O5 完成 → status=0
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller2, @cat_study, '考研英语真题近十年（含解析册）',
        '做过一遍已用铅笔标注，可擦除，适合提前刷题，解析册一起出。【DEMO-V1】',
        25.00, 0, 93, 0, 2, '北区', '图书馆正门', '2026-10-09 14:00:00');
SET @g05 := LAST_INSERT_ID();

-- G06 已被 O6 锁定（待买家确认）→ status=0
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller, @cat_digital, '半入耳式蓝牙耳机（带收纳盒）',
        '九成新，续航约 6 小时，音质清晰无杂音，换了新耳机出闲置。【DEMO-V1】',
        89.00, 0, 74, 0, 2, '东区', '三食堂门口', '2026-10-18 20:00:00');
SET @g06 := LAST_INSERT_ID();

-- G07 已被 O7 锁定（待买家确认）→ status=0
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller, @cat_digital, '罗技 K380 多设备蓝牙键盘',
        '可连三台设备，键程舒适适合图书馆码字，键帽无打油，附原装电池。【DEMO-V1】',
        55.00, 0, 87, 0, 3, '东区', '三食堂门口', '2026-10-19 09:30:00');
SET @g07 := LAST_INSERT_ID();

-- G08 已被 O8 锁定（待交易）→ status=0
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller, @cat_book, '数据结构教材（严蔚敏版）',
        '重点章节有便签，配套习题答案打印版一并送，适合期末复习。【DEMO-V1】',
        32.00, 0, 52, 0, 3, '北区', '图书馆正门', '2026-10-26 15:00:00');
SET @g08 := LAST_INSERT_ID();

-- G09 已被 O9 锁定（待交易）→ status=0
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller2, @cat_clothes, '运动跑鞋 42 码',
        '只跑了两次操场，鞋底磨损轻微，鞋盒在，尺码买大了一码。【DEMO-V1】',
        70.00, 0, 66, 0, 3, '西区', '体育馆侧门', '2026-10-27 10:30:00');
SET @g09 := LAST_INSERT_ID();

-- G10 O10 已取消 → 商品回到在售 status=1
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller, @cat_digital, '平板保护壳（磁吸翻盖）',
        '买了平板后多买的壳，可立可躺，全新未拆封，一直闲置。【DEMO-V1】',
        20.00, 1, 48, 0, 2, '东区', '三食堂门口', '2026-10-20 18:00:00');
SET @g10 := LAST_INSERT_ID();

-- G11 O11 已取消 → 商品回到在售 status=1
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller, @cat_life, '宿舍电热水壶 1.2L',
        '小型电热水壶，烧水快，毕业带不走，当面可试。【DEMO-V1】',
        35.00, 1, 59, 0, 3, '北区', '5号楼下', '2026-10-22 19:00:00');
SET @g11 := LAST_INSERT_ID();

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller, @cat_digital, '罗技无线鼠标（静音款）',
        '静音微动，电池可换，滚轮顺滑，桌面清理出闲置。【DEMO-V1】',
        45.00, 1, 78, 1, 3, '东区', '三食堂门口', '2026-10-05 09:30:00');
SET @g12 := LAST_INSERT_ID();

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller2, @cat_book, '大学英语四级词汇书',
        '翻过一遍，无笔记无涂画，适合备考四级或六级打底。【DEMO-V1】',
        15.00, 1, 43, 1, 4, '北区', '图书馆正门', '2026-10-06 10:30:00');
SET @g13 := LAST_INSERT_ID();

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller, @cat_dorm, '宿舍收纳盒三件套（可叠放）',
        '放文具零食都合适，成色新，搬宿舍打包出。【DEMO-V1】',
        22.00, 1, 61, 2, 2, '北区', '5号楼下', '2026-10-07 14:00:00');
SET @g14 := LAST_INSERT_ID();

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller2, @cat_sport, '瑜伽垫加厚款（8mm）',
        '加厚防滑，用过几次，附收纳绑带，宿舍练操课用。【DEMO-V1】',
        30.00, 1, 57, 0, 3, '西区', '体育馆侧门', '2026-10-09 09:00:00');
SET @g15 := LAST_INSERT_ID();

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller, @cat_clothes, '白色帆布鞋 37 码',
        '穿过两次，鞋面干净无污渍，尺码不合适转出。【DEMO-V1】',
        40.00, 1, 69, 1, 3, '南区', '校园南门门口', '2026-10-10 16:00:00');
SET @g16 := LAST_INSERT_ID();

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller2, @cat_study, '高数手写笔记（一学期整理）',
        '整理了一学期的公式与题型，字迹工整，适合期末冲刺。【DEMO-V1】',
        12.00, 1, 84, 1, 3, '北区', '图书馆正门', '2026-10-11 08:30:00');
SET @g17 := LAST_INSERT_ID();

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller, @cat_dorm, '宿舍遮光床帘（含支架）',
        '遮光款，安装简单，支架齐全，换床位后不再需要。【DEMO-V1】',
        25.00, 1, 62, 2, 2, '东区', '三食堂门口', '2026-10-12 13:00:00');
SET @g18 := LAST_INSERT_ID();

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller, @cat_music, '民谣吉他 41 寸（附琴包）',
        '云杉面单，音色明亮，附琴包与变调夹，自学两年后闲置。【DEMO-V1】',
        380.00, 1, 112, 1, 4, '南区', '艺术楼排练室', '2026-10-13 15:00:00');
SET @g19 := LAST_INSERT_ID();

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller2, @cat_office, '家用彩色打印机（附墨盒）',
        '可打印作业与资料，剩余墨盒一并送，搬宿舍出。【DEMO-V1】',
        260.00, 1, 96, 2, 3, '东区', '三食堂门口', '2026-10-15 10:00:00');
SET @g20 := LAST_INSERT_ID();

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller, @cat_digital, '20000mAh 移动电源（双向快充）',
        '容量足，可上飞机，换了新充电宝出闲置。【DEMO-V1】',
        65.00, 1, 73, 1, 3, '西区', '体育馆侧门', '2026-10-16 18:30:00');
SET @g21 := LAST_INSERT_ID();

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller2, @cat_life, '保温杯 500ml（304 不锈钢）',
        '保温约 6 小时，杯身无磕碰，多买了一个。【DEMO-V1】',
        28.00, 1, 45, 1, 2, '北区', '5号楼下', '2026-10-17 09:00:00');
SET @g22 := LAST_INSERT_ID();

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller, @cat_book, '专业课教材两本（计网 + 操作系统）',
        '考点标注完整，配套课件打印件一并送，适合备考。【DEMO-V1】',
        38.00, 1, 68, 1, 4, '北区', '图书馆正门', '2026-10-18 10:30:00');
SET @g23 := LAST_INSERT_ID();

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status,
                     view_count, favorite_count, condition_level, campus, location, create_time)
VALUES (@demo_seller2, @cat_other, '晴雨两用自动伞',
        '自动开合，伞骨结实，宿舍备用多了一把。【DEMO-V1】',
        19.00, 1, 39, 1, 3, '南区', '校园南门门口', '2026-10-20 08:00:00');
SET @g24 := LAST_INSERT_ID();


-- =============================================================
-- 4. 商品图片（24 张主图，全部引用本机已存在的种子图）
--    访问路径：/api/files/goods/seed-*.jpg（WebConfig: /files/** → 上传目录）
--    磁盘实际文件名：seed-accessories/basketball/calculator/camera/guitar/
--    keyboard/lamp/notes/phone/sneakers/storage/textbook.jpg（共 12 张）
--    不使用 trae-api-cn.mchost.guru 外链。
-- =============================================================

INSERT INTO `goods_image` (goods_id, image_url, sort_order, is_primary, create_time) VALUES
(@g01, '/api/files/goods/seed-phone.jpg',       0, 1, '2026-10-05 10:05:00'),
(@g02, '/api/files/goods/seed-textbook.jpg',    0, 1, '2026-10-06 16:05:00'),
(@g03, '/api/files/goods/seed-basketball.jpg',  0, 1, '2026-10-07 09:05:00'),
(@g04, '/api/files/goods/seed-lamp.jpg',        0, 1, '2026-10-08 11:05:00'),
(@g05, '/api/files/goods/seed-notes.jpg',       0, 1, '2026-10-09 14:05:00'),
(@g06, '/api/files/goods/seed-accessories.jpg', 0, 1, '2026-10-18 20:05:00'),
(@g07, '/api/files/goods/seed-keyboard.jpg',    0, 1, '2026-10-19 09:35:00'),
(@g08, '/api/files/goods/seed-textbook.jpg',    0, 1, '2026-10-26 15:05:00'),
(@g09, '/api/files/goods/seed-sneakers.jpg',    0, 1, '2026-10-27 10:35:00'),
(@g10, '/api/files/goods/seed-accessories.jpg', 0, 1, '2026-10-20 18:05:00'),
(@g11, '/api/files/goods/seed-storage.jpg',     0, 1, '2026-10-22 19:05:00'),
(@g12, '/api/files/goods/seed-accessories.jpg', 0, 1, '2026-10-05 09:35:00'),
(@g13, '/api/files/goods/seed-textbook.jpg',    0, 1, '2026-10-06 10:35:00'),
(@g14, '/api/files/goods/seed-storage.jpg',     0, 1, '2026-10-07 14:05:00'),
(@g15, '/api/files/goods/seed-basketball.jpg',  0, 1, '2026-10-09 09:05:00'),
(@g16, '/api/files/goods/seed-sneakers.jpg',    0, 1, '2026-10-10 16:05:00'),
(@g17, '/api/files/goods/seed-notes.jpg',       0, 1, '2026-10-11 08:35:00'),
(@g18, '/api/files/goods/seed-storage.jpg',     0, 1, '2026-10-12 13:05:00'),
(@g19, '/api/files/goods/seed-guitar.jpg',      0, 1, '2026-10-13 15:05:00'),
(@g20, '/api/files/goods/seed-calculator.jpg',  0, 1, '2026-10-15 10:05:00'),
(@g21, '/api/files/goods/seed-phone.jpg',       0, 1, '2026-10-16 18:35:00'),
(@g22, '/api/files/goods/seed-storage.jpg',     0, 1, '2026-10-17 09:05:00'),
(@g23, '/api/files/goods/seed-textbook.jpg',    0, 1, '2026-10-18 10:35:00'),
(@g24, '/api/files/goods/seed-accessories.jpg', 0, 1, '2026-10-20 08:05:00');


-- =============================================================
-- 5. 订单（11 笔：5 已完成 / 2 待买家确认 / 2 待交易 / 2 已取消）
--    规则：seller_id 必须等于商品发布者；0/1/2 的商品 status=0；
--          已取消订单的商品已回上架 status=1（见第 3 步 G10/G11）。
-- =============================================================

INSERT INTO `orders` (goods_id, buyer_id, seller_id, order_status, create_time)
VALUES (@g01, @demo_buyer, @demo_seller, 2, '2026-10-08 12:30:00');
SET @o1 := LAST_INSERT_ID();

INSERT INTO `orders` (goods_id, buyer_id, seller_id, order_status, create_time)
VALUES (@g02, @demo_buyer2, @demo_seller2, 2, '2026-10-10 15:00:00');
SET @o2 := LAST_INSERT_ID();

INSERT INTO `orders` (goods_id, buyer_id, seller_id, order_status, create_time)
VALUES (@g03, @demo_student, @demo_seller, 2, '2026-10-12 10:00:00');
SET @o3 := LAST_INSERT_ID();

INSERT INTO `orders` (goods_id, buyer_id, seller_id, order_status, create_time)
VALUES (@g04, @demo_buyer, @demo_seller, 2, '2026-10-14 18:00:00');
SET @o4 := LAST_INSERT_ID();

INSERT INTO `orders` (goods_id, buyer_id, seller_id, order_status, create_time)
VALUES (@g05, @demo_buyer2, @demo_seller2, 2, '2026-10-16 11:00:00');
SET @o5 := LAST_INSERT_ID();

INSERT INTO `orders` (goods_id, buyer_id, seller_id, order_status, create_time)
VALUES (@g06, @demo_buyer, @demo_seller, 1, '2026-10-20 19:00:00');
SET @o6 := LAST_INSERT_ID();

INSERT INTO `orders` (goods_id, buyer_id, seller_id, order_status, create_time)
VALUES (@g07, @demo_student, @demo_seller, 1, '2026-10-21 20:00:00');
SET @o7 := LAST_INSERT_ID();

INSERT INTO `orders` (goods_id, buyer_id, seller_id, order_status, create_time)
VALUES (@g08, @demo_buyer, @demo_seller, 0, '2026-10-29 09:30:00');
SET @o8 := LAST_INSERT_ID();

INSERT INTO `orders` (goods_id, buyer_id, seller_id, order_status, create_time)
VALUES (@g09, @demo_buyer2, @demo_seller2, 0, '2026-10-30 14:00:00');
SET @o9 := LAST_INSERT_ID();

INSERT INTO `orders` (goods_id, buyer_id, seller_id, order_status, cancel_reason, cancel_by, cancel_time, create_time)
VALUES (@g10, @demo_buyer, @demo_seller, 3,
        '临时不需要了，改天再买', @demo_buyer, '2026-10-23 09:00:00', '2026-10-22 21:00:00');
SET @o10 := LAST_INSERT_ID();

INSERT INTO `orders` (goods_id, buyer_id, seller_id, order_status, cancel_reason, cancel_by, cancel_time, create_time)
VALUES (@g11, @demo_student, @demo_seller, 3,
        '当面验收发现水垢较多，协商后退单', @demo_seller, '2026-10-25 10:00:00', '2026-10-24 12:00:00');
SET @o11 := LAST_INSERT_ID();


-- =============================================================
-- 6. 评价（5 条，仅针对已完成订单 O1~O5，一单一评，评价人=买家）
-- =============================================================

INSERT INTO `evaluation` (order_id, user_id, content, score, create_time) VALUES
(@o1, @demo_buyer,  '约在三食堂门口验机，电池和外观都符合描述，卖家提前到了，沟通很顺畅。', 5, '2026-10-08 14:00:00'),
(@o2, @demo_buyer2, '教材比想象中保存得好，笔记很淡几乎不影响阅读，价格也实在。', 4, '2026-10-10 17:00:00'),
(@o3, @demo_student,'篮球手感不错，卖家还带了打气筒，面交过程很省心。', 5, '2026-10-12 12:00:00'),
(@o4, @demo_buyer,  '台灯亮度三档都正常，续航和描述一致，就是外壳有一点旧痕，整体满意。', 4, '2026-10-18 21:00:00'),
(@o5, @demo_buyer2, '真题册和解析都在，按约定时间在图书馆正门交易，很靠谱的学姐。', 5, '2026-10-16 13:00:00');


-- =============================================================
-- 7. 收藏（15 条：12 条 demo_buyer + 3 条剧情账号）
--    goods.favorite_count 已在第 3 步按真实行数写入（G14/G18/G20 各 2，其余 1）
-- =============================================================

INSERT INTO `favorite` (user_id, goods_id, create_time) VALUES
(@demo_buyer, @g12, '2026-10-21 10:00:00'),
(@demo_buyer, @g13, '2026-10-21 10:05:00'),
(@demo_buyer, @g14, '2026-10-21 10:10:00'),
(@demo_buyer, @g16, '2026-10-22 09:00:00'),
(@demo_buyer, @g17, '2026-10-22 09:05:00'),
(@demo_buyer, @g18, '2026-10-24 20:00:00'),
(@demo_buyer, @g19, '2026-10-25 15:00:00'),
(@demo_buyer, @g20, '2026-10-26 11:00:00'),
(@demo_buyer, @g21, '2026-10-27 19:00:00'),
(@demo_buyer, @g22, '2026-10-28 10:00:00'),
(@demo_buyer, @g23, '2026-10-29 14:00:00'),
(@demo_buyer, @g24, '2026-10-30 09:00:00'),
(@demo_student, @g14, '2026-10-22 21:00:00'),
(@demo_student, @g18, '2026-10-25 20:00:00'),
(@demo_buyer2, @g20, '2026-10-26 20:00:00');


-- =============================================================
-- 8. 求购（6 条：5 进行中 + 1 已关闭）
--    已关闭的那条 = 发布者已选定报价（WantedService.acceptQuote 会把求购同步置 2）
-- =============================================================

INSERT INTO `wanted_post` (user_id, title, category_id, budget_min, budget_max, campus, description, status, response_count, create_time, update_time)
VALUES (@demo_buyer, '求购一台二手 iPad 9 64G', @cat_digital, 800.00, 1200.00, '东区',
        '上网课和记笔记用，希望屏幕无划痕，可当面验机，预算可以再谈。', 1, 2, '2026-10-20 19:00:00', '2026-10-21 09:00:00');
SET @w1 := LAST_INSERT_ID();

INSERT INTO `wanted_post` (user_id, title, category_id, budget_min, budget_max, campus, description, status, response_count, create_time, update_time)
VALUES (@demo_buyer, '求购《计算机网络》谢希仁版教材', @cat_book, 20.00, 50.00, '北区',
        '期末复习急用，有笔记也可以，价格好商量，能当天面交最好。', 1, 1, '2026-10-22 15:00:00', '2026-10-22 16:00:00');
SET @w2 := LAST_INSERT_ID();

INSERT INTO `wanted_post` (user_id, title, category_id, budget_min, budget_max, campus, description, status, response_count, create_time, update_time)
VALUES (@demo_student, '求购宿舍用小型加湿器', @cat_life, 20.00, 40.00, '北区',
        '冬天开空调太干，希望静音、水箱好清洗，能正常使用就行。', 1, 0, '2026-10-24 10:00:00', '2026-10-24 10:00:00');
SET @w3 := LAST_INSERT_ID();

INSERT INTO `wanted_post` (user_id, title, category_id, budget_min, budget_max, campus, description, status, response_count, create_time, update_time)
VALUES (@demo_student, '求购羽毛球拍一对', @cat_sport, 100.00, 200.00, '西区',
        '体育课要用，拍面不要变形，可以约体育馆试打，成色过得去就行。', 1, 0, '2026-10-26 09:00:00', '2026-10-26 09:00:00');
SET @w4 := LAST_INSERT_ID();

INSERT INTO `wanted_post` (user_id, title, category_id, budget_min, budget_max, campus, description, status, response_count, create_time, update_time)
VALUES (@demo_buyer2, '求购一把尤克里里', @cat_music, 200.00, 400.00, '南区',
        '零基础入门，希望配件齐全，音准没问题，可以当面听一下音色。', 1, 0, '2026-10-27 16:00:00', '2026-10-27 16:00:00');
SET @w5 := LAST_INSERT_ID();

INSERT INTO `wanted_post` (user_id, title, category_id, budget_min, budget_max, campus, description, status, response_count, create_time, update_time)
VALUES (@demo_buyer, '求购一把机械键盘（写代码用）', @cat_digital, 100.00, 250.00, '东区',
        '希望轴体正常、键帽无缺失，能试打最好，青轴红轴都可以考虑。', 2, 2, '2026-10-28 10:00:00', '2026-10-28 19:30:00');
SET @w6 := LAST_INSERT_ID();


-- =============================================================
-- 9. 求购报价（5 条：3 待选 / 1 已选定 / 1 未入选）
--    W6 的两条报价符合 acceptQuote 规则：选定一条=1，其余自动=2，求购同步关闭。
-- =============================================================

INSERT INTO `wanted_response` (wanted_id, user_id, message, price, status, create_time) VALUES
(@w1, @demo_seller,  '有一台自用 iPad 9 64G，屏幕完好，含充电线，可以当面验机。', 950.00, 0, '2026-10-20 20:00:00'),
(@w1, @demo_seller2, '家里多一台 64G，含保护壳，东区可以面交，价格可小刀。', 1020.00, 0, '2026-10-21 09:00:00'),
(@w2, @demo_seller2, '有这本，笔记不多，图书馆正门可以给你，35 元带走。', 35.00, 0, '2026-10-22 16:00:00'),
(@w6, @demo_seller,  '有一把 87 键青轴键盘，键帽齐全，可以约时间试打。', 150.00, 1, '2026-10-28 18:00:00'),
(@w6, @demo_student, '自用红轴键盘，手感很好，只是键帽有点打油，便宜出。', 180.00, 2, '2026-10-28 19:00:00');


-- =============================================================
-- 10. 社区帖子（9 条，覆盖 4 个主题标签）
--     3 条带图，全部引用已存在的 seed 图；like_count / comment_count 与第 11/12 步一致。
-- =============================================================

INSERT INTO `community_post` (user_id, title, topic, content, image_url, goods_id, like_count, comment_count, view_count, create_time, update_time)
VALUES (@demo_seller2, '整理书桌翻出三本闲置教材', '闲置分享',
        '最近整理书架翻出三本用过的教材，四级词汇那本基本没怎么翻过。与其压箱底，不如转给下一届的同学，已经挂在商品中心了，价格都好商量。顺便说一句，书里只有少量铅笔标记，可以擦掉。',
        '/api/files/goods/seed-textbook.jpg', @g13, 3, 3, 86, '2026-10-20 21:00:00', '2026-10-21 12:00:00');
SET @p1 := LAST_INSERT_ID();

INSERT INTO `community_post` (user_id, title, topic, content, image_url, goods_id, like_count, comment_count, view_count, create_time, update_time)
VALUES (@demo_seller, 'K380 键盘用了两年的真实感受', '使用体验',
        '这把键盘陪我在图书馆写了两年作业和代码。最大的优点是能同时连三台设备，切换很顺手；缺点是按键偏软，长时间打字手会有点累。现在打算出掉换一把手感更硬的，感兴趣的可以看看同款商品链接。',
        NULL, @g07, 2, 2, 74, '2026-10-22 20:00:00', '2026-10-23 10:00:00');
SET @p2 := LAST_INSERT_ID();

INSERT INTO `community_post` (user_id, title, topic, content, image_url, goods_id, like_count, comment_count, view_count, create_time, update_time)
VALUES (@demo_student, '第一次校园面交，我是这样避坑的', '交易经验',
        '第一次在平台买二手，总结几点：一是尽量约在人多的食堂门口或图书馆门口；二是当面验货，别只看照片；三是贵重物品记得当面确认功能，别急着点确认收货。全程也就十几分钟，比想象中顺利。',
        NULL, NULL, 2, 2, 95, '2026-10-24 19:00:00', '2026-10-25 09:00:00');
SET @p3 := LAST_INSERT_ID();

INSERT INTO `community_post` (user_id, title, topic, content, image_url, goods_id, like_count, comment_count, view_count, create_time, update_time)
VALUES (@demo_seller2, '毕业季，把闲置交给下一届', '环保循环',
        '收拾行李时发现，很多东西其实还能用很久：收纳盒、床帘、台灯。丢掉可惜，带走又重。打包挂在平台后，两天就出得差不多了，买家也都是同校同学，见面聊两句还挺有意思。',
        '/api/files/goods/seed-storage.jpg', NULL, 2, 3, 108, '2026-10-25 20:00:00', '2026-10-26 11:00:00');
SET @p4 := LAST_INSERT_ID();

INSERT INTO `community_post` (user_id, title, topic, content, image_url, goods_id, like_count, comment_count, view_count, create_time, update_time)
VALUES (@demo_buyer2, '宿舍清理出的一批小物', '闲置分享',
        '换宿舍前把桌面上用不上的小物都拍了一遍：保护壳、数据线、收纳袋。单件不值钱，打包转给需要的人反而更快。整理的过程也挺解压的。',
        NULL, NULL, 1, 1, 63, '2026-10-26 18:00:00', '2026-10-26 18:00:00');
SET @p5 := LAST_INSERT_ID();

INSERT INTO `community_post` (user_id, title, topic, content, image_url, goods_id, like_count, comment_count, view_count, create_time, update_time)
VALUES (@demo_seller, '卖家视角：怎样拍出让人放心的商品图', '交易经验',
        '拍照建议：自然光下拍照，不要开美颜；把瑕疵单独拍一张，比只说九成新更有说服力；把配件一起拍进画面。透明一点，沟通成本会低很多，也少了很多来回问价的消息。',
        '/api/files/goods/seed-phone.jpg', NULL, 1, 2, 81, '2026-10-28 20:00:00', '2026-10-29 09:00:00');
SET @p6 := LAST_INSERT_ID();

INSERT INTO `community_post` (user_id, title, topic, content, image_url, goods_id, like_count, comment_count, view_count, create_time, update_time)
VALUES (@demo_student, '加厚瑜伽垫在宿舍真的实用吗', '使用体验',
        '用了半个月，体感是：8mm 确实比薄垫舒服，但收纳占地方；在宿舍铺开练操课动作没问题，楼下能听到一点点声音。如果宿舍空间紧张，建议买可折叠款。',
        NULL, NULL, 1, 2, 57, '2026-10-29 21:00:00', '2026-10-30 10:00:00');
SET @p7 := LAST_INSERT_ID();

INSERT INTO `community_post` (user_id, title, topic, content, image_url, goods_id, like_count, comment_count, view_count, create_time, update_time)
VALUES (@demo_seller2, '考研资料转让，附复习时间表', '闲置分享',
        '把去年用过的真题和笔记整理成一包，附上自己按月份排的复习时间表。资料不是越新越好，关键是能坚持按计划做完。需要的同学可以直接私聊我。',
        NULL, NULL, 0, 1, 66, '2026-10-30 16:00:00', '2026-10-30 16:00:00');
SET @p8 := LAST_INSERT_ID();

INSERT INTO `community_post` (user_id, title, topic, content, image_url, goods_id, like_count, comment_count, view_count, create_time, update_time)
VALUES (@demo_buyer, '一次以物换物的尝试', '环保循环',
        '用一本不再看的参考书，换到了室友闲置的小台灯，双方都觉得赚了。二手交易不只是买卖，有时候交换更适合。平台如果能加一个换物标签，应该会挺有趣。',
        NULL, NULL, 0, 2, 49, '2026-10-31 10:00:00', '2026-10-31 11:00:00');
SET @p9 := LAST_INSERT_ID();


-- =============================================================
-- 11. 社区评论（18 条：P1 3 / P2 2 / P3 2 / P4 3 / P5 1 / P6 2 / P7 2 / P8 1 / P9 2）
-- =============================================================

INSERT INTO `community_comment` (post_id, user_id, content, create_time) VALUES
(@p1, @demo_seller,  '教材保存得不错，我正好在找这本。', '2026-10-20 21:20:00'),
(@p1, @demo_student, '铅笔笔记能擦掉这点很加分。', '2026-10-20 22:00:00'),
(@p1, @demo_buyer2,  '四级词汇那本我正好需要，怎么联系你？', '2026-10-21 11:00:00'),

(@p2, @demo_buyer,   '也在用同款，确实适合图书馆。', '2026-10-22 20:40:00'),
(@p2, @demo_buyer2,  '三设备切换会不会偶尔断连？', '2026-10-23 09:30:00'),

(@p3, @demo_seller,  '补充一点，验货时记得看序列号或保修信息。', '2026-10-24 19:30:00'),
(@p3, @demo_buyer2,  '食堂门口确实最稳妥，人多也不尴尬。', '2026-10-25 08:00:00'),

(@p4, @demo_buyer,   '毕业季真的会产生很多还能用的东西。', '2026-10-25 20:30:00'),
(@p4, @demo_seller,  '收纳盒还有吗，想收两个。', '2026-10-26 09:00:00'),
(@p4, @demo_student, '这种循环利用比直接丢掉有意义多了。', '2026-10-26 10:30:00'),

(@p5, @demo_student, '保护壳还在的话我想要一个。', '2026-10-26 19:00:00'),

(@p6, @demo_buyer,   '瑕疵单独拍一张这个建议很实用。', '2026-10-28 21:00:00'),
(@p6, @demo_student, '光线真的重要，我之前在宿舍拍出来全是黄的。', '2026-10-29 08:30:00'),

(@p7, @demo_seller2, '8mm 确实舒服，就是卷起来有点粗。', '2026-10-29 22:00:00'),
(@p7, @demo_student, '对，我最后是挂在床尾收纳的。', '2026-10-30 09:30:00'),

(@p8, @demo_buyer2,  '时间表能单独发一份吗，想参考一下。', '2026-10-30 17:00:00'),

(@p9, @demo_seller,  '换物这个想法不错，双方都省事。', '2026-10-31 10:30:00'),
(@p9, @demo_student, '我上次也是换的，比卖还快。', '2026-10-31 11:00:00');


-- =============================================================
-- 12. 社区点赞（12 条，遵守 uk_post_user(post_id, user_id) 一人一赞）
-- =============================================================

INSERT INTO `community_like` (post_id, user_id, create_time) VALUES
(@p1, @demo_seller,  '2026-10-20 21:15:00'),
(@p1, @demo_student, '2026-10-20 21:50:00'),
(@p1, @demo_buyer2,  '2026-10-21 10:50:00'),
(@p2, @demo_buyer,   '2026-10-22 20:30:00'),
(@p2, @demo_student, '2026-10-23 12:00:00'),
(@p3, @demo_buyer,   '2026-10-24 19:20:00'),
(@p3, @demo_seller2, '2026-10-25 09:00:00'),
(@p4, @demo_buyer,   '2026-10-25 20:20:00'),
(@p4, @demo_student, '2026-10-26 10:20:00'),
(@p5, @demo_seller,  '2026-10-26 18:30:00'),
(@p6, @demo_buyer2,  '2026-10-28 20:40:00'),
(@p7, @demo_buyer,   '2026-10-29 21:30:00');


-- =============================================================
-- 13. 私聊消息（18 条 / 3 个会话）
--     session_key 规则：min(双方ID)-max(双方ID)-商品ID（无商品用 0），
--     与 ChatService.sessionKeyOf 完全一致，避免会话被拆成两个。
--     会话 A：demo_buyer ↔ demo_seller（G01，8 条，含 1 条订单卡片）
--     会话 B：demo_buyer2 ↔ demo_seller2（G02，6 条，含 1 条图片）
--     会话 C：demo_buyer ↔ demo_seller（无商品，4 条，其中 2 条未读→买家红点）
-- =============================================================

SET @sess_a := CONCAT(LEAST(@demo_buyer, @demo_seller), '-', GREATEST(@demo_buyer, @demo_seller), '-', @g01);
SET @sess_b := CONCAT(LEAST(@demo_buyer2, @demo_seller2), '-', GREATEST(@demo_buyer2, @demo_seller2), '-', @g02);
SET @sess_c := CONCAT(LEAST(@demo_buyer, @demo_seller), '-', GREATEST(@demo_buyer, @demo_seller), '-', 0);

-- 会话 A
INSERT INTO `chat_message` (session_key, from_user_id, to_user_id, goods_id, content, message_type, related_id, is_read, create_time) VALUES
(@sess_a, @demo_buyer,  @demo_seller, @g01, '同学你好，这台 iPhone 12 还在吗？', 'text', NULL, 1, '2026-10-07 20:00:00'),
(@sess_a, @demo_seller, @demo_buyer,  @g01, '在的，自用机，无拆修，可以当面验机。', 'text', NULL, 1, '2026-10-07 20:05:00'),
(@sess_a, @demo_buyer,  @demo_seller, @g01, '电池健康度多少？价格还能少一点吗？', 'text', NULL, 1, '2026-10-07 20:10:00'),
(@sess_a, @demo_seller, @demo_buyer,  @g01, '电池健康 88%，价格已经是底价了，可以送你原装充电线。', 'text', NULL, 1, '2026-10-07 20:12:00'),
(@sess_a, @demo_buyer,  @demo_seller, @g01, '行，那我明天中午在三食堂门口看一下。', 'text', NULL, 1, '2026-10-07 20:15:00'),
(@sess_a, @demo_seller, @demo_buyer,  @g01, '好的，明天十二点我在门口等你。', 'text', NULL, 1, '2026-10-07 20:18:00'),
(@sess_a, @demo_buyer,  @demo_seller, @g01, '我已经下单了，麻烦你看一下订单。', 'text', NULL, 1, '2026-10-08 12:30:00'),
(@sess_a, @demo_buyer,  @demo_seller, @g01, '订单卡片：iPhone 12 128G 95新 ¥2200.00，当前状态待交易', 'order', @o1, 1, '2026-10-08 12:31:00');

-- 会话 B
INSERT INTO `chat_message` (session_key, from_user_id, to_user_id, goods_id, content, message_type, related_id, is_read, create_time) VALUES
(@sess_b, @demo_buyer2,  @demo_seller2, @g02, '学姐你好，高数教材是同济第七版吗？', 'text', NULL, 1, '2026-10-09 19:00:00'),
(@sess_b, @demo_seller2, @demo_buyer2,  @g02, '/api/files/goods/seed-textbook.jpg', 'image', NULL, 1, '2026-10-09 19:03:00'),
(@sess_b, @demo_buyer2,  @demo_seller2, @g02, '看起来保存得不错，习题解析也一起出吗？', 'text', NULL, 1, '2026-10-09 19:08:00'),
(@sess_b, @demo_seller2, @demo_buyer2,  @g02, '对，解析册一起出，45 元。', 'text', NULL, 1, '2026-10-09 19:10:00'),
(@sess_b, @demo_buyer2,  @demo_seller2, @g02, '那我拍了，明天在图书馆正门交易可以吗？', 'text', NULL, 1, '2026-10-10 09:00:00'),
(@sess_b, @demo_seller2, @demo_buyer2,  @g02, '可以，我十点到，到了给你发消息。', 'text', NULL, 1, '2026-10-10 09:10:00');

-- 会话 C（最后两条为卖家发来、买家未读）
INSERT INTO `chat_message` (session_key, from_user_id, to_user_id, goods_id, content, message_type, related_id, is_read, create_time) VALUES
(@sess_c, @demo_buyer,  @demo_seller, NULL, '同学，你主页那台打印机还在卖吗？', 'text', NULL, 1, '2026-10-30 21:00:00'),
(@sess_c, @demo_seller, @demo_buyer,  NULL, '还在的，墨盒还剩一半，可以当场试打印。', 'text', NULL, 1, '2026-10-30 21:03:00'),
(@sess_c, @demo_seller, @demo_buyer,  NULL, '如果今晚要的话，我可以顺路送到东区楼下。', 'text', NULL, 0, '2026-10-31 08:30:00'),
(@sess_c, @demo_seller, @demo_buyer,  NULL, '看到消息回我一下就行，我下午都在这边。', 'text', NULL, 0, '2026-10-31 08:32:00');


-- =============================================================
-- 14. 交易申诉（2 条：1 待处理 + 1 已受理）
--     A1 针对已完成订单 O4（台灯），买家申诉商品存在未描述的裂纹；
--     A2 针对待确认订单 O6，卖家申诉买家未确认收货。
--     evidences 为 JSON 数组文本，URL 必须是 /api/files/ 开头。
-- =============================================================

INSERT INTO `trade_appeal` (order_id, user_id, reason, detail, evidences, status, handle_by, handle_comment, create_time, handle_time) VALUES
(@o4, @demo_buyer, '商品与描述不符',
 '当面只看清外壳有旧痕，回去用了几天才发现灯罩底部有一处裂纹，描述里没有提到，担心继续使用不安全，希望协商补偿或退货。',
 '["/api/files/goods/seed-lamp.jpg"]', 0, NULL, NULL,
 '2026-10-20 10:00:00', NULL);

INSERT INTO `trade_appeal` (order_id, user_id, reason, detail, evidences, status, handle_by, handle_comment, create_time, handle_time) VALUES
(@o6, @demo_seller, '买家迟迟不确认收货',
 '耳机已在约定时间当面交付并试听正常，买家三天没有点击确认收货，影响我的成交记录，请协助处理。',
 NULL, 1, @demo_admin, '已联系买家核实，买家确认收到且使用正常，已提醒其完成确认收货操作。',
 '2026-10-23 20:00:00', '2026-10-24 10:00:00');


-- =============================================================
-- 15. 卖家申请（3 条：2 待审核 + 1 已通过）
--     待审核的 2 条用于管理后台「卖家申请」红点与审核演示；
--     申请原因长度均 ≥ 10 字，符合 SellerApplicationService 校验。
-- =============================================================

INSERT INTO `seller_application` (user_id, reason, status, reviewer_id, review_comment, create_time, review_time) VALUES
(@demo_seller, '毕业前整理出一批数码产品和教材需要出售，希望开通卖家权限，方便在平台发布商品。',
 1, @demo_admin, '信息属实，同意开通卖家权限。', '2026-10-02 09:00:00', '2026-10-03 10:00:00');

INSERT INTO `seller_application` (user_id, reason, status, reviewer_id, review_comment, create_time, review_time) VALUES
(@demo_buyer, '想把自己闲置的教材和数码配件整理出售，希望开通卖家权限，方便在校内转给需要的同学。',
 0, NULL, NULL, '2026-10-27 10:00:00', NULL);

INSERT INTO `seller_application` (user_id, reason, status, reviewer_id, review_comment, create_time, review_time) VALUES
(@demo_student, '宿舍清理出一批用不上的生活用品和运动器材，希望申请成为卖家，把它们转给需要的同学。',
 0, NULL, NULL, '2026-10-30 09:00:00', NULL);


-- =============================================================
-- 16. 管理员操作记录（4 条，仅文案，不产生额外数据变更）
-- =============================================================

INSERT INTO `admin_record` (admin_id, operation, create_time) VALUES
(@demo_admin, '审核卖家申请：demo_seller 通过', '2026-10-03 10:00:00'),
(@demo_admin, '受理交易申诉：订单 O6 买家未确认收货', '2026-10-24 10:00:00'),
(@demo_admin, '审核商品信息：抽查 9 件在售商品，均符合校园闲置规范', '2026-10-28 15:00:00'),
(@demo_admin, '平台数据巡检：核对演示数据冗余计数与关联关系', '2026-10-31 09:00:00');


-- =============================================================
-- 17. 提交事务
-- =============================================================

COMMIT;


-- =============================================================
-- 18. 执行后验收（只读；已提交后运行，不修改任何数据）
--     每行给出 actual 与 expected，expected 与设计数量一致。
-- =============================================================

SELECT '账号(6)' AS item,
       (SELECT COUNT(*) FROM `user` WHERE username IN
          ('demo_buyer','demo_seller','demo_admin','demo_seller2','demo_student','demo_buyer2')) AS actual,
       6 AS expected
UNION ALL SELECT '商品(24)',
       (SELECT COUNT(*) FROM `goods` WHERE description LIKE '%【DEMO-V1】%'), 24
UNION ALL SELECT '商品在售(15)',
       (SELECT COUNT(*) FROM `goods` WHERE description LIKE '%【DEMO-V1】%' AND status = 1), 15
UNION ALL SELECT '商品下架(9)',
       (SELECT COUNT(*) FROM `goods` WHERE description LIKE '%【DEMO-V1】%' AND status = 0), 9
UNION ALL SELECT '商品图(24)',
       (SELECT COUNT(*) FROM `goods_image` gi JOIN `goods` g ON gi.goods_id = g.goods_id
         WHERE g.description LIKE '%【DEMO-V1】%'), 24
UNION ALL SELECT '订单(11)',
       (SELECT COUNT(*) FROM `orders` o JOIN `goods` g ON o.goods_id = g.goods_id
         WHERE g.description LIKE '%【DEMO-V1】%'), 11
UNION ALL SELECT '评价(5)',
       (SELECT COUNT(*) FROM `evaluation` e JOIN `orders` o ON e.order_id = o.order_id
         JOIN `goods` g ON o.goods_id = g.goods_id WHERE g.description LIKE '%【DEMO-V1】%'), 5
UNION ALL SELECT '收藏(15)',
       (SELECT COUNT(*) FROM `favorite` f JOIN `goods` g ON f.goods_id = g.goods_id
         WHERE g.description LIKE '%【DEMO-V1】%'), 15
UNION ALL SELECT '求购(6)',
       (SELECT COUNT(*) FROM `wanted_post` w JOIN `user` u ON w.user_id = u.user_id
         WHERE u.username LIKE 'demo\\_%'), 6
UNION ALL SELECT '求购报价(5)',
       (SELECT COUNT(*) FROM `wanted_response` r JOIN `user` u ON r.user_id = u.user_id
         WHERE u.username LIKE 'demo\\_%'), 5
UNION ALL SELECT '社区帖子(9)',
       (SELECT COUNT(*) FROM `community_post` p JOIN `user` u ON p.user_id = u.user_id
         WHERE u.username LIKE 'demo\\_%'), 9
UNION ALL SELECT '社区评论(18)',
       (SELECT COUNT(*) FROM `community_comment` c JOIN `user` u ON c.user_id = u.user_id
         WHERE u.username LIKE 'demo\\_%'), 18
UNION ALL SELECT '社区点赞(12)',
       (SELECT COUNT(*) FROM `community_like` l JOIN `user` u ON l.user_id = u.user_id
         WHERE u.username LIKE 'demo\\_%'), 12
UNION ALL SELECT '私聊消息(18)',
       (SELECT COUNT(*) FROM `chat_message` m JOIN `user` u ON m.from_user_id = u.user_id
         WHERE u.username LIKE 'demo\\_%'), 18
UNION ALL SELECT '申诉(2)',
       (SELECT COUNT(*) FROM `trade_appeal` a JOIN `user` u ON a.user_id = u.user_id
         WHERE u.username LIKE 'demo\\_%'), 2
UNION ALL SELECT '卖家申请(3)',
       (SELECT COUNT(*) FROM `seller_application` s JOIN `user` u ON s.user_id = u.user_id
         WHERE u.username LIKE 'demo\\_%'), 3
UNION ALL SELECT '管理记录(4)',
       (SELECT COUNT(*) FROM `admin_record` r JOIN `user` u ON r.admin_id = u.user_id
         WHERE u.username LIKE 'demo\\_%'), 4;

-- 状态与关联一致性（每项都应为 0）
SELECT '订单卖家与商品发布者不一致' AS check_item,
       (SELECT COUNT(*) FROM `orders` o JOIN `goods` g ON o.goods_id = g.goods_id
         WHERE g.description LIKE '%【DEMO-V1】%' AND o.seller_id <> g.user_id) AS bad_rows
UNION ALL SELECT '订单状态与商品状态矛盾（0/1/2 订单应对应下架商品）',
       (SELECT COUNT(*) FROM `orders` o JOIN `goods` g ON o.goods_id = g.goods_id
         WHERE g.description LIKE '%【DEMO-V1】%'
           AND o.order_status IN (0,1,2) AND g.status <> 0)
UNION ALL SELECT '已取消订单的商品未回上架',
       (SELECT COUNT(*) FROM `orders` o JOIN `goods` g ON o.goods_id = g.goods_id
         WHERE g.description LIKE '%【DEMO-V1】%'
           AND o.order_status = 3 AND g.status <> 1)
UNION ALL SELECT 'favorite_count 与真实收藏数不一致',
       (SELECT COUNT(*) FROM `goods` g
         WHERE g.description LIKE '%【DEMO-V1】%'
           AND g.favorite_count <> (SELECT COUNT(*) FROM `favorite` f WHERE f.goods_id = g.goods_id))
UNION ALL SELECT 'response_count 与真实报价数不一致',
       (SELECT COUNT(*) FROM `wanted_post` w
         WHERE w.title IN ('求购一台二手 iPad 9 64G','求购《计算机网络》谢希仁版教材',
                           '求购宿舍用小型加湿器','求购羽毛球拍一对','求购一把尤克里里','求购一把机械键盘（写代码用）')
           AND w.response_count <> (SELECT COUNT(*) FROM `wanted_response` r WHERE r.wanted_id = w.wanted_id))
UNION ALL SELECT 'comment_count 与真实评论数不一致',
       (SELECT COUNT(*) FROM `community_post` p
         WHERE p.title IN ('整理书桌翻出三本闲置教材','K380 键盘用了两年的真实感受',
                           '第一次校园面交，我是这样避坑的','毕业季，把闲置交给下一届',
                           '宿舍清理出的一批小物','卖家视角：怎样拍出让人放心的商品图',
                           '加厚瑜伽垫在宿舍真的实用吗','考研资料转让，附复习时间表',
                           '一次以物换物的尝试')
           AND p.comment_count <> (SELECT COUNT(*) FROM `community_comment` c WHERE c.post_id = p.post_id))
UNION ALL SELECT 'like_count 与真实点赞数不一致',
       (SELECT COUNT(*) FROM `community_post` p
         WHERE p.title IN ('整理书桌翻出三本闲置教材','K380 键盘用了两年的真实感受',
                           '第一次校园面交，我是这样避坑的','毕业季，把闲置交给下一届',
                           '宿舍清理出的一批小物','卖家视角：怎样拍出让人放心的商品图',
                           '加厚瑜伽垫在宿舍真的实用吗','考研资料转让，附复习时间表',
                           '一次以物换物的尝试')
           AND p.like_count <> (SELECT COUNT(*) FROM `community_like` l WHERE l.post_id = p.post_id))
UNION ALL SELECT '私聊消息 session_key 与双方ID不一致',
       (SELECT COUNT(*) FROM `chat_message` m JOIN `user` u ON m.from_user_id = u.user_id
         WHERE u.username LIKE 'demo\\_%'
           AND m.session_key <> CONCAT(LEAST(m.from_user_id, m.to_user_id), '-',
                                       GREATEST(m.from_user_id, m.to_user_id), '-',
                                       IFNULL(m.goods_id, 0)))
UNION ALL SELECT '商品图引用了不存在的 seed 文件（应为 0，仅校验命名白名单）',
       (SELECT COUNT(*) FROM `goods_image` gi JOIN `goods` g ON gi.goods_id = g.goods_id
         WHERE g.description LIKE '%【DEMO-V1】%'
           AND gi.image_url NOT IN (
             '/api/files/goods/seed-accessories.jpg','/api/files/goods/seed-basketball.jpg',
             '/api/files/goods/seed-calculator.jpg','/api/files/goods/seed-camera.jpg',
             '/api/files/goods/seed-guitar.jpg','/api/files/goods/seed-keyboard.jpg',
             '/api/files/goods/seed-lamp.jpg','/api/files/goods/seed-notes.jpg',
             '/api/files/goods/seed-phone.jpg','/api/files/goods/seed-sneakers.jpg',
             '/api/files/goods/seed-storage.jpg','/api/files/goods/seed-textbook.jpg'));

-- 账号角色速查（供登录页/README 核对；不含密码哈希）
SELECT username, role, status, create_time
FROM `user`
WHERE username IN ('demo_buyer','demo_seller','demo_admin','demo_seller2','demo_student','demo_buyer2')
ORDER BY user_id;
