#!/bin/bash

# 随机图库 - 一键构建和导出脚本
# 功能：Maven打包 -> Docker构建 -> 导出tar文件

set -e  # 遇到错误立即退出

echo "🚀 开始构建随机图库应用..."

# 步骤1: Maven打包
echo "📦 步骤1: Maven打包..."
mvn clean package -DskipTests
if [ $? -eq 0 ]; then
    echo "✅ Maven打包成功"
else
    echo "❌ Maven打包失败"
    exit 1
fi

# 步骤2: Docker构建
echo "🐳 步骤2: Docker镜像构建..."
docker build -t helloworld:latest .
if [ $? -eq 0 ]; then
    echo "✅ Docker镜像构建成功"
else
    echo "❌ Docker镜像构建失败"
    exit 1
fi

# 步骤3: 导出tar文件
echo "📁 步骤3: 导出Docker镜像为tar文件..."
docker save -o helloworld.tar helloworld:latest
if [ $? -eq 0 ]; then
    echo "✅ Docker镜像导出成功"
else
    echo "❌ Docker镜像导出失败"
    exit 1
fi

# 显示文件信息
echo ""
echo "🎉 构建完成！"
echo "📊 文件信息："
ls -lh helloworld.tar
echo ""
echo "📋 使用说明："
echo "   导入镜像: docker load -i helloworld.tar"
echo "   运行容器: docker run -p 8086:8086 helloworld:latest"
echo "   自定义数据库: docker run -p 8086:8086 -e DB_HOST=your_host -e DB_USERNAME=your_user -e DB_PASSWORD=your_pass helloworld:latest"

