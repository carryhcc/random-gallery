'use client';

import { useQuery } from '@tanstack/react-query';
import { systemApi, groupApi } from '@/lib/api';
import Link from 'next/link';
import {
  Image as ImageIcon,
  Images,
  Film,
  Grid3x3,
  List,
  Download,
  Folder,
  FileImage,
  ChevronRight
} from 'lucide-react';
import { useRouter } from 'next/navigation';

import { EnvSelector } from '@/components/env-selector';
import { ThemeToggle } from '@/components/theme-toggle';

export default function Home() {
  const router = useRouter();

  // 获取环境统计信息
  const { data: envInfo, isLoading } = useQuery({
    queryKey: ['env-info'],
    // 统计信息可以复用 env-selector 中获取的缓存，这里主要是为了展示数量
    // 如果 env-selector 已经触发了 fetching，这里会直接用缓存
    staleTime: 30000,
    queryFn: async () => {
      const response = await systemApi.getCurrentEnvInfo();
      return response.data;
    },
  });

  // 处理随机套图点击
  const handleRandomGroup = async () => {
    try {
      const response = await groupApi.getRandomGroupInfo();
      if (response.code === 200 && response.data?.groupId) {
        router.push(`/pic-list?groupId=${response.data.groupId}&groupName=${encodeURIComponent(response.data.groupName || '随机套图')}`);
      } else {
        router.push('/pic-list/');
      }
    } catch (error) {
      console.error('获取随机分组失败:', error);
      router.push('/pic-list/');
    }
  };

  const menuItems = [
    {
      href: '/pic/',
      icon: ImageIcon,
      label: '随机图片',
      desc: '浏览随机单张图片',
      gradient: 'from-blue-500 to-cyan-500',
    },
    {
      href: '/gallery/',
      icon: Grid3x3,
      label: '随机画廊',
      desc: '瀑布流浏览模式',
      gradient: 'from-purple-500 to-pink-500',
    },
    {
      href: '/random-gif/',
      icon: Film,
      label: '随机动图',
      desc: '全屏滑动体验',
      gradient: 'from-orange-500 to-red-500',
    },
    {
      onClick: handleRandomGroup,
      icon: Images,
      label: '随机套图',
      desc: '随机获取一组图片',
      gradient: 'from-green-500 to-emerald-500',
    },
    {
      href: '/groups/',
      icon: List,
      label: '分组列表',
      desc: '查看所有分组',
      gradient: 'from-indigo-500 to-blue-500',
    },
    {
      href: '/download/',
      icon: Download,
      label: '图片下载',
      desc: '管理下载的作品',
      gradient: 'from-pink-500 to-rose-500',
    },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50 dark:from-zinc-950 dark:via-zinc-900 dark:to-zinc-950">
      <main className="container mx-auto px-4 py-4 max-w-2xl">
        {/* 顶部导航栏 */}
        <div className="flex items-center justify-between mb-8 px-1">
          <EnvSelector />
          <ThemeToggle />
        </div>

        {/* 标题区域 */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center gap-3 mb-3">
            <div className="p-2.5 rounded-2xl bg-gradient-to-br from-blue-500 to-purple-600 shadow-lg shadow-blue-500/30">
              <Images className="h-7 w-7 text-white" />
            </div>
            <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-400 dark:to-purple-400 bg-clip-text text-transparent">
              随机图库
            </h1>
          </div>

          {/* 环境统计信息 */}
          {isLoading ? (
            <div className="flex items-center justify-center gap-2 text-zinc-500 dark:text-zinc-400">
              <div className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-zinc-300 border-t-blue-600"></div>
              <span className="text-sm">加载中...</span>
            </div>
          ) : envInfo ? (
            <div className="flex items-center justify-center gap-5 text-sm text-zinc-600 dark:text-zinc-400">
              <div className="flex items-center gap-1.5">
                <Folder className="h-4 w-4 text-blue-500" />
                <span>{envInfo.groupCount || 0} 分组</span>
              </div>
              <div className="flex items-center gap-1.5">
                <FileImage className="h-4 w-4 text-purple-500" />
                <span>{envInfo.picCount || 0} 图片</span>
              </div>
            </div>
          ) : null}
        </div>

        {/* 菜单列表 - 移动端优化的单列大卡片 */}
        <div className="space-y-3">
          {menuItems.map((item, index) => {
            const Icon = item.icon;
            const Component = item.href ? Link : 'button';

            return (
              <Component
                key={item.label}
                href={item.href || '#'}
                onClick={item.onClick}
                className="
                  group relative w-full overflow-hidden
                  rounded-2xl
                  bg-white dark:bg-zinc-900
                  border border-zinc-200 dark:border-zinc-800
                  p-5
                  transition-all duration-300
                  hover:scale-[1.02] hover:shadow-lg
                  active:scale-[0.98]
                  flex items-center gap-4 text-left
                  min-h-[88px]
                  animate-in fade-in-0 slide-in-from-bottom-3
                "
                style={{
                  animationDelay: `${index * 60}ms`,
                  animationFillMode: 'backwards',
                }}
              >
                {/* 渐变背景装饰 */}
                <div className={`
                  absolute inset-0 opacity-0 group-hover:opacity-5 dark:group-hover:opacity-10
                  bg-gradient-to-br ${item.gradient}
                  transition-opacity duration-300
                `} />

                {/* 图标 */}
                <div className={`
                  relative flex-shrink-0
                  p-3.5 rounded-xl
                  bg-gradient-to-br ${item.gradient}
                  shadow-md
                  transition-transform duration-300
                  group-hover:scale-110
                `}>
                  <Icon className="h-7 w-7 text-white" />
                </div>

                {/* 文字内容 */}
                <div className="flex-1 min-w-0">
                  <h3 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100 mb-1">
                    {item.label}
                  </h3>
                  <p className="text-sm text-zinc-500 dark:text-zinc-400 truncate">
                    {item.desc}
                  </p>
                </div>

                {/* 箭头指示 */}
                <ChevronRight className="flex-shrink-0 h-5 w-5 text-zinc-400 dark:text-zinc-600 transition-transform duration-300 group-hover:translate-x-1" />

                {/* 光泽效果 */}
                <div className="
                  absolute inset-0 -translate-x-full
                  bg-gradient-to-r from-transparent via-white/10 to-transparent
                  group-hover:translate-x-full
                  transition-transform duration-700
                " />
              </Component>
            );
          })}
        </div>

        {/* 底部装饰 */}
        <div className="mt-8 text-center text-xs text-zinc-400 dark:text-zinc-500">
          <p>专业图片浏览平台</p>
        </div>
      </main>
    </div>
  );
}
