#!/bin/bash
set -euo pipefail

BUILD_TOOL_IMAGE="${BUILD_TOOL_IMAGE:-maven:3.9-eclipse-temurin-21-alpine}"
TEMP_RUN_IMAGE="${TEMP_RUN_IMAGE:-temp-build-image:latest}"
TAR_NAME="${TAR_NAME:-random-gallery.tar}"
PLATFORM="${PLATFORM:-linux/amd64}"
CLEAN_BUILD="${CLEAN_BUILD:-false}"

if [[ "$CLEAN_BUILD" == "true" ]]; then
  MVN_GOAL="clean package"
else
  MVN_GOAL="package"
fi

echo "🚀 开始跨平台构建流程..."
echo "📌 Build platform: $PLATFORM"
echo "📌 Maven goal: $MVN_GOAL -DskipTests"

LOCAL_M2="$HOME/.m2"

echo "📦 步骤1: 容器内编译..."
docker run --rm \
    -v "$LOCAL_M2":/root/.m2 \
    -v "$(pwd)":/app \
    -w /app \
    "$BUILD_TOOL_IMAGE" \
    mvn $MVN_GOAL -DskipTests

echo "🐳 步骤2: 构建临时运行镜像..."
docker build --platform "$PLATFORM" --tag "$TEMP_RUN_IMAGE" .

echo "📁 步骤3: 导出 Docker 镜像为 $TAR_NAME..."
docker save -o "$TAR_NAME" "$TEMP_RUN_IMAGE"

echo "🧹 步骤4: 清理本项目临时产物..."
docker rmi "$TEMP_RUN_IMAGE"

echo ""
echo "✅ 构建完成！"
echo "📁 产物文件: $TAR_NAME"
echo "💡 可选参数: CLEAN_BUILD=true PLATFORM=linux/arm64 TAR_NAME=xxx.tar ./build-and-export.sh"
