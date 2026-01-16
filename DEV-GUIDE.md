# Random Gallery 开发环境快速启动指南

## 🚀 一键启动

### 启动服务
```bash
./dev-start.sh
```

这个脚本会自动：
1. 检查并安装前端依赖
2. 启动后端 Spring Boot 服务 (端口 8086)
3. 启动前端 Next.js 开发服务器 (端口 3000)
4. 显示服务状态和访问地址

### 停止服务

**方式一：按 Ctrl+C**
在运行 `dev-start.sh` 的终端按 `Ctrl+C`，会自动停止所有服务。

**方式二：使用停止脚本**
```bash
./dev-stop.sh
```

## 📝 日志查看

开发服务器的日志会输出到两个文件：

```bash
# 查看后端日志
tail -f backend.log

# 查看前端日志
tail -f frontend.log
```

## 🔧 常见问题

### 端口被占用

如果提示端口 3000 或 8086 被占用：

```bash
# 查找占用端口的进程
lsof -i :3000
lsof -i :8086

# 杀死进程
kill -9 <PID>

# 或使用停止脚本
./dev-stop.sh
```

### Maven 依赖问题

```bash
# 清理并重新下载依赖
mvn clean install
```

### Node.js 依赖问题

```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
cd ..
```

### 数据库连接失败

检查 `src/main/resources/db.yaml` 配置：
```yaml
db:
  host: localhost
  port: 3306
  name: your_database
  username: your_username
  password: your_password
```

## 🎯 开发流程

1. **启动服务**
   ```bash
   ./dev-start.sh
   ```

2. **访问前端**
   打开浏览器访问 http://localhost:3000

3. **开发**
   - 修改前端代码会自动热重载
   - 修改后端代码需要手动重启后端服务

4. **停止服务**
   ```bash
   # 按 Ctrl+C 或运行
   ./dev-stop.sh
   ```

## 📊 服务地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | http://localhost:3000 | Next.js 开发服务器 |
| 后端 | http://localhost:8086 | Spring Boot API |
| API测试 | http://localhost:8086/api/system/env/currentInfo | 测试API是否正常 |

## 💡 开发提示

- **热重载**: 前端修改自动刷新，无需重启
- **API代理**: 前端的 `/api/*` 请求会自动代理到后端
- **环境切换**: 左上角下拉菜单可切换数据库环境
- **主题切换**: 右上角按钮切换深色/浅色模式
- **PWA测试**: 在 Chrome 开发者工具的 Application 标签查看

## 🔍 调试技巧

### 前端调试
```bash
# 查看前端实时日志
tail -f frontend.log

# Chrome DevTools
# F12 -> Console/Network 查看请求
```

### 后端调试
```bash
# 查看后端实时日志
tail -f backend.log

# 或查看 Spring Boot 日志
tail -f logs/app.log
```

### API测试

```bash
# 测试环境信息接口
curl http://localhost:8086/api/system/env/currentInfo

# 测试随机图片接口
curl http://localhost:8086/api/pic/random/one
```

## 🎨 开发建议

1. **优先使用一键启动脚本** - 简单方便
2. **及时查看日志** - 发现问题更快
3. **善用热重载** - 提升开发效率
4. **API优先设计** - 前后端并行开发

## 📦 生产构建

开发完成后，使用构建脚本打包：

```bash
./build-all.sh
```

会生成：
- `target/random-gallery-*.jar` - 完整的应用包
- `random-gallery-*.tar.gz` - Docker 镜像

## ✨ 下一步

- 查看 [README.md](README.md) 了解完整功能
- 查看 [walkthrough.md](walkthrough.md) 了解项目架构
- 开始愉快的开发吧！ 🚀
