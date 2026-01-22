'use client';

import Image, { ImageProps } from 'next/image';
import { useHeicConverter } from '@/hooks/use-heic-converter';
import { useSafeMode } from '@/contexts/safe-mode-context';
import { Loader2 } from 'lucide-react';

const SAFE_IMAGE_URL = 'http://yanxuan.nosdn.127.net/1541445967645114dd75f6b0edc4762d.png';

interface HeicSafeImageProps extends Omit<ImageProps, 'src'> {
    src: string;
    showLoader?: boolean;
}

/**
 * Image component that automatically handles HEIC format conversion and safe mode
 */
export function HeicSafeImage({ src, showLoader = true, alt, ...props }: HeicSafeImageProps) {
    const { isSafeMode } = useSafeMode();

    // 如果是安全模式，直接使用安全图片，跳过 HEIC 转换
    const effectiveSrc = isSafeMode ? SAFE_IMAGE_URL : src;
    const { convertedUrl, isConverting } = useHeicConverter(effectiveSrc);

    if (isConverting && showLoader) {
        return (
            <div className="absolute inset-0 flex flex-col items-center justify-center bg-zinc-100 dark:bg-zinc-800 gap-2">
                <Loader2 className="h-5 w-5 animate-spin text-blue-600 dark:text-blue-400" />
                <span className="text-xs text-zinc-500 dark:text-zinc-400">转换中...</span>
            </div>
        );
    }

    if (!convertedUrl) {
        return (
            <div className="absolute inset-0 flex items-center justify-center bg-zinc-100 dark:bg-zinc-800">
                <span className="text-xs text-zinc-400">加载失败</span>
            </div>
        );
    }

    return <Image {...props} src={convertedUrl} alt={alt} />;
}
