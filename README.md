# 随机图库 (Random Gallery)

一个基于 **Spring Boot + Next.js** 的现代化前后端分离图片展示应用，支持多环境切换、分组管理和图片浏览功能。

## 🌟 功能特性

### 核心功能
- **随机图片展示** - 获取随机单张图片
- **随机画廊** - 瀑布流无限滚动
- **随机套图浏览** - 获取随机套图集合
- **分组管理** - 支持图片分组查询和管理
- **随机动图** - 全屏沉浸式体验
- **图片下载** - 小红书作品下载管理

### 技术特性
- **现代化UI** - React 19 + Tailwind CSS，响应式设计
- **深色模式** - 完整的深色主题支持
- **环境切换** - 运行时动态切换数据库环境
- **PWA支持** - 可安装到主屏幕，离线访问
- **前后端分离** - Next.js静态导出，单包部署

---

## 🚀 快速开始

### 环境要求
- **Java** 21+
- **Maven** 3.6+
- **MySQL** 5.7+
- **Node.js** 20+ (仅开发环境需要)
- Docker (可选)

### 开发环境

#### 方式一：一键启动（推荐）⭐

```bash
# 同时启动前后端服务
./dev-start.sh

# 访问地址：
# - 前端: http://localhost:3000
# - 后端: http://localhost:8086

# 按 Ctrl+C 停止所有服务
# 或者使用停止脚本
./dev-stop.sh
```

#### 方式二：分别启动

#### 1. 启动后端
```bash
# 配置数据库 (src/main/resources/db.yaml)
mvn spring-boot:run
# 访问 http://localhost:8086
```

#### 2. 启动前端
```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:3000
```

### 生产部署

#### 方式一：统一构建（推荐）

```bash
# 一键构建前后端
./build-all.sh

# 运行 JAR 包
java -jar target/random-gallery-0.0.1-SNAPSHOT.jar

# 或使用 Docker
docker load < random-gallery-*.tar.gz
docker run -p 8086:8086 random-gallery:latest
```

#### 方式二：手动构建

```bash
# 1. 构建前端
cd frontend
npm run build:prod
cd ..

# 2. 构建后端（已包含前端）
mvn clean package -DskipTests

# 3. 运行
java -jar target/random-gallery-*.jar
```

---

## 📖 API 文档

### 图片相关接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/pic/random/one` | GET | 获取随机单张图片 |
| `/api/pic/list` | POST | 根据分组查询图片列表 |

### 分组管理接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/group/randomGroupInfo` | GET | 获取随机分组信息 |
| `/api/group/list/paged` | POST | 分页查询分组列表 |

### 系统管理接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/system/env/currentInfo` | GET | 获取当前环境信息 |
| `/api/system/env/{env}` | GET | 切换环境（dev/test/prod） |

### 下载管理接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/xhs/work/list` | POST | 获取作品列表 |
| `/api/xhs/work/{workId}` | GET | 获取作品详情 |
| `/api/xhs/work/addDownloadTask` | POST | 添加下载任务 |

---

## 🏗️ 项目结构

```
random-gallery/
├── frontend/                      # Next.js 前端项目
│   ├── src/
│   │   ├── app/                  # 页面和路由
│   │   ├── components/           # React 组件
│   │   └── lib/                  # 工具库和API
│   ├── public/                   # 静态资源
│   ├── next.config.ts            # Next.js 配置
│   └── package.json
│
├── src/main/                     # Spring Boot 后端
│   ├── java/                     # Java源码
│   └── resources/
│       ├── static/               # 前端构建产物（自动生成）
│       ├── mapper/               # MyBatis 映射
│       └── application.yml       # 应用配置
│
├── pom.xml                       # Maven 配置
├── Dockerfile                    # Docker 镜像构建
└── build-all.sh                  # 统一构建脚本
```

---

## ⚙️ 配置说明

### 数据库配置 (`src/main/resources/db.yaml`)

```yaml
db:
  host: localhost
  port: 3306
  name: your_database
  username: your_username
  password: your_password
```

### 前端环境变量

**开发环境** (frontend/.env.local):
```env
NEXT_PUBLIC_API_URL=http://localhost:8086/api
```

**生产环境** (frontend/.env.production):
```env
NEXT_PUBLIC_API_URL=/api
```

---

## 🎨 技术栈

### 前端
- **框架**: Next.js 16.1.2 (App Router)
- **UI库**: React 19.2.3
- **语言**: TypeScript 5
- **样式**: Tailwind CSS 4
- **状态管理**: Zustand + TanStack Query
- **HTTP**: Axios
- **图标**: Lucide React
- **主题**: next-themes
- **PWA**: @ducanh2912/next-pwa

### 后端
- **框架**: Spring Boot 3.4.0
- **数据库**: MySQL 8.0
- **ORM**: MyBatis-Plus 3.5.15
- **语言**: Java 21
- **工具**: Lombok, Hutool

---

## 🔧 开发指南

### 添加新页面

1. 在 `frontend/src/app/` 下创建路由目录
2. 创建 `page.tsx` 和 `layout.tsx`
3. 在 `lib/api.ts` 中添加API接口
4. 使用 TanStack Query 进行数据获取

### 环境切换

- 左上角下拉菜单可切换开发/测试/生产环境
- 切换后自动刷新统计信息
- 支持运行时动态切换数据源

### 主题切换

- 右上角按钮切换深色/浅色模式
- 支持系统偏好自动切换
- 所有组件完整适配深色模式

---

## 📦 部署说明

### Docker 部署（推荐）

```bash
# 1. 构建
./build-all.sh

# 2. 加载镜像
docker load < random-gallery-*.tar.gz

# 3. 运行
docker run -d \
  --name random-gallery \
  --restart unless-stopped \
  -p 8086:8086 \
  -e DB_HOST=your-db-host \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=your-pass \
  random-gallery:latest
```

### 传统部署

```bash
# 1. 构建
./build-all.sh

# 2. 运行
java -jar target/random-gallery-*.jar \
  --spring.datasource.url=jdbc:mysql://host:3306/db
```

---

## 📝 更新日志

### v2.0.0 (2026-01-15)
- ✅ 前后端完全分离
- ✅ 使用 Next.js 15 + React 19 重构前端
- ✅ 移除 Freemarker 模板引擎
- ✅ 新增深色模式支持
- ✅ 新增 PWA 支持
- ✅ 全面移动端优化
- ✅ 瀑布流布局优化
- ✅ 统一构建脚本

### v1.0.0
- 基于 Spring Boot + Freemarker 的传统架构

---

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

---

## 📄 许可证

本项目采用 MIT 许可证

---

## ⭐ Star History

如果这个项目对您有帮助，请给它一个星标！

---

## 📞 联系方式

如有问题或建议，请通过以下方式联系：
- 提交 Issue
- 发送邮件
- 项目讨论区