'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { SafeModeProvider } from '@/contexts/safe-mode-context';
import { useState, type ReactNode } from 'react';

export function QueryProvider({ children }: { children: ReactNode }) {
    const [queryClient] = useState(
        () =>
            new QueryClient({
                defaultOptions: {
                    queries: {
                        staleTime: 60 * 1000, // 1 minute
                        refetchOnWindowFocus: false,
                    },
                },
            })
    );

    return (
        <QueryClientProvider client={queryClient}>
            <SafeModeProvider>
                {children}
            </SafeModeProvider>
        </QueryClientProvider>
    );
}
