'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { downloadApi } from '@/lib/api';
import { use } from 'react';
import { ChevronLeft, Loader2, X, Download, Trash2, ExternalLink, Heart, Star, MessageCircle, Share2, User } from 'lucide-react';
import Link from 'next/link';
import { HeicSafeImage } from '@/components/heic-safe-image';
import { useState, useEffect, useRef } from 'react';
import { useRouter } from 'next/navigation';

// 确认对话框组件
function ConfirmDialog({
    isOpen,
    onClose,
    onConfirm,
    message
}: {
    isOpen: boolean;
    onClose: () => void;
    onConfirm: () => void;
    message: string;
}) {
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-[60] flex items-center justify-center">
            <div className="absolute inset-0 bg-black/60" onClick={onClose} />
            <div className="relative bg-white dark:bg-zinc-900 rounded-2xl p-6 max-w-sm mx-4 shadow-2xl animate-in zoom-in-95">
                <div className="flex items-center gap-3 mb-4">
                    <div className="p-2 rounded-full bg-orange-100 dark:bg-orange-900/30">
                        <Trash2 className="h-5 w-5 text-orange-600 dark:text-orange-400" />
                    </div>
                    <h3 className="font-semibold text-zinc-900 dark:text-zinc-100">确认删除</h3>
                </div>
                <p className="text-sm text-zinc-600 dark:text-zinc-400 mb-6">{message}</p>
                <div className="flex gap-3">
                    <button
                        onClick={onClose}
                        className="flex-1 px-4 py-2 rounded-xl bg-zinc-100 dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 font-medium hover:bg-zinc-200 dark:hover:bg-zinc-700 transition-colors"
                    >
                        取消
                    </button>
                    <button
                        onClick={() => {
                            onConfirm();
                            onClose();
                        }}
                        className="flex-1 px-4 py-2 rounded-xl bg-red-600 text-white font-medium hover:bg-red-700 transition-colors"
                    >
                        删除
                    </button>
                </div>
            </div>
        </div>
    );
}

// Live Photo 视频组件
function LivePhoto({ src, index }: { src: string; index: number }) {
    const videoRef = useRef<HTMLVideoElement>(null);
    const [isPlaying, setIsPlaying] = useState(false);

    const handleMouseEnter = () => {
        if (videoRef.current) {
            videoRef.current.currentTime = 0;
            videoRef.current.play();
            setIsPlaying(true);
        }
    };

    const handleMouseLeave = () => {
        if (videoRef.current) {
            videoRef.current.pause();
            videoRef.current.currentTime = 0;
            setIsPlaying(false);
        }
    };

    const handleTouch = () => {
        if (videoRef.current) {
            if (isPlaying) {
                videoRef.current.pause();
                videoRef.current.currentTime = 0;
                setIsPlaying(false);
            } else {
                videoRef.current.currentTime = 0;
                videoRef.current.play();
                setIsPlaying(true);
            }
        }
    };

    return (
        <div
            className="relative w-full h-full group"
            onMouseEnter={handleMouseEnter}
            onMouseLeave={handleMouseLeave}
            onTouchStart={handleTouch}
        >
            <video
                ref={videoRef}
                loop
                muted
                playsInline
                preload="metadata"
                className="absolute inset-0 w-full h-full object-cover"
            >
                <source src={src} type="video/mp4" />
            </video>
            <div className="absolute top-2 left-2 flex items-center gap-1 px-2 py-1 rounded-full bg-black/70 backdrop-blur-sm">
                <div className={`w-1.5 h-1.5 rounded-full ${isPlaying ? 'bg-red-500 animate-pulse' : 'bg-white'}`} />
                <span className="text-[10px] font-semibold text-white tracking-wider">LIVE</span>
            </div>
            {!isPlaying && (
                <div className="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none">
                    <span className="text-xs text-white bg-black/50 px-3 py-1 rounded-full">悬停播放</span>
                </div>
            )}
        </div>
    );
}

export default function DownloadDetailPage({
    params,
}: {
    params: Promise<{ workId: string }>;
}) {
    const { workId } = use(params);
    const router = useRouter();
    const queryClient = useQueryClient();
    const [selectedImageIndex, setSelectedImageIndex] = useState<number | null>(null);
    const [confirmDialog, setConfirmDialog] = useState<{ isOpen: boolean; message: string; onConfirm: () => void }>({
        isOpen: false,
        message: '',
        onConfirm: () => { },
    });

    // 获取作品详情
    const { data: workDetail, isLoading } = useQuery({
        queryKey: ['work-detail', workId],
        queryFn: async () => {
            const response = await downloadApi.getWorkDetail(workId);
            return response.data;
        },
    });

    // 删除作品
    const deleteWorkMutation = useMutation({
        mutationFn: () => downloadApi.deleteWork(workId),
        onSuccess: () => {
            (window as any).showToast?.('作品已删除', 'success');
            setTimeout(() => router.push('/download'), 1000);
        },
        onError: () => {
            (window as any).showToast?.('删除失败', 'error');
        },
    });

    // 删除单个媒体
    const deleteMediaMutation = useMutation({
        mutationFn: (mediaId: number) => downloadApi.deleteMedia(mediaId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['work-detail', workId] });
            (window as any).showToast?.('已删除', 'success');
        },
        onError: () => {
            (window as any).showToast?.('删除失败', 'error');
        },
    });

    const baseInfo = workDetail?.baseInfo;
    const images = workDetail?.images || [];
    const gifs = workDetail?.gifs || [];
    const allMedia = [...images, ...gifs].sort((a, b) => a.sortIndex - b.sortIndex);

    // 处理标签
    const tags = baseInfo?.workTags?.split(/\s+/).filter(Boolean) || [];

    // 提取话题标签
    const extractTopics = (desc: string | undefined) => {
        if (!desc) return [];
        const topicRegex = /##+([^#\[]+)\[话题\]#+/g;
        const topics: string[] = [];
        let match;
        while ((match = topicRegex.exec(desc)) !== null) {
            topics.push(match[1].trim());
        }
        return topics;
    };
    const topics = extractTopics(baseInfo?.workDescription);

    // 下载功能
    const downloadMedia = async (url: string, index: number, type: string) => {
        try {
            (window as any).showToast?.('正在下载...', 'info');
            const response = await fetch(url);
            if (!response.ok) throw new Error('下载失败');

            const blob = await response.blob();
            const blobUrl = URL.createObjectURL(blob);
            const ext = type === 'GIF' ? '.mp4' : '.jpg';
            const filename = `media_${index + 1}${ext}`;

            const a = document.createElement('a');
            a.href = blobUrl;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            setTimeout(() => URL.revokeObjectURL(blobUrl), 100);

            (window as any).showToast?.('下载成功', 'success');
        } catch (error) {
            console.error('下载失败:', error);
            (window as any).showToast?.('下载失败', 'error');
        }
    };

    // 键盘导航
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if (selectedImageIndex === null) return;

            if (e.key === 'Escape') {
                setSelectedImageIndex(null);
            } else if (e.key === 'ArrowLeft') {
                setSelectedImageIndex((prev) => {
                    if (prev === null) return null;
                    return prev === 0 ? allMedia.length - 1 : prev - 1;
                });
            } else if (e.key === 'ArrowRight') {
                setSelectedImageIndex((prev) => {
                    if (prev === null) return null;
                    return prev === allMedia.length - 1 ? 0 : prev + 1;
                });
            }
        };

        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [selectedImageIndex, allMedia.length]);

    if (isLoading) {
        return (
            <div className="min-h-screen bg-zinc-50 dark:bg-zinc-950 flex items-center justify-center">
                <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-zinc-50 dark:bg-zinc-950">
            {/* 顶部导航 */}
            <div className="sticky top-0 z-40 bg-white/90 dark:bg-zinc-900/90 backdrop-blur-md border-b border-zinc-200 dark:border-zinc-800">
                <div className="container mx-auto px-4 py-3.5">
                    <div className="flex items-center justify-between">
                        <Link
                            href="/download"
                            className="inline-flex items-center gap-2 text-zinc-900 dark:text-zinc-100 hover:text-blue-600 dark:hover:text-blue-400 transition-colors active:scale-95"
                        >
                            <ChevronLeft className="h-5 w-5" />
                            <span className="font-medium">返回列表</span>
                        </Link>
                        <Link
                            href="/"
                            className="px-4 py-2 rounded-xl bg-zinc-100 dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 font-medium hover:bg-zinc-200 dark:hover:bg-zinc-700 transition-colors text-sm"
                        >
                            首页
                        </Link>
                    </div>
                </div>
            </div>

            {/* 作品信息 */}
            <div className="container mx-auto px-4 py-6 max-w-4xl">
                <div className="bg-white dark:bg-zinc-900 rounded-2xl border border-zinc-200 dark:border-zinc-800 p-6 mb-6">
                    <h1 className="text-2xl font-bold text-zinc-900 dark:text-zinc-100 mb-4">
                        {baseInfo?.workTitle || '无标题'}
                    </h1>

                    {/* 作者信息 */}
                    <div className="flex items-center gap-3 mb-4 pb-4 border-b border-zinc-200 dark:border-zinc-800">
                        <User className="h-5 w-5 text-zinc-400" />
                        <Link
                            href={`/download?authorId=${baseInfo?.authorId}`}
                            className="font-medium text-zinc-900 dark:text-zinc-100 hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
                        >
                            {baseInfo?.authorNickname || '未知作者'}
                        </Link>
                        {baseInfo?.authorUrl && (
                            <a
                                href={baseInfo.authorUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="p-1.5 rounded-lg bg-zinc-100 dark:bg-zinc-800 hover:bg-zinc-200 dark:hover:bg-zinc-700 transition-colors"
                                title="访问作者主页"
                            >
                                <ExternalLink className="h-4 w-4 text-zinc-600 dark:text-zinc-400" />
                            </a>
                        )}
                    </div>

                    {/* 统计信息 */}
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-4">
                        <div className="flex items-center gap-2 text-sm">
                            <Heart className="h-4 w-4 text-red-500" />
                            <span className="text-zinc-600 dark:text-zinc-400">{baseInfo?.likeCount || '0'} 点赞</span>
                        </div>
                        <div className="flex items-center gap-2 text-sm">
                            <Star className="h-4 w-4 text-yellow-500" />
                            <span className="text-zinc-600 dark:text-zinc-400">{baseInfo?.collectCount || '0'} 收藏</span>
                        </div>
                        <div className="flex items-center gap-2 text-sm">
                            <MessageCircle className="h-4 w-4 text-blue-500" />
                            <span className="text-zinc-600 dark:text-zinc-400">{baseInfo?.commentCount || '0'} 评论</span>
                        </div>
                        <div className="flex items-center gap-2 text-sm">
                            <Share2 className="h-4 w-4 text-green-500" />
                            <span className="text-zinc-600 dark:text-zinc-400">{baseInfo?.shareCount || '0'} 分享</span>
                        </div>
                    </div>

                    {/* 发布时间 */}
                    {baseInfo?.publishTime && (
                        <div className="text-sm text-zinc-500 dark:text-zinc-400 mb-4">
                            发布时间：{baseInfo.publishTime}
                        </div>
                    )}

                    {/* 话题标签 */}
                    {topics.length > 0 && (
                        <div className="flex flex-wrap gap-2 mb-4">
                            {topics.map((topic, i) => (
                                <span
                                    key={i}
                                    className="px-3 py-1 rounded-full bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 text-sm"
                                >
                                    {topic}
                                </span>
                            ))}
                        </div>
                    )}

                    {/* 标签 */}
                    {tags.length > 0 && (
                        <div className="flex flex-wrap gap-2 mb-4">
                            {tags.map((tag, i) => (
                                <Link
                                    key={i}
                                    href={`/download?tag=${encodeURIComponent(tag)}`}
                                    className="px-3 py-1 rounded-full bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300 text-sm hover:bg-purple-200 dark:hover:bg-purple-900/50 transition-colors cursor-pointer"
                                >
                                    {tag}
                                </Link>
                            ))}
                        </div>
                    )}

                    {/* 删除作品按钮 */}
                    <div className="pt-4 border-t border-zinc-200 dark:border-zinc-800">
                        <button
                            onClick={() => setConfirmDialog({
                                isOpen: true,
                                message: '确定要删除这个作品吗？删除后将无法恢复。',
                                onConfirm: () => deleteWorkMutation.mutate(),
                            })}
                            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 hover:bg-red-100 dark:hover:bg-red-900/30 transition-colors font-medium text-sm"
                        >
                            <Trash2 className="h-4 w-4" />
                            删除作品
                        </button>
                    </div>
                </div>

                {/* 图片区域 */}
                {images.length > 0 && (
                    <div className="mb-8">
                        <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100 mb-4 flex items-center gap-2">
                            <span>图片</span>
                            <span className="text-sm text-zinc-500">({images.length})</span>
                        </h2>
                        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                            {images.map((img: any, index: number) => {
                                const globalIndex = allMedia.findIndex(m => m.id === img.id);
                                return (
                                    <div
                                        key={img.id}
                                        className="relative rounded-xl overflow-hidden bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 group aspect-[3/4]"
                                    >
                                        <div className="relative w-full h-full cursor-pointer" onClick={() => setSelectedImageIndex(globalIndex)}>
                                            <HeicSafeImage
                                                src={img.mediaUrl}
                                                alt={`Image ${index + 1}`}
                                                fill
                                                className="object-cover"
                                                unoptimized
                                                loading="lazy"
                                            />
                                        </div>
                                        <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                                            <button
                                                onClick={() => downloadMedia(img.mediaUrl, index, 'IMAGE')}
                                                className="p-2 rounded-full bg-white/20 hover:bg-white/30 transition-colors backdrop-blur-sm"
                                                title="下载"
                                            >
                                                <Download className="h-5 w-5 text-white" />
                                            </button>
                                            <button
                                                onClick={() => setConfirmDialog({
                                                    isOpen: true,
                                                    message: '确定要删除这张图片吗？',
                                                    onConfirm: () => deleteMediaMutation.mutate(img.id),
                                                })}
                                                className="p-2 rounded-full bg-white/20 hover:bg-white/30 transition-colors backdrop-blur-sm"
                                                title="删除"
                                            >
                                                <Trash2 className="h-5 w-5 text-white" />
                                            </button>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                )}

                {/* GIF/实况照片区域 */}
                {gifs.length > 0 && (
                    <div>
                        <h2 className="text-lg font-semibold text-zinc-900 dark:text-zinc-100 mb-4 flex items-center gap-2">
                            <span>实况照片</span>
                            <span className="text-sm text-zinc-500">({gifs.length})</span>
                        </h2>
                        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                            {gifs.map((gif: any, index: number) => {
                                const globalIndex = allMedia.findIndex(m => m.id === gif.id);
                                return (
                                    <div
                                        key={gif.id}
                                        className="relative rounded-xl overflow-hidden bg-white dark:bg-zinc-900 border border-zinc-200 dark:border-zinc-800 group aspect-[3/4]"
                                    >
                                        <div className="relative w-full h-full cursor-pointer" onClick={() => setSelectedImageIndex(globalIndex)}>
                                            <LivePhoto src={gif.mediaUrl} index={index} />
                                        </div>
                                        <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                                            <button
                                                onClick={() => downloadMedia(gif.mediaUrl, index, 'GIF')}
                                                className="p-2 rounded-full bg-white/20 hover:bg-white/30 transition-colors backdrop-blur-sm"
                                                title="下载"
                                            >
                                                <Download className="h-5 w-5 text-white" />
                                            </button>
                                            <button
                                                onClick={() => setConfirmDialog({
                                                    isOpen: true,
                                                    message: '确定要删除这个实况照片吗？',
                                                    onConfirm: () => deleteMediaMutation.mutate(gif.id),
                                                })}
                                                className="p-2 rounded-full bg-white/20 hover:bg-white/30 transition-colors backdrop-blur-sm"
                                                title="删除"
                                            >
                                                <Trash2 className="h-5 w-5 text-white" />
                                            </button>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                )}

                {/* 空状态 */}
                {images.length === 0 && gifs.length === 0 && (
                    <div className="text-center py-20">
                        <p className="text-zinc-500 dark:text-zinc-400">暂无媒体内容</p>
                    </div>
                )}
            </div>

            {/* 图片查看器 */}
            {selectedImageIndex !== null && (
                <div
                    className="fixed inset-0 z-50 bg-black/95 flex items-center justify-center"
                    onClick={() => setSelectedImageIndex(null)}
                >
                    <div className="relative w-full h-full flex items-center justify-center">
                        {allMedia[selectedImageIndex]?.mediaType === 'GIF' ? (
                            <video
                                src={allMedia[selectedImageIndex].mediaUrl}
                                className="max-w-full max-h-full object-contain"
                                autoPlay
                                loop
                                muted
                                playsInline
                            />
                        ) : (
                            <HeicSafeImage
                                src={allMedia[selectedImageIndex]?.mediaUrl || ''}
                                alt="Preview"
                                fill
                                className="object-contain"
                                unoptimized
                            />
                        )}
                    </div>

                    {/* 关闭按钮 */}
                    <button
                        className="absolute top-4 right-4 p-2 rounded-full bg-white/10 backdrop-blur-sm text-white hover:bg-white/20 transition-colors active:scale-95"
                        onClick={() => setSelectedImageIndex(null)}
                    >
                        <X className="h-6 w-6" />
                    </button>

                    {/* 左右切换按钮 */}
                    {allMedia.length > 1 && (
                        <>
                            <button
                                className="absolute left-4 top-1/2 -translate-y-1/2 p-3 rounded-full bg-white/10 backdrop-blur-sm text-white hover:bg-white/20 transition-colors active:scale-95"
                                onClick={(e) => {
                                    e.stopPropagation();
                                    setSelectedImageIndex((prev) => prev === null ? null : prev === 0 ? allMedia.length - 1 : prev - 1);
                                }}
                            >
                                <ChevronLeft className="h-6 w-6" />
                            </button>
                            <button
                                className="absolute right-4 top-1/2 -translate-y-1/2 p-3 rounded-full bg-white/10 backdrop-blur-sm text-white hover:bg-white/20 transition-colors active:scale-95"
                                onClick={(e) => {
                                    e.stopPropagation();
                                    setSelectedImageIndex((prev) => prev === null ? null : prev === allMedia.length - 1 ? 0 : prev + 1);
                                }}
                            >
                                <ChevronLeft className="h-6 w-6 rotate-180" />
                            </button>
                        </>
                    )}

                    {/* 图片计数 */}
                    <div className="absolute bottom-4 left-1/2 -translate-x-1/2 px-4 py-2 rounded-full bg-white/10 backdrop-blur-sm text-white text-sm">
                        {selectedImageIndex + 1} / {allMedia.length}
                    </div>
                </div>
            )}

            {/* 确认对话框 */}
            <ConfirmDialog
                isOpen={confirmDialog.isOpen}
                onClose={() => setConfirmDialog({ ...confirmDialog, isOpen: false })}
                onConfirm={confirmDialog.onConfirm}
                message={confirmDialog.message}
            />
        </div>
    );
}
