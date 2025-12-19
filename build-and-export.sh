#!/bin/bash
set -e

# 设置镜像名称
IMAGE_NAME="random-gallery:latest"
TAR_NAME="random-gallery.tar"

echo "🚀 开始跨平台构建流程 (复用本地 Maven 缓存)..."

# 1. 获取本地 .m2 路径
LOCAL_M2="$HOME/.m2"

# 2. 步骤1: 容器内编译 (解决 JDK 21 兼容性问题)
echo "📦 步骤1: 正在容器内编译..."
docker run --rm \
    -v "$LOCAL_M2":/root/.m2 \
    -v "$(pwd)":/app \
    -w /app \
    maven:3.9-eclipse-temurin-21-alpine \
    mvn clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ Maven 编译打包成功"
else
    echo "❌ Maven 编译失败"
    exit 1
fi

# 3. 步骤2: 构建 AMD64 运行镜像 (使用默认 Dockerfile)
echo "🐳 步骤2: 正在构建 AMD64 运行镜像..."
docker build --platform linux/amd64 -t $IMAGE_NAME .

# 4. 步骤3: 导出镜像
echo "📁 步骤3: 导出 Docker 镜像..."
docker save -o $TAR_NAME $IMAGE_NAME

if [ $? -eq 0 ]; then
    echo "✅ 镜像导出成功: $TAR_NAME"
    echo "📊 验证镜像架构:"
    docker inspect $IMAGE_NAME | grep Architecture
else
    echo "❌ 镜像导出失败"
    exit 1
fi

echo ""
echo "🎉 构建完成！"