'use client';

import { Moon, Sun } from 'lucide-react';
import { useTheme } from 'next-themes';
import { useEffect, useState } from 'react';

interface ThemeToggleProps {
    className?: string;
}

export function ThemeToggle({ className }: ThemeToggleProps) {
    const { theme, setTheme } = useTheme();
    const [mounted, setMounted] = useState(false);

    useEffect(() => {
        setMounted(true);
    }, []);

    if (!mounted) {
        return null;
    }

    const toggleTheme = () => {
        setTheme(theme === 'dark' ? 'light' : 'dark');
    };

    return (
        <button
            onClick={toggleTheme}
            className={`
                flex h-10 w-10 items-center justify-center
                rounded-full
                bg-white dark:bg-zinc-800
                border border-zinc-200 dark:border-zinc-700
                shadow-sm
                transition-all duration-200
                hover:bg-zinc-50 dark:hover:bg-zinc-700
                active:scale-95
                ${className || ''}
            `}
            title={theme === 'dark' ? '切换到浅色模式' : '切换到深色模式'}
        >
            {theme === 'dark' ? (
                <Sun className="h-4 w-4 text-amber-500" />
            ) : (
                <Moon className="h-4 w-4 text-blue-600" />
            )}
        </button>
    );
}
