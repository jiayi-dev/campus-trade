# campus-trade 公开演示环境：数据库审计与完整体验数据方案

> 审计日期：2026-10-31
> 审计对象：后端 `campus-trade`（`v1.1`）、前端 `campus-trade-web`（`v1.1`）、本地 MySQL `campus_trade`、本地上传目录 `D:\campus-trade-uploads`
> 本轮性质：**只读审计 + 方案设计 + 文档编写**。未执行任何 SQL 写操作，未创建账号，未修改代码/配置/图片，未执行 Git 操作。

## 阅读约定

| 标记 | 含义 |
|---|---|
| ✅ 已证实 | 已由源码、SQL 脚本或数据库只读查询直接验证 |
| 💡 设计建议 | 基于现状提出，尚未落地 |
| ⏳ 待验证 | 需要后续执行或部署后才能确认 |

---

## 0. 结论速览

1. ✅ 项目实际已实现的功能远超两个 README 的旧描述：共 **20 个 Controller**、**21 张数据库表**、**27 个前端视图组件**，覆盖商品、订单、聊天、社区、求购、评价、申诉、卖家工作台、管理后台、运营数据中心与 AI 助手。
2. ✅ 两个仓库都在 `v1.1`，HEAD 与 `origin/v1.1` 一致，工作区干净：前端无改动；后端仅 `seed-images/` 未跟踪，符合要求。
3. ✅ 当前库有 27 个用户、62 件商品、22 个订单、14 条评价、28 条聊天消息，订单四种状态全覆盖，关联完整性总体良好（本次核查的孤立关联均为 0）。
4. ✅ 数据库存在 5 类需要在演示前处理的数据瑕疵：1 条 `goods.status = NULL`、10 条 `favorite_count` 与真实收藏数不一致、`seller_application` 没有待审核记录（后台红点永远不出现）、12 条商品图仍是 IDE 外链、21 件在售商品无图。
5. ✅ Docker 部署有 3 个硬阻塞：`pom.xml` 仍 `<skip>true</skip>`（打不出可执行 fat jar）、`application.yml` 写死 `active: local` 而后端无 `application-prod.yml`、本机 `mvn` 不在 PATH 且两个仓库都无 Maven Wrapper。
6. ⏳ 演示账号、演示数据、登录页演示入口、生产 profile、Dockerfile/Compose/Nginx 配置均**尚未创建**，本报告给出可执行设计，等待授权后落地。

---

## 1. 当前项目实际功能清单（源码核对）

### 1.1 总体规模（✅ 已证实）

| 项目 | 实测结果 |
|---|---|
| 后端 Controller | 20 个 |
| 后端 Service | 20 个（另有 `ai/*` 工具类、`LlmClient`、`EmbeddingClient`） |
| MyBatis Mapper XML | 与 mapper 接口配套，位于 `src/main/resources/mapper/` |
| 数据库表 | 21 张（详见第 2 节） |
| 前端视图组件 | 27 个（`src/views/*.vue`） |
| 前端路由 | `createWebHistory`，含公开页与登录页共 27 条路由记录 |
| 知识库文档 | 6 个 Markdown（`src/main/resources/knowledge/01~06`） |
| 鉴权方式 | 自实现 Token（UUID + 内存 Map，12 小时），**非 JWT** |
| 密码加密 | `BCryptPasswordEncoder`（默认强度 10） |

### 1.2 功能模块逐项核对

> "缺口"一列只写真实存在的限制，不虚构功能。

| # | 模块 | 状态 | 前端页面 / 路由 | 主要后端接口（context-path `/api`） | 依赖表 | 可演示性 / 缺口 |
|---|---|---|---|---|---|---|
| 1 | 注册 | ✅ | `Register.vue` `/register` | `POST /user/register` | `user` | 可演示。固定写入 `role=student`、`status=1`，手机号查重 |
| 2 | 学生/卖家登录 | ✅ | `Login.vue` `/login` | `POST /auth/student/login`、`POST /auth/seller/login` | `user` | 可演示。两入口共用逻辑，拒绝 `admin` 角色 |
| 3 | 管理员登录 | ✅ | `Login.vue`（身份切换） | `POST /auth/admin/login` | `user` | 可演示。严格校验 `role=admin`，失败统一 401 |
| 4 | 退出登录 | ⚠️ 前端实现 | Layout 退出入口 | **无服务端登出接口** | — | 仅清除本地 `token/role`；Token 在服务端仍有效至 12 小时过期 |
| 5 | 角色与权限 | ✅ | 路由 `meta.roles` + 守卫 | `AuthInterceptor`（RBAC） | `user` | 可演示。越权返回 403；`GET /orders` 仅管理员 |
| 6 | 商品分类 | ✅ | 商品列表/发布向导 | `GET /categories` | `category` | 可演示，10 个分类 |
| 7 | 商品列表/搜索/筛选/分页 | ✅ | `GoodsList.vue` `/goods` | `GET /goods/page` | `goods`、`goods_image`、`category` | 可演示，游客可访问 |
| 8 | 商品详情 / 浏览量 | ✅ | `GoodsDetail.vue` `/goods/:id` | `GET /goods/{id}`、`POST /goods/{id}/view` | `goods`、`goods_image` | 可演示，游客可看；本人浏览不计数 |
| 9 | 商品发布 | ✅ | `GoodsPublishWizard.vue` `/goods/publish` | `POST /goods`、`POST /goods/{id}/images` | `goods`、`goods_image` | 可演示，仅 `seller/admin` 可发布；向导含 4 步与实时预览 |
| 10 | 商品编辑 / 下架 / 删除 / 批量 | ✅ | `Publish.vue` `/publish?id=`、`SellerDashboard.vue` | `PUT /goods`、`PUT /goods/{id}/status`、`DELETE /goods/{id}`、`POST /goods/batch` | `goods` | 可演示，含归属校验 |
| 11 | 商品统计接口 | ✅ | 卖家工作台 / 运营数据中心 | `GET /goods/statistics`、`/categoryAvg`、`/priceLevelCount`、`/my` | `goods` | 可演示 |
| 12 | 收藏 | ✅ | `MeCollections.vue` `/me/collections`、详情页 | `POST/DELETE /favorite/{goodsId}`、`GET /favorite/my`、批量查询 | `favorite`、`goods` | 可演示，`(user_id, goods_id)` 唯一 |
| 13 | 商品留言板 | ✅ | 商品详情页 | `GET /messages/goods/{goodsId}`、`POST /messages` | `message` | 可演示（与私聊是两张表，语义不同） |
| 14 | 求购 + 多条报价 | ✅ | `Wanted.vue` `/wanted`、`/wanted/:wantedId` | `GET/POST/PUT /wanted`、`POST /wanted/{id}/responses`、`PUT .../accept` | `wanted_post`、`wanted_response` | 可演示，已支持报价金额与 0/1/2 报价状态 |
| 15 | 订单创建 / 取消 / 状态流转 | ✅ | `OrderList.vue` `/orders`、`OrderCenter.vue` `/orders/center/:orderId` | `POST /orders`、`PUT /orders/{id}/status`、`PUT /orders/{id}/cancel`、`GET /orders/buyer|seller` | `orders`、`goods` | 可演示，0→1 卖家交付、1→2 买家收货、仅 0/1 可取消且商品回上架 |
| 16 | 用户间私聊 / 消息中心 | ✅ | `MessageCenter.vue` `/messages` | `GET /chat/sessions`、`/history`、`POST /chat/send`、`GET /chat/unread`、`/contacts` | `chat_message` | 可演示，支持文本/图片/订单卡片、未读红点 |
| 17 | 社区动态 / 评论 / 点赞 | ✅ | `Community.vue` `/community`、`CommunityDetail.vue` `/community/:postId` | `GET /community/page`、`POST /community`、`POST /{id}/comments`、`POST /{id}/like` | `community_post`、`community_comment`、`community_like` | 可演示，点赞唯一键、计数按真实行数刷新 |
| 18 | 评价与卖家信誉 | ✅ | 订单中心、`SellerProfile.vue` `/seller/:id` | `POST /evaluations`、`GET /evaluations/order|seller|goods` | `evaluation`、`orders` | 可演示，仅买家对已完成订单评价，一单一评（`order_id` 唯一） |
| 19 | 交易申诉与处理 | ✅ | 订单中心、管理后台 | `POST /appeals`、`GET /appeals/my`、`GET /appeals`、`PUT /appeals/{id}/handle` | `trade_appeal` | 可演示，管理员处理为 1 已受理 / 2 已驳回；同一订单同一用户仅一条待处理 |
| 20 | 头像 / 图片上传 | ✅ | 个人中心、发布向导、聊天、申诉 | `POST /upload/image`、`GET/PUT /user/avatar` | `user`、`goods_image` | 可演示，UUID 重命名 + 扩展名白名单 + 文件头魔数校验，上限 5MB |
| 21 | 卖家申请 | ✅ | `SellerApply.vue` `/seller-apply` | `POST /seller/apply`、`GET /seller/application/my` | `seller_application` | 可演示，学生提交，管理员审核后 `role` 变 `seller` |
| 22 | 卖家工作台 | ✅ | `SellerDashboard.vue` `/seller`、`SellerWorkbench.vue` `/seller/dashboard` | `GET /seller/...`、`GET /goods/my`、订单/统计接口 | `goods`、`orders`、`evaluation` | 可演示，工作台含 ECharts 图表 |
| 23 | 管理后台 | ✅ | `AdminDashboard.vue` `/admin` | `GET/PUT /admin/users`、`/admin/goods`、`/admin/orders`、`/admin/seller-applications`、`/admin/records` | `user`、`goods`、`orders`、`seller_application`、`admin_record` | 可演示，仅「卖家申请」Tab 有红点（已按你的要求收敛） |
| 24 | 运营数据中心 | ✅ | `AdminAnalytics.vue` `/admin/analytics` | `GET /admin/analytics/overview` | 多表聚合 | 可演示，仅管理员 |
| 25 | AI 助手 | ✅ | `AiAssistant.vue` `/ai-assistant` | `POST /ai/chat/stream`（SSE）、`/ai/conversations` | `conversation`、`conversation_message`、`kb_doc`、`kb_chunk` | 可演示。**不是 Mock**：SSE 流式 + Tool Calling + RAG 知识库；`llm.api-key` 为空时降级为关键词问答 |
| 26 | 知识库管理 | ✅ | 管理后台入口（重建接口） | `POST /admin/knowledge/rebuild` | `kb_doc`、`kb_chunk` | 可演示，但需配置 Embedding Key 才能重建向量 |
| 27 | 个人中心 | ✅ | `Profile.vue` `/profile` | `GET/PUT /user/profile`、`PUT /user/password`、`GET /user/profile/overview` | `user`、`goods`、`favorite`、`orders` | 可演示，改密后使该用户全部 Token 失效 |

### 1.3 仍然缺失或受限的能力（不要写成已实现）

| 项 | 事实 | 演示影响 |
|---|---|---|
| 服务端登出 | 无 `/auth/logout`，Token 仅自然过期 | 退出后旧 Token 仍可用，演示无影响，需在文档说明 |
| 原价字段 | `goods` 表**没有** `original_price`，发布向导也没有 | 商品只能展示现价；演示数据不设计"原价"，不虚构 |
| 收货地址/物流 | 无地址表、无物流单号字段，交易为校内面交模式 | 订单流程按"卖家交付 → 买家确认"演示 |
| 支付 | 无支付表/接口 | 订单不涉及支付，避免在文案中暗示在线支付 |
| 管理员二次确认/防误操作 | 后台下架/删除/禁用直接执行 | 公开演示需依赖数据重置脚本兜底 |
| 演示账号入口 | `Login.vue` 目前只有身份切换，无演示账号卡片 | 需新增前端组件（本轮未改代码） |

---

## 2. 数据库结构与数据统计（✅ 全部来自只读查询）

### 2.1 基本信息

| 项 | 值 |
|---|---|
| MySQL 版本 | 8.0.22 |
| 库名 | `campus_trade` |
| 表数量 | **21 张**（不是 README 旧写的 10 张） |
| 库体积 | 约 1.28 MB |
| 字符集 | `utf8`（服务端默认），业务表多为 `utf8mb4` |
| 认证插件 | `caching_sha2_password` |

### 2.2 表清单与精确行数

| # | 表 | 行数 | 主键最大值 | 说明 |
|---|---|---:|---:|---|
| 1 | `user` | 27 | 30 | 账号与角色 |
| 2 | `category` | 10 | 10 | 商品分类 |
| 3 | `goods` | 62 | 70 | 商品 |
| 4 | `goods_image` | 53 | 93 | 商品图片（48 个不同 URL） |
| 5 | `orders` | 22 | 22 | 订单 |
| 6 | `favorite` | 32 | 42 | 收藏 |
| 7 | `evaluation` | 14 | 14 | 订单评价 |
| 8 | `message` | 15 | 17 | 商品留言板 |
| 9 | `seller_application` | 8 | 8 | 卖家申请 |
| 10 | `admin_record` | 19 | 19 | 管理员操作记录 |
| 11 | `chat_message` | 28 | 28 | 用户私聊 |
| 12 | `conversation` | 2 | 6 | AI 会话 |
| 13 | `conversation_message` | 14 | 46 | AI 会话消息 |
| 14 | `community_post` | 11 | 11 | 社区动态 |
| 15 | `community_comment` | 4 | 4 | 社区评论 |
| 16 | `community_like` | 2 | 2 | 社区点赞 |
| 17 | `wanted_post` | 8 | 8 | 求购需求 |
| 18 | `wanted_response` | 5 | 8 | 求购报价 |
| 19 | `trade_appeal` | 6 | 6 | 交易申诉 |
| 20 | `kb_doc` | 6 | 6 | 知识库文档 |
| 21 | `kb_chunk` | 7 | 7 | 知识库分块 |

> 注：`user` 行数 27 但主键最大值 30、`goods` 62/70、`goods_image` 53/93、`favorite` 32/42 均存在自增空洞，说明历史上发生过删除。新增演示数据时**不要手写主键**，交给 `AUTO_INCREMENT`。

### 2.3 字段、必填与唯一约束（关键表）

| 表 | 必填（NOT NULL） | 唯一约束 | 外键 | 备注 |
|---|---|---|---|---|
| `user` | `username`、`password`、`role`、`create_time` | `username`、`phone` | — | `phone` 可空但注册必填；`avatar` 可空 |
| `category` | `category_name`、`create_time` | `category_name` | — | — |
| `goods` | `user_id`、`goods_name`、`price`、`create_time` | 无 | `user_id→user`(CASCADE)、`category_id→category`(SET NULL) | `status` 可空、默认 1；`condition_level` 可空 |
| `goods_image` | `goods_id`、`image_url` | 无 | **无外键** | 删除商品不会级联删图，需业务层清理 |
| `orders` | `goods_id`、`buyer_id`、`seller_id`、`create_time` | 无 | 三个外键均 CASCADE | `order_status` 可空、默认 0；取消三字段可空 |
| `favorite` | `user_id`、`goods_id`、`create_time` | `uk_user_goods(user_id, goods_id)` | 两个 CASCADE | — |
| `evaluation` | `order_id`、`create_time` | `order_id` | `order_id→orders`、`user_id→user` | `score`/`content` 数据库可空，业务层强制 1~5 分、≥5 字 |
| `message` | `goods_id`、`user_id`、`content`、`create_time` | 无 | 两个 CASCADE | 商品留言板 |
| `seller_application` | `user_id`、`reason`、`status`、`create_time` | 无 | `user_id`、`reviewer_id` | `status` 默认 0 |
| `admin_record` | `admin_id`、`operation`、`create_time` | 无 | `admin_id→user` CASCADE | — |
| `chat_message` | `session_key`、`from_user_id`、`to_user_id`、`content`、`message_type`、`is_read`、`create_time` | 无 | **无外键** | 索引：`(session_key, chat_id)`、`(to_user_id, is_read)` |
| `community_post` | `user_id`、`title`、`topic`、`content`、三个计数、`create_time`、`update_time` | 无 | `user_id` CASCADE、`goods_id` SET NULL | 计数为冗余字段，需与真实行数一致 |
| `community_comment` | `post_id`、`user_id`、`content`、`create_time` | 无 | 两个 CASCADE | — |
| `community_like` | `post_id`、`user_id`、`create_time` | `uk_post_user(post_id, user_id)` | 两个 CASCADE | 一人一赞 |
| `wanted_post` | `user_id`、`title`、`status`、`response_count` | 无 | 无 | `status` 默认 1 |
| `wanted_response` | `wanted_id`、`user_id`、`message`、`status`、`create_time` | 无（`v1.2-wanted-quotes.sql` 已删除原本的唯一键） | 无 | `price` 可空=面议 |
| `trade_appeal` | `order_id`、`user_id`、`reason`、`status`、`create_time` | 无 | 无 | `evidences` 为 JSON 数组文本 |
| `conversation` | `user_id`、`title`、`create_time`、`update_time` | 无 | 无 | — |
| `conversation_message` | `conversation_id`、`role`、`content`、`create_time` | 无 | 无 | `extra_json` 可空 |
| `kb_doc` | `title`、`version`、`enabled`、`create_time` | `title` | 无 | — |
| `kb_chunk` | `doc_id`、`seq`、`content`、`content_hash`、`create_time` | 无 | 无 | `embedding` 为 LONGTEXT |

### 2.4 状态枚举（✅ 以代码/SQL 为准）

| 字段 | 取值 | 含义 | 来源 |
|---|---|---|---|
| `user.role` | `student` / `seller` / `admin` | 学生 / 卖家 / 管理员 | `UserService.register`、`AuthService`、`AuthInterceptor` |
| `user.status` | `1` / `0` | 正常 / 禁用（禁用后登录被拒） | `UserService.updateStatus`、`AuthService` |
| `goods.status` | `1` / `0` | 在售 / 已下架（含下单锁定、售出） | `GoodsService`、`OrderService` |
| `goods.condition_level` | `1~5` | 全新 / 99新 / 95新 / 9成新 / 8成新及以下 | `GoodsPublishWizard.vue` |
| `orders.order_status` | `0` / `1` / `2` / `3` | 待交易 / 待买家确认 / 已完成 / 已取消 | `OrderService` |
| `seller_application.status` | `0` / `1` / `2` | 待审核 / 通过 / 拒绝 | `SellerApplicationService`、`AdminService` |
| `trade_appeal.status` | `0` / `1` / `2` | 待处理 / 已受理 / 已驳回 | `TradeAppealService.handle` |
| `wanted_post.status` | `1` / `2` | 进行中 / 已关闭 | `WantedService.updateStatus` |
| `wanted_response.status` | `0` / `1` / `2` | 待选 / 已选定 / 未入选 | `v1.2-wanted-quotes.sql`、`WantedService.acceptQuote` |
| `chat_message.message_type` | `text` / `image` / `order` | 文本 / 图片 / 订单卡片 | `ChatService`、`v1.2-chat-media.sql` |
| `chat_message.is_read` | `0` / `1` | 未读 / 已读 | `ChatService` |

### 2.5 关系结构（文字 ER）

```text
user 1─n goods n─1 category
goods 1─n goods_image
goods 1─n orders（goods_id）
user 1─n orders（buyer_id / seller_id）
orders 1─1 evaluation（order_id 唯一）
user 1─n favorite n─1 goods
goods 1─n message（留言板）
user 1─n seller_application；user(admin) 1─n seller_application(reviewer_id)
user 1─n admin_record
user 1─n chat_message（from_user_id / to_user_id，弱关联无外键）
user 1─n community_post 1─n community_comment / community_like
user 1─n wanted_post 1─n wanted_response（弱关联无外键）
orders 1─n trade_appeal（弱关联无外键）
user 1─n conversation 1─n conversation_message
kb_doc 1─n kb_chunk
```

### 2.6 当前数据分布（✅ 只读统计）

| 维度 | 分布 |
|---|---|
| 用户角色 | `admin` 1 / `seller` 9 / `student` 17 |
| 用户状态 | `status=1` 共 27（无禁用账号） |
| 商品状态 | `1` 在售 48 / `0` 下架 13 / **`NULL` 1（异常）** |
| 商品分类分布 | 数码 14、生活 11、图书 6、学习 4、宿舍 4、运动 3、服装 3、乐器 1、办公 1、其他 1 |
| 订单状态 | `0` 待交易 5 / `1` 待确认 4 / `2` 已完成 11 / `3` 已取消 2 |
| 评价分数 | 5 分 8 / 4 分 5 / 1 分 1 |
| 私聊消息类型 | `text` 19 / `order` 6 / `image` 3 |
| 卖家申请 | `1` 通过 6 / `2` 拒绝 2 / **`0` 待审核 0** |
| 求购 | 进行中 7 / 已关闭 1 |
| 报价 | 待选 5（无已选定/未入选） |
| 申诉 | **6 条全部 `status=0` 待处理** |
| 管理员操作记录 | 19 条 |

### 2.7 关联完整性与数据质量

| 检查项 | 结果 | 结论 |
|---|---|---|
| `goods` 指向不存在的用户 | 0 | ✅ 无孤立 |
| `orders.seller_id` 与 `goods.user_id` 不一致 | 0 | ✅ 一致 |
| `evaluation` 指向不存在的订单 | 0 | ✅ 无孤立 |
| `favorite` 指向不存在的商品 | 0 | ✅ 无孤立 |
| 学生账号名下持有商品 | 0 | ✅ `v1.2-fix-role-data.sql` 的历史问题当前不存在 |
| 重复 `username` / `phone` | 0 / 0 | ✅ 唯一约束正常 |
| 同一订单重复评价 | 0 | ✅ 唯一约束正常 |
| 在售商品被重复下单 | 0 | ✅ |
| `goods.status IS NULL` | **1**（`goods_id=13`，测试默认状态商品） | ⚠️ 异常：列表按 `status=1` 过滤不可见，后台/编辑页可能出现状态异常 |
| `goods.favorite_count` 与 `favorite` 真实数不一致 | **10 条** | ⚠️ 数据瑕疵，影响列表"收藏数"展示 |
| 12 条商品图为 IDE 外链 | **12 条** | ⚠️ 公开部署可能无法访问 |
| 在售商品无图 | **21 件** | ⚠️ 展示占位图，影响观感 |
| `seller_application` 待审核 | **0** | ⚠️ 管理后台「卖家申请」红点不会出现 |
| `trade_appeal` 已处理示例 | **0**（全待处理） | ⚠️ 后台只能看到"待处理"一种状态 |

---

## 3. 现有 SQL 文件审查（逐个说明）

> 判断"是否执行过"仅依据可观察证据：表/列是否存在、行数与标记、`INSERT IGNORE`/`NOT EXISTS` 结果。无法证明的一律写"无法确认"。

| 文件 | 用途 | 已执行证据 | 可重复执行 | 重复风险 / 依赖 | 适合公开演示 |
|---|---|---|---|---|---|
| `schema.sql` | DROP 重建 10 张基础表并写入初始化结构 | ✅ 10 张表存在 | ❌ 会 DROP | 破坏全部数据；仅限全新空库 | ❌ 不可在现库执行 |
| `data.sql` | 5 个演示用户 + 10 分类 + 8 商品 + 3 图 + 3 收藏 + 3 订单 + 1 评价 + 2 留言 + 2 申请 + 2 记录，**显式主键** | ⚠️ 当前库**不含** `admin_demo/chen_sell/li_sell/wang_stu/zhao_stu` 任一账号，说明现库不是由该脚本初始化 | ❌ 主键冲突 | 只能在空库执行一次；依赖固定 ID | ❌ 与现库不匹配，不能补数据 |
| `dev-seed-goods-v1.3.sql` | 25 件测试商品，`【SEED-V131】` 标记 + `NOT EXISTS` | ✅ 库中恰有 **25** 条该标记商品 | ✅ 可重复 | 只插 `goods`、不插图片；依赖 `user_id=2/3` 与分类存在 | ⚠️ 可留作测试，不建议作为演示主数据（无图、无交易关系） |
| `v1.2-chat.sql` | 新建 `chat_message` | ✅ 表存在，28 行 | ✅ `IF NOT EXISTS` | 无 | ✅ |
| `v1.2-chat-media.sql` | `chat_message` 增加 `message_type/related_id` | ✅ 两列存在 | ❌ 第二次报 Duplicate column | 报错可忽略，不改数据 | ✅ 只需执行一次 |
| `v1.2-conversation.sql` | 新建 `conversation` / `conversation_message` | ✅ 两表存在 | ✅ `IF NOT EXISTS` | 无 | ✅ |
| `v1.2-community.sql` | 新建社区 3 表 | ✅ 3 表存在 | ✅ `IF NOT EXISTS` | 无 | ✅ |
| `v1.2-wanted.sql` | 新建求购 2 表 | ✅ 2 表存在 | ✅ `IF NOT EXISTS` | 无 | ✅ |
| `v1.2-wanted-quotes.sql` | 删除报价唯一键，增加 `price/status` | ✅ 两列存在，5 条报价 `status=0` | ❌ 第二次 `DROP INDEX` 失败 | 仅改结构 | ✅ 只需一次 |
| `v1.2-order-cancel-appeal.sql` | `orders` 增加取消三列 + 新建 `trade_appeal` | ✅ 列与表存在 | ⚠️ ALTER 不幂等；CREATE 幂等 | 第二次报 Duplicate column | ✅ 只需一次 |
| `v1.2-appeal-evidences.sql` | `trade_appeal` 增加 `evidences` | ✅ 列存在，3 条有值 | ❌ 不幂等 | 报错可忽略 | ✅ 只需一次 |
| `v1.2-user-avatar.sql` | `user` 增加 `avatar` | ✅ 列存在，1 个用户有头像 | ❌ 不幂等 | 报错可忽略 | ✅ 只需一次 |
| `v1.2-fix-goods-images.sql` | 把 trae/picsum 外链改成本地 `seed-*.jpg` | ❌ **未执行**：库中仍有 12 条外链，且 `seed-*` 引用为 0 | ✅ 可重复（只改无效 URL） | 依赖 `goods_id` 列表与本地 12 张种子图；会覆盖这些商品的图片 | ⚠️ 可修外链，但映射按旧 goods_id，需先核对；不建议直接跑 |
| `v1.2-goods-demo-images.sql` | 给无图商品插入 trae 外链主图 | ✅ 极可能就是当前 12 条外链的来源 | ✅ `NOT EXISTS` | **会继续产生不可移植外链**，不适合公开部署 | ❌ 禁止用于演示环境 |
| `v1.2-fix-role-data.sql` | 历史脏数据修正：学生名下商品 | ✅ 当前诊断结果为 0 条 | ❌ 方案 A/B 互斥，含 `UPDATE role` | 直接执行会改角色；必须人工逐段选择 | ❌ 本轮禁止执行；现库无此问题，无需执行 |

### 3.1 SQL 结论

- ✅ 结构与迁移脚本（`v1.2-*.sql` 的建表/加列部分）**都已在当前库生效**。
- ❌ `data.sql` 与当前库数据不匹配，**不能**用来补演示账号。
- ❌ `v1.2-goods-demo-images.sql` 会产生失效外链，**不要**在演示环境执行。
- ⚠️ `dev-seed-goods-v1.3.sql` 的 25 件商品可作为"已有测试商品"保留，但演示主数据应另行设计（有图、有交易链路）。
- 💡 公开演示环境建议：**新建独立演示库**，按"结构 → 基础分类 → 演示账号 → 演示数据"的顺序一次性构建，不复用现库。

---

## 4. 图片数据核查（✅ 只读）

### 4.1 数据库引用

| 来源 | 数量 | 说明 |
|---|---:|---|
| `goods_image` 总行 | 53 | 48 个不同 URL |
| 其中本地 `/api/files/` | 41 | 可随上传目录迁移 |
| 其中 `http(s)` 外链（trae-api-cn） | 12 | ⏳ 公开部署可访问性未验证，脚本注释称浏览器不可访问 |
| `user.avatar` 非空 | 1 | 本地相对 URL |
| `community_post.image_url` | 3 | — |
| `chat_message.message_type='image'` | 3 | 内容为 URL |
| `trade_appeal.evidences` 非空 | 3 | JSON 数组 |

### 4.2 本地磁盘

| 项 | 值 |
|---|---|
| 上传目录 | `D:\campus-trade-uploads` |
| 文件总数 | **86** |
| 总体积 | **16.45 MB** |
| 种子图 `seed-*.jpg` | **12** 张（accessories / basketball / calculator / camera / guitar / keyboard / lamp / notes / phone / sneakers / storage / textbook） |
| 缺失文件 | **0**（既有全量双向核对结论） |
| 无引用文件 | 约 33（86 − 已引用去重数），多为历史 UUID 文件与 12 张种子图（当前 `goods_image` 未引用 seed-*） |
| 在售无图商品 | **21** |

### 4.3 部署适配性结论

- ✅ 数据库只存相对 URL（`/api/files/goods/xxx.jpg`），不含机器绝对路径，**适合 Linux 容器**。
- ✅ 图片上传统一落到 `goods/` 子目录，静态映射 `file:{uploadDir}/`，容器只需挂载同一目录。
- ⚠️ 12 条外链不符合"自托管可迁移"原则；演示数据**不得使用**这些 URL。
- ⚠️ 21 件在售商品无图，建议演示前用现有本地图片补齐，或作为"待上传"演示场景保留少量。
- ✅ 本轮未移动、复制、重命名或删除任何图片。

---

## 5. 敏感信息风险检查

| 风险点 | 现状 | 级别 | 建议（本轮未改） |
|---|---|---|---|
| `application-local.yml` 含数据库口令与 LLM Key | ✅ 已被 `.gitignore` 第 35 行忽略；Git 工作区干净 | 中 | 保持忽略；Docker 构建上下文用 `.dockerignore` 排除 |
| 真实用户手机号/邮箱 | ✅ 库中 27 个账号存在真实风格资料 | 高 | 公开演示用**独立演示库**，不导出、不复制真实用户；报告只给聚合统计 |
| BCrypt 密码哈希 | ✅ 未在任何文档中输出 | 高 | 保持；演示账号只使用固定演示哈希 |
| `application.yml` 的 MyBatis `StdOutImpl` | ✅ 会把 SQL 与参数打到标准输出 | 中 | 生产 profile 关闭 SQL 日志，避免手机号进入容器日志 |
| CORS `allowedOriginPatterns("*")` + `allowCredentials(true)` | ✅ `WebConfig` 现状 | 中 | 生产 Nginx 同源部署，收紧允许来源 |
| 上传目录静态暴露 | ✅ 文件名 UUID 防枚举，但无鉴权 | 低 | 演示可接受；生产可加限速/防盗链 |
| 演示管理员密码公开 | 尚未创建 | 中 | 仅演示库；提供每日重置/一键恢复；限制后台高风险操作 |
| Git 提交敏感文件 | ✅ 两仓库工作区无 `.env`、无 dump、无配置副本 | — | 继续禁止提交 `application-local.yml`、`seed-images/`、备份 SQL |

> ✅ 本轮报告未输出任何真实密码、哈希、手机号、邮箱或服务器信息。

---

## 6. 公开演示账号设计

### 6.1 角色事实（✅）

- 登录按 `username` 查库，`BCryptPasswordEncoder.matches` 校验。
- 注册接口固定 `role=student`，因此**卖家/管理员演示账号必须通过 SQL 创建**（不能靠注册）。
- 管理员入口只接受 `role=admin`；学生/卖家入口拒绝 `admin`。
- 账号名唯一、手机号唯一；当前库**不存在** `demo_buyer / demo_seller / demo_admin / demo_student / demo_seller2` 任一账号（已查，均为空闲）。

### 6.2 演示账号表（💡 设计，待创建）

| 用户名 | 密码 | 角色 | 定位 | 可体验功能 | 是否公开 |
|---|---|---|---|---|---|
| `demo_buyer` | `123456` | student | 买家视角 | 浏览/搜索、详情、收藏、下单、聊天、确认收货、评价、发帖、求购、报价、申请卖家 | ✅ 公开（共享） |
| `demo_seller` | `123456` | seller | 卖家视角 | 发布/编辑/下架商品、上传图片、查看订单、确认交付、卖家工作台、报价、聊天 | ✅ 公开（共享） |
| `demo_admin` | `123456` | admin | 管理视角 | 后台用户/商品/订单、卖家申请审核、申诉处理、运营数据中心、知识库重建 | ✅ 公开（共享） |
| `demo_seller2` | `123456` | seller | 剧情卖家（第二卖家） | 提供多卖家商品与评价来源 | ❌ 不公开（用于数据自洽） |
| `demo_student` | `123456` | student | 剧情学生 | 社区互动、求购、形成订单/评价多样性 | ❌ 不公开 |
| `demo_buyer2` | `123456` | student | 剧情买家 | 给第二卖家贡献已完成订单/评价 | ❌ 不公开 |

- 密码 `123456` 仅用于**虚构演示账号**，与数据库口令、LLM Key 无关。
- 公开共享账号需在页面上明确标注"公共演示账号，请勿修改密码/删除数据"。
- 哈希复用仓库中已验证可用的 BCrypt 值（`data.sql` 同款，强度 10）：
  `$2a$10$z4BPekrJbXlby6jIcX28NOKPILTmy23EDxcLUOB0jyFhtBDF4YVRm`
  ⏳ 创建后必须实际登录一次验证，再写入 README。
- 手机号使用明显虚构号段，并在脚本中先查重（例如 `19900000001`~`19900000003`）；**不要使用真实手机号**。

### 6.3 创建方式（不绕过加密机制）

1. **前置检查**（只读）：
   ```sql
   SELECT username FROM `user` WHERE username IN
     ('demo_buyer','demo_seller','demo_admin','demo_seller2','demo_student','demo_buyer2');
   SELECT phone FROM `user` WHERE phone IN ('19900000001','19900000002','19900000003');
   ```
2. **插入账号**（示例，实际脚本待授权后生成）：
   ```sql
   INSERT INTO `user` (username, password, phone, email, role, status, create_time)
   SELECT 'demo_buyer', '$2a$10$z4BPekrJbXlby6jIcX28NOKPILTmy23EDxcLUOB0jyFhtBDF4YVRm',
          '19900000001', 'demo_buyer@campus-demo.local', 'student', 1, NOW()
   FROM DUAL
   WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE username='demo_buyer');
   ```
3. **验证**：用三个公开账号分别访问 `/login` 对应身份入口，确认返回 `token` 与真实 `role`。
4. **不写入**任何代码、配置文件或 Git 历史；演示口令只放在部署文档与登录页提示中。

### 6.4 登录页演示入口（💡 设计，需前端改动，本轮未实施）

在 `Login.vue` 身份切换区下方增加"演示账号"卡片：

| 身份 | 账号 | 密码 | 一键填入 |
|---|---|---|---|
| 买家（学生） | demo_buyer | 123456 | 按钮 |
| 卖家 | demo_seller | 123456 | 按钮 |
| 管理员 | demo_admin | 123456 | 按钮 |

要求：

- 点击"填入"只填充表单，不自动提交，避免多人同时登录造成会话混乱。
- 卡片显著标注"公共演示账号，数据可能被其他访客修改"。
- 管理员入口提示"仅用于演示审核与统计"。
- 部署后需在 README 的"公开演示"小节填写**真实可访问地址**；⏳ 地址未验证前留空，不得编造。

---

## 7. 演示数据设计（💡 设计，本轮不执行）

### 7.1 规模

| 类型 | 设计数量 | 说明 |
|---|---:|---|
| 用户 | 6 | 3 个公开 + 3 个剧情账号 |
| 商品 | 24 | 15 在售（含 2 件取消后回上架）+ 9 件已被订单锁定 |
| 求购 | 6 | 5 条进行中 + 1 条已关闭 |
| 报价 | 5 | 覆盖待选/已选定/未入选 |
| 订单 | 11 | 5 已完成 / 2 待确认 / 2 待交易 / 2 已取消 |
| 商品评价 | 5 | 仅对已完成订单，一单一评 |
| 收藏 | 15 | 12 条 demo_buyer + 3 条剧情用户 |
| 聊天消息 | 18 | 3 个会话，含 1 条订单卡片、1 条图片、2 条未读 |
| 社区帖子 | 9 | 覆盖 4 个主题标签 |
| 社区评论 | 18 | 分布到各帖子 |
| 社区点赞 | 12 | 满足 `(post_id,user_id)` 唯一 |
| 申诉 | 2 | 1 待处理 + 1 已受理 |
| 卖家申请 | 3 | 2 待审核（制造后台红点）+ 1 已通过 |
| 管理记录 | 4 | 审核/处理/下架等 |

### 7.2 商品清单（24 件，图片只用**已存在**的 12 张种子图）

> 图片路径统一 `/api/files/goods/seed-xxx.jpg`；分类 ID 与现库一致；成色 1~5；校区仅用 东区/西区/南区/北区。

| 编号 | 商品 | 分类 | 价格 | 成色 | 校区 | 卖家 | 状态 | 图片 |
|---|---|---|---:|---|---|---|---|---|
| G01 | iPhone 12 128G 95新 | 1 数码 | 2200 | 3 | 东区 | demo_seller | 0（订单 O1 已完成） | seed-phone |
| G02 | 高等数学教材全套 | 2 图书 | 45 | 4 | 北区 | demo_seller2 | 0（O2 已完成） | seed-textbook |
| G03 | 室内篮球 7 号 | 4 运动 | 60 | 3 | 西区 | demo_seller | 0（O3 已完成） | seed-basketball |
| G04 | 宿舍折叠台灯 | 3 生活 | 18 | 2 | 北区 | demo_seller | 0（O4 已完成） | seed-lamp |
| G05 | 考研英语真题近十年 | 6 学习 | 25 | 2 | 北区 | demo_seller2 | 0（O5 已完成） | seed-notes |
| G06 | 半入耳蓝牙耳机 | 1 数码 | 89 | 2 | 东区 | demo_seller | 0（O6 待买家确认） | seed-accessories |
| G07 | 罗技 K380 键盘 | 1 数码 | 55 | 3 | 东区 | demo_seller | 0（O7 待买家确认） | seed-keyboard |
| G08 | 数据结构教材 | 2 图书 | 32 | 3 | 北区 | demo_seller | 0（O8 待交易） | seed-textbook |
| G09 | 运动跑鞋 42 码 | 5 服装 | 70 | 3 | 西区 | demo_seller2 | 0（O9 待交易） | seed-sneakers |
| G10 | 平板保护壳 | 1 数码 | 20 | 2 | 东区 | demo_seller | 1（O10 已取消，回上架） | seed-accessories |
| G11 | 宿舍电热水壶 | 3 生活 | 35 | 3 | 北区 | demo_seller | 1（O11 已取消，回上架） | seed-storage |
| G12 | 罗技无线鼠标 | 1 数码 | 45 | 3 | 东区 | demo_seller | 1 在售 | seed-accessories |
| G13 | 英语四级词汇书 | 2 图书 | 15 | 4 | 北区 | demo_seller2 | 1 在售 | seed-textbook |
| G14 | 宿舍收纳盒三件套 | 7 宿舍 | 22 | 2 | 北区 | demo_seller | 1 在售 | seed-storage |
| G15 | 瑜伽垫加厚款 | 4 运动 | 30 | 3 | 西区 | demo_seller2 | 1 在售 | seed-basketball |
| G16 | 帆布鞋 37 码 | 5 服装 | 40 | 3 | 南区 | demo_seller | 1 在售 | seed-sneakers |
| G17 | 高数手写笔记 | 6 学习 | 12 | 3 | 北区 | demo_seller2 | 1 在售 | seed-notes |
| G18 | 宿舍床帘 | 7 宿舍 | 25 | 2 | 东区 | demo_seller | 1 在售 | seed-storage |
| G19 | 民谣吉他 41 寸 | 8 乐器 | 380 | 4 | 南区 | demo_seller | 1 在售 | seed-guitar |
| G20 | 家用打印机 | 9 办公 | 260 | 3 | 东区 | demo_seller2 | 1 在售 | seed-calculator |
| G21 | 20000mAh 充电宝 | 1 数码 | 65 | 3 | 西区 | demo_seller | 1 在售 | seed-phone |
| G22 | 保温杯 500ml | 3 生活 | 28 | 2 | 北区 | demo_seller2 | 1 在售 | seed-storage |
| G23 | 专业课教材两本 | 2 图书 | 38 | 4 | 北区 | demo_seller | 1 在售 | seed-textbook |
| G24 | 晴雨两用伞 | 10 其他 | 19 | 3 | 南区 | demo_seller2 | 1 在售 | seed-accessories |

说明：

- G01~G09 是订单锁定商品（`status=0`）；G10/G11 的订单已取消，按 `OrderService.cancelOrder` 的规则回到 `status=1`。
- 原价字段不存在，因此不设计"原价/折扣"。
- 描述末尾统一加 `【DEMO-V1】` 标记，便于识别与回滚；不写入真实成交或真实用户信息。

### 7.3 订单设计（11 条）

| 订单 | 商品 | 买家 | 卖家 | 状态 | 剧情 | 后续 |
|---|---|---|---|---|---|---|
| O1 | G01 | demo_buyer | demo_seller | 2 已完成 | 手机面交成功 | 评价 5 分 |
| O2 | G02 | demo_buyer2 | demo_seller2 | 2 已完成 | 教材交易 | 评价 4 分 |
| O3 | G03 | demo_student | demo_seller | 2 已完成 | 篮球交易 | 评价 5 分 |
| O4 | G04 | demo_buyer | demo_seller | 2 已完成 | 台灯交易 | 评价 4 分 |
| O5 | G05 | demo_buyer2 | demo_seller2 | 2 已完成 | 考研资料 | 评价 5 分 |
| O6 | G06 | demo_buyer | demo_seller | 1 待买家确认 | 卖家已交付耳机 | demo_buyer 可点"确认收货" |
| O7 | G07 | demo_student | demo_seller | 1 待买家确认 | 键盘已交付 | — |
| O8 | G08 | demo_buyer | demo_seller | 0 待交易 | 刚下单 | demo_seller 可点"确认交付" |
| O9 | G09 | demo_buyer2 | demo_seller2 | 0 待交易 | 刚下单 | — |
| O10 | G10 | demo_buyer | demo_seller | 3 已取消 | 买家填"临时不需要了" | 商品已回上架 |
| O11 | G11 | demo_student | demo_seller | 3 已取消 | 卖家填"物品有瑕疵" | 商品已回上架 |

- 取消字段：`cancel_reason`、`cancel_by`（取消人 `user_id`）、`cancel_time`。
- 每条订单的 `goods_id`、`buyer_id`、`seller_id` 必须真实存在；`seller_id` 必须等于商品发布者。
- 不设计"已完成订单商品仍在售"的矛盾数据。

### 7.4 评价（5 条）

- 仅评价 O1~O5，`user_id` = 对应买家，`score` = 5/4/5/4/5。
- 文案围绕"守时、成色、沟通"，标注为演示评价（描述含 `【DEMO-V1】` 或不写真实姓名）。
- 不伪造真实成交金额差异，价格以订单商品现价为准。

### 7.5 收藏 / 求购 / 社区 / 聊天 / 申诉

| 类型 | 设计要点 |
|---|---|
| 收藏 | demo_buyer 收藏 12 件在售商品；剧情用户 3 条；插入后按真实 `COUNT(*)` 回写 `goods.favorite_count` |
| 求购 | 6 条：3C、教材、宿舍用品、运动、乐器、演出服；5 进行中 1 已关闭 |
| 报价 | 5 条：3 待选、1 已选定（该求购仍保持进行中，除非手动关闭）、1 未入选；`price` 有值；回写 `response_count` |
| 社区帖子 | 9 条，主题为 `闲置分享/使用体验/交易经验/环保循环`；3 条带图（复用 `seed-*`），其余无图 |
| 社区评论 | 18 条，评论者分散在 5 个账号；回写 `comment_count` |
| 社区点赞 | 12 条，遵守唯一键；回写 `like_count` |
| 聊天 | 3 个会话：demo_buyer↔demo_seller（G01 8 条，含订单卡片）、demo_buyer↔demo_seller2（G02 6 条）、demo_buyer↔demo_seller（无商品 4 条）；2 条卖家发来的未读消息用于红点 |
| 申诉 | 2 条：O1 买家申诉"成色与描述不符"（待处理，证据引用 `seed-phone.jpg`）；O4 卖家申诉"买家迟迟不确认"（已受理，`handle_by=demo_admin`） |
| 卖家申请 | demo_buyer 待审核、demo_student 待审核、demo_seller 已通过（审核人 demo_admin） |
| 管理记录 | 4 条：通过申请、处理申诉、下架商品、禁用/启用测试账号（仅文案，不真禁用公开账号） |

### 7.6 聊天 `session_key` 规则（必须严格遵守）

格式：`min(用户A,用户B)-max(用户A,用户B)-商品ID`，无商品时商品 ID 用 `0`。示例：

```sql
SET @buyer_id  = (SELECT user_id FROM `user` WHERE username='demo_buyer');
SET @seller_id = (SELECT user_id FROM `user` WHERE username='demo_seller');
SET @g01_id    = (SELECT goods_id FROM goods WHERE goods_name='iPhone 12 128G 95新' AND description LIKE '%【DEMO-V1】%');
SET @session_key = CONCAT(LEAST(@buyer_id, @seller_id), '-', GREATEST(@buyer_id, @seller_id), '-', @g01_id);
```

写错 `session_key` 会导致会话列表与历史消息拆成两个会话，这是演示数据最常见的翻车点。

### 7.7 表依赖顺序

```text
category（已存在，不新增）
  └─ user（先建 6 个演示账号）
       ├─ goods
       │    ├─ goods_image
       │    ├─ orders（依赖 goods + user ×2）
       │    │    ├─ evaluation（依赖 orders，order_id 唯一）
       │    │    └─ trade_appeal（依赖 orders）
       │    ├─ favorite（依赖 user + goods）
       │    └─ message（可选，依赖 goods + user）
       ├─ wanted_post → wanted_response
       ├─ community_post → community_comment / community_like
       ├─ chat_message（依赖 user ×2，弱关联 goods/orders）
       ├─ seller_application（依赖 user，reviewer 可为 demo_admin）
       ├─ admin_record（依赖 demo_admin）
       └─ conversation / conversation_message（可选，AI 演示）
```

### 7.8 幂等与回滚设计

- 所有演示数据带 `【DEMO-V1】` 标记（商品描述、评价内容、帖子正文等）。
- 用户按 `username LIKE 'demo_%'` 识别。
- 插入统一使用 `INSERT ... SELECT ... WHERE NOT EXISTS`，重复执行不新增。
- 回滚脚本按子表→父表顺序删除（`evaluation`→`orders`→`favorite`→`goods_image`→`goods`→`wanted_response`→`community_*`→`chat_message`→`trade_appeal`→`seller_application`→`admin_record`→`user`），**仅删演示标记数据**，本轮不执行。

---

## 8. 完整 SQL 执行顺序与注意事项（草案，不执行）

### 8.1 推荐执行顺序

| 步骤 | 脚本（建议命名） | 内容 | 执行前提 | 验收 |
|---|---|---|---|---|
| 0 | 备份/新建演示库 | 不导出真实库；新建 `campus_trade_demo` | 获得授权 | 库为空 |
| 1 | `schema.sql` | 10 张基础表 | 仅空库 | 10 表存在 |
| 2 | `v1.2-chat.sql` → `chat-media.sql` | 私聊表 + 类型字段 | 表已建 | 2 列存在 |
| 3 | `v1.2-conversation.sql` | AI 会话表 | — | 2 表存在 |
| 4 | `v1.2-community.sql` | 社区 3 表 | — | 3 表存在 |
| 5 | `v1.2-wanted.sql` → `wanted-quotes.sql` | 求购表 + 报价字段 | — | `price/status` 存在 |
| 6 | `v1.2-order-cancel-appeal.sql` → `appeal-evidences.sql` | 取消字段 + 申诉表/证据 | — | 列存在 |
| 7 | `v1.2-user-avatar.sql` | 头像字段 | — | 列存在 |
| 8 | 分类种子（可复用现库 10 类） | `category` 10 条 | — | 10 条 |
| 9 | 新建 `v1.3-demo-accounts.sql` | 6 个演示账号 | 步骤 8 | 6 条，角色正确 |
| 10 | 新建 `v1.3-demo-goods.sql` | 24 商品 + 图片 | 账号存在 | 15 在售 / 9 下架 |
| 11 | 新建 `v1.3-demo-orders.sql` | 11 订单 | 商品/账号存在 | 四状态覆盖 |
| 12 | 新建 `v1.3-demo-evaluations.sql` | 5 评价 | 已完成订单 | `order_id` 唯一 |
| 13 | 新建 `v1.3-demo-interactions.sql` | 收藏/求购/社区/聊天/申诉/申请/记录 | 上述数据 | 计数一致 |
| 14 | 新建 `v1.3-demo-verify.sql` | 只读验收查询 | — | 全部断言通过 |

### 8.2 注意事项

1. **不要**在当前含 27 个真实资料账号的库上直接跑演示数据；优先独立演示库。
2. **不要**执行 `schema.sql`（会 DROP）；若在现库补数据，跳过步骤 1~7。
3. 结构脚本（`ALTER TABLE`）不幂等，报 `Duplicate column` 属正常，不要重复跑。
4. 不写死主键；用 `SET @var := (SELECT ...)` 取新 ID。
5. 订单与商品状态必须成对：`order_status` 为 0/1/2 的商品 `status=0`；已取消订单的商品 `status=1`。
6. 冗余计数（`favorite_count`、`response_count`、`like_count`、`comment_count`）插入后必须按真实行数回写。
7. 图片 URL 只允许 `/api/files/goods/seed-*.jpg`（已存在）或真实外链；本轮演示数据只用前者。
8. 全部脚本先 `START TRANSACTION`，验收失败 `ROLLBACK`。
9. 演示账号密码哈希为 BCrypt，禁止改成明文或 MD5。
10. 执行前后各跑一次只读验收 SQL，记录行数。

---

## 9. 可实际操作的体验路线（每步对应真实页面与接口）

### 路线 A：买家体验（demo_buyer / 123456，学生入口）

| 步骤 | 页面 | 接口 | 说明 / 验收 |
|---|---|---|---|
| 1 登录 | `/login` | `POST /auth/student/login` | 返回 `role=student`，进入 `/home` |
| 2 浏览搜索 | `/goods` | `GET /goods/page`、`GET /categories` | 关键词（如"键盘"）命中 G07；筛选分类/价格 |
| 3 查看详情 | `/goods/:id` | `GET /goods/{id}`、`GET /goods/{id}/images`、`POST /goods/{id}/view` | 图片正常显示，浏览量 +1 |
| 4 收藏 | 详情页 | `POST /favorite/{goodsId}`、`GET /favorite/my` | `/me/collections` 出现该商品 |
| 5 下单 | 详情页"立即购买" | `POST /orders` | 订单状态 0，商品变下架 |
| 6 聊天 | `/messages` | `GET /chat/sessions`、`POST /chat/send` | 与卖家会话可见历史消息 |
| 7 查看订单 | `/orders`、`/orders/center/:id` | `GET /orders/buyer` | 新订单在"待交易" |
| 8 确认收货 | 订单中心 | `PUT /orders/{id}/status` (target=2) | 仅当状态=1 时可点 |
| 9 评价 | 订单中心 | `POST /evaluations` | 仅已完成订单可评，一单一评 |

> 体验建议：先用已有 O6（状态 1）直接演示"确认收货 → 评价"，再新下一单演示完整链路。

### 路线 B：卖家体验（demo_seller / 123456，卖家入口）

| 步骤 | 页面 | 接口 | 说明 / 验收 |
|---|---|---|---|
| 1 登录 | `/login`（卖家身份） | `POST /auth/seller/login` | 进入 `/home` |
| 2 发布商品 | `/goods/publish` | `POST /upload/image`、`POST /goods`、`POST /goods/{id}/images` | 4 步向导，上传图片后详情可见 |
| 3 管理商品 | `/seller` | `GET /goods/my`、`PUT /goods`、`PUT /goods/{id}/status` | 编辑价格/下架 |
| 4 查看订单 | `/orders`（卖家视图） | `GET /orders/seller` | O8 待交易、O6/O7 待买家确认 |
| 5 处理订单 | 订单中心 | `PUT /orders/{id}/status` (target=1) | O8 点"确认交付"成功 |
| 6 工作台 | `/seller/dashboard` | 统计/订单/评价接口 | ECharts 图表有数据 |
| 7 报价 | `/wanted` | `POST /wanted/{id}/responses` | 可对求购报价 |

### 路线 C：管理员体验（demo_admin / 123456，管理员入口）

| 步骤 | 页面 | 接口 | 说明 / 验收 |
|---|---|---|---|
| 1 登录 | `/login`（管理员身份） | `POST /auth/admin/login` | 进入 `/home`，左侧出现管理入口 |
| 2 后台首页 | `/admin` | `GET /admin/users` 等 | 「卖家申请」Tab **有红点**（2 条待审核） |
| 3 审核申请 | `/admin` | `PUT /admin/seller-applications/{id}/approve` | demo_buyer 变 seller；红点数减少 |
| 4 用户/商品/订单 | `/admin` | `GET /admin/users|goods|orders` | 分页、筛选、禁用/下架/删除 |
| 5 申诉处理 | `/admin` | `GET /appeals`、`PUT /appeals/{id}/handle` | O1 申诉可受理/驳回 |
| 6 操作记录 | `/admin` | `GET /admin/records` | 审核与申诉操作留痕 |
| 7 数据统计 | `/admin/analytics` | `GET /admin/analytics/overview` | 图表有真实增量 |

> ⚠️ 公开共享管理员账号的审核/下架/删除会永久改变演示数据。建议每日重置演示库，或在页面标注"演示用，可随意操作，次日恢复"。

### 路线 D：社区与求购体验

| 步骤 | 页面 | 接口 | 说明 / 验收 |
|---|---|---|---|
| 1 浏览社区 | `/community` | `GET /community/page` | 9 条帖子，4 类主题 |
| 2 看详情/评论 | `/community/:postId` | `GET /community/{id}`、`GET /{id}/comments` | 评论按时间展示 |
| 3 发帖 | `/community` | `POST /community` | 标题 ≤60、正文 ≤2000、主题必选 |
| 4 评论/点赞 | 详情页 | `POST /community/{id}/comments`、`POST /community/{id}/like` | 点赞可取消，计数同步 |
| 5 浏览求购 | `/wanted` | `GET /wanted/page` | 6 条需求 |
| 6 报价 | `/wanted/:id` | `POST /wanted/{id}/responses` | 不能报价自己的求购 |
| 7 选定报价 | 求购详情（发布者） | `PUT /wanted/{id}/responses/{responseId}/accept` | 其他报价变"未入选" |
| 8 收藏/互动 | `/me/collections` | `GET /favorite/my` | 收藏与足迹 |

### 路线 E（加分）：AI 助手与个人中心（游客可试）

| 步骤 | 页面 | 接口 | 说明 |
|---|---|---|---|
| 1 游客问答 | `/ai-assistant` | `POST /ai/chat/stream` | 未配置 Key 时走关键词降级，仍可演示 |
| 2 登录后问答 | 同上 | 同上（携带 Token） | 可查本人订单/卖家信用 |
| 3 历史会话 | 同上 | `GET /ai/conversations` | 仅登录用户有历史 |
| 4 个人中心 | `/profile` | `GET/PUT /user/profile`、`PUT /user/avatar` | 改资料/头像/密码 |

---

## 10. Docker 部署前置条件复核

| # | 检查项 | 实测结论 | 处理建议 |
|---|---|---|---|
| 1 | `pom.xml` 是否仍有 `<skip>true</skip>` | ✅ 仍有（`spring-boot-maven-plugin` 配置） | 打镜像前需去掉或改为可配置；本轮不改 |
| 2 | 能否生成含 `BOOT-INF/` 的可执行 JAR | ❌ 不能。`skip=true` 只产普通 jar；`mvn package` 后无 fat jar | 去掉 skip 后 `mvn clean package` 验证 `BOOT-INF/` |
| 3 | 本机 Maven / IDEA Maven / Wrapper | ✅ `mvn` 不在 PATH；两仓库均无 `mvnw/mvnw.cmd`；IDEA 内置 Maven 可用（需在 IDEA 中执行） | 推荐加 Maven Wrapper 或 Docker 多阶段构建用 `maven:3.8-jdk-17` |
| 4 | `application.yml` 是否硬编码 `local` | ✅ `spring.profiles.active: local` | 改为 `${SPRING_PROFILES_ACTIVE:local}` 或新增 prod profile |
| 5 | 是否需要生产 profile | 💡 需要。当前只有 `application.yml` + 被 gitignore 的 `application-local.yml` | 新增 `application-prod.yml`（不含密钥，全部环境变量） |
| 6 | 数据库连接是否支持环境变量 | ❌ 当前 `application-local.yml` 写死 localhost/账号口令 | prod 用 `${DB_HOST}`、`${DB_PORT}`、`${DB_NAME}`、`${DB_USER}`、`${DB_PASSWORD}` |
| 7 | 上传目录是否支持 `/app/uploads` | ⚠️ 代码支持 `app.upload.dir` 配置，但默认值是 `D:/campus-trade-uploads` | prod 配置 `app.upload.dir: ${APP_UPLOAD_DIR:/app/uploads}`，挂载卷 |
| 8 | Java 中是否仍有 Windows 硬编码 | ✅ 2 处 `@Value("${app.upload.dir:D:/campus-trade-uploads}")`：`WebConfig.java:19`、`UploadController.java:39`；另有 `application-local.yml` 的 `D:/campus-trade-uploads` | 保留默认值不影响 Linux（会被 prod 覆盖），但建议统一改为 `${APP_UPLOAD_DIR:/app/uploads}`（需代码改动，待授权） |
| 9 | 前端是否硬编码 localhost API | ✅ 未发现。`request.js` 的 `baseURL: '/api'`；`vite.config.js` 仅开发代理指向 localhost | 生产由 Nginx 同源代理 |
| 10 | Nginx 是否需要 SPA fallback | 💡 需要。路由为 `createWebHistory`，刷新 `/goods/12` 会 404 | `try_files $uri $uri/ /index.html;` |
| 11 | `/api/` 反代如何保留 context-path | ✅ 后端 `context-path=/api`，前端请求也以 `/api` 开头 | `location /api/ { proxy_pass http://backend:8080; }`（**不要** rewrite 掉 `/api`）；图片 `/api/files/goods/...` 同样走此规则 |
| 12 | MySQL 与上传文件持久化 | 💡 尚未配置 | `mysql_data` 卷 + `uploads_data` 卷挂到 `/app/uploads`；容器重建不丢数据 |
| 13 | 额外阻塞项 | ✅ `seed-images/` 未跟踪（部署复制种子图需单独处理）；`.dockerignore` 不存在；无 Dockerfile/Compose | 新建 `.dockerignore` 排除 `target/`、`seed-images/`、`application-local.yml` |

### 10.1 建议的部署拓扑（💡）

```text
浏览器 → Nginx（80/443，SPA + /api 反代）
              ├─ /            → 前端静态 dist
              └─ /api/*       → campus-trade:8080/api/*（保留前缀）
                                   ├─ MySQL 8.0（mysql_data 卷）
                                   └─ /app/uploads（uploads_data 卷）
```

关键点：

- 前端与后端同源，`baseURL='/api'` 与图片相对 URL 均无需改动。
- 后端只需暴露内网 8080，不对外映射。
- `SPRING_PROFILES_ACTIVE=prod`、`APP_UPLOAD_DIR=/app/uploads`、DB/LLM 全部走环境变量。
- 健康检查可用 `GET /api/home/overview`（公开接口）。

---

## 11. 从本地到公开演示环境的实施顺序

> 每一步都需要单独授权；本轮均未执行。

| 阶段 | 动作 | 产出 | 验收 |
|---|---|---|---|
| P0 | 备份现库（本地 dump，不入 Git） | 备份文件 | 可恢复 |
| P1 | 新建演示库并建结构（21 表） | `campus_trade_demo` | `SHOW TABLES` = 21 |
| P2 | 执行 v1.2 结构脚本（按第 8 节顺序） | 表/列齐全 | 列检查通过 |
| P3 | 写入分类 + 演示账号 | 6 账号 | 三角色可登录 |
| P4 | 写入演示商品/订单/评价/互动数据 | 第 7 节规模 | 验收 SQL 全过 |
| P5 | 修复图片：演示商品只引用存在的 seed 图；不动现有目录 | 无缺失引用 | 双向核对缺失 0 |
| P6 | 前端新增登录页演示账号卡片 | 3 个账号一键填入 | 本地浏览器验证 |
| P7 | 新增 prod profile + 环境变量 | 无硬编码 | 本地以 prod 启动成功 |
| P8 | 去掉 pom `skip`，验证 fat jar | 可执行 jar | `jar tf` 含 `BOOT-INF/` |
| P9 | 新增 Dockerfile / docker-compose / Nginx / `.dockerignore` | 可构建 | 本地 `docker compose up` 全绿 |
| P10 | 复制种子图与必要图片到卷目录 | 图片可访问 | `/api/files/goods/seed-*.jpg` 200 |
| P11 | 部署到公网，配置域名/HTTPS | 可访问地址 | 游客可浏览、可登录 |
| P12 | 更新两个 README 的演示入口与部署命令 | 文档与现状一致 | 链接可点、命令可复现 |
| P13 | 演示前回归 + 数据重置演练 | 重置脚本 | 重置后验收 SQL 全过 |

---

## 12. 每一步的验收标准

| 验收项 | 通过标准 |
|---|---|
| 账号 | 3 个公开账号分别从对应入口登录成功；角色与页面权限一致；错误密码返回 401 |
| 商品 | 列表 ≥15 件在售；详情图片全部 200；搜索"键盘"命中；浏览量随访问 +1 |
| 下单 | 下单后订单状态 0、商品下架；不能买自己的商品；并发下第二人失败提示"手慢了" |
| 订单流转 | 卖家 0→1 成功、买家 1→2 成功；错误角色操作返回业务错误；取消后商品回上架 |
| 评价 | 仅已完成订单可评；重复评价被拒；卖家主页出现评分与评价 |
| 收藏 | 收藏/取消幂等；`favorite_count` 与真实数一致 |
| 聊天 | 会话列表、历史、未读红点正确；图片与订单卡片可展示；不能给自己发消息 |
| 社区 | 发帖/评论/点赞成功；计数与真实行数一致；删除自己帖子级联清理评论点赞 |
| 求购 | 发布/报价/选定成功；不能报价自己的求购；已关闭求购拒绝报价 |
| 申诉 | 仅订单参与方可申诉；同订单同用户仅一条待处理；管理员处理状态 1/2 生效 |
| 后台 | 待审核申请形成红点；审核后红点减少；用户/商品/订单/记录可查 |
| 数据中心 | 概览接口返回非空统计 |
| 图片 | DB 引用与磁盘文件缺失数 0；无外链；容器重启后仍可访问 |
| Docker | `docker compose up -d` 后前端可打开、`/api/home/overview` 200、上传后重启容器图片仍在 |
| 文档 | README 的地址、账号、命令均已实测；无敏感信息 |

---

## 13. 风险与回滚方案

| 风险 | 影响 | 预防 | 回滚 |
|---|---|---|---|
| 在现库直接写演示数据 | 污染真实资料 | 使用独立演示库 | 恢复 P0 备份 |
| `schema.sql` 误执行 | 全库表被 DROP | 演示库才执行；现库禁止 | 从备份恢复 |
| 订单/商品状态不一致 | 详情"已售但可下单"等矛盾 | 按第 7 节状态矩阵生成；验收 SQL 断言 | 删除演示订单并回写商品状态 |
| 冗余计数错误 | 收藏/评论数字不符 | 插入后按 `COUNT(*)` 回写 | 重新回写 |
| `session_key` 写错 | 会话被拆散 | 用 `LEAST/GREATEST` 生成；验收会话数 | 删除错误消息重插 |
| 外链图片失效 | 页面空白 | 演示数据只用本地已存在图片 | 替换 URL 为 seed 图 |
| 共享管理员误操作 | 数据被改/删 | 每日重置；页面提示 | 运行重置脚本 |
| 演示账号密码公开 | 后台被恶意操作 | 仅演示库；限制公网暴露面 | 改密/重置库 |
| Docker 镜像带入本地配置 | 密钥泄露 | `.dockerignore` 排除 `application-local.yml` | 重建镜像并轮换 Key |
| Maven 打包失败 | 无法部署 | 先验证 `BOOT-INF/`；或用多阶段 Docker 构建 | 回退到 IDEA 内置 Maven 产物 |

---

## 14. 需要单独授权的后续动作

1. 新建演示库与执行全部演示 SQL（含 v1.2 结构脚本）。
2. 创建/验证 6 个演示账号，并把账号写进登录页与 README。
3. 前端新增登录页演示账号卡片（改 `Login.vue`）。
4. 新增 `application-prod.yml`，把数据源、上传目录、LLM Key 全部改为环境变量。
5. 修改 `pom.xml` 去掉 `<skip>true</skip>`，并验证 fat jar。
6. 新增 Dockerfile / docker-compose / Nginx 配置 / `.dockerignore`。
7. 复制种子图到部署卷目录（不移动、不改名现有图片）。
8. Git 提交与推送（本轮未执行，`seed-images/` 继续不提交）。

---

## 15. 本轮文档变更

| 文件 | 类型 | 说明 |
|---|---|---|
| `campus-trade/DEPLOYMENT_AND_DEMO_DATA_PLAN.md` | 新增 | 本报告 |
| `campus-trade/README.md` | 修改 | 按实际代码更新功能、结构、启动、数据库与文档入口 |
| `campus-trade-web/README.md` | 修改 | 修正失效链接、补全页面与真实能力、增加部署与演示入口占位 |

> 三个文件均为 Markdown，未触碰业务代码、数据库、配置与图片，未执行 SQL，未执行 Git 提交。
