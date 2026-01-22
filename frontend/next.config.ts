import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // 静态导出配置 - 开发环境暂时禁用，避免动态路由问题
  // 生产环境如需静态导出，需要为动态路由添加 generateStaticParams
  // output: 'export',

  // 输出目录（生产构建时输出到后端 static 目录）
  distDir: process.env.NODE_ENV === 'production'
    ? '../src/main/resources/static'
    : '.next',

  // 图片优化配置（静态导出需要禁用）
  images: {
    unoptimized: true,
  },

  // 路径配置
  basePath: '',
  trailingSlash: true,
};

export default nextConfig;
