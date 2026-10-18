# 校园二手交易平台 · 后端（campus-trade）

Spring Boot 2.6.13 + MyBatis + MySQL 8.0 后端服务，为前端提供 RESTful API。

项目整体介绍、系统架构、角色权限、核心业务与升级路线见仓库根目录 [README](../README.md)。

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 2.6.13 |
| 持久层 | MyBatis（mybatis-spring-boot-starter） | 2.2.2 |
| 数据库 | MySQL | 8.0 |
| 密码加密 | Spring Security Crypto（BCrypt） | — |
| 工具 | Lombok | — |

## 快速启动

1. 初始化数据库（会 DROP 重建全部 10 张表，仅用于全新环境）：

   ```bash
   mysql -u root -p < sql/schema.sql
   mysql -u root -p < sql/data.sql
   ```

2. 修改 `src/main/resources/application-local.yml` 中的数据库连接（url / username / password）。

3. 编译并启动：

   ```bash
   mvn compile
   ```

   使用 IDEA 运行主类 `com.jiayi.campustrade.CampusTradeApplication`（pom 中 spring-boot-maven-plugin 配置了 `skip`，`mvn spring-boot:run` 不可用）。

服务地址：`http://localhost:8080/api`

## 模块结构

```text
src/main/java/com/jiayi/campustrade
├── CampusTradeApplication.java    # 启动类
├── auth/                          # AuthContext（ThreadLocal）/ AuthInterceptor（RBAC）/ TokenManager
├── config/WebConfig.java          # 拦截器注册 + CORS
├── exception/                     # BusinessException + GlobalExceptionHandler（统一 JSON 响应）
├── controller/                    # 12 个 Controller（Auth/User/Goods/Category/Favorite/Message/Order/Home/Seller/Admin/Upload/Ai）
├── service/                       # 业务层（含 Ownership 资源归属校验）
├── mapper/                        # MyBatis Mapper 接口
└── entity/                        # 10 个实体
```

## 统一异常响应

业务异常统一抛出 `BusinessException`，由 `GlobalExceptionHandler`（`@RestControllerAdvice`）兜底转换为统一 JSON：

```json
{ "code": 400, "message": "商品已经售出", "data": null }
```

登录拦截器 401 / 403 与未预期异常（500）均返回同一结构。

## 数据库

数据库 `campus_trade`，共 10 张表：`user`、`category`、`goods`、`goods_image`、`favorite`、`message`、`orders`、`evaluation`、`seller_application`、`admin_record`。

- `sql/schema.sql`：从真实运行库 mysqldump 导出的结构（主键 / AUTO_INCREMENT / NOT NULL / DEFAULT / 索引 / 外键 / utf8mb4）。
- `sql/data.sql`：演示数据（账号均为虚构，密码统一 `123456`）。
