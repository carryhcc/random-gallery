'use client';

import { useSafeMode } from '@/contexts/safe-mode-context';
import { ImgHTMLAttributes } from 'react';

const SAFE_IMAGE_URL = 'http://yanxuan.nosdn.127.net/1541445967645114dd75f6b0edc4762d.png';

interface SafeImageProps extends ImgHTMLAttributes<HTMLImageElement> {
    src: string;
}

/**
 * 原生 img 标签的安全模式包装组件
 * 当安全模式开启时，自动替换图片源为安全图片
 */
export function SafeImage({ src, alt, ...props }: SafeImageProps) {
    const { isSafeMode } = useSafeMode();

    const displaySrc = isSafeMode ? SAFE_IMAGE_URL : src;

    return <img src={displaySrc} alt={alt} {...props} />;
}
