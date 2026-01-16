import { apiClient } from './api-client';
import type { ApiResponse, Picture, PictureGroup, EnvInfo, Environment, XhsAuthor, XhsTag } from './types';

// System API
export const systemApi = {
    // Get current environment info
    getCurrentEnvInfo: async (): Promise<ApiResponse<EnvInfo>> => {
        return apiClient.get('/system/env/currentInfo');
    },

    // Switch environment
    switchEnvironment: async (env: Environment): Promise<ApiResponse<string>> => {
        return apiClient.get(`/system/env/${env}`);
    },
};

// Picture API
export const pictureApi = {
    // Get random picture
    getRandomPicture: async (): Promise<ApiResponse<Picture>> => {
        return apiClient.get('/pic/random/one');
    },

    // Get picture list by query
    getPictureList: async (params: { groupId?: number; pageIndex?: number; pageSize?: number }): Promise<ApiResponse<Picture[]>> => {
        return apiClient.post('/pic/list', params);
    },
};

// Group API  
export const groupApi = {
    // Get random group info
    getRandomGroupInfo: async (groupId?: number): Promise<ApiResponse<PictureGroup>> => {
        const url = groupId ? `/group/randomGroupInfo?groupId=${groupId}` : '/group/randomGroupInfo';
        return apiClient.get(url);
    },

    // Get group list with pagination
    getGroupList: async (params: { page?: number; pageSize?: number; groupName?: string }): Promise<ApiResponse<any>> => {
        return apiClient.post('/group/list', {
            ...params,
            pageIndex: params.page, // 后端 PageQuery 使用 pageIndex 字段
            page: undefined, // 移除 page 字段避免混淆
            groupName: params.groupName || undefined, // 添加搜索参数
        });
    },

    // Load more groups (for gallery)
    loadMore: async (page: number, refresh?: boolean): Promise<ApiResponse<any>> => {
        const url = `/group/loadMore?page=${page}${refresh ? '&refresh=true' : ''}`;
        return apiClient.get(url);
    },
};

// Download API (XHS Works)
export const downloadApi = {
    // Get work list
    getWorkList: async (params: any): Promise<ApiResponse<any>> => {
        let url = `/xhsWork/list?page=${params.page || 1}`;
        if (params.authorId) url += `&authorId=${params.authorId}`;
        if (params.tags) url += `&tags=${params.tags}`;
        if (params.search) url += `&search=${params.search}`;
        return apiClient.get(url);
    },

    // Get work detail
    getWorkDetail: async (workId: string): Promise<ApiResponse<any>> => {
        return apiClient.get(`/xhsWork/detail/${encodeURIComponent(workId)}`);
    },

    // Add download task
    addDownloadTask: async (url: string): Promise<ApiResponse<any>> => {
        return apiClient.post('/download/xhs', { url });
    },

    // Get all GIF IDs
    getAllGifIds: async (): Promise<ApiResponse<number[]>> => {
        return apiClient.get('/xhsWork/allGifIds');
    },

    // Get GIF by ID
    getGifById: async (id: number): Promise<ApiResponse<any>> => {
        return apiClient.get(`/xhsWork/gifById/${id}`);
    },

    // Get authors list
    getAuthors: async (): Promise<ApiResponse<XhsAuthor[]>> => {
        return apiClient.get('/xhsWork/authors');
    },

    // Get tags list
    getTags: async (): Promise<ApiResponse<XhsTag[]>> => {
        return apiClient.get('/xhsWork/tags');
    },

    // Delete work
    deleteWork: async (workId: string): Promise<ApiResponse<any>> => {
        return apiClient.delete(`/xhsWork/${workId}`);
    },

    // Delete media
    deleteMedia: async (mediaId: number): Promise<ApiResponse<any>> => {
        return apiClient.delete(`/xhsWork/media/${mediaId}`);
    },
};
