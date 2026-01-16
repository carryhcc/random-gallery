#!/bin/bash
# 构建处理
set -e

echo "🚀 开始构建前后端统一包..."

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 1. 构建前端echo -e "${BLUE}📦 Step 1/4: 构建前端...${NC}"
cd frontend
npm run build:prod
cd ..

# 2. 验证前端构建产物
echo -e "${BLUE}✅ Step 2/4: 验证前端构建产物...${NC}"
if [ -d "src/main/resources/static" ]; then
    echo -e "${GREEN}前端构建产物已生成到 src/main/resources/static${NC}"
    ls -lh src/main/resources/static/ | head -10
else
    echo -e "${YELLOW}警告: 未找到前端构建产物目录${NC}"
fi

# 3. 构建后端
echo -e "${BLUE}🔨 Step 3/4: 构建后端 (包含前端静态文件)...${NC}"
mvn clean package -DskipTests

# 4. 构建 Docker 镜像
echo -e "${BLUE}🐳 Step 4/4: 构建 Docker 镜像...${NC}"
if [ -f "Dockerfile" ]; then
    docker build -t random-gallery:latest .
    
    # 导出镜像
    echo -e "${BLUE}💾 导出 Docker 镜像为 tar.gz...${NC}"
    docker save random-gallery:latest | gzip > random-gallery-$(date +%Y%m%d-%H%M%S).tar.gz
    
    echo -e "${GREEN}✅ 构建完成！${NC}"
    echo -e "${GREEN}📦 镜像文件: random-gallery-$(date +%Y%m%d-%H%M%S).tar.gz${NC}"
    echo -e "${BLUE}🚀 运行命令:${NC}"
    echo -e "   docker load < random-gallery-*.tar.gz"
    echo -e "   docker run -p 8086:8086 random-gallery:latest"
else
    echo -e "${YELLOW}未找到 Dockerfile，跳过 Docker 构建${NC}"
    echo -e "${GREEN}✅ 后端 JAR 包已生成: target/random-gallery-*.jar${NC}"
fi
