'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { downloadApi } from '@/lib/api';
import { useState, useRef, useEffect, useMemo } from 'react';
import { ChevronLeft, Search, User, Tag, X, Loader2, ArrowRight } from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import type { XhsAuthor, XhsTag } from '@/lib/types';
import _ from 'lodash';

// Marquee Component
const MarqueeRow = ({
    items,
    renderItem,
    speed = 30
}: {
    items: any[];
    renderItem: (item: any) => React.ReactNode;
    speed?: number;
}) => {
    // Duplicate items to creating seamless loop
    const displayItems = useMemo(() => [...items, ...items, ...items], [items]);

    if (!items.length) return null;

    return (
        <div className="flex overflow-hidden group py-2 select-none">
            <div
                className="flex gap-3 animate-marquee whitespace-nowrap"
                style={{ animationDuration: `${speed}s` }}
            >
                {displayItems.map((item, index) => (
                    <div key={`${index}-${Math.random()}`}>
                        {renderItem(item)}
                    </div>
                ))}
            </div>
        </div>
    );
};

export default function DownloadPage() {
    const router = useRouter();
    const queryClient = useQueryClient();

    // State
    const [searchTerm, setSearchTerm] = useState('');
    const [isSearchFocused, setIsSearchFocused] = useState(false);
    const [selectedAuthor, setSelectedAuthor] = useState<XhsAuthor | null>(null);
    const [selectedTag, setSelectedTag] = useState<XhsTag | null>(null);

    // Random Seed for shuffling (changes on mount/refresh)
    const [randomSeed, setRandomSeed] = useState(0);
    useEffect(() => setRandomSeed(Math.random()), []);

    // Fetch Authors
    const { data: authors } = useQuery({
        queryKey: ['xhs-authors'],
        queryFn: async () => (await downloadApi.getAuthors()).data || [],
        staleTime: 5 * 60 * 1000,
    });

    // Fetch Tags
    const { data: tags } = useQuery({
        queryKey: ['xhs-tags'],
        queryFn: async () => (await downloadApi.getTags()).data || [],
        staleTime: 5 * 60 * 1000,
    });

    // Shuffled Lists
    const shuffledAuthors = useMemo(() => {
        if (!authors) return [];
        return _.shuffle(authors);
    }, [authors, randomSeed]);

    const shuffledTags = useMemo(() => {
        if (!tags) return [];
        return _.shuffle(tags);
    }, [tags, randomSeed]);

    // Fetch Works
    const { data: worksData, isLoading: isWorksLoading } = useQuery({
        queryKey: ['xhs-works', searchTerm, selectedAuthor?.authorId, selectedTag?.id],
        queryFn: async () => {
            const res = await downloadApi.getWorkList({
                page: 1,
                pageSize: 50,
                search: searchTerm,
                authorId: selectedAuthor?.authorId,
                tagId: selectedTag?.id,
            });
            return res.data?.works || [];
        },
    });

    // Add Task Mutation
    const addTaskMutation = useMutation({
        mutationFn: (url: string) => downloadApi.addDownloadTask(url),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['xhs-works'] });
            setSearchTerm('');
            (window as any).showToast?.('任务添加成功', 'success');
        },
    });

    const handleSearchOrAdd = () => {
        if (searchTerm.includes('xhslink.com') || searchTerm.includes('xiaohongshu.com')) {
            addTaskMutation.mutate(searchTerm);
        } else {
            setIsSearchFocused(false);
        }
    };

    return (
        <div className="min-h-screen bg-zinc-50 dark:bg-zinc-950 pb-safe">
            {/* Sticky Header */}
            <div className="sticky top-0 z-50 bg-white/95 dark:bg-zinc-900/95 backdrop-blur-md border-b border-zinc-200 dark:border-zinc-800 transition-all duration-300">
                <div className="container mx-auto px-4 py-3">
                    <div className="flex items-center gap-3">
                        <Link href="/" className="flex-shrink-0 p-2 -ml-2 text-zinc-600 dark:text-zinc-400 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-full transition-colors">
                            <ChevronLeft className="w-6 h-6" />
                        </Link>

                        <div className="flex-1 relative group">
                            <div className="absolute inset-y-0 left-3 flex items-center pointer-events-none">
                                <Search className="w-4 h-4 text-zinc-400 group-focus-within:text-red-500 transition-colors" />
                            </div>
                            <input
                                type="text"
                                className="w-full h-10 pl-10 pr-10 bg-zinc-100 dark:bg-zinc-800 rounded-full text-sm outline-none focus:ring-2 focus:ring-red-500/20 transition-all dark:text-zinc-100 placeholder:text-zinc-400"
                                placeholder="搜索标题、作者或粘贴链接..."
                                value={searchTerm}
                                onFocus={() => setIsSearchFocused(true)}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                onKeyDown={(e) => e.key === 'Enter' && handleSearchOrAdd()}
                            />
                            {searchTerm && (
                                <button
                                    onClick={() => setSearchTerm('')}
                                    className="absolute inset-y-0 right-3 flex items-center text-zinc-400 hover:text-zinc-600"
                                >
                                    <X className="w-4 h-4" />
                                </button>
                            )}
                        </div>

                        <button
                            onClick={handleSearchOrAdd}
                            className="flex-shrink-0 px-4 py-2 bg-red-500 text-white text-sm font-medium rounded-full active:scale-95 transition-transform shadow-md shadow-red-500/20"
                        >
                            搜索
                        </button>
                    </div>

                    {/* Active Filters */}
                    {(selectedAuthor || selectedTag) && (
                        <div className="flex items-center gap-2 mt-3 animate-in slide-in-from-top-2">
                            {selectedAuthor && (
                                <span className="inline-flex items-center gap-1 px-3 py-1 text-xs font-medium text-red-600 bg-red-50 dark:bg-red-900/20 dark:text-red-400 rounded-full border border-red-100 dark:border-red-900/30">
                                    <User className="w-3 h-3" />
                                    {selectedAuthor.authorNickname}
                                    <button onClick={() => setSelectedAuthor(null)} className="ml-1 hover:text-red-800"><X className="w-3 h-3" /></button>
                                </span>
                            )}
                            {selectedTag && (
                                <span className="inline-flex items-center gap-1 px-3 py-1 text-xs font-medium text-blue-600 bg-blue-50 dark:bg-blue-900/20 dark:text-blue-400 rounded-full border border-blue-100 dark:border-blue-900/30">
                                    <Tag className="w-3 h-3" />
                                    {selectedTag.tagName}
                                    <button onClick={() => setSelectedTag(null)} className="ml-1 hover:text-blue-800"><X className="w-3 h-3" /></button>
                                </span>
                            )}
                        </div>
                    )}
                </div>

                {/* Danmaku Recommendation Panel */}
                {isSearchFocused && !searchTerm && (
                    <div
                        className="border-t border-zinc-100 dark:border-zinc-800 bg-white dark:bg-zinc-900 animate-in slide-in-from-top-5 duration-300"
                    >
                        <div className="py-2 space-y-1">
                            {/* Author Rows (3 rows) */}
                            {_.chunk(shuffledAuthors, Math.ceil(shuffledAuthors.length / 3)).map((chunk: XhsAuthor[], i: number) => (
                                <MarqueeRow
                                    key={`author-row-${i}`}
                                    items={chunk}
                                    speed={150 + (i * 30)} // Very slow speeds: 150s, 180s, 210s
                                    renderItem={(author) => (
                                        <button
                                            onClick={() => { setSelectedAuthor(author); setIsSearchFocused(false); }}
                                            className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-zinc-100 dark:bg-zinc-800 text-xs font-medium text-zinc-600 dark:text-zinc-300 hover:bg-red-50 hover:text-red-600 dark:hover:bg-red-900/20 dark:hover:text-red-400 transition-colors"
                                        >
                                            <User className="w-3 h-3 opacity-50" />
                                            <span>{author.authorNickname}</span>
                                            <span className="opacity-50 text-[10px]">({author.workCount})</span>
                                        </button>
                                    )}
                                />
                            ))}

                            {/* Tag Rows (5 rows) */}
                            {_.chunk(shuffledTags, Math.ceil(shuffledTags.length / 5)).map((chunk: XhsTag[], i: number) => (
                                <MarqueeRow
                                    key={`tag-row-${i}`}
                                    items={chunk}
                                    speed={200 + (i * 20)} // Very slow speeds: 200s, 220s, 240s, 260s, 280s
                                    renderItem={(tag) => (
                                        <button
                                            onClick={() => { setSelectedTag(tag); setIsSearchFocused(false); }}
                                            className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-zinc-100 dark:bg-zinc-800 text-xs font-medium text-zinc-600 dark:text-zinc-300 hover:bg-blue-50 hover:text-blue-600 dark:hover:bg-blue-900/20 dark:hover:text-blue-400 transition-colors"
                                        >
                                            <Tag className="w-3 h-3 opacity-50" />
                                            <span>{tag.tagName}</span>
                                            <span className="opacity-50 text-[10px]">({tag.workCount})</span>
                                        </button>
                                    )}
                                />
                            ))}
                        </div>
                    </div>
                )}
            </div>

            {/* Main Content Grid */}
            <div className="container mx-auto px-2 py-4">
                {isWorksLoading ? (
                    <div className="flex justify-center py-20">
                        <Loader2 className="w-8 h-8 animate-spin text-zinc-300" />
                    </div>
                ) : worksData?.length === 0 ? (
                    <div className="flex flex-col items-center justify-center py-20 text-zinc-400">
                        <Search className="w-12 h-12 mb-3 opacity-20" />
                        <p className="text-sm">没有找到相关内容</p>
                    </div>
                ) : (
                    <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 md:grid-cols-4">
                        {worksData?.map((work: any) => (
                            <Link
                                key={work.workId}
                                href={`/download/${work.workId}`}
                                className="group relative break-inside-avoid"
                            >
                                <div className="relative aspect-[3/4] rounded-lg overflow-hidden bg-zinc-200 dark:bg-zinc-800 mb-2">
                                    <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
                                    <img
                                        src={work.coverImageUrl}
                                        alt={work.workTitle}
                                        className="absolute inset-0 w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
                                        loading="lazy"
                                    />
                                    <div className="absolute top-2 right-2 bg-black/50 backdrop-blur-sm px-1.5 py-0.5 rounded text-[10px] text-white font-medium flex items-center gap-1">
                                        {work.imageCount > 0 && <span>{work.imageCount}图</span>}
                                        {work.gifCount > 0 && <span>{work.gifCount}动图</span>}
                                    </div>
                                    <div className="absolute bottom-2 left-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity z-10">
                                        <div className="flex items-center text-white text-xs gap-1">
                                            <ArrowRight className="w-3 h-3" />
                                            查看详情
                                        </div>
                                    </div>
                                </div>
                                <h3 className="text-sm font-medium text-zinc-900 dark:text-zinc-100 line-clamp-2 leading-tight mb-1">
                                    {work.workTitle || '无标题'}
                                </h3>
                                <div className="flex items-center justify-between text-xs text-zinc-500 dark:text-zinc-400">
                                    <div className="flex items-center gap-1">
                                        <div className="w-4 h-4 rounded-full bg-zinc-200 dark:bg-zinc-700 flex-shrink-0 overflow-hidden flex items-center justify-center">
                                            <User className="w-3 h-3 text-zinc-400" />
                                        </div>
                                        <span className="truncate max-w-[80px]">{work.authorNickname}</span>
                                    </div>
                                    <span className="text-[10px] opacity-70">
                                        {work.publishTime?.split('_')[0]}
                                    </span>
                                </div>
                            </Link>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}
