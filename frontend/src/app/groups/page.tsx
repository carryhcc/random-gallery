'use client';

import { useQuery } from '@tanstack/react-query';
import { groupApi, systemApi } from '@/lib/api';
import { useState, useEffect } from 'react';
import { ChevronLeft, Loader2, Search, ChevronRight } from 'lucide-react';
import { HeicSafeImage } from '@/components/heic-safe-image';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

export default function GroupsPage() {
    const router = useRouter();
    const [page, setPage] = useState(1);
    const [searchTerm, setSearchTerm] = useState('');
    const [debouncedSearchTerm, setDebouncedSearchTerm] = useState('');
    const pageSize = 5;

    // 防抖处理搜索词
    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedSearchTerm(searchTerm);
            setPage(1); // 搜索条件变化时重置页码
        }, 500);
        return () => clearTimeout(timer);
    }, [searchTerm]);

    // 获取当前环境信息，用于依赖更新
    const { data: envInfo } = useQuery<{ env: string }>({
        queryKey: ['env-info'],
        queryFn: async () => {
            const response = await systemApi.getCurrentEnvInfo();
            return response.data;
        },
        staleTime: Infinity,
    });

    // 获取分组列表
    const { data, isLoading } = useQuery({
        // 将 envInfo?.env 加入依赖，确保环境切换时重新请求
        queryKey: ['groups-list', page, pageSize, debouncedSearchTerm, envInfo?.env],
        queryFn: async () => {
            console.log('Fetching groups:', { page, pageSize, groupName: debouncedSearchTerm, env: envInfo?.env });
            const response = await groupApi.getGroupList({
                page,
                pageSize,
                groupName: debouncedSearchTerm || undefined,
            });
            console.log('Groups response:', response);
            return response.data;
        },
        // 只有当环境信息加载完成后才启用查询（可选，视需求而定，这里保持总是启用但依赖 env）
        enabled: true,
    });

    const groups = data?.list || [];
    const total = data?.total || 0;
    const totalPages = Math.ceil(total / pageSize);

    console.log('Render state:', { page, pageSize, total, totalPages, groupsCount: groups.length });

    const handleGroupClick = (group: any) => {
        router.push(`/pic-list?groupId=${group.groupId}&groupName=${encodeURIComponent(group.groupName || '套图')}`);
    };

    return (
        <div className="min-h-screen bg-zinc-50 dark:bg-zinc-950">
            {/* 顶部导航 */}
            <div className="sticky top-0 z-40 bg-white/90 dark:bg-zinc-900/90 backdrop-blur-md border-b border-zinc-200 dark:border-zinc-800">
                <div className="container mx-auto px-4 py-3.5">
                    <div className="flex items-center justify-between mb-3">
                        <Link
                            href="/"
                            className="flex items-center gap-2 text-zinc-900 dark:text-zinc-100 hover:text-blue-600 dark:hover:text-blue-400 transition-colors active:scale-95"
                        >
                            <ChevronLeft className="h-5 w-5" />
                            <span className="font-medium">返回</span>
                        </Link>
                        <h1 className="text-lg font-bold text-zinc-900 dark:text-zinc-100">分组列表</h1>
                        <div className="w-16" />
                    </div>

                    {/* 搜索框 */}
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4.5 w-4.5 text-zinc-400" />
                        <input
                            type="text"
                            placeholder="搜索分组..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="
                                w-full pl-10 pr-4 py-2.5
                                rounded-xl
                                bg-white dark:bg-zinc-900
                                border border-zinc-200 dark:border-zinc-800
                                text-zinc-900 dark:text-zinc-100
                                placeholder:text-zinc-400
                                focus:outline-none focus:ring-2 focus:ring-blue-500
                                text-sm
                            "
                        />
                    </div>
                </div>
            </div>

            {/* 内容区域 */}
            <div className="container mx-auto px-4 py-4 max-w-2xl">
                {isLoading ? (
                    <div className="flex items-center justify-center py-20">
                        <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
                    </div>
                ) : groups.length > 0 ? (
                    <>
                        {/* 分组列表 - 移动端单列 */}
                        <div className="space-y-3 mb-6">
                            {groups.map((group: any, index: number) => (
                                <button
                                    key={group.groupId}
                                    onClick={() => handleGroupClick(group)}
                                    className="
                                        group relative w-full
                                        rounded-xl overflow-hidden
                                        bg-white dark:bg-zinc-900
                                        border border-zinc-200 dark:border-zinc-800
                                        p-4
                                        text-left
                                        transition-all duration-300
                                        hover:shadow-lg hover:scale-[1.02]
                                        active:scale-[0.98]
                                        flex items-center gap-3.5
                                        min-h-[76px]
                                        animate-in fade-in-0 slide-in-from-bottom-2
                                    "
                                    style={{
                                        animationDelay: `${index * 20}ms`,
                                        animationFillMode: 'backwards',
                                    }}
                                >
                                    {/* 渐变背景 */}
                                    <div className="
                                        absolute inset-0 opacity-0 group-hover:opacity-5
                                        bg-gradient-to-br from-blue-500 to-purple-600
                                        transition-opacity duration-300
                                    " />

                                    {/* 封面图 */}
                                    <div className="relative flex-shrink-0 w-20 h-20 rounded-lg overflow-hidden bg-zinc-100 dark:bg-zinc-800 border border-zinc-200 dark:border-zinc-700">
                                        <HeicSafeImage
                                            src={group.groupUrl}
                                            alt={group.groupName || 'Group cover'}
                                            fill
                                            className="object-cover"
                                            unoptimized
                                            loading="lazy"
                                        />
                                    </div>

                                    {/* 内容 */}
                                    <div className="flex-1 min-w-0 py-1">
                                        <h3 className="font-semibold text-zinc-900 dark:text-zinc-100 mb-1.5 line-clamp-1 text-base">
                                            {group.groupName || `分组 ${group.groupId}`}
                                        </h3>
                                        <div className="flex items-center gap-3 text-sm text-zinc-500 dark:text-zinc-400">
                                            <span>ID: {group.groupId}</span>
                                            {group.groupCount !== undefined && (
                                                <>
                                                    <span className="w-1 h-1 rounded-full bg-zinc-300 dark:bg-zinc-600" />
                                                    <span>{group.groupCount} 张</span>
                                                </>
                                            )}
                                        </div>
                                    </div>

                                    {/* 箭头 */}
                                    <ChevronRight className="flex-shrink-0 h-5 w-5 text-zinc-400 dark:text-zinc-600 transition-transform duration-300 group-hover:translate-x-1" />
                                </button>
                            ))}
                        </div>

                        {/* 分页 - 移动端优化 */}
                        {total > 0 && (
                            <div className="flex items-center justify-between gap-3 px-1">
                                <button
                                    onClick={() => setPage(p => Math.max(1, p - 1))}
                                    disabled={page === 1}
                                    className="
                                        flex-1 px-4 py-3 rounded-xl
                                        bg-white dark:bg-zinc-900
                                        border border-zinc-200 dark:border-zinc-800
                                        text-zinc-900 dark:text-zinc-100 font-medium
                                        disabled:opacity-40 disabled:cursor-not-allowed
                                        hover:bg-zinc-50 dark:hover:bg-zinc-800
                                        active:scale-95
                                        transition-all
                                    "
                                >
                                    上一页
                                </button>
                                <div className="px-4 py-3 text-sm text-zinc-600 dark:text-zinc-400 font-medium whitespace-nowrap">
                                    {page} / {totalPages}
                                </div>
                                <button
                                    onClick={() => setPage(p => Math.min(totalPages, p + 1))}
                                    disabled={page === totalPages}
                                    className="
                                        flex-1 px-4 py-3 rounded-xl
                                        bg-white dark:bg-zinc-900
                                        border border-zinc-200 dark:border-zinc-800
                                        text-zinc-900 dark:text-zinc-100 font-medium
                                        disabled:opacity-40 disabled:cursor-not-allowed
                                        hover:bg-zinc-50 dark:hover:bg-zinc-800
                                        active:scale-95
                                        transition-all
                                    "
                                >
                                    下一页
                                </button>
                            </div>
                        )}
                    </>
                ) : (
                    <div className="text-center py-20">
                        <p className="text-zinc-500 dark:text-zinc-400">
                            {searchTerm ? '没有找到匹配的分组' : '暂无分组数据'}
                        </p>
                    </div>
                )}
            </div>
        </div>
    );
}
