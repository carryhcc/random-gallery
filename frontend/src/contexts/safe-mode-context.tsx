'use client';

import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';

interface SafeModeContextType {
    isSafeMode: boolean;
    toggleSafeMode: () => void;
}

const SafeModeContext = createContext<SafeModeContextType | undefined>(undefined);

const SAFE_MODE_KEY = 'safe-mode-enabled';

export function SafeModeProvider({ children }: { children: ReactNode }) {
    const [isSafeMode, setIsSafeMode] = useState(false);
    const [isClient, setIsClient] = useState(false);

    // 客户端挂载后读取 localStorage
    useEffect(() => {
        setIsClient(true);
        const saved = localStorage.getItem(SAFE_MODE_KEY);
        if (saved !== null) {
            setIsSafeMode(saved === 'true');
        }
    }, []);

    const toggleSafeMode = () => {
        setIsSafeMode((prev) => {
            const newValue = !prev;
            if (isClient) {
                localStorage.setItem(SAFE_MODE_KEY, String(newValue));
            }
            return newValue;
        });
    };

    return (
        <SafeModeContext.Provider value={{ isSafeMode, toggleSafeMode }}>
            {children}
        </SafeModeContext.Provider>
    );
}

export function useSafeMode() {
    const context = useContext(SafeModeContext);
    if (context === undefined) {
        // 在开发环境抛出错误，但在生产环境返回默认值以避免崩溃
        if (process.env.NODE_ENV === 'development') {
            console.error('useSafeMode must be used within a SafeModeProvider');
        }
        // 返回默认值，避免页面崩溃
        return { isSafeMode: false, toggleSafeMode: () => { } };
    }
    return context;
}
