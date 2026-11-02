# 校园二手交易平台 · 后端（campus-trade）

Spring Boot + MyBatis + MySQL 后端服务，为校园二手交易平台前端提供 RESTful API，覆盖商品、订单、私聊、社区、求购、评价、申诉、卖家工作台与管理员后台。

- 前端仓库：[campus-trade-web](https://github.com/jiayi-dev/campus-trade-web)
- 部署与演示数据方案：[DEPLOYMENT_AND_DEMO_DATA_PLAN.md](DEPLOYMENT_AND_DEMO_DATA_PLAN.md)

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 2.6.13 |
| 持久层 | MyBatis（mybatis-spring-boot-starter） | 2.2.2 |
| 数据库 | MySQL | 8.0 |
| 密码加密 | Spring Security Crypto（BCrypt） | — |
| 鉴权 | 自实现 Token（UUID + 内存 Map，12 小时过期） | — |
| 工具 | Lombok | — |
| AI | OpenAI-Compatible LLM + SSE 流式 + Tool Calling + RAG | — |

## 功能模块

| 模块 | 主要接口（context-path `/api`） | 说明 |
|------|------|------|
| 认证 | `POST /auth/student/login`、`/auth/seller/login`、`/auth/admin/login`、`GET /auth/me` | 学生/卖家共用入口，管理员入口严格校验 `role=admin` |
| 用户 | `POST /user/register`、`GET/PUT /user/profile`、`PUT /user/password`、`GET/PUT /user/avatar` | 注册默认 `student`，个人资料与密码修改 |
| 商品 | `GET /goods/page`、`GET /goods/{id}`、`POST/PUT/DELETE /goods`、`PUT /goods/{id}/status` | 搜索、筛选、分页、发布、编辑、上下架、批量操作 |
| 分类 | `GET /categories` | 10 个校园闲置分类 |
| 收藏 | `POST/DELETE /favorite/{goodsId}`、`GET /favorite/my` | `(user_id, goods_id)` 唯一 |
| 留言 | `GET /messages/goods/{goodsId}`、`POST /messages` | 商品详情页留言板，与私聊分表 |
| 订单 | `POST /orders`、`PUT /orders/{id}/status`、`PUT /orders/{id}/cancel`、`GET /orders/buyer|seller` | 0 待交易 → 1 待买家确认 → 2 已完成；0/1 可取消并回上架 |
| 私聊 | `GET /chat/sessions`、`/chat/history`、`POST /chat/send`、`GET /chat/unread` | 支持文本、图片、订单卡片与未读红点 |
| 求购 | `GET/POST/PUT /wanted`、`POST /wanted/{id}/responses`、`PUT .../accept` | 多条报价，报价状态 0 待选 / 1 已选定 / 2 未入选 |
| 社区 | `GET /community/page`、`POST /community`、`POST /community/{id}/comments`、`POST /community/{id}/like` | 动态、评论、点赞，计数按真实行数刷新 |
| 评价 | `POST /evaluations`、`GET /evaluations/seller/{id}`、`/goods/{id}` | 仅买家对已完成订单评价，一单一评 |
| 申诉 | `POST /appeals`、`GET /appeals/my`、`GET /appeals`、`PUT /appeals/{id}/handle` | 支持最多 5 张证据图，管理员受理/驳回 |
| 卖家 | `POST /seller/apply`、`GET /seller/application/my`、卖家工作台统计接口 | 学生申请 → 管理员审核 → `role` 升级 |
| 后台 | `/admin/users`、`/admin/goods`、`/admin/orders`、`/admin/seller-applications`、`/admin/records` | 用户、商品、订单、申请审核与操作留痕 |
| 数据中心 | `GET /admin/analytics/overview` | 平台运营数据聚合 |
| 上传 | `POST /upload/image` | UUID 重命名、扩展名白名单、文件头魔数校验，上限 5MB |
| AI 助手 | `POST /ai/chat`、`POST /ai/chat/stream`、`/ai/conversations`、`POST /admin/knowledge/rebuild` | SSE 流式问答、工具调用、RAG 知识库；未配置 Key 时降级 |

> 未实现：服务端登出接口、商品原价字段、收货地址/物流、在线支付。前端退出仅清除本地 Token。

## 目录结构

```text
src/main/java/com/jiayi/campustrade
├── CampusTradeApplication.java    # 启动类
├── auth/                          # TokenManager / AuthInterceptor（RBAC）/ AuthContext（ThreadLocal）
├── config/                        # WebConfig（拦截器、静态资源、CORS）、AiProperties
├── exception/                     # BusinessException + GlobalExceptionHandler（统一 JSON）
├── controller/                    # 20 个 Controller
├── service/                       # 业务层（含资源归属校验、订单状态机）
│   └── ai/                        # AI 工具：商品搜索、订单状态、卖家信用
├── mapper/                        # MyBatis Mapper 接口
└── entity/                        # 实体类

src/main/resources
├── mapper/                        # MyBatis XML
├── knowledge/                     # 6 个 Markdown 知识库文档（AI 助手 RAG 数据源）
├── application.yml                # 公共配置（context-path=/api，默认激活 local）
└── application-local.yml          # 本地配置（数据库/上传目录/LLM），已被 .gitignore 忽略
```

## 本地启动

### 1. 环境要求

- JDK 17
- MySQL 8.0
- Maven（当前仓库未提供 Maven Wrapper；若命令行没有 `mvn`，使用 IDEA 内置 Maven）

### 2. 初始化数据库

全新环境按顺序执行：

```bash
mysql -u root -p < sql/schema.sql          # 基础 10 张表（会 DROP 重建，仅限空库）
mysql -u root -p < sql/data.sql            # 可选演示数据（显式主键，仅限空库执行一次）

mysql -u root -p < sql/v1.2-chat.sql
mysql -u root -p < sql/v1.2-chat-media.sql
mysql -u root -p < sql/v1.2-conversation.sql
mysql -u root -p < sql/v1.2-community.sql
mysql -u root -p < sql/v1.2-wanted.sql
mysql -u root -p < sql/v1.2-wanted-quotes.sql
mysql -u root -p < sql/v1.2-order-cancel-appeal.sql
mysql -u root -p < sql/v1.2-appeal-evidences.sql
mysql -u root -p < sql/v1.2-user-avatar.sql
```

说明：

- `schema.sql` 只包含最初的 10 张表；聊天、社区、求购、申诉、AI 会话、知识库等表由 `v1.2-*.sql` 补充，完整库共 21 张表。
- `ALTER TABLE` 类脚本不幂等，重复执行报 `Duplicate column` 属正常。
- **不要**在已有业务数据的库上执行 `schema.sql`。

### 3. 配置本地参数

`application-local.yml` 不入库，需要自行创建，至少包含：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_trade?serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: <你的数据库账号>
    password: <你的数据库密码>

app:
  upload:
    dir: D:/campus-trade-uploads        # 本机图片目录，Linux 可改为 /app/uploads

llm:
  base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
  api-key: ${LLM_API_KEY:}              # 留空时 AI 助手降级为关键词问答
```

> 请勿把真实口令、API Key 提交到仓库。

### 4. 启动

当前 `pom.xml` 中 `spring-boot-maven-plugin` 配置了 `<skip>true</skip>`，因此：

- IDEA 中直接运行主类 `com.jiayi.campustrade.CampusTradeApplication`；
- `mvn compile` 可用于编译检查；
- 如需可执行 fat jar，必须先移除 `skip`（详见部署方案第 10 节）。

服务地址：`http://localhost:8080/api`

## 接口约定

- 统一响应：`{ "code": 400, "message": "商品已经售出", "data": null }`，由 `GlobalExceptionHandler` 兜底。
- 鉴权：请求头 `Authorization: Bearer <token>`；401 未登录 / 403 无权限。
- 公开接口：`GET /goods/page`、`GET /goods/{id}`、`/goods/{id}/images`、`GET /categories`、`GET /evaluations/seller|goods/{id}`、`GET /home/overview`、`GET /home/showcase`、`POST /ai/chat/stream`（游客可选鉴权）。
- 图片：上传返回相对 URL `/api/files/goods/xxx.jpg`，由 `WebConfig` 映射到上传目录，换部署地址无需迁移数据。

## 数据库

数据库 `campus_trade`，共 21 张表：

- 基础表：`user`、`category`、`goods`、`goods_image`、`favorite`、`message`、`orders`、`evaluation`、`seller_application`、`admin_record`
- V1.2 业务表：`chat_message`、`conversation`、`conversation_message`、`community_post`、`community_comment`、`community_like`、`wanted_post`、`wanted_response`、`trade_appeal`
- AI 知识库：`kb_doc`、`kb_chunk`

关键约束：`user.username` / `user.phone` 唯一，`evaluation.order_id` 唯一，`favorite(user_id, goods_id)` 唯一，`community_like(post_id, user_id)` 唯一，`category.category_name` 唯一。

## Docker 部署

当前仓库**尚未包含** Dockerfile / docker-compose / Nginx 配置，部署前存在已知阻塞项（`pom.xml` 的 `skip`、`application.yml` 写死 `local` profile、上传目录默认值）。完整的前置条件、实施顺序与验收标准见：

- [DEPLOYMENT_AND_DEMO_DATA_PLAN.md](DEPLOYMENT_AND_DEMO_DATA_PLAN.md) 第 10、11 节

部署完成并实测后，再在本节补充经过验证的启动命令与镜像构建命令。

## 公开演示环境

> 状态：尚未部署，演示账号尚未创建。
> 部署与账号验证完成后，在此填写访问地址、`demo_buyer` / `demo_seller` / `demo_admin` 三个公共演示账号及密码，并标注“公共演示数据，可能被其他访客修改”。

## 演示数据库初始化（方案 A · 独立实例）

独立 MySQL 实例下从零建好 21 张表 + 导入演示数据的最小三步：

1. 建库：[sql/00-create-demo-database.sql](sql/00-create-demo-database.sql)
2. 建表（21 张，整合 schema.sql + 全部 v1.2 结构变更）：[sql/01-create-demo-schema.sql](sql/01-create-demo-schema.sql)
3. 演示数据：[sql/demo-data.sql](sql/demo-data.sql)

> 完整步骤、验收 SQL、备份恢复与误操作防护见 [DEMO_DATA_GUIDE.md](DEMO_DATA_GUIDE.md)。原本地 `localhost:3306` 的开发库禁止用于演示。

## 文档

- 部署与演示数据方案：[DEPLOYMENT_AND_DEMO_DATA_PLAN.md](DEPLOYMENT_AND_DEMO_DATA_PLAN.md)
- 演示数据库完整步骤：[DEMO_DATA_GUIDE.md](DEMO_DATA_GUIDE.md)
- 前端仓库：[campus-trade-web](https://github.com/jiayi-dev/campus-trade-web)

## 安全约定

- `application-local.yml`、真实数据库备份、图片文件、API Key 一律不提交。
- 密码使用 BCrypt 存储，演示账号也只写入 BCrypt 哈希。
- 图片上传做扩展名白名单与文件头魔数校验，文件名使用 UUID。
- 数据库中的真实用户资料不得导出或复制进演示数据。
