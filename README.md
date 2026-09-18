# 校园二手交易平台（后端）

## 项目简介

校园二手交易平台后端项目，基于 Spring Boot + MyBatis + MySQL 开发，采用前后端分离架构，为前端提供 RESTful API 服务。

实现校园二手交易核心业务流程，包括多角色登录认证、商品管理、订单查询、收藏管理、数据统计等功能，并通过 Token 机制与 RBAC 角色权限控制保障接口安全。

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 2.6.13 |
| 持久层 | MyBatis | 3.5.9 |
| 数据库 | MySQL | 8.0 |
| 密码加密 | Spring Security Crypto (BCrypt) | 5.6.8 |
| 工具 | Lombok | 1.18.24 |
| JDK | Java | 17 |
| 构建工具 | Maven | - |

## 核心功能

### 认证与权限

- 多角色登录：管理员（admin）、卖家（seller）、学生（student）
- Token 身份认证：UUID 生成，有效期 12 小时，服务端内存存储
- RBAC 角色权限控制：基于请求路径的管理员/卖家权限拦截
- BCrypt 密码加密存储与校验

### 商品模块

- 商品动态条件分页查询（名称、分类、价格区间）
- 商品详情查询
- 商品新增 / 编辑 / 删除
- 批量新增商品
- 我的商品列表

### 统计模块

- 首页统计：商品总数、用户总数、分类数、商品总价
- 各分类平均价格
- 商品价格区间分布

### 交易模块

- 订单列表查询

### 收藏模块

- 批量收藏处理（存在则更新时间，不存在则新增）

### 用户模块

- 用户注册
- 用户信息查询

## 项目结构

```
src/main/java/com/jiayi/campustrade
├── CampusTradeApplication.java    # 启动类
├── auth/                          # 认证模块
│   ├── AuthContext.java           # 当前登录用户上下文（ThreadLocal）
│   ├── AuthInterceptor.java       # 登录校验 + RBAC 权限拦截器
│   └── TokenManager.java          # Token 生成、校验、过期管理
├── config/
│   └── WebConfig.java             # 拦截器注册 + CORS 跨域配置
├── controller/                    # 控制层
│   ├── AuthController.java        # 登录接口
│   ├── UserController.java        # 用户注册/查询
│   ├── GoodsController.java       # 商品 CRUD + 统计
│   ├── OrderController.java       # 订单查询
│   ├── FavoriteController.java    # 收藏处理
│   └── AdminController.java       # 管理员接口
├── entity/                        # 实体类（8张表）
│   ├── User.java
│   ├── Category.java
│   ├── Goods.java
│   ├── Orders.java
│   ├── Message.java
│   ├── Favorite.java
│   ├── Evaluation.java
│   └── AdminRecord.java
├── mapper/                        # 数据访问层接口
└── service/                       # 业务逻辑层

src/main/resources
├── application.yml                # 主配置（端口、上下文路径、MyBatis）
├── application-local.yml          # 本地数据库配置
└── mapper/                        # MyBatis XML 映射文件
    ├── UserMapper.xml
    ├── GoodsMapper.xml
    ├── FavoriteMapper.xml
    └── OrderMapper.xml
```

## 数据库设计

数据库名：`campus_trade`，共 8 张核心表：

| 表名 | 实体类 | 说明 |
|------|--------|------|
| `user` | User | 用户表（管理员/卖家/学生） |
| `category` | Category | 商品分类表 |
| `goods` | Goods | 商品表 |
| `orders` | Orders | 订单表 |
| `message` | Message | 留言表 |
| `favorite` | Favorite | 收藏表 |
| `evaluation` | Evaluation | 评价表 |
| `admin_record` | AdminRecord | 管理员操作记录表 |

### user 表关键字段

| 字段 | 类型 | 说明 |
|------|------|------|
| user_id | int | 主键 |
| username | varchar | 用户名 |
| password | varchar | BCrypt 加密密码 |
| phone | varchar | 手机号 |
| email | varchar | 邮箱 |
| role | varchar | 角色：admin / seller / student |
| status | int | 状态：1 启用，0 禁用 |
| create_time | datetime | 注册时间 |

### goods 表关键字段

| 字段 | 类型 | 说明 |
|------|------|------|
| goods_id | int | 主键 |
| user_id | int | 发布者用户ID |
| category_id | int | 分类ID |
| goods_name | varchar | 商品名称 |
| description | text | 商品描述 |
| price | decimal | 价格 |
| status | int | 状态 |
| create_time | datetime | 发布时间 |

## API 接口文档

服务基础路径：`http://localhost:8080/api`

### 认证接口（无需 Token）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/admin/login` | 管理员登录 |
| POST | `/auth/student/login` | 学生登录 |
| POST | `/auth/seller/login` | 卖家登录 |

登录请求体：
```json
{
  "username": "admin",
  "password": "123456"
}
```

登录成功响应：
```json
{
  "token": "xxxxxxxxxxxxxxxx",
  "userId": 1,
  "username": "admin",
  "role": "admin"
}
```

### 用户接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/user/register` | 用户注册 | 公开 |
| GET | `/user/{id}` | 查询用户 | 需登录 |

### 商品接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/goods/page` | 分页查询 | 需登录 |
| GET | `/goods/{id}` | 商品详情 | 需登录 |
| POST | `/goods` | 新增商品 | 需登录 |
| PUT | `/goods` | 修改商品 | 需登录 |
| DELETE | `/goods/{id}` | 删除商品 | 需登录 |
| POST | `/goods/batch` | 批量新增 | 需登录 |
| GET | `/goods/statistics` | 首页统计 | 需登录 |
| GET | `/goods/categoryAvg` | 分类均价 | 需登录 |
| GET | `/goods/priceLevelCount` | 价格区间分布 | 需登录 |
| GET | `/goods/my` | 我的商品 | 需登录 |

分页查询参数：
```
goodsName   商品名称（模糊匹配，可选）
categoryId  分类ID（可选）
minPrice    最低价格（可选）
maxPrice    最高价格（可选）
pageNum     页码，默认 1
pageSize    每页条数，默认 5
```

### 订单接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/orders` | 订单列表 | 需登录 |

### 收藏接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/favorite/batch` | 批量收藏处理 | 需登录 |

### 管理员接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/admin/test` | 管理员测试 | admin |

## 认证机制

### Token 使用方式

登录成功后，前端需在后续请求头中携带 Token：

```
Authorization: Bearer <token>
```

或直接：

```
Authorization: <token>
```

### 白名单路径（无需 Token）

- `/auth/**` —— 所有登录接口
- `/user/register` —— 用户注册
- `/error` —— 错误页

### 权限控制规则

| 路径前缀 | 允许角色 |
|----------|----------|
| `/api/admin/**` | admin |
| `/api/seller/**` | seller / admin |
| 其他路径 | 任意已登录用户 |

### Token 管理

- 生成方式：UUID 去横线
- 存储方式：服务端 `ConcurrentHashMap`
- 有效期：12 小时
- 过期策略：惰性删除（访问时检测并清除）

## 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+

## 快速开始

### 1. 创建数据库

```sql
CREATE DATABASE campus_trade DEFAULT CHARACTER SET utf8mb4;
```

### 2. 配置数据库连接

修改 `src/main/resources/application-local.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/campus_trade?serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: 你的密码
```

### 3. 编译运行

```bash
mvn clean compile
mvn spring-boot:run
```

启动成功后访问：

```
http://localhost:8080/api
```

## 前端项目

前端仓库：[https://github.com/jiayi-dev/campus-trade-web](https://github.com/jiayi-dev/campus-trade-web)

前端技术栈：Vue3 + Vite + Element Plus + Axios + Vue Router

## 项目信息

- 项目名称：校园二手交易平台
- 开发模式：前后端分离
- 后端技术：Spring Boot + MyBatis + MySQL
- 前端技术：Vue3 + Vite + Element Plus
