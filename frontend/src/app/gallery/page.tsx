'use client';

import { useQuery } from '@tanstack/react-query';
import { groupApi } from '@/lib/api';
import { useState, useEffect, useRef, useCallback } from 'react';
import { ChevronLeft, Loader2, Images } from 'lucide-react';
import { HeicSafeImage } from '@/components/heic-safe-image';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

export default function GalleryPage() {
    const router = useRouter();
    const [groups, setGroups] = useState<any[]>([]);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const observerRef = useRef<IntersectionObserver | null>(null);
    const loadMoreRef = useRef<HTMLDivElement>(null);
    const isLoadingRef = useRef(false);

    // 使用原始的 loadMore API
    const { data, isLoading, isFetching } = useQuery({
        queryKey: ['gallery-groups', page],
        queryFn: async () => {
            const response = await groupApi.loadMore(page, page === 0);
            return response;
        },
        enabled: hasMore && page >= 0,
        staleTime: 30000, // 30秒内不重复请求
    });

    useEffect(() => {
        if (data && data.code === 200 && data.data) {
            const images = data.data.images || [];
            const hasMoreFromApi = data.data.hasMore;

            if (images.length > 0) {
                setGroups(prev => {
                    // 去重：使用 Set 来确保不添加重复的分组
                    const newGroups = images.filter((newGroup: any) =>
                        !prev.some(existingGroup => existingGroup.groupId === newGroup.groupId)
                    );
                    return [...prev, ...newGroups];
                });
            }

            // 使用 API 返回的 hasMore 字段
            setHasMore(hasMoreFromApi !== false);
            isLoadingRef.current = false;
        }
    }, [data]);

    // 加载更多的回调
    const loadMore = useCallback(() => {
        if (!isLoadingRef.current && !isFetching && hasMore) {
            isLoadingRef.current = true;
            setPage(p => p + 1);
        }
    }, [isFetching, hasMore]);

    // 无限滚动
    useEffect(() => {
        if (observerRef.current) {
            observerRef.current.disconnect();
        }

        if (!hasMore) return;

        observerRef.current = new IntersectionObserver(
            (entries) => {
                if (entries[0].isIntersecting) {
                    loadMore();
                }
            },
            {
                threshold: 0.1,
                rootMargin: '100px' // 提前100px开始加载
            }
        );

        if (loadMoreRef.current) {
            observerRef.current.observe(loadMoreRef.current);
        }

        return () => {
            if (observerRef.current) {
                observerRef.current.disconnect();
            }
        };
    }, [hasMore, loadMore]);

    const handleGroupClick = (group: any) => {
        router.push(`/pic-list?groupId=${group.groupId}&groupName=${encodeURIComponent(group.groupName || '分组')}`);
    };

    return (
        <div className="min-h-screen bg-zinc-50 dark:bg-zinc-950">
            {/* 顶部导航 */}
            <div className="sticky top-0 z-40 bg-white/90 dark:bg-zinc-900/90 backdrop-blur-md border-b border-zinc-200 dark:border-zinc-800">
                <div className="container mx-auto px-4 py-3.5 flex items-center justify-between">
                    <Link
                        href="/"
                        className="flex items-center gap-2 text-zinc-900 dark:text-zinc-100 hover:text-blue-600 dark:hover:text-blue-400 transition-colors active:scale-95"
                    >
                        <ChevronLeft className="h-5 w-5" />
                        <span className="font-medium">返回</span>
                    </Link>
                    <h1 className="text-lg font-bold text-zinc-900 dark:text-zinc-100">随机画廊</h1>
                    <div className="w-16" />
                </div>
            </div>

            {/* 分组卡片列表 - 移动端2列网格 */}
            <div className="container mx-auto px-3 py-4 max-w-2xl">
                {isLoading && page === 0 ? (
                    <div className="flex items-center justify-center py-20">
                        <div className="text-center">
                            <Loader2 className="h-8 w-8 animate-spin text-blue-600 mx-auto mb-3" />
                            <p className="text-zinc-600 dark:text-zinc-400 text-sm">加载中...</p>
                        </div>
                    </div>
                ) : groups.length === 0 ? (
                    <div className="text-center py-20">
                        <p className="text-zinc-500 dark:text-zinc-400">暂无分组</p>
                    </div>
                ) : (
                    <div className="grid grid-cols-2 gap-3">
                        {groups.map((group, index) => (
                            <button
                                key={group.groupId}
                                onClick={() => handleGroupClick(group)}
                                className="
                                    group relative
                                    rounded-xl overflow-hidden
                                    transition-all duration-300
                                    hover:shadow-lg active:scale-[0.98]
                                    animate-in fade-in-0 zoom-in-95
                                "
                                style={{
                                    animationDelay: `${(index % 10) * 30}ms`,
                                    animationFillMode: 'backwards',
                                }}
                            >
                                {/* 图片区域 */}
                                <div className="relative aspect-[4/3] bg-zinc-100 dark:bg-zinc-800">
                                    <HeicSafeImage
                                        src={group.groupUrl}
                                        alt={group.groupName || `分组 ${group.groupId}`}
                                        fill
                                        className="object-cover transition-transform duration-300 group-hover:scale-105"
                                        unoptimized
                                        loading="lazy"
                                    />
                                    {/* 渐变遮罩 */}
                                    <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent" />

                                    {/* 图片数量标签 (右上角) */}
                                    <div className="absolute top-2 right-2 px-2.5 py-1 rounded-lg bg-white/90 dark:bg-zinc-900/90 backdrop-blur-sm flex items-center gap-1.5 shadow-sm">
                                        <Images className="h-3.5 w-3.5 text-blue-600 dark:text-blue-400" />
                                        <span className="text-xs font-semibold text-zinc-900 dark:text-zinc-100">
                                            {group.groupCount}
                                        </span>
                                    </div>

                                    {/* 左下角信息 (仅分组名称) */}
                                    <div className="absolute bottom-0 left-0 right-0 p-3">
                                        <h3 className="font-semibold text-white line-clamp-1 text-sm drop-shadow-lg">
                                            {group.groupName || `分组 ${group.groupId}`}
                                        </h3>
                                    </div>
                                </div>
                            </button>
                        ))}
                    </div>
                )}

                {/* 加载更多触发器 */}
                <div ref={loadMoreRef} className="py-8 flex justify-center">
                    {isFetching && hasMore && (
                        <div className="flex items-center gap-2 text-zinc-500">
                            <Loader2 className="h-5 w-5 animate-spin" />
                            <span className="text-sm">加载更多...</span>
                        </div>
                    )}
                    {!hasMore && groups.length > 0 && (
                        <div className="text-zinc-400 text-sm">
                            没有更多了
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
