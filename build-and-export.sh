#!/bin/bash
set -e

# 定义镜像名称
BUILD_TOOL_IMAGE="maven:3.9-eclipse-temurin-21-alpine"
TEMP_RUN_IMAGE="temp-build-image:latest"
TAR_NAME="random-gallery.tar"

echo "🚀 开始跨平台构建流程..."

# 1. 获取本地 .m2 路径
LOCAL_M2="$HOME/.m2"

# 2. 步骤1: 容器内编译 (如果本地没有镜像，会自动下载)
echo "📦 步骤1: 正在容器内编译..."
docker run --rm \
    -v "$LOCAL_M2":/root/.m2 \
    -v "$(pwd)":/app \
    -w /app \
    $BUILD_TOOL_IMAGE \
    mvn clean package -DskipTests

# 3. 步骤2: 构建 AMD64 运行镜像
echo "🐳 步骤2: 正在构建临时运行镜像..."
docker build --platform linux/amd64 -t $TEMP_RUN_IMAGE .

# 4. 步骤3: 导出镜像
echo "📁 步骤3: 导出 Docker 镜像为 $TAR_NAME..."
docker save -o $TAR_NAME $TEMP_RUN_IMAGE

# 5. 步骤4: 清理（只清理本项目产生的临时镜像，保留 Maven 工具）
echo "🧹 步骤4: 正在清理本项目临时产物..."
docker rmi $TEMP_RUN_IMAGE
docker image prune -f

echo ""
echo "✅ 构建完成！"
echo "📁 产物文件: $TAR_NAME"
echo "💡 提示: 本地保留了 Maven 镜像以加速下次构建，如需彻底删除请运行: docker rmi $BUILD_TOOL_IMAGE"