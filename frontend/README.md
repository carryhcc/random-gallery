# Random Gallery Frontend

基于 Next.js 15 + React 19 的现代化前端应用

## 🚀 快速开始

### 开发环境

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

访问 http://localhost:3000

### 生产构建

```bash
# 构建并输出到后端static目录
npm run build:prod
```

## 📦 技术栈

- **框架**: Next.js 15 (App Router)
- **UI库**: React 19
- **语言**: TypeScript
- **样式**: Tailwind CSS 4
- **状态管理**: Zustand + TanStack Query
- **HTTP**: Axios
- **图标**: Lucide React
- **主题**: next-themes
- **PWA**: @ducanh2912/next-pwa

## 📁 项目结构

```
src/
├── app/                    # 路由和页面
│   ├── layout.tsx         # 全局布局
│   ├── page.tsx           # 首页
│   ├── pic/               # 随机图片
│   └── ...
├── components/            # 组件
│   ├── providers/        # Context Providers
│   ├── ui/               # UI组件
│   └── ...
└── lib/                   # 工具库
    ├── api.ts            # API接口
    ├── api-client.ts     # Axios配置
    ├── types.ts          # TypeScript类型
    ├── store.ts          # 状态管理
    └── utils.ts          # 工具函数
```

## 🎨 已完成页面

- [x] 首页
- [x] 随机图片
- [ ] 随机画廊 
- [ ] 套图列表
- [ ] 分组列表
- [ ] 随机动图
- [ ] 下载管理
- [ ] 作品详情

## 🔧 环境变量

### 开发环境 (.env.local)
```
NEXT_PUBLIC_API_URL=http://localhost:8086/api
```

### 生产环境 (.env.production)
```
NEXT_PUBLIC_API_URL=/api
```

## 📝 开发注意事项

1. 所有API调用使用 `lib/api.ts` 中的接口
2. 使用 TanStack Query 管理服务端状态
3. 组件优先使用 Tailwind CSS
4. 移动端优先设计
5. 支持深色模式

## 🚢 部署

生产构建会自动输出到 `../backend/src/main/resources/static` 目录，与后端一起打包部署。
