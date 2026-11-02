#!/bin/sh
# ============================================================
# 后端容器启动脚本
# ------------------------------------------------------------
# 作用：解决「named volume 挂载会覆盖镜像内种子图片」的问题。
#   · 镜像内 /app/seed-images/ 存放原始素材（phone.jpg 等）
#   · 每次启动检查 /app/uploads/goods/seed-*.jpg，缺失才补齐，
#     已存在的文件（含用户后续上传的图片）绝不覆盖
#   · 完成后 exec 启动 Java 进程，保证 JVM 收到容器停止信号
# ============================================================
set -e

UPLOAD_GOODS_DIR="${APP_UPLOAD_DIR:-/app/uploads}/goods"
SEED_DIR="/app/seed-images"

mkdir -p "$UPLOAD_GOODS_DIR"

if [ -d "$SEED_DIR" ]; then
    for src in "$SEED_DIR"/*.jpg; do
        [ -e "$src" ] || continue
        name=$(basename "$src")
        dest="$UPLOAD_GOODS_DIR/seed-$name"
        if [ ! -f "$dest" ]; then
            cp "$src" "$dest"
        fi
    done
fi

exec java -jar /app/app.jar
