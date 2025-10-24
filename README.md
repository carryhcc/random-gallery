# 随机图库 (Random Gallery)

一个基于 Spring Boot 的随机图片展示应用，支持多环境切换、分组管理和图片浏览功能。

## 🌟 功能特性

### 核心功能
- **随机图片展示** - 获取随机单张图片
- **随机套图浏览** - 获取随机套图集合
- **分组管理** - 支持图片分组查询和管理
- **分页浏览** - 支持分页加载，提升用户体验
- **多环境支持** - 支持开发、测试、生产环境切换

### 技术特性
- **现代化UI** - 响应式设计，支持桌面端和移动端
- **环境切换** - 运行时动态切换数据库环境
- **缓存优化** - 内置缓存机制，提升访问性能
- **Docker支持** - 完整的容器化部署方案
- **RESTful API** - 标准化的API接口设计

## 🚀 快速开始

### 环境要求
- Java 17+
- Maven 3.6+
- MySQL 5.7+
- Docker (可选)

### 本地开发

1. **克隆项目**
```bash
git clone <repository-url>
cd random-gallery
```

2. **配置数据库**
编辑 `src/main/resources/db.yaml` 文件：
```yaml
db:
  host: localhost
  port: 3306
  name: your_database
  username: your_username
  password: your_password
```

3. **运行应用**
```bash
mvn spring-boot:run
```

应用将在 `http://localhost:8086` 启动

### Docker 部署

#### 方式一：使用构建脚本（推荐）
```bash
# 一键构建和导出
./build-and-export.sh
```

#### 方式二：手动构建
```bash
# 1. Maven打包
mvn clean package -DskipTests

# 2. 构建Docker镜像
docker build -t helloworld:latest .

# 3. 运行容器
docker run -p 8086:8086 helloworld:latest
```

#### 自定义数据库配置
```bash
docker run -p 8086:8086 \
  -e DB_HOST=your_host \
  -e DB_USERNAME=your_user \
  -e DB_PASSWORD=your_pass \
  helloworld:latest
```

## 📖 API 文档

### 图片相关接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/pic/random` | GET | 获取随机单张图片 |
| `/api/pic/group` | GET | 获取随机套图 |
| `/api/pic/group/paged` | GET | 分页获取套图 |

### 分组管理接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/group/list` | POST | 查询分组列表 |
| `/api/group/count` | POST | 查询分组总数 |
| `/api/group/list/paged` | POST | 分页查询分组列表 |

### 系统管理接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/system/env/current` | GET | 获取当前环境 |
| `/api/system/env/dev` | GET | 切换到开发环境 |
| `/api/system/env/test` | GET | 切换到测试环境 |
| `/api/system/env/prod` | GET | 切换到生产环境 |

### 页面路由

| 路由 | 描述 |
|------|------|
| `/` | 首页 |
| `/showPic` | 随机图片页面 |
| `/showPicList` | 图片列表页面 |
| `/groupList` | 分组列表页面 |

## 🏗️ 项目结构

```
src/
├── main/
│   ├── java/com/example/randomGallery/
│   │   ├── controller/          # 控制器层
│   │   ├── entity/             # 实体类
│   │   ├── server/             # 服务层
│   │   ├── utils/              # 工具类
│   │   └── config/             # 配置类
│   └── resources/
│       ├── static/             # 静态资源
│       ├── templates/          # 模板文件
│       └── mapper/             # MyBatis映射文件
└── test/                       # 测试代码
```

## ⚙️ 配置说明

### 应用配置 (`application.yml`)
```yaml
server:
  port: 8086

spring:
  application:
    name: random-gallery
  datasource:
    url: jdbc:mysql://${db.host}:${db.port}/${db.name}?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false
    username: ${db.username}
    password: ${db.password}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 数据库配置 (`db.yaml`)
```yaml
db:
  host: 111.111.111.111
  port: 3306
  name: test
  username: root
  password: root
```

## 🔧 开发指南

### 环境切换功能
应用支持运行时动态切换数据库环境：
- 通过Web界面右上角的环境切换器
- 通过API接口 `/api/system/env/{env}`

### 缓存机制
- 内置 `CacheService` 提供缓存功能
- 支持随机ID和分组ID的缓存
- 定时器机制自动刷新缓存

### 数据库设计
应用使用MySQL数据库，主要表结构：
- 图片信息表：存储图片URL和基本信息
- 分组表：存储图片分组信息
- 支持多环境数据库配置

## 📦 部署说明

### 生产环境部署
1. 确保数据库连接配置正确
2. 使用构建脚本生成Docker镜像
3. 配置环境变量进行数据库连接
4. 使用反向代理（如Nginx）进行负载均衡

### 监控和日志
- 应用日志输出到 `logs/app.log`
- 支持不同级别的日志配置
- 建议配置日志轮转和监控告警

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系方式

如有问题或建议，请通过以下方式联系：
- 提交 Issue
- 发送邮件
- 项目讨论区

---

⭐ 如果这个项目对您有帮助，请给它一个星标！