'use client';

import { useQuery } from '@tanstack/react-query';
import { downloadApi } from '@/lib/api';
import { useState, useEffect, useCallback } from 'react';
import { ChevronLeft, Loader2 } from 'lucide-react';
import Link from 'next/link';
import Image from 'next/image';

export default function RandomGifPage() {
    const [allGifIds, setAllGifIds] = useState<number[]>([]);
    const [shuffledGifIds, setShuffledGifIds] = useState<number[]>([]);
    const [currentIndex, setCurrentIndex] = useState(-1);
    const [touchStart, setTouchStart] = useState(0);
    const [touchEnd, setTouchEnd] = useState(0);
    const [isInitialized, setIsInitialized] = useState(false);

    // Fisher-Yates 洗牌算法
    const shuffleArray = (array: number[]) => {
        const arr = [...array];
        for (let i = arr.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [arr[i], arr[j]] = [arr[j], arr[i]];
        }
        return arr;
    };

    // 初始化 GIF 列表
    const { data: gifIds, isLoading: isLoadingIds } = useQuery({
        queryKey: ['all-gif-ids'],
        queryFn: async () => {
            const response = await downloadApi.getAllGifIds();
            return response;
        },
        refetchOnWindowFocus: false,
    });

    useEffect(() => {
        if (gifIds && gifIds.code === 200 && gifIds.data && gifIds.data.length > 0) {
            setAllGifIds(gifIds.data);
            const shuffled = shuffleArray(gifIds.data);
            setShuffledGifIds(shuffled);
            setCurrentIndex(0);
            setIsInitialized(true);
        }
    }, [gifIds]);

    // 获取当前 GIF 数据
    const { data: currentGif, isLoading: isLoadingGif, refetch } = useQuery({
        queryKey: ['random-gif', currentIndex],
        queryFn: async () => {
            if (!isInitialized || currentIndex === -1 || !shuffledGifIds[currentIndex]) {
                return null;
            }
            const response = await downloadApi.getGifById(shuffledGifIds[currentIndex]);
            return response;
        },
        enabled: isInitialized && currentIndex >= 0 && currentIndex < shuffledGifIds.length,
        refetchOnWindowFocus: false,
    });

    // 预加载下一张
    useQuery({
        queryKey: ['random-gif', currentIndex + 1],
        queryFn: async () => {
            const nextIndex = currentIndex < shuffledGifIds.length - 1 ? currentIndex + 1 : 0;
            if (shuffledGifIds[nextIndex]) {
                const response = await downloadApi.getGifById(shuffledGifIds[nextIndex]);
                return response;
            }
            return null;
        },
        enabled: isInitialized && currentIndex >= 0,
        refetchOnWindowFocus: false,
    });

    const handleSwipeUp = useCallback(() => {
        if (!isInitialized) return;

        if (currentIndex < shuffledGifIds.length - 1) {
            setCurrentIndex(prev => prev + 1);
        } else {
            // 重新洗牌
            const newShuffled = shuffleArray(allGifIds);
            setShuffledGifIds(newShuffled);
            setCurrentIndex(0);
        }
    }, [currentIndex, shuffledGifIds, allGifIds, isInitialized]);

    const handleSwipeDown = useCallback(() => {
        if (!isInitialized) return;

        if (currentIndex > 0) {
            setCurrentIndex(prev => prev - 1);
        }
    }, [currentIndex, isInitialized]);

    // 触摸手势处理
    const handleTouchStart = (e: React.TouchEvent) => {
        setTouchStart(e.targetTouches[0].clientY);
    };

    const handleTouchMove = (e: React.TouchEvent) => {
        setTouchEnd(e.targetTouches[0].clientY);
    };

    const handleTouchEnd = () => {
        if (!touchStart || !touchEnd) return;

        const distance = touchStart - touchEnd;
        const minSwipeDistance = 50;

        if (distance > minSwipeDistance) {
            // 向上滑动
            handleSwipeUp();
        } else if (distance < -minSwipeDistance) {
            // 向下滑动
            handleSwipeDown();
        }

        setTouchStart(0);
        setTouchEnd(0);
    };

    if (isLoadingIds || (isLoadingGif && currentIndex === 0)) {
        return (
            <div className="fixed inset-0 bg-black flex items-center justify-center">
                <div className="text-center">
                    <Loader2 className="h-10 w-10 animate-spin text-white mx-auto mb-3" />
                    <p className="text-white/70 text-sm">加载中...</p>
                </div>
            </div>
        );
    }

    if (!isInitialized || !currentGif || currentGif.code !== 200 || !currentGif.data) {
        return (
            <div className="fixed inset-0 bg-black flex items-center justify-center">
                <div className="text-center text-white">
                    <p className="text-lg mb-4">暂无可用的动图</p>
                    <Link
                        href="/"
                        className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-white/10 hover:bg-white/20 transition-colors"
                    >
                        <ChevronLeft className="h-5 w-5" />
                        返回首页
                    </Link>
                </div>
            </div>
        );
    }

    const gifData = currentGif.data;

    return (
        <div
            className="fixed inset-0 bg-black"
            onTouchStart={handleTouchStart}
            onTouchMove={handleTouchMove}
            onTouchEnd={handleTouchEnd}
        >
            {/* 顶部导航 */}
            <div className="absolute top-0 left-0 right-0 z-50 bg-gradient-to-b from-black/60 to-transparent">
                <div className="flex items-center justify-between px-4 py-3 safe-top">
                    <Link
                        href="/"
                        className="flex items-center gap-2 text-white/90 hover:text-white transition-colors active:scale-95"
                    >
                        <ChevronLeft className="h-6 w-6" />
                        <span className="font-medium">返回</span>
                    </Link>
                    <h1 className="text-white/90 font-semibold">随机动图</h1>
                    <div className="w-20" />
                </div>
            </div>

            {/* GIF 显示区域 */}
            <div className="relative w-full h-full flex items-center justify-center">
                {gifData.mediaUrl && (
                    <div className="relative w-full h-full animate-in fade-in-0 duration-300">
                        <video
                            src={gifData.mediaUrl}
                            className="w-full h-full object-contain"
                            autoPlay
                            loop
                            muted
                            playsInline
                        />
                    </div>
                )}
            </div>

            {/* 底部信息栏 */}
            {(gifData.workTitle || gifData.authorNickname) && (
                <div className="absolute bottom-0 left-0 right-0 z-50 bg-gradient-to-t from-black/60 to-transparent pb-safe">
                    <div className="px-4 py-4 space-y-2">
                        {gifData.workTitle && (
                            <div className="text-white font-medium line-clamp-2">
                                {gifData.workTitle}
                            </div>
                        )}
                        {gifData.authorNickname && (
                            <div className="text-white/70 text-sm">
                                作者: {gifData.authorNickname}
                            </div>
                        )}
                    </div>
                </div>
            )}

            {/* 底部提示 */}
            <div className="absolute bottom-20 left-0 right-0 z-40 pb-safe pointer-events-none">
                <div className="flex flex-col items-center gap-3">
                    {/* 滑动提示 */}
                    <div className="px-6 py-2.5 rounded-full bg-white/10 backdrop-blur-sm">
                        <p className="text-white/70 text-sm">
                            {currentIndex === 0 ? '向上滑动查看更多' : '上下滑动切换'}
                        </p>
                    </div>

                    {/* 索引指示器 */}
                    <div className="px-4 py-2 rounded-full bg-white/10 backdrop-blur-sm">
                        <span className="text-white/70 text-xs font-medium">
                            #{currentIndex + 1} / {shuffledGifIds.length}
                        </span>
                    </div>
                </div>
            </div>

            {/* 加载指示器 */}
            {isLoadingGif && currentIndex > 0 && (
                <div className="absolute inset-0 bg-black/40 flex items-center justify-center z-40">
                    <Loader2 className="h-8 w-8 animate-spin text-white" />
                </div>
            )}
        </div>
    );
}
