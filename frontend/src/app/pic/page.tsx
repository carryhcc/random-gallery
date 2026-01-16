'use client';

import { useQuery } from '@tanstack/react-query';
import { pictureApi } from '@/lib/api';
import { useState, useEffect } from 'react';
import { ChevronLeft, RefreshCw, Loader2 } from 'lucide-react';
import { HeicSafeImage } from '@/components/heic-safe-image';
import Link from 'next/link';

export default function PicPage() {
    const [currentPicId, setCurrentPicId] = useState<number | null>(null);

    // 获取随机图片
    const { data: picData, isLoading, refetch, isFetching } = useQuery({
        queryKey: ['random-pic', currentPicId],
        queryFn: async () => {
            const response = await pictureApi.getRandomPicture();
            return response.data;
        },
    });

    useEffect(() => {
        if (picData && !currentPicId) {
            setCurrentPicId(picData.id);
        }
    }, [picData, currentPicId]);

    const handleNext = () => {
        refetch();
    };

    if (isLoading) {
        return (
            <div className="fixed inset-0 bg-zinc-50 dark:bg-zinc-950 flex items-center justify-center">
                <div className="text-center">
                    <Loader2 className="h-10 w-10 animate-spin text-blue-600 mx-auto mb-3" />
                    <p className="text-zinc-600 dark:text-zinc-400 text-sm">加载中...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="fixed inset-0 bg-black flex flex-col">
            {/* 顶部导航栏 - 半透明悬浮 */}
            <div className="absolute top-0 left-0 right-0 z-50 bg-gradient-to-b from-black/60 to-transparent">
                <div className="flex items-center justify-between px-4 py-3 safe-top">
                    <Link
                        href="/"
                        className="flex items-center gap-2 text-white/90 hover:text-white transition-colors active:scale-95"
                    >
                        <ChevronLeft className="h-6 w-6" />
                        <span className="font-medium">返回</span>
                    </Link>
                    <h1 className="text-white/90 font-semibold">随机图片</h1>
                    <div className="w-20" /> {/* 占位 */}
                </div>
            </div>

            {/* 图片显示区域 - 全屏 */}
            <div className="flex-1 relative flex items-center justify-center">
                {picData?.picUrl && (
                    <div className="relative w-full h-full">
                        <HeicSafeImage
                            src={picData.picUrl}
                            alt="随机图片"
                            fill
                            className="object-contain"
                            unoptimized
                            priority
                        />
                    </div>
                )}
            </div>

            {/* 底部固定操作栏 */}
            <div className="absolute bottom-0 left-0 right-0 z-50 bg-gradient-to-t from-black/60 to-transparent pb-safe">
                <div className="px-4 py-6 flex items-center justify-center">
                    <button
                        onClick={handleNext}
                        disabled={isFetching}
                        className="
                            flex items-center gap-2.5 px-8 py-4
                            rounded-full
                            bg-white/90 hover:bg-white
                            backdrop-blur-sm
                            text-zinc-900 font-semibold
                            transition-all duration-200
                            hover:scale-105
                            active:scale-95
                            disabled:opacity-50 disabled:cursor-not-allowed
                            shadow-lg shadow-black/20
                            min-h-[56px]
                        "
                    >
                        {isFetching ? (
                            <>
                                <Loader2 className="h-5 w-5 animate-spin" />
                                <span>加载中...</span>
                            </>
                        ) : (
                            <>
                                <RefreshCw className="h-5 w-5" />
                                <span>换一张</span>
                            </>
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
}
