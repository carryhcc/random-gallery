# random-gallery

基于 Spring Boot 3 + MyBatis-Plus + Freemarker 的随机图库项目，支持随机图片/分组浏览、小红书作品管理、HEIC 转换和多环境数据切换。

## 1. 功能概览

- 随机单图：随机返回一张图片信息。
- 分组浏览：随机分组、分组分页加载、按分组查询图片。
- 作品管理：支持小红书作品列表筛选、详情、删除、下载任务入库。
- GIF 浏览：随机 GIF、按 ID 获取 GIF。
- HEIC 处理：自动识别 HEIC/HEIF 并通过转换接口输出 JPEG。
- 运行时能力：环境切换（dev/test/prod）、隐私模式开关、防重复提交、API 日志切面。

## 2. 技术栈

- Java 21
- Spring Boot 3.4.2
- MyBatis-Plus 3.5.16
- MySQL（通过 `db.yaml` 配置）
- Freemarker
- Caffeine Cache
- Maven
- Docker

## 3. 本地运行

### 3.1 环境要求

- JDK 21+
- Maven 3.9+
- 可访问的 MySQL

### 3.2 配置数据库

项目通过 `src/main/resources/application.yml` 引入 `db.yaml`：

```yaml
db:
  host: localhost
  port: 3306
  name: your_database
  username: your_username
  password: your_password
```

推荐方式：直接通过环境变量覆盖默认值。

### 3.3 启动项目

```bash
mvn spring-boot:run
```

默认端口：`8086`

启动后可访问：

- 首页：[http://127.0.0.1:8086](http://127.0.0.1:8086)

## 4. Docker 部署

### 4.1 本地构建并运行

```bash
mvn clean package -DskipTests
docker build -t random-gallery:main .
docker run -p 8086:8086 random-gallery:main
```

### 4.2 传入数据库环境变量

```bash
docker run -p 8086:8086 \
  -e DB_HOST=your_host \
  -e DB_PORT=3306 \
  -e DB_NAME=your_db \
  -e DB_USERNAME=your_user \
  -e DB_PASSWORD=your_password \
  random-gallery:main
```

### 4.3 从 GHCR 拉取并运行（固定标签）

```bash
docker pull ghcr.io/<your-github-owner>/random-gallery:main
docker run -p 8086:8086 ghcr.io/<your-github-owner>/random-gallery:main
```

### 4.4 一键构建并导出镜像包

```bash
./build-and-export.sh
```

脚本会生成：`random-gallery.tar`

## 5. 配置说明

### 5.1 关键应用配置

- `server.port`：默认 `8086`
- `config.env`：默认环境（`dev`）
- `image.safe-mode.enabled`：隐私模式默认开启
- `image.safe-mode.placeholder-url`：隐私模式占位图（默认 `/icons/404.svg`）

### 5.2 外部服务配置

以下能力依赖外部 HTTP 服务：

- 下载解析服务：`other.downloader.url`（默认 `http://${db.host}:5556/xhs/detail`）
- HEIC 转换服务：`other.imaginary.url`（默认 `http://${db.host}:6363/convert?...`）

如果不部署这两个服务，对应下载/HEIC 转换功能会不可用。

### 5.3 多环境数据切换

运行中可通过接口切换环境：`dev` / `test` / `prod`。

项目内部按下列表名规则读取数据：

- 图片表：`cc_pic_all_{env}`
- 分组表：`cc_pic_group_{env}`

## 6. 主要页面路由

- `/`：静态首页
- `/showPic`：随机单图页面
- `/showPicList`：随机套图页面
- `/groupList`：分组列表页
- `/randomGallery`：作品画廊页
- `/download`：下载管理页
- `/downloadDetail?workId=...`：作品详情页
- `/downloadList`：下载列表页
- `/randomGif`：随机 GIF 页面

## 7. 主要 API

统一返回结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1730000000000
}
```

### 7.1 图片与分组

- `GET /api/pic/random/one`：随机单图
- `POST /api/pic/list`：按条件查询图片（`PicQry`）
- `GET /api/pic/download?groupId=1`：下载分组图片 ZIP
- `GET /api/group/randomGroupInfo`：随机分组信息（可传 `groupId`）
- `POST /api/group/list`：分组分页查询（`GroupQry`）
- `GET /api/group/loadMore?page=0&refresh=false`：前端加载更多分组

### 7.2 XHS作品

- `POST /api/xhsWork/download`：新增下载任务
- `GET /api/xhsWork/list`：作品分页/筛选
- `GET /api/xhsWork/detail/{workId}`：作品详情
- `DELETE /api/xhsWork/{workId}`：删除作品（软删）
- `DELETE /api/xhsWork/media/{id}`：删除媒体（软删）
- `GET /api/xhsWork/authors`：作者列表
- `GET /api/xhsWork/tags`：标签列表
- `GET /api/xhsWork/randomGif`：随机 GIF
- `GET /api/xhsWork/allGifIds`：全部 GIF ID
- `GET /api/xhsWork/gifById/{id}`：按 ID 获取 GIF
- `POST /api/xhsWork/migrate`：执行历史数据迁移

### 7.3 系统与图像转换

- `GET /api/system/privacy-mode`：查询/设置隐私模式（`enabled=true|false`）
- `GET /api/system/env/current`：当前环境
- `GET /api/system/env/currentInfo`：当前环境统计
- `GET /api/system/env/switch?env=dev|test|prod`：切换环境
- `GET /api/system/env/dev`：切到 dev
- `GET /api/system/env/test`：切到 test
- `GET /api/system/env/prod`：切到 prod
- `GET /api/system/up/group`：刷新分组统计
- `GET /api/image/convert-heic?url=...`：HEIC 转 JPEG

## 8. 项目结构

```text
src
├── main
│   ├── java/com/example/randomGallery
│   │   ├── controller      # Web/REST 入口
│   │   ├── service         # 业务服务
│   │   ├── service/mapper  # MyBatis Mapper 接口
│   │   ├── entity          # DO/VO/QO/通用分页实体
│   │   ├── config          # Spring、AOP、缓存等配置
│   │   ├── runner          # 启动任务
│   │   └── utils           # 工具类
│   └── resources
│       ├── mapper          # MyBatis XML
│       ├── templates       # Freemarker 页面
│       └── static          # 静态资源
└── test
```

## 9. 常用命令

```bash
mvn test
mvn clean package
mvn clean package -DskipTests
```

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
