-- =============================================================
-- 校园二手交易平台 V1.3.1 · 开发测试商品补充脚本（dev-seed-goods-v1.3.sql）
--
-- ⚠️ 性质：开发/演示用测试数据，不是生产数据
-- ⚠️ 用法：请先在 Navicat 中审阅，确认后再手动执行
-- ⚠️ 数量：共 25 条（V1.3.1 精简版，由原 40 条缩减而来）
-- ⚠️ 安全：
--   1. 本脚本**只 INSERT、不 UPDATE、不 DELETE**，不影响任何已有业务数据
--   2. 具备重复执行保护：通过「测试商品统一标记 + NOT EXISTS」保证多次执行
--      不会产生重复商品
--   3. 不创建订单、收藏、留言、评价等交易/互动关系，只补充商品本身
--   4. 图片字段全部留空（不插 goods_image），由前端 resolveGoodsImage 显示
--      统一的奶油色品类兜底卡，之后可在「我的店铺 → 编辑商品」里逐条上传真实图片
--
-- 关联合法性说明（已按 schema.sql / data.sql / 前端代码核对）：
--   · goods.user_id     → user.user_id      使用 2（chen_sell，seller）与 3（li_sell，seller），
--                                            均为 data.sql 中既有的合法卖家账号
--   · goods.category_id → category.category_id  使用 1/2/3/4/5/6/7/10 中已存在的分类
--                                            （1 数码产品 2 图书教材 3 生活用品 4 运动用品
--                                              5 服装鞋帽 6 学习资料 7 宿舍用品 10 其他）
--   · goods.status      = 1（在售），公开列表/首页/专题集市只显示 status=1
--   · condition_level   取值 1~5，与 GoodsPublishWizard 中成色枚举一致
--   · campus            取值 东区/西区/南区/北区，与 GoodsPublishWizard 中校区选项一致
--
-- 专题命中说明（依据前端 Discover.vue 实际筛选逻辑，关键词必须出现在**标题**中，
--               后端 goodsName 搜索只匹配 goods_name，不匹配 description）：
--   · 「考研考公与毕业季闲置」专题 = 标题含「考研」（无分类限制）
--       → 命中 4 条学习资料（标题全部含「考研」）
--   · 「新生入学必备」专题 = categoryId=3(生活用品) + 标题含「宿舍」
--       （前端 matchCategoryId(['生活用品','宿舍用品']) 先命中生活用品）
--       → 命中 1 条：宿舍收纳盒三件套（category_id=3，标题含宿舍）
--   · 「宿舍改造与收纳」专题 = categoryId=7(宿舍用品) + 标题含「收纳」
--       → 命中 1 条：宿舍收纳挂袋（门后款）（category_id=7，标题含收纳）
--   · 「数码设备与学习装备」专题 = categoryId=1(数码产品) + 热度排序
--       → 命中 5 条数码产品
--   · 「低预算实用好物」专题 = 价格 ≤50 升序（无分类/标题限制）
--       → 命中多条低价商品
--   · 「环保循环与闲置再利用」专题 = 浏览+收藏加权排序（无过滤条件）
--       → 全部 25 条按热度参与
--
-- 重复执行保护方案：
--   所有测试商品的 description 末尾都带有统一标记 【SEED-V131】，
--   每条 INSERT 前用 NOT EXISTS 检查「同名 + 含该标记」的记录，
--   已存在则跳过，保证脚本可重复执行、不会产生重复数据。
-- =============================================================

USE campus_trade;

-- -------------------------------------------------------------
-- 1. 数码产品（category_id = 1），共 5 条
-- -------------------------------------------------------------
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 2, 1, '罗技 K380 蓝牙键盘', '多设备切换顺畅，图书馆自习敲代码很安静，功能正常。【SEED-V131】', 55.00, 1, 12, 0, 3, '东区', '三食堂门口', '2026-10-10 10:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '罗技 K380 蓝牙键盘' AND description LIKE '%【SEED-V131】%');

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 2, 1, '小米 Redmi Buds 4 青春版耳机', '续航正常，佩戴舒适，带充电仓，适合通勤听歌。【SEED-V131】', 49.00, 1, 8, 0, 3, '东区', '三食堂门口', '2026-10-11 11:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '小米 Redmi Buds 4 青春版耳机' AND description LIKE '%【SEED-V131】%');

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 2, 1, '罗技 G304 无线鼠标', '手感轻盈，电池耐用，适合日常学习与轻度游戏。【SEED-V131】', 89.00, 1, 15, 0, 2, '东区', '图书馆正门', '2026-10-12 14:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '罗技 G304 无线鼠标' AND description LIKE '%【SEED-V131】%');

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 3, 1, '24 寸 1080P 显示器', '色彩正常无坏点，带 HDMI 线，宿舍外接笔记本刚好。【SEED-V131】', 320.00, 1, 22, 0, 4, '北区', '5号楼下', '2026-10-13 09:30:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '24 寸 1080P 显示器' AND description LIKE '%【SEED-V131】%');

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 2, 1, '20000mAh 移动电源', '双向快充，能上高铁，容量实标，外观无明显划痕。【SEED-V131】', 65.00, 1, 9, 0, 3, '东区', '三食堂门口', '2026-10-15 19:30:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '20000mAh 移动电源' AND description LIKE '%【SEED-V131】%');

-- -------------------------------------------------------------
-- 2. 图书教材（category_id = 2），共 4 条
-- -------------------------------------------------------------
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 3, 2, '高等数学（同济第七版）上下册', '考研基础必备，内页干净无大量笔记，无缺页。【SEED-V131】', 25.00, 1, 18, 0, 4, '北区', '图书馆正门', '2026-10-10 09:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '高等数学（同济第七版）上下册' AND description LIKE '%【SEED-V131】%');

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 2, 2, '大学英语四级词汇书', '乱序版，几乎全新，只拆了塑封。【SEED-V131】', 15.00, 1, 5, 0, 1, '东区', '三食堂门口', '2026-10-12 17:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '大学英语四级词汇书' AND description LIKE '%【SEED-V131】%');

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 3, 2, '计算机网络（谢希仁版）', '计网考研/期末通用，重点已用荧光笔标注。【SEED-V131】', 20.00, 1, 10, 0, 3, '北区', '图书馆正门', '2026-10-13 11:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '计算机网络（谢希仁版）' AND description LIKE '%【SEED-V131】%');

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 3, 2, '操作系统概念（恐龙书）', '经典教材，中文第九版，适合配套课程使用。【SEED-V131】', 30.00, 1, 9, 0, 3, '北区', '5号楼下', '2026-10-14 14:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '操作系统概念（恐龙书）' AND description LIKE '%【SEED-V131】%');

-- -------------------------------------------------------------
-- 3. 生活用品（category_id = 3），共 3 条
--    其中「宿舍收纳盒三件套」标题含「宿舍」，命中「新生入学必备」专题
-- -------------------------------------------------------------
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 2, 3, '折叠小台灯（USB 充电）', '三档调光，折叠后不占地方，熄灯后学习神器。【SEED-V131】', 22.00, 1, 14, 0, 2, '东区', '三食堂门口', '2026-10-10 15:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '折叠小台灯（USB 充电）' AND description LIKE '%【SEED-V131】%');

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 3, 3, '桌面小风扇（静音款）', '三档风力，声音很小，宿舍/图书馆都能用。【SEED-V131】', 25.00, 1, 16, 0, 2, '北区', '5号楼下', '2026-10-11 10:30:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '桌面小风扇（静音款）' AND description LIKE '%【SEED-V131】%');

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 2, 3, '宿舍收纳盒三件套', '不同尺寸组合，装袜子内衣杂物刚好。【SEED-V131】', 20.00, 1, 12, 0, 2, '东区', '三食堂门口', '2026-10-13 16:30:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '宿舍收纳盒三件套' AND description LIKE '%【SEED-V131】%');

-- -------------------------------------------------------------
-- 4. 运动用品（category_id = 4），共 2 条
-- -------------------------------------------------------------
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 2, 4, '羽毛球拍（双拍+球）', '入门训练拍，送一桶球，适合体育课/社团。【SEED-V131】', 50.00, 1, 11, 0, 3, '西区', '体育馆侧门', '2026-10-10 18:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '羽毛球拍（双拍+球）' AND description LIKE '%【SEED-V131】%');

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 2, 4, '哑铃（5kg×2 可调）', '宿舍健身入门，重量可调，不占地方。【SEED-V131】', 60.00, 1, 9, 0, 3, '西区', '体育馆侧门', '2026-10-12 16:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '哑铃（5kg×2 可调）' AND description LIKE '%【SEED-V131】%');

-- -------------------------------------------------------------
-- 5. 服装鞋帽（category_id = 5），共 2 条
-- -------------------------------------------------------------
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 3, 5, '白色帆布鞋（38 码）', '只穿过两次，鞋底干净，适合春夏搭配。【SEED-V131】', 25.00, 1, 8, 0, 2, '北区', '5号楼下', '2026-10-10 14:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '白色帆布鞋（38 码）' AND description LIKE '%【SEED-V131】%');

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 3, 5, '牛仔外套（M 码）', '版型正常，无破损污渍，春秋刚好。【SEED-V131】', 45.00, 1, 10, 0, 3, '北区', '图书馆正门', '2026-10-11 09:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '牛仔外套（M 码）' AND description LIKE '%【SEED-V131】%');

-- -------------------------------------------------------------
-- 6. 学习资料（category_id = 6），共 4 条
--    标题全部含「考研」→ 命中「考研考公与毕业季闲置」专题
-- -------------------------------------------------------------
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 3, 6, '考研政治核心考案', '知识点覆盖全，配套习题已做完，适合二轮复习。【SEED-V131】', 20.00, 1, 13, 0, 3, '北区', '图书馆正门', '2026-10-10 08:30:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '考研政治核心考案' AND description LIKE '%【SEED-V131】%');

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 3, 6, '考研数学复习全书（数一）', '基础+提高篇合一，例题解析详细，书角轻微磨损。【SEED-V131】', 35.00, 1, 17, 0, 3, '北区', '图书馆正门', '2026-10-11 08:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '考研数学复习全书（数一）' AND description LIKE '%【SEED-V131】%');

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 2, 6, '考研英语二真题详解', '近五年真题+逐句解析，作文模板已标注。【SEED-V131】', 22.00, 1, 11, 0, 2, '东区', '三食堂门口', '2026-10-12 08:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '考研英语二真题详解' AND description LIKE '%【SEED-V131】%');

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 2, 6, '考研专业课笔记（计算机）', '数据结构+计组+操作系统三科手写笔记，扫描版打印。【SEED-V131】', 28.00, 1, 6, 0, 3, '东区', '三食堂门口', '2026-10-14 08:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '考研专业课笔记（计算机）' AND description LIKE '%【SEED-V131】%');

-- -------------------------------------------------------------
-- 7. 宿舍用品（category_id = 7），共 4 条
--    其中「宿舍收纳挂袋（门后款）」标题含「收纳」，
--    命中「宿舍改造与收纳」专题（categoryId=7 + 标题含收纳）
-- -------------------------------------------------------------
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 2, 7, '宿舍床帘（遮光款）', '两片+顶，遮光效果好，上铺下铺都能装。【SEED-V131】', 35.00, 1, 10, 0, 2, '东区', '三食堂门口', '2026-10-10 19:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '宿舍床帘（遮光款）' AND description LIKE '%【SEED-V131】%');

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 3, 7, '宿舍桌面置物架（双层）', '铁质承重好，放书放杂物都稳。【SEED-V131】', 18.00, 1, 8, 0, 3, '北区', '5号楼下', '2026-10-11 16:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '宿舍桌面置物架（双层）' AND description LIKE '%【SEED-V131】%');

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 3, 7, '宿舍鞋架（三层）', '组装简单，能放 6-9 双鞋，不占过道。【SEED-V131】', 15.00, 1, 5, 0, 3, '北区', '5号楼下', '2026-10-13 10:30:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '宿舍鞋架（三层）' AND description LIKE '%【SEED-V131】%');

INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 2, 7, '宿舍收纳挂袋（门后款）', '宿舍收纳补充，挂门后放袜子/小物件刚好。【SEED-V131】', 10.00, 1, 4, 0, 2, '东区', '三食堂门口', '2026-10-14 09:30:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '宿舍收纳挂袋（门后款）' AND description LIKE '%【SEED-V131】%');

-- -------------------------------------------------------------
-- 8. 其他（category_id = 10），共 1 条
--    注意：该条仅出现在商品列表/首页，不会被「宿舍改造与收纳」专题命中
--    （该专题固定 categoryId=7，而本条 category_id=10；关键词只在标题中检索）
-- -------------------------------------------------------------
INSERT INTO `goods` (user_id, category_id, goods_name, description, price, status, view_count, favorite_count, condition_level, campus, location, create_time)
SELECT 2, 10, '宿舍收纳箱（大号带盖）', '大容量，放换季衣物/被子刚好，带盖防尘。【SEED-V131】', 22.00, 1, 6, 0, 3, '东区', '三食堂门口', '2026-10-14 11:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM goods WHERE goods_name = '宿舍收纳箱（大号带盖）' AND description LIKE '%【SEED-V131】%');

-- =============================================================
-- 自检查询（执行完上面 25 条 INSERT 后，在 Navicat 中手动运行）
-- =============================================================

-- ① 测试商品总数（预期 = 25；若曾执行过旧版 40 条脚本，实际值可能更大，
--    此时用「②分类分布」对照下方预期分布即可确认本批 25 条已就位）
SELECT COUNT(*) AS seed_goods_count
FROM goods
WHERE description LIKE '%【SEED-V131】%';

-- ② 测试商品分类分布（预期：数码产品 5 / 图书教材 4 / 生活用品 3 /
--    运动用品 2 / 服装鞋帽 2 / 学习资料 4 / 宿舍用品 4 / 其他 1，合计 25）
SELECT c.category_id, c.category_name AS 分类, COUNT(*) AS 数量
FROM goods g
LEFT JOIN category c ON g.category_id = c.category_id
WHERE g.description LIKE '%【SEED-V131】%'
GROUP BY c.category_id, c.category_name
ORDER BY c.category_id;

-- ③ 关键词覆盖核对
--    标题含「考研」（预期 4 条，命中考研考公专题）
SELECT COUNT(*) AS exam_theme_hits FROM goods
WHERE description LIKE '%【SEED-V131】%' AND goods_name LIKE '%考研%';
--    categoryId=7 且标题含「收纳」（预期 1 条，命中宿舍改造与收纳专题）
SELECT COUNT(*) AS dorm_theme_hits FROM goods g
WHERE g.description LIKE '%【SEED-V131】%' AND g.category_id = 7 AND g.goods_name LIKE '%收纳%';
--    categoryId=3 且标题含「宿舍」（预期 1 条，命中新生入学必备专题）
SELECT COUNT(*) AS freshman_theme_hits FROM goods g
WHERE g.description LIKE '%【SEED-V131】%' AND g.category_id = 3 AND g.goods_name LIKE '%宿舍%';
-- =============================================================
