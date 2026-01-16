'use client';

import { useQuery, useInfiniteQuery } from '@tanstack/react-query';
import { pictureApi, groupApi } from '@/lib/api';
import { useSearchParams, useRouter } from 'next/navigation';
import { ChevronLeft, Loader2, X } from 'lucide-react';
import { HeicSafeImage } from '@/components/heic-safe-image';
import Link from 'next/link';
import { useState, Suspense, useEffect } from 'react';
import { useInView } from 'react-intersection-observer';

function PicListContent() {
    const searchParams = useSearchParams();
    const router = useRouter(); // Use useRouter for navigation
    const groupId = searchParams.get('groupId');
    const [selectedImage, setSelectedImage] = useState<string | null>(null);
    const { ref, inView } = useInView();

    // 1. 获取分组详情
    const { data: groupInfo } = useQuery({
        queryKey: ['group-info', groupId],
        queryFn: async () => {
            if (!groupId) return null;
            const response = await groupApi.getRandomGroupInfo(parseInt(groupId));
            return response.data;
        },
        enabled: !!groupId,
    });

    // 2. 无限加载图片列表
    // 默认每页 6 条
    const PAGE_SIZE = 6;
    const {
        data,
        fetchNextPage,
        hasNextPage,
        isFetchingNextPage,
        status,
    } = useInfiniteQuery({
        queryKey: ['pic-list-infinite', groupId],
        queryFn: async ({ pageParam = 1 }) => {
            console.log('Fetching pics page:', pageParam);
            const response = await pictureApi.getPictureList({
                groupId: groupId ? parseInt(groupId) : undefined,
                pageIndex: pageParam as number,
                pageSize: PAGE_SIZE,
            });
            return response.data || [];
        },
        initialPageParam: 1,
        getNextPageParam: (lastPage, allPages) => {
            // 如果最后一页的数据少于 PAGE_SIZE，说明没有更多数据了
            if (!lastPage || lastPage.length < PAGE_SIZE) return undefined;

            // 检查是否达到总数 (如果有 groupInfo)
            const currentCount = allPages.reduce((acc, page) => acc + page.length, 0);
            if (groupInfo?.groupCount && currentCount >= groupInfo.groupCount) return undefined;

            return allPages.length + 1;
        },
        enabled: !!groupId,
    });

    // 监听滚动到底部
    useEffect(() => {
        if (inView && hasNextPage) {
            fetchNextPage();
        }
    }, [inView, fetchNextPage, hasNextPage]);

    // 计算已加载数量
    const allPictures = data?.pages.flatMap((page) => page) || [];
    const loadedCount = allPictures.length;

    if (!groupId) {
        return (
            <div className="min-h-screen bg-zinc-50 dark:bg-zinc-950 flex items-center justify-center p-4">
                <div className="text-center">
                    <p className="text-zinc-600 dark:text-zinc-400 mb-4">请选择一个分组</p>
                    <Link
                        href="/"
                        className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-blue-600 to-purple-600 text-white font-medium hover:shadow-lg transition-all active:scale-95"
                    >
                        <ChevronLeft className="h-4 w-4" />
                        返回首页
                    </Link>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-zinc-50 dark:bg-zinc-950">
            {/* 顶部导航 - 固定 */}
            <div className="sticky top-0 z-40 bg-white/90 dark:bg-zinc-900/90 backdrop-blur-md border-b border-zinc-200 dark:border-zinc-800">
                <div className="container mx-auto px-4 py-3.5">
                    <div className="flex items-center justify-between mb-2">
                        <button
                            onClick={() => router.back()}
                            className="flex items-center gap-2 text-zinc-900 dark:text-zinc-100 hover:text-blue-600 dark:hover:text-blue-400 transition-colors active:scale-95"
                        >
                            <ChevronLeft className="h-5 w-5" />
                            <span className="font-medium">返回</span>
                        </button>
                        <div className="w-16" />
                    </div>

                    {/* 分组信息头部 */}
                    <div className="text-center">
                        <h1 className="text-lg font-bold text-zinc-900 dark:text-zinc-100 line-clamp-1">
                            {groupInfo?.groupName || '加载中...'}
                            <span className="ml-2 text-base font-normal text-zinc-500 dark:text-zinc-400">
                                ({groupInfo?.groupCount || 0}/{loadedCount})
                            </span>
                        </h1>
                    </div>
                </div>
            </div>

            {/* 内容区域 */}
            <div className="container mx-auto px-3 py-4 max-w-2xl">
                {status === 'pending' ? (
                    <div className="flex items-center justify-center py-20">
                        <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
                    </div>
                ) : allPictures.length > 0 ? (
                    <div className="grid grid-cols-2 gap-3">
                        {allPictures.map((pic: any, index: number) => (
                            <div
                                key={`${pic.id}-${index}`}
                                className="
                                    relative rounded-xl overflow-hidden
                                    bg-white dark:bg-zinc-900
                                    border border-zinc-200 dark:border-zinc-800
                                    cursor-pointer
                                    transition-all duration-300
                                    hover:shadow-lg active:scale-[0.98]
                                    animate-in fade-in-0 zoom-in-95
                                "
                                onClick={() => setSelectedImage(pic.picUrl)}
                            >
                                <div className="relative aspect-[3/4]">
                                    <HeicSafeImage
                                        src={pic.picUrl}
                                        alt={`Picture ${index + 1}`}
                                        fill
                                        className="object-cover"
                                        unoptimized
                                        loading="lazy"
                                    />
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="text-center py-20">
                        <p className="text-zinc-500 dark:text-zinc-400">
                            该分组暂无图片
                        </p>
                    </div>
                )}

                {/* 加载更多指示器 */}
                {hasNextPage && (
                    <div ref={ref} className="flex items-center justify-center py-8">
                        {isFetchingNextPage ? (
                            <div className="flex items-center gap-2 text-zinc-500 dark:text-zinc-400">
                                <Loader2 className="h-5 w-5 animate-spin" />
                                <span className="text-sm">正在加载更多...</span>
                            </div>
                        ) : (
                            <span className="text-sm text-zinc-400">加载更多</span>
                        )}
                    </div>
                )}

                {!hasNextPage && allPictures.length > 0 && (
                    <div className="text-center py-8 text-xs text-zinc-400 dark:text-zinc-600">
                        已加载全部图片
                    </div>
                )}
            </div>

            {/* 图片预览 Modal */}
            {selectedImage && (
                <div
                    className="fixed inset-0 z-50 bg-black/95 flex items-center justify-center"
                    onClick={() => setSelectedImage(null)}
                >
                    <div className="relative w-full h-full">
                        <HeicSafeImage
                            src={selectedImage}
                            alt="Preview"
                            fill
                            className="object-contain"
                            unoptimized
                        />
                    </div>
                    <button
                        className="absolute top-4 right-4 p-2 rounded-full bg-white/10 backdrop-blur-sm text-white hover:bg-white/20 transition-colors active:scale-95"
                        onClick={() => setSelectedImage(null)}
                    >
                        <X className="h-6 w-6" />
                    </button>
                </div>
            )}
        </div>
    );
}

export default function PicListPage() {
    return (
        <Suspense fallback={
            <div className="min-h-screen bg-zinc-50 dark:bg-zinc-950 flex items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
            </div>
        }>
            <PicListContent />
        </Suspense>
    );
}
