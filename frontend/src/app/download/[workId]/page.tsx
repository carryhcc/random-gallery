'use client';

import { useQuery } from '@tanstack/react-query';
import { downloadApi } from '@/lib/api';
import { use } from 'react';
import { ChevronLeft, Loader2, X } from 'lucide-react';
import Link from 'next/link';
import { HeicSafeImage } from '@/components/heic-safe-image';
import { useState } from 'react';

export default function DownloadDetailPage({
    params,
}: {
    params: Promise<{ workId: string }>;
}) {
    const { workId } = use(params);
    const [selectedImage, setSelectedImage] = useState<string | null>(null);

    // 获取作品详情
    const { data: workDetail, isLoading } = useQuery({
        queryKey: ['work-detail', workId],
        queryFn: async () => {
            const response = await downloadApi.getWorkDetail(workId);
            return response.data;
        },
    });

    if (isLoading) {
        return (
            <div className="min-h-screen bg-zinc-50 dark:bg-zinc-950 flex items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
            </div>
        );
    }

    const images = workDetail?.mediaList || [];

    return (
        <div className="min-h-screen bg-zinc-50 dark:bg-zinc-950">
            {/* 顶部导航 */}
            <div className="sticky top-0 z-40 bg-white/90 dark:bg-zinc-900/90 backdrop-blur-md border-b border-zinc-200 dark:border-zinc-800">
                <div className="container mx-auto px-4 py-3.5">
                    <Link
                        href="/download"
                        className="inline-flex items-center gap-2 text-zinc-900 dark:text-zinc-100 hover:text-blue-600 dark:hover:text-blue-400 transition-colors active:scale-95"
                    >
                        <ChevronLeft className="h-5 w-5" />
                        <span className="font-medium">返回列表</span>
                    </Link>
                </div>
            </div>

            {/* 作品信息 */}
            <div className="container mx-auto px-4 py-4 max-w-2xl">
                <div className="bg-white dark:bg-zinc-900 rounded-2xl border border-zinc-200 dark:border-zinc-800 p-5 mb-4">
                    <h1 className="text-xl font-bold text-zinc-900 dark:text-zinc-100 mb-3 line-clamp-3">
                        {workDetail?.workTitle || '无标题'}
                    </h1>
                    <div className="space-y-1.5 text-sm text-zinc-600 dark:text-zinc-400">
                        <div className="flex items-center gap-2">
                            <span className="text-zinc-500 dark:text-zinc-500">作者:</span>
                            <span className="font-medium">{workDetail?.authorName || '未知'}</span>
                        </div>
                        {workDetail?.createTime && (
                            <div className="flex items-center gap-2">
                                <span className="text-zinc-500 dark:text-zinc-500">时间:</span>
                                <span>{new Date(workDetail.createTime).toLocaleString('zh-CN')}</span>
                            </div>
                        )}
                        <div className="flex items-center gap-2">
                            <span className="text-zinc-500 dark:text-zinc-500">图片:</span>
                            <span className="font-medium text-blue-600 dark:text-blue-400">{images.length} 张</span>
                        </div>
                    </div>
                </div>

                {/* 图片网格 - 移动端2列 */}
                {images.length > 0 ? (
                    <div className="grid grid-cols-2 gap-3">
                        {images.map((media: any, index: number) => (
                            <div
                                key={media.id || index}
                                className="
                                    relative rounded-xl overflow-hidden
                                    bg-white dark:bg-zinc-900
                                    border border-zinc-200 dark:border-zinc-800
                                    cursor-pointer
                                    transition-all duration-300
                                    hover:shadow-lg active:scale-[0.98]
                                    animate-in fade-in-0 zoom-in-95
                                "
                                style={{
                                    animationDelay: `${index * 20}ms`,
                                    animationFillMode: 'backwards',
                                }}
                                onClick={() => setSelectedImage(media.url)}
                            >
                                <div className="relative aspect-[3/4]">
                                    <HeicSafeImage
                                        src={media.url}
                                        alt={`Image ${index + 1}`}
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
                        <p className="text-zinc-500 dark:text-zinc-400">暂无图片</p>
                    </div>
                )}
            </div>

            {/* 图片预览 Modal - 移动端优化 */}
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
