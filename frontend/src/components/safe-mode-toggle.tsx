'use client';

import { useSafeMode } from '@/contexts/safe-mode-context';
import { Eye, EyeOff } from 'lucide-react';
import { useEffect, useState } from 'react';

/**
 * 安全模式切换按钮
 * 固定在页面右下角，用于切换是否显示真实图片
 */
export function SafeModeToggle() {
    const { isSafeMode, toggleSafeMode } = useSafeMode();
    const [mounted, setMounted] = useState(false);

    // 只在客户端渲染，避免 hydration mismatch
    useEffect(() => {
        setMounted(true);
    }, []);

    // 在服务端或未挂载时不渲染任何内容
    if (!mounted) {
        return null;
    }

    return (
        <button
            onClick={toggleSafeMode}
            className={`
                fixed bottom-6 right-6 z-50
                p-4 rounded-full shadow-lg
                transition-all duration-300
                active:scale-95
                ${isSafeMode
                    ? 'bg-blue-600 hover:bg-blue-700 text-white'
                    : 'bg-white dark:bg-zinc-800 hover:bg-zinc-100 dark:hover:bg-zinc-700 text-zinc-900 dark:text-zinc-100'
                }
            `}
            title={isSafeMode ? '安全模式开启' : '安全模式关闭'}
        >
            {isSafeMode ? (
                <EyeOff className="w-6 h-6" />
            ) : (
                <Eye className="w-6 h-6" />
            )}
        </button>
    );
}
