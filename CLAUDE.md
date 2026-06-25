# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此代码仓库中工作时提供指导。

## 构建和运行命令

### 开发环境
```bash
mvn spring-boot:run                    # 本地运行应用
mvn clean package                      # 构建 JAR
mvn test                              # 运行测试
mvn clean package -DskipTests          # 跳过测试构建（生产环境）
```

### Docker 部署
```bash
./build-and-export.sh                 # 跨平台构建（推荐）
docker build -t helloworld:latest .
docker run -p 8086:8086 helloworld:latest
```

使用自定义数据库配置：
```bash
docker run -p 8086:8086 -e DB_HOST=host -e DB_USERNAME=user -e DB_PASSWORD=pass helloworld:latest
```

## 架构概览

### 技术栈
- Java 21 + Spring Boot 3.4.2
- Maven 3.9+ 构建系统
- MySQL 8.0+ + MyBatis-Plus 3.5.16
- Freemarker 模板引擎
- Caffeine 缓存
- Docker + Alpine Linux

### 分层架构
```
controller/    - REST API 端点 (@RestController)
server/        - 服务层（业务逻辑）
entity/        - JPA 实体和值对象 (VOs)
mapper/        - MyBatis XML 映射器（数据库查询）
config/        - Spring 配置类
utils/         - 工具类
```

### 配置管理

**两级配置系统：**
1. `application.yml` - Spring Boot 主配置，包含 `spring.config.import: classpath:db.yaml`
2. `db.yaml` - 外部数据库配置，支持环境变量默认值

**环境变量优先级 (db.yaml)：**
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- 未设置时回退到 db.yaml 中的默认值

**运行时环境切换：**
- API 端点：`/api/system/env/{env}` (dev/test/prod)
- 动态更新应用配置

### 数据库 URL 模板
```
jdbc:mysql://${db.host}:${db.port}/${db.name}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
```

### 缓存策略
- `CacheService` 中的 Caffeine 缓存
- 缓存随机 ID 和分组 ID 以提升性能
- 通过定时器机制自动刷新缓存

### AOP 切面
应用使用 AspectJ 处理横切关注点：
- API 日志记录
- 访问追踪
- 表单提交防护
- 安全编码 (OWASP)

### 响应格式
所有 API 响应使用 `Result<T>` 包装类，保证格式统一。

## 核心 API 端点

- `/api/pic/random` - 获取随机单张图片
- `/api/pic/group` - 获取随机图片组
- `/api/pic/group/paged` - 分页获取图片组
- `/api/group/list` - 查询分组列表
- `/api/group/list/paged` - 分页查询分组列表
- `/api/system/env/current` - 获取当前环境
- `/api/system/env/{env}` - 切换环境

## 外部服务配置

应用集成了 `application.yml` 中配置的外部服务：
- **下载服务**: `http://${db.host}:5556/xhs/detail`
- **图片处理服务**: `http://${db.host}:6363/convert?type=jpeg&quality=90`

这些服务运行在与数据库相同的主机上 (`db.host`)。

## 图片安全模式

当 `image.safe-mode.enabled: true`（默认）时，图片加载失败会显示 `/icons/404.svg` 而非损坏图片。

## 代码质量

- 已配置 Qodana 进行静态分析 (`qodana.yaml`)
- maven-compiler-plugin 中配置了 Lombok 注解处理器
- MyBatis-Plus 通过 Slf4j 记录带参数的 SQL

## 测试部署

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **random-gallery** (2091 symbols, 3940 relationships, 165 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> Index stale? Run `node .gitnexus/run.cjs analyze` from the project root — it auto-selects an available runner. No `.gitnexus/run.cjs` yet? `npx gitnexus analyze` (npm 11 crash → `npm i -g gitnexus`; #1939).

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows. For regression review, compare against the default branch: `detect_changes({scope: "compare", base_ref: "main"})`.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `rename` which understands the call graph.
- NEVER commit changes without running `detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/random-gallery/context` | Codebase overview, check index freshness |
| `gitnexus://repo/random-gallery/clusters` | All functional areas |
| `gitnexus://repo/random-gallery/processes` | All execution flows |
| `gitnexus://repo/random-gallery/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
