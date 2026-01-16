'use client';

import { useEffect, useState } from 'react';

export function Toaster() {
    const [toasts, setToasts] = useState<Array<{ id: string; message: string; type: 'success' | 'error' | 'info' }>>([]);

    useEffect(() => {
        // 全局toast函数
        (window as any).showToast = (message: string, type: 'success' | 'error' | 'info' = 'success') => {
            const id = Math.random().toString(36).substring(7);
            setToasts((prev) => [...prev, { id, message, type }]);

            setTimeout(() => {
                setToasts((prev) => prev.filter((t) => t.id !== id));
            }, 3000);
        };
    }, []);

    return (
        <div className="fixed top-6 left-1/2 -translate-x-1/2 z-[100] flex flex-col gap-2 w-full max-w-[300px] pointer-events-none">
            {toasts.map((toast) => (
                <div
                    key={toast.id}
                    className={`
                        mx-auto px-4 py-2.5 
                        rounded-full 
                        bg-white/90 dark:bg-zinc-800/90
                        backdrop-blur-md
                        border border-zinc-200/50 dark:border-zinc-700/50
                        shadow-xl shadow-zinc-200/20 dark:shadow-black/20
                        text-sm font-medium
                        text-zinc-900 dark:text-zinc-100
                        flex items-center gap-2.5
                        animate-in slide-in-from-top-2 zoom-in-95 fade-in-0
                        pointer-events-auto
                    `}
                >
                    {toast.type === 'success' && <div className="w-2 h-2 rounded-full bg-green-500 shrink-0" />}
                    {toast.type === 'error' && <div className="w-2 h-2 rounded-full bg-red-500 shrink-0" />}
                    {toast.type === 'info' && <div className="w-2 h-2 rounded-full bg-blue-500 shrink-0" />}
                    {toast.message}
                </div>
            ))}
        </div>
    );
}
