// API Base URL configuration
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || '/api';

// Environment types
export type Environment = 'dev' | 'test' | 'prod';

// API Response type
export interface ApiResponse<T = any> {
    code: number;
    message: string;
    data: T;
}

// Picture type
export interface Picture {
    id: number;
    groupId: number;
    picUrl: string;
    timestamp?: string;
}

// Group type
export interface PictureGroup {
    groupId: number;
    groupName?: string;
    picCount?: number;
    groupCount?: number;
    groupUrl?: string; // Cover image for the group
}

// Environment Info type
export interface EnvInfo {
    env: Environment;
    groupCount: number;
    picCount: number;
}

// XHS Work types
export interface XhsWork {
    workId: string;
    workTitle: string;
    authorId: string;
    authorName: string;
    createTime?: string;
    updateTime?: string;
}

// XHS Author type
export interface XhsAuthor {
    authorId: string;
    authorNickname: string;
    authorUrl: string;
    workCount: number;
}

// XHS Tag type
export interface XhsTag {
    id: number;
    tagName: string;
    workCount: number;
}
