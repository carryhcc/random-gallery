'use client';

import { ChevronDown } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { systemApi } from '@/lib/api';
import type { Environment } from '@/lib/types';

const ENV_LABELS: Record<Environment, string> = {
    dev: '开发环境',
    test: '测试环境',
    prod: '生产环境',
};

interface EnvSelectorProps {
    className?: string;
}

export function EnvSelector({ className }: EnvSelectorProps) {
    const [isOpen, setIsOpen] = useState(false);
    const queryClient = useQueryClient();

    // 获取当前环境信息
    const { data: envInfo } = useQuery({
        queryKey: ['env-info'],
        queryFn: async () => {
            const response = await systemApi.getCurrentEnvInfo();
            return response.data;
        },
        refetchInterval: 30000,
    });

    // 切换环境
    const switchEnvMutation = useMutation({
        mutationFn: (env: Environment) => systemApi.switchEnvironment(env),
        onSuccess: () => {
            queryClient.cancelQueries();
            queryClient.removeQueries();
            queryClient.invalidateQueries({ queryKey: ['env-info'] });
            (window as any).showToast?.('环境切换成功', 'success');
        },
        onError: () => {
            (window as any).showToast?.('环境切换失败', 'error');
        },
    });

    const handleEnvChange = (env: Environment) => {
        setIsOpen(false);
        if (env !== envInfo?.env) {
            switchEnvMutation.mutate(env);
        }
    };

    // 点击外部关闭下拉菜单
    useEffect(() => {
        const handleClickOutside = (e: MouseEvent) => {
            const target = e.target as HTMLElement;
            if (!target.closest('.env-selector')) {
                setIsOpen(false);
            }
        };

        document.addEventListener('click', handleClickOutside);
        return () => document.removeEventListener('click', handleClickOutside);
    }, []);

    if (!envInfo) return null;

    return (
        <div className={`env-selector relative z-40 ${className || ''}`}>
            <button
                onClick={() => setIsOpen(!isOpen)}
                className="
                    flex items-center gap-1.5 px-3 py-1.5
                    rounded-full
                    bg-white dark:bg-zinc-800
                    border border-zinc-200 dark:border-zinc-700
                    shadow-sm
                    text-xs font-medium text-zinc-600 dark:text-zinc-300
                    transition-all duration-200
                    hover:bg-zinc-50 dark:hover:bg-zinc-700
                "
            >
                <div className={`w-1.5 h-1.5 rounded-full ${envInfo.env === 'prod' ? 'bg-green-500' : envInfo.env === 'dev' ? 'bg-blue-500' : 'bg-orange-500'}`}></div>
                <span>{ENV_LABELS[envInfo.env]}</span>
                <ChevronDown className={`h-3 w-3 transition-transform text-zinc-400 ${isOpen ? 'rotate-180' : ''}`} />
            </button>

            {isOpen && (
                <div className="
                    absolute top-full left-0 mt-2 w-32
                    rounded-lg
                    bg-white dark:bg-zinc-800
                    border border-zinc-200 dark:border-zinc-700
                    shadow-xl
                    overflow-hidden
                    animate-in fade-in-0 zoom-in-95
                ">
                    {(['dev', 'test', 'prod'] as Environment[]).map((env) => (
                        <button
                            key={env}
                            onClick={() => handleEnvChange(env)}
                            className={`
                                w-full px-3 py-2 text-left text-xs
                                transition-colors
                                hover:bg-zinc-50 dark:hover:bg-zinc-700
                                ${envInfo?.env === env ? 'text-blue-600 dark:text-blue-400 font-medium bg-blue-50 dark:bg-blue-900/20' : 'text-zinc-600 dark:text-zinc-300'}
                            `}
                        >
                            {ENV_LABELS[env]}
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
}
