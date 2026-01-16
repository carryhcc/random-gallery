import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Environment } from './types';

interface AppState {
    // Current environment
    currentEnv: Environment;
    setCurrentEnv: (env: Environment) => void;

    // Theme (will be managed by next-themes, but we keep it here for reference)
    theme: 'light' | 'dark' | 'system';
    setTheme: (theme: 'light' | 'dark' | 'system') => void;
}

export const useAppStore = create<AppState>()(
    persist(
        (set) => ({
            currentEnv: 'dev',
            setCurrentEnv: (env) => set({ currentEnv: env }),

            theme: 'system',
            setTheme: (theme) => set({ theme }),
        }),
        {
            name: 'app-storage',
        }
    )
);
