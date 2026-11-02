# 演示数据使用指南（DEMO_DATA_GUIDE）

配套脚本（按执行顺序）：

| 顺序 | 文件 | 作用 |
|---|---|---|
| 1 | `sql/00-create-demo-database.sql` | 在独立演示实例创建空库 `campus_trade`（utf8mb4 / utf8mb4_unicode_ci） |
| 2 | `sql/01-create-demo-schema.sql` | 在该库创建 21 张表的完整结构（schema.sql + v1.2 变更合并版，无 DROP） |
| 3 | `sql/demo-data.sql` | 写入 6 账号 / 24 商品 / 11 订单等演示数据 |

适用版本：campus-trade `v1.1`（Spring Boot 2.6.13 / MySQL 8.0.22 / utf8mb4）
本轮性质：**只生成脚本与文档，不执行 SQL、不创建数据库、不写入账号**；文件随仓库提交，实际执行时机由部署者决定。

---

## 1. 演示数据设计目标

| 目标 | 说明 |
|---|---|
| 真实业务链路 | 不是随机插表：24 件商品按分类分布，11 笔订单覆盖四种状态，评价只挂在已完成订单上，商品状态与订单状态严格配对 |
| 三条完整体验线 | 买家（下单→聊天→确认收货→评价）、卖家（发布→交付→工作台→报价）、管理员（审核申请→处理申诉→后台数据）都能点到真实数据 |
| 与代码规则一致 | 所有枚举值、唯一约束、计数冗余（`favorite_count`/`response_count`/`like_count`/`comment_count`）都与 Service 层逻辑一致 |
| 与真实数据隔离 | 只写入独立演示库；账号、手机号、邮箱全部虚构；不复制任何真实用户资料 |
| 可识别、可回滚 | 商品描述末尾统一带 `【DEMO-V1】`；账号统一为 `demo_*`；脚本可重复执行时安全中止 |
| 图片可迁移 | 24 张商品图全部引用本机已存在的 12 张种子图，不使用 IDE 外链 |

---

## 2. 六个账号的角色与使用方式

| 用户名 | 角色 | 定位 | 能体验的功能 | 是否公开 |
|---|---|---|---|---|
| `demo_buyer` | student | 买家视角 | 浏览/搜索、商品详情、收藏、下单、与卖家私聊、确认收货、评价、发帖、求购、报价、申请成为卖家 | 公开 |
| `demo_seller` | seller | 主力卖家 | 发布/编辑/上下架商品、上传图片、卖家订单处理（确认交付）、卖家工作台、对求购报价 | 公开 |
| `demo_admin` | admin | 管理员 | 管理后台用户/商品/订单、卖家申请审核、申诉处理、操作记录、运营数据中心 | 公开 |
| `demo_seller2` | seller | 第二卖家（剧情） | 提供第二卖家商品、第二组订单与评价来源 | 不公开 |
| `demo_student` | student | 剧情学生 | 社区互动、求购、订单与评价多样性、待审核卖家申请 | 不公开 |
| `demo_buyer2` | student | 剧情买家 | 给第二卖家贡献已完成订单与评价 | 不公开 |

角色事实：

- `demo_admin` 只能从「管理员」入口登录；学生/卖家入口会拒绝 `admin` 角色（`AuthService` 校验）。
- `demo_seller` / `demo_seller2` 已具备 `seller` 角色，因此可以发布商品。
- `demo_buyer` / `demo_student` 当前是 `student`，各自留有一条**待审核**卖家申请，管理员审核后角色升级为 `seller`，这正是后台红点与审核流程的演示素材。

---

## 3. 公开账号与密码

| 身份 | 用户名 | 密码 | 登录入口 |
|---|---|---|---|
| 买家（学生） | `demo_buyer` | `123456` | 学生入口 |
| 卖家 | `demo_seller` | `123456` | 卖家入口 |
| 管理员 | `demo_admin` | `123456` | 管理员入口 |

说明：

- 密码以 BCrypt（强度 10）哈希写入 `user.password`，**不是明文**；哈希值与项目 `BCryptPasswordEncoder` 校验逻辑一致。
- 6 个哈希均为本轮新生成（各自独立随机盐），**未复制**项目中任何既有账号的密码哈希。
- 已用项目实际依赖的 `spring-security-crypto 5.6.8` 实测：`matches("123456", hash) = true`（6 个哈希逐一验证）。
- 公开账号建议在登录页与 README 标注「公共演示账号，数据可能被其他访客修改，请勿改密码」。
- 脚本不输出、也不包含数据库账号口令或任何真实用户密码哈希。

---

## 4. 数据规模统计（目标 vs 脚本实际）

| 数据类型 | 目标 | 脚本实际 | 说明 |
|---|---:|---:|---|
| 演示账号 | 6 | 6 | 3 公开 + 3 剧情 |
| 商品 | 24 | 24 | 15 在售 + 9 已下单锁定 |
| 订单 | 11 | 11 | 5 已完成 / 2 待买家确认 / 2 待交易 / 2 已取消 |
| 评价 | 5 | 5 | 仅 O1~O5，`order_id` 唯一 |
| 收藏 | 15 | 15 | 12 条 demo_buyer + 3 条剧情账号 |
| 求购 | 6 | 6 | 5 进行中 + 1 已关闭（已选定报价） |
| 求购报价 | 5 | 5 | 3 待选 / 1 已选定 / 1 未入选 |
| 社区帖子 | 9 | 9 | 覆盖 4 个主题标签 |
| 社区评论 | 18 | 18 | 分散到 9 条帖子 |
| 聊天记录 | 18 | 18 | 3 个会话；16 文本 + 1 图片 + 1 订单卡片；2 条未读 |
| 交易申诉 | 2 | 2 | 1 待处理 + 1 已受理 |
| 卖家申请 | 3 | 3 | 2 待审核 + 1 已通过 |
| 附加：商品图片 | — | 24 | 每件 1 张主图，引用 12 张种子图 |
| 附加：商品分类 | — | 10 | 与前端筛选项一致 |
| 附加：社区点赞 | — | 12 | 遵守 `(post_id, user_id)` 唯一 |
| 附加：管理员操作记录 | — | 4 | 审核 / 受理 / 巡检文案 |

---

## 5. 各表之间的关联关系

```text
category(10) ──< goods(24) ──< goods_image(24)
                   │
                   ├──< orders(11) ──< evaluation(5)
                   │        └──────< trade_appeal(2)      （弱关联，无外键）
                   ├──< favorite(15)
                   └──< community_post(9).goods_id（可空，SET NULL）

user(6)
 ├──< goods（发布者）
 ├──< orders（buyer_id / seller_id，seller_id 必须等于商品发布者）
 ├──< favorite
 ├──< wanted_post(6) ──< wanted_response(5)                （弱关联，无外键）
 ├──< community_post(9) ──< community_comment(18)
 │                      └─< community_like(12)
 ├──< chat_message(18)                                     （弱关联，无外键）
 ├──< seller_application(3) ← reviewer: demo_admin
 └──< admin_record(4)（admin_id = demo_admin）
```

关键一致性规则（脚本已按此生成）：

1. `orders.seller_id` = 对应 `goods.user_id`。
2. `order_status` ∈ {0,1,2} 的商品 `status = 0`；`order_status = 3` 的商品 `status = 1`（取消后回上架）。
3. `evaluation` 只能挂 `order_status = 2` 的订单，且 `order_id` 唯一。
4. `favorite` 中 `(user_id, goods_id)` 不重复，`goods.favorite_count` = 真实收藏行数。
5. `wanted_post.response_count` = 真实报价行数；选定报价后该求购 `status = 2`（代码会把其他报价置 2）。
6. `community_post.like_count / comment_count` = 真实行数。
7. `chat_message.session_key` = `min(from,to)-max(from,to)-goodsId`（无商品用 0）。
8. `trade_appeal`：同一订单同一用户最多一条待处理；`handle_by` 只在已处理申诉上有值。

> 与旧方案的一处修正：部署方案第 7.5 节写的「选定报价后求购仍保持进行中」与代码不符。`WantedService.acceptQuote` 第 261 行会把求购同步置为 `2 已关闭`，脚本按代码真实行为生成。

---

## 6. 图片文件与商品对应关系

访问路径统一为 `/api/files/goods/seed-*.jpg`（后端 `context-path=/api`，`WebConfig` 将 `/files/**` 映射到 `app.upload.dir`）。

磁盘实际情况（本轮只读核对）：

| 位置 | 文件 | 状态 |
|---|---|---|
| `D:\campus-trade-uploads\goods\` | `seed-accessories.jpg` 等 12 个带 `seed-` 前缀的文件 | ✅ 已存在，可直接访问 |
| `campus-trade\seed-images\web\` | `accessories.jpg` 等 12 个无前缀原件 | ⚠️ 未跟踪，仅作母版，**保持不提交** |

对应关系：

| 商品 | 图片文件 | 商品 | 图片文件 |
|---|---|---|---|
| G01 iPhone 12 | `seed-phone.jpg` | G13 四级词汇书 | `seed-textbook.jpg` |
| G02 高数教材 | `seed-textbook.jpg` | G14 收纳盒三件套 | `seed-storage.jpg` |
| G03 室内篮球 | `seed-basketball.jpg` | G15 瑜伽垫 | `seed-basketball.jpg` |
| G04 折叠台灯 | `seed-lamp.jpg` | G16 帆布鞋 | `seed-sneakers.jpg` |
| G05 英语真题 | `seed-notes.jpg` | G17 高数笔记 | `seed-notes.jpg` |
| G06 蓝牙耳机 | `seed-accessories.jpg` | G18 遮光床帘 | `seed-storage.jpg` |
| G07 K380 键盘 | `seed-keyboard.jpg` | G19 民谣吉他 | `seed-guitar.jpg` |
| G08 数据结构教材 | `seed-textbook.jpg` | G20 打印机 | `seed-calculator.jpg` |
| G09 跑鞋 42 码 | `seed-sneakers.jpg` | G21 移动电源 | `seed-phone.jpg` |
| G10 平板保护壳 | `seed-accessories.jpg` | G22 保温杯 | `seed-storage.jpg` |
| G11 电热水壶 | `seed-storage.jpg` | G23 专业课教材 | `seed-textbook.jpg` |
| G12 无线鼠标 | `seed-accessories.jpg` | G24 晴雨伞 | `seed-accessories.jpg` |

另有 3 处复用了种子图：社区帖子 P1/P4/P6 的配图、申诉 A1 的证据图（`seed-lamp.jpg`）、私聊会话 B 的图片消息，均使用同一批文件。

注意事项：

- 仅使用上述 12 个已存在文件名，未编造任何图片名。
- 不使用 `trae-api-cn.mchost.guru` 外链（前端 `image.js` 已把该域名视为无图）。
- 本轮未复制、移动、删除、重命名任何图片；部署时需另行把 12 张种子图放入容器/服务器的上传目录（P10 阶段，单独授权）。

---

## 7. 新建独立演示数据库的前置条件（方案 A）

方案 A = **独立 MySQL 实例**，不是同一个实例里的第二个库：

| 项 | 方案 A 约定 |
|---|---|
| 实例 | 演示环境使用独立的 MySQL 8.0 实例（Docker 容器 / 云数据库 / 另一台机器） |
| 库名 | 仍为 `campus_trade`，与代码默认库名一致，无需改配置 |
| 隔离 | 与本机开发实例 `localhost:3306` 物理隔离，数据互不可见 |
| 原开发库 | 保持原样，不迁移、不清空、不复用 |

前置条件：

1. 一个可用的 MySQL 8.0 实例，且与本机开发实例不是同一个。
2. 拥有建库、建表、写数据权限的账号（账号口令由部署者自行保管，不写入仓库）。
3. 已确认连接指向新实例：`SELECT @@hostname, @@port;` 与本机开发实例不同。
4. 本机已存在 12 张种子图（脚本只写引用，不写文件；图片缺失不影响数据正确性，页面会走占位图）。
5. 本轮只生成脚本，**未连接任何数据库、未创建库表、未写入数据**。

---

## 8. 数据库结构初始化顺序

演示库初始化只需要 3 个文件，顺序固定：

| 步骤 | 文件 | 作用 | 是否会改数据 |
|---|---|---|---|
| 1 | `sql/00-create-demo-database.sql` | 创建空库 `campus_trade`（utf8mb4 / utf8mb4_unicode_ci） | 只建库，不建表 |
| 2 | `sql/01-create-demo-schema.sql` | 创建 21 张表（完整字段、索引、唯一键、外键） | 只建结构，无业务数据 |
| 3 | `sql/demo-data.sql` | 写入 6 账号 / 24 商品 / 11 订单等演示数据 | 单事务写入，末尾 COMMIT |

执行命令（主机/端口/账号由部署者替换，**必须带 `-h`、`-P` 指向新实例**）：

```bash
mysql -h <新实例地址> -P <新实例端口> -u <账号> -p < sql/00-create-demo-database.sql
mysql -h <新实例地址> -P <新实例端口> -u <账号> -p campus_trade < sql/01-create-demo-schema.sql
mysql -h <新实例地址> -P <新实例端口> -u <账号> -p campus_trade < sql/demo-data.sql
```

### 8.1 `01-create-demo-schema.sql` 的来源与取舍

该文件是把原始结构脚本合并成一次的 `CREATE TABLE`，并刻意排除了不适合独立演示初始化的语句：

| 原始来源 | 合并内容 |
|---|---|
| `sql/schema.sql` | 10 张基础表（`admin_record`/`category`/`evaluation`/`favorite`/`goods`/`goods_image`/`message`/`orders`/`seller_application`/`user`） |
| `v1.2-user-avatar.sql` | `user.avatar` |
| `v1.2-order-cancel-appeal.sql` | `orders.cancel_reason/cancel_by/cancel_time`、`trade_appeal` 表 |
| `v1.2-appeal-evidences.sql` | `trade_appeal.evidences` |
| `v1.2-chat.sql` + `v1.2-chat-media.sql` | `chat_message`（含 `message_type`、`related_id`） |
| `v1.2-conversation.sql` | `conversation`、`conversation_message` |
| `v1.2-community.sql` | `community_post`、`community_comment`、`community_like` |
| `v1.2-wanted.sql` + `v1.2-wanted-quotes.sql` | `wanted_post`、`wanted_response`（含 `price`、`status`，已去掉旧唯一键） |
| 实体类 + Mapper（仓库无 DDL） | `kb_doc`、`kb_chunk`（按 `KbDoc`/`KbChunk` 与 Mapper 的列名还原） |

对原始 `schema.sql` 的处理：

- 排除 `CREATE DATABASE` / `USE campus_trade`（建库交给 00 脚本，01 脚本不切库）；
- 排除 `DROP TABLE IF EXISTS`（演示库为空，且任何情况下都不应出现破坏性语句）；
- 排除 mysqldump 的 `SET FOREIGN_KEY_CHECKS=0` 等会话级开关；
- 保留全部字段类型、默认值、索引、唯一键与外键约束。

原始 `schema.sql`、`data.sql`、全部 `v1.2-*.sql` **保持原样未改动**，仅作历史记录，不再用于演示库初始化。

`kb_doc` / `kb_chunk` 在仓库中没有对应 SQL 文件（历史库中直接建表），01 脚本按实体与 Mapper 还原列名；若后续拿到原始 DDL，应以原始 DDL 为准并同步该文件。

### 8.2 执行失败如何处理

| 现象 | 原因 | 处理 |
|---|---|---|
| `ERROR 1049 Unknown database 'campus_trade'` | 00 未执行或连接参数指向了别的实例 | 先执行 00，并确认 `-h/-P` |
| `ERROR 1050 Table 'xxx' already exists` | 该库非空（重复执行或连错库） | **先停止**，确认实例身份；确认是演示实例后 `DROP DATABASE campus_trade;` 从 00 重来 |
| `ERROR 1064 / 1054` | 脚本被编辑器改坏或执行了旧文件 | 用仓库原文件重跑 |
| `demo-data.sql` 报 `ERROR 1062 Duplicate entry 'demo_buyer'` | 账号已存在（重复执行） | 事务未提交、连接断开即整体回滚；确认后按重置流程重来 |
| 账号能登录但列表空 | 数据脚本未成功提交 | 重跑第 11 节验收 SQL 对比数量 |

原则：**失败就停，不要在原开发实例上“再来一次”**。

### 8.3 避免误操作原开发库

1. 三个脚本都**不含 `USE`**，目标库完全由命令行 `campus_trade` 参数或客户端选中决定。
2. 01 脚本首行有自检 `SELECT @@hostname, @@port, DATABASE();`，执行后第一时间核对。
3. 00 脚本使用 `CREATE DATABASE IF NOT EXISTS`，即使误连原实例也不会损坏数据（库已存在则不动），但**绝不代表可以继续执行 01**。
4. 执行 01 前确认目标库为空（`SHOW TABLES;` 返回 0 张表）。
5. 原开发实例的 `campus_trade` 严禁执行 01（表已存在，会直接报 1050）。

---

## 9. 演示数据脚本执行顺序

1. 准备独立 MySQL 实例，确认连接参数指向新实例。
2. 执行 `sql/00-create-demo-database.sql`，核对输出 `db_name=campus_trade`、`charset=utf8mb4`、`collation=utf8mb4_unicode_ci`。
3. 执行 `sql/01-create-demo-schema.sql`，核对输出 `table_count = 21`。
4. 执行 `sql/demo-data.sql`（单事务，末尾 `COMMIT`）。
5. 脚本内部顺序：分类 → 账号 → 商品 → 商品图 → 订单 → 评价 → 收藏 → 求购 → 报价 → 社区帖子 → 评论 → 点赞 → 私聊 → 申诉 → 卖家申请 → 管理员记录 → COMMIT → 只读验收。
6. 阅读脚本末尾「验收」输出，逐项对比 `actual` 与 `expected`；再执行第 11 节的补充 SQL。
7. 按第 12、13 节验证三种角色登录与业务流程。

---

## 10. 执行前检查事项

| # | 检查 | SQL / 操作 | 期望 |
|---|---|---|---|
| 1 | 库身份 | `SELECT DATABASE(), @@hostname, @@port;` | 演示库 / 演示实例 |
| 2 | 表数量 | `SHOW TABLES;` | 21 张 |
| 3 | 结构齐全 | `SHOW COLUMNS FROM chat_message LIKE 'message_type';`、`SHOW COLUMNS FROM orders LIKE 'cancel_reason';`、`SHOW COLUMNS FROM trade_appeal LIKE 'evidences';`、`SHOW COLUMNS FROM \`user\` LIKE 'avatar';` | 均有结果 |
| 4 | 未误连开发库 | `SELECT COUNT(*) FROM \`user\`;` | 演示库应为 0（或仅演示账号） |
| 5 | 账号未占用 | `SELECT username FROM \`user\` WHERE username LIKE 'demo%';` | 空 |
| 6 | 手机号未占用 | `SELECT phone FROM \`user\` WHERE phone LIKE '199000000%';` | 空 |
| 7 | 图片文件 | 查看 `D:\campus-trade-uploads\goods\seed-*.jpg` | 12 个文件 |
| 8 | 备份 | 若演示库已有内容，先 `mysqldump` 备份 | 有备份文件 |

脚本开头已内置第 1 项自检；若 `DATABASE()` 不是目标库，应立刻停止（`USE` 不存在会直接报错）。

---

## 11. 执行后验证 SQL

`demo-data.sql` 末尾已附两组只读验收查询。补充人工核对：

```sql
-- 1. 数据规模
SELECT status, COUNT(*) FROM goods WHERE description LIKE '%【DEMO-V1】%' GROUP BY status;  -- 1:15, 0:9
SELECT order_status, COUNT(*) FROM orders o JOIN goods g ON o.goods_id=g.goods_id
  WHERE g.description LIKE '%【DEMO-V1】%' GROUP BY order_status;                          -- 2:5,1:2,0:2,3:2
SELECT status, COUNT(*) FROM seller_application s JOIN user u ON s.user_id=u.user_id
  WHERE u.username LIKE 'demo%';                                                            -- 0:2,1:1
SELECT status, COUNT(*) FROM trade_appeal a JOIN user u ON a.user_id=u.user_id
  WHERE u.username LIKE 'demo%';                                                            -- 0:1,1:1
SELECT status, COUNT(*) FROM wanted_post w JOIN user u ON w.user_id=u.user_id
  WHERE u.username LIKE 'demo%';                                                            -- 1:5,2:1
SELECT status, COUNT(*) FROM wanted_response r JOIN user u ON r.user_id=u.user_id
  WHERE u.username LIKE 'demo%';                                                            -- 0:3,1:1,2:1

-- 2. 计数冗余一致性（应全部返回 0 行）
SELECT g.goods_id, g.favorite_count,
       (SELECT COUNT(*) FROM favorite f WHERE f.goods_id=g.goods_id) AS real_count
FROM goods g WHERE g.description LIKE '%【DEMO-V1】%' AND g.favorite_count <>
       (SELECT COUNT(*) FROM favorite f WHERE f.goods_id=g.goods_id);

-- 3. 无 NULL 状态、无外链图片
SELECT COUNT(*) FROM goods WHERE description LIKE '%【DEMO-V1】%' AND status IS NULL;  -- 0
SELECT COUNT(*) FROM goods_image gi JOIN goods g ON gi.goods_id=g.goods_id
  WHERE g.description LIKE '%【DEMO-V1】%' AND gi.image_url LIKE 'http%';               -- 0

-- 4. 未读红点素材
SELECT COUNT(*) FROM chat_message m JOIN user u ON m.to_user_id=u.user_id
  WHERE u.username='demo_buyer' AND m.is_read=0;                                        -- 2
```

---

## 12. 如何确认账号可以正常登录

1. 后端以演示库启动（数据源指向演示库）。
2. `POST /api/auth/student/login`，请求体 `{"username":"demo_buyer","password":"123456"}` → 返回 `token` 且 `role=student`。
3. `POST /api/auth/seller/login`，`demo_seller / 123456` → `role=seller`。
4. `POST /api/auth/admin/login`，`demo_admin / 123456` → `role=admin`。
5. 反向验证：用 `demo_admin` 走学生入口应返回 401（角色不匹配）；错误密码也应 401。
6. 前端 `/login` 三种身份分别登录，确认进入 `/home` 时左侧菜单与角色一致（管理员额外出现后台入口）。

---

## 13. 三类角色的完整体验流程

### 买家（demo_buyer）

1. `/goods` 搜索「键盘」，命中 G07；分类/价格筛选可用。
2. `/goods/:id` 打开 G12/G19，图片正常，浏览量 +1。
3. 收藏 G19 → `/me/collections` 可见。
4. 对 G12 点击购买 → 订单状态 0，商品变下架（G12 原在售）。
5. `/messages` 打开与 demo_seller 的会话：会话 A/G01 有订单卡片，会话 C 有 2 条未读红点。
6. `/orders` 找到 O6（待买家确认），点「确认收货」→ 状态 2。
7. 对 O6 评价 → 卖家 demo_seller 主页评分更新。
8. （可选）`/seller-apply` 用 demo_buyer 已有待审核申请，可看到「审核中」。

### 卖家（demo_seller）

1. `/seller` 商品管理：24 件中有 15 件属于 demo_seller，可编辑/上下架。
2. `/goods/publish` 走 4 步向导发布新商品（上传图片、定价、确认）。
3. `/orders` 卖家视图：O8 待交易 → 点「确认交付」变状态 1。
4. `/seller/dashboard` 经营工作台：ECharts 有真实成交/评价数据。
5. `/wanted` 对 W1 再次报价（同一条求购支持多条报价，可观察「待选」列表与选定流程）。
6. `/messages` 回复买家，验证会话与未读。

### 管理员（demo_admin）

1. `/admin`「卖家申请」Tab **有红点**：demo_buyer、demo_student 两条待审核。
2. 审核 demo_buyer → 通过后其角色变 seller，红点减一。
3. `/admin` 用户/商品/订单 Tab 可分页筛选；对演示商品做下架/删除会真实改变数据（需重置）。
4. 申诉处理：A1（待处理）可受理/驳回；A2 已是「已受理」，只能查看。
5. `/admin` 操作记录可见 4 条巡检/审核文案；新审核会追加真实记录。
6. `/admin/analytics` 运营数据中心：概览统计非空。

### 社区与求购

`/community` 9 条帖子（4 类主题），P1/P4/P6 有配图；`/wanted` 6 条需求，W6 已关闭且展示 1 条已选定 + 1 条未入选。

---

## 14. 备份、恢复与重置

### 备份（演示库）

```bash
mysqldump -u <账号> -p --single-transaction --routines --triggers <库名> > demo_backup_YYYYMMDD.sql
```

备份文件放在仓库之外（例如仓库根目录外的 `.local-backups/` 目录，已在 `.gitignore` 中排除），**不要提交 Git**。

### 恢复

```bash
mysql -u <账号> -p <库名> < demo_backup_YYYYMMDD.sql
```

### 重置（推荐用于公开演示，每天一次）

1. 删除演示库：`DROP DATABASE campus_trade;`（**只允许在独立演示实例执行；本机开发实例严禁**）。
2. 重建并重跑第 8 节结构脚本 + `demo-data.sql`。
3. 重跑第 11 节验收 SQL，确认计数回到设计值。

### 只清演示数据（可选，需单独授权，本轮不执行）

按子表→父表顺序删除 demo 账号及其数据（`goods_image` 无外键，需先按商品删除）：

```sql
-- 仅演示库
DELETE gi FROM goods_image gi
  JOIN goods g ON gi.goods_id = g.goods_id
  JOIN `user` u ON g.user_id = u.user_id
 WHERE u.username LIKE 'demo\_%';
DELETE e FROM evaluation e JOIN `user` u ON e.user_id = u.user_id WHERE u.username LIKE 'demo\_%';
DELETE a FROM trade_appeal a JOIN `user` u ON a.user_id = u.user_id WHERE u.username LIKE 'demo\_%';
-- 其余子表由删除用户 / 商品时的外键级联清理
DELETE FROM `user` WHERE username LIKE 'demo\_%';
```

---

## 15. 已知限制与未覆盖功能

| 项 | 现状 / 原因 |
|---|---|
| 多图商品 | 每件商品只有 1 张主图；12 张种子图数量有限，详情页多图画廊只演示 1 张 |
| 商品留言板 `message` | 未生成数据（不在本轮目标规模内），但功能可正常使用 |
| AI 会话 `conversation` / `conversation_message` | 未生成；登录用户历史会话为空，游客仍可用 SSE 问答（无 Key 时走关键词降级） |
| 知识库 `kb_doc` / `kb_chunk` | 未生成；需通过 `POST /api/admin/knowledge/rebuild` 重建（依赖 Embedding Key） |
| 图片文件落地 | 脚本只写数据库引用；部署时需把 12 张 `seed-*.jpg` 放入上传目录，否则页面走奶油色占位图 |
| 用户头像 | 6 个演示账号 `avatar` 为空，默认头像正常显示 |
| 支付 / 物流 / 原价 | 项目本身不支持，演示数据未虚构这些字段或业务 |
| 申诉「已驳回(2)」/ 卖家申请「已拒绝(2)」 | 目标数量为 2 条申诉、3 条申请，未覆盖「驳回/拒绝」终态；管理员在演示中驳回待处理记录即可产生，无需预置 |
| 服务端登出 | 项目无 `/auth/logout`，退出仅清本地 Token |
| 公开管理员风险 | 共享 `demo_admin` 可审核/下架/删除，数据会被真实修改；需每日重置 |
| 演示库命名 | 按方案 A 固定使用独立实例上的 `campus_trade`，不再需要改库名或生成 schema 副本 |
| 密码强度 | 演示口令固定 `123456`，仅用于虚构账号；不得用于任何真实环境 |
| `seed-images/` | 保持未跟踪，未提交 Git；脚本只引用上传目录中已存在的 `seed-*` 文件 |

---

## 附：本轮静态自检结论

| 检查项 | 结果 |
|---|---|
| 21 张表存在性 | `schema.sql` 10 张 + 8 个 `v1.2-*.sql` 脚本共 11 张新增 → 与现库 21 张一致 |
| 字段存在性 | 逐表对照 `information_schema.COLUMNS` 只读查询，脚本所用字段全部存在 |
| 非空 / 默认值 | 必填字段均显式赋值；`status`、`count` 等默认值字段也显式写入，避免歧义 |
| 唯一约束 | `username`、`phone`、`favorite(user,goods)`、`community_like(post,user)`、`evaluation.order_id` 均无冲突 |
| 外键 | 所有引用均指向脚本内已创建的行；`seller_id` = 商品发布者 |
| 插入顺序 | 分类→账号→商品→图片→订单→评价/收藏→求购/报价→社区→私聊→申诉→申请→记录 |
| 密码哈希 | `BCryptPasswordEncoder`（5.6.8）实测 `123456` 校验通过 |
| 图片文件名 | 12 个文件名与磁盘一致，无编造、无外链 |
| 主键策略 | 不使用固定主键；`LAST_INSERT_ID()` + 自然键解析关联 ID |
| 对原库影响 | 三个脚本均不含破坏性语句；01 为纯 `CREATE TABLE`、00 为 `CREATE DATABASE IF NOT EXISTS`；库名固定 `campus_trade`，隔离靠独立实例 + `-h/-P` 连接参数 |
| 重复执行 | 账号唯一键冲突会中止，未提交事务整体回滚 |
| 已核对但与旧方案有差异 | 选定报价会关闭求购（按代码实现）；聊天第二会话为 demo_buyer2 ↔ demo_seller2（与订单 O2 一致） |
