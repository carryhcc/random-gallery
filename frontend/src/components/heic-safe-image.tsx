'use client';

import Image, { ImageProps } from 'next/image';
import { useHeicConverter } from '@/hooks/use-heic-converter';
import { Loader2 } from 'lucide-react';

interface HeicSafeImageProps extends Omit<ImageProps, 'src'> {
    src: string;
    showLoader?: boolean;
}

/**
 * Image component that automatically handles HEIC format conversion
 */
export function HeicSafeImage({ src, showLoader = true, alt, ...props }: HeicSafeImageProps) {
    const { convertedUrl, isConverting } = useHeicConverter(src);

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
