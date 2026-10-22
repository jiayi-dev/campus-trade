-- =============================================================
-- campus-trade V1.2 历史测试数据修正：学生身份名下存在商品
--
-- 背景：
--   后端 GoodsService.insert 只允许 role = seller / admin 发布商品，
--   学生必须先走「申请成为卖家 → 管理员审核」。
--   线上库中出现的「学生名下有商品」，是早期测试数据直接 INSERT
--   绕过服务层造成的脏数据，不是权限漏洞。
--
-- 使用方式：
--   1. 本脚本仅供人工在 Navicat / MySQL 客户端逐段执行；
--   2. 先执行【第一步 诊断】，根据结果选择【方案 A】或【方案 B】；
--   3. 两方案互斥，不要同时执行；
--   4. 执行前请自行备份数据库。
--
-- 作者：项目维护   日期：2026-10-22
-- =============================================================


-- =============================================================
-- 第一步：诊断（只读，可安全执行）
-- =============================================================

-- 1.1 哪些非卖家账号名下还有商品
SELECT
    u.user_id,
    u.username,
    u.role,
    COUNT(g.goods_id) AS goods_count
FROM `user` u
JOIN goods g ON g.user_id = u.user_id
WHERE u.role NOT IN ('seller', 'admin')
GROUP BY u.user_id, u.username, u.role
ORDER BY goods_count DESC;

-- 1.2 这些账号是否已经产生交易（有订单则禁止直接转移商品归属）
SELECT
    u.user_id,
    u.username,
    u.role,
    COUNT(DISTINCT o.order_id) AS order_count
FROM `user` u
LEFT JOIN orders o ON o.seller_id = u.user_id
WHERE u.role NOT IN ('seller', 'admin')
  AND EXISTS (SELECT 1 FROM goods g WHERE g.user_id = u.user_id)
GROUP BY u.user_id, u.username, u.role;

-- 1.3 这些账号是否有已通过的卖家申请（有则只差角色字段没同步）
SELECT
    a.application_id,
    a.user_id,
    u.username,
    u.role,
    a.status
FROM seller_application a
JOIN `user` u ON a.user_id = u.user_id
WHERE a.status = 1
  AND u.role = 'student';


-- =============================================================
-- 方案 A（推荐）：已有商品 / 已有订单的账号直接升级为卖家
--
-- 适用：
--   - 该账号确实长期在用，且名下商品已有订单；
--   - 转移商品归属会破坏 orders.seller_id 与 goods.user_id 的一致性。
--
-- 说明：
--   - 只升级「名下已有商品」的学生，不影响其他学生账号；
--   - 升级后该账号无需重新登录，Layout 会通过 /auth/me 同步角色；
--   - 建议同时补一条已通过的卖家申请记录，保证申请列表口径一致。
-- =============================================================

-- 执行前确认影响范围
SELECT u.user_id, u.username, COUNT(g.goods_id) AS goods_count
FROM `user` u
JOIN goods g ON g.user_id = u.user_id
WHERE u.role = 'student'
GROUP BY u.user_id, u.username;

START TRANSACTION;

-- A.1 先补「已通过」的卖家申请记录（必须在改角色之前执行，
--     否则条件 role='seller' 会误伤其他历史卖家账号）
INSERT INTO seller_application
    (user_id, reason, status, reviewer_id, review_time, create_time)
SELECT
    u.user_id,
    '历史数据修正：该账号名下已有商品，统一补录卖家身份',
    1,
    (SELECT user_id FROM `user` WHERE role = 'admin' ORDER BY user_id LIMIT 1),
    NOW(),
    NOW()
FROM `user` u
WHERE u.role = 'student'
  AND EXISTS (SELECT 1 FROM goods g WHERE g.user_id = u.user_id)
  AND NOT EXISTS (
      SELECT 1 FROM seller_application a
      WHERE a.user_id = u.user_id AND a.status = 1
  );

-- A.2 再升级角色
UPDATE `user` u
SET u.role = 'seller'
WHERE u.role = 'student'
  AND EXISTS (SELECT 1 FROM goods g WHERE g.user_id = u.user_id);

-- 确认无误后提交；如结果不符预期，执行 ROLLBACK;
COMMIT;


-- =============================================================
-- 方案 B：把商品归属转移给指定卖家（仅在无订单时可用）
--
-- 适用：
--   - 该学生账号只是测试账号，名下商品没有产生任何订单；
--   - 希望保持「学生不能卖东西」的演示语义。
--
-- 用法：
--   把下面的 '王五' 改成你的目标卖家 username，再整段执行。
--   若 1.2 诊断出的 order_count > 0，请改用方案 A。
-- =============================================================

-- B.0 设置目标卖家（必须是 seller）
-- SET @target_seller_id = (
--     SELECT user_id FROM `user` WHERE username = '王五' AND role = 'seller' LIMIT 1
-- );
-- SELECT @target_seller_id;

-- B.1 确认要转移的商品均无订单
-- SELECT g.goods_id, g.goods_name
-- FROM goods g
-- LEFT JOIN orders o ON o.goods_id = g.goods_id
-- WHERE g.user_id = (SELECT user_id FROM `user` WHERE username = '张三' LIMIT 1)
--   AND o.order_id IS NULL;

-- B.2 执行转移（确认 B.1 无订单后再执行）
-- START TRANSACTION;
-- UPDATE goods g
-- JOIN `user` u ON g.user_id = u.user_id
-- SET g.user_id = @target_seller_id
-- WHERE u.username = '张三'
--   AND u.role = 'student';
-- COMMIT;

-- 注意：若商品被 favorite 表引用，收藏关系不受影响（收藏针对 goods_id，
-- 与发布者无关）；但商品详情页的卖家信息会变成目标卖家，请确认业务可接受。


-- =============================================================
-- 第二步：验证（只读）
-- =============================================================

-- 2.1 不应再有非卖家账号持有商品
SELECT u.user_id, u.username, u.role, COUNT(g.goods_id) AS goods_count
FROM `user` u
JOIN goods g ON g.user_id = u.user_id
WHERE u.role NOT IN ('seller', 'admin')
GROUP BY u.user_id, u.username, u.role;

-- 2.2 商品的发布者与订单卖家是否一致（应返回 0 行）
SELECT o.order_id, o.seller_id, g.goods_id, g.user_id
FROM orders o
JOIN goods g ON o.goods_id = g.goods_id
WHERE o.seller_id <> g.user_id;


-- =============================================================
-- 说明
-- =============================================================
-- 1. 本脚本不会自动删除任何商品、订单或用户；
-- 2. 方案 A 只改 role 并补申请记录，是最小改动、可回滚；
-- 3. 若执行后发现异常，可在事务内 ROLLBACK，或按备份恢复；
-- 4. 执行完成后，学生账号再访问 /goods/publish 会看到
--    「成为卖家后即可发布商品」的引导面板，卖家账号正常进入向导。
