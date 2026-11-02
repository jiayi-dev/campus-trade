# syntax=docker/dockerfile:1
# ============================================================
# 校园二手交易平台 · 后端镜像（多阶段构建）
# ------------------------------------------------------------
# builder：容器内用 Maven 3.9 + JDK 17 打出 Spring Boot fat jar，
#          不依赖 Windows 宿主机安装 Maven，也不读取宿主机的 target/。
# runtime：仅保留 JRE 17 + fat jar + 种子图片，镜像体积小、可复现。
# ============================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 拷贝源码并打包；skip=false 时 spring-boot-maven-plugin 的 repackage
# 会生成包含 BOOT-INF/ 的可执行 fat jar
COPY pom.xml ./
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre

# 运行用户（非 root）
RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app app

WORKDIR /app

# 可执行 fat jar（spring-boot repackage 产物）
COPY --from=builder /build/target/*.jar /app/app.jar

# 种子图片源文件：仅作为「首次启动填充上传目录」的素材，不直接对外提供
COPY seed-images/web/ /app/seed-images/

# 启动脚本：先补齐 /app/uploads/goods 中缺失的种子图，再启动应用
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

# 上传目录（会被 compose 的 named volume 覆盖），预先建好并授权给运行用户，
# 这样 named volume 首次初始化时会继承该属主，容器内可正常写入
RUN mkdir -p /app/uploads/goods \
    && chmod 0755 /app/docker-entrypoint.sh \
    && chown -R app:app /app

USER app

ENV SPRING_PROFILES_ACTIVE=prod \
    APP_UPLOAD_DIR=/app/uploads \
    TZ=Asia/Shanghai

EXPOSE 8080

ENTRYPOINT ["sh", "/app/docker-entrypoint.sh"]
