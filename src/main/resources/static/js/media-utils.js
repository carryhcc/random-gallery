/**
 * 媒体格式处理工具（简化版）
 * - 前端直接使用后端返回的媒体 URL
 * - 支持 MOV/MP4/WebM/LIVP 等视频格式检测和处理
 * - 提供统一的媒体设置接口
 */

// 常量定义
const VIDEO_EXTENSIONS = ['.mov', '.mp4', '.webm', '.livp', '.m4v', '.avi'];
const IMAGE_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg'];

// Masonry 布局更新防抖计时器
let masonryLayoutTimer = null;
const MASONRY_DEBOUNCE_DELAY = 50;

/**
 * 防抖触发 Masonry 布局更新
 * 避免图片批量加载时频繁触发布局计算
 */
function triggerMasonryLayout() {
    if (masonryLayoutTimer) {
        clearTimeout(masonryLayoutTimer);
    }
    masonryLayoutTimer = setTimeout(doMasonryLayout, MASONRY_DEBOUNCE_DELAY);
}

/**
 * 执行 Masonry 布局更新
 * 尝试多种方式查找并更新 Masonry 实例
 */
function doMasonryLayout() {
    try {
        // 缓存 DOM 查询
        const masonryGrids = document.querySelectorAll('.masonry-grid');
        if (!masonryGrids.length) return;

        // 方式 1: 通过 Masonry.data 获取实例并更新布局
        if (typeof Masonry !== 'undefined' && Masonry.data) {
            masonryGrids.forEach(function (grid) {
                const msnry = Masonry.data(grid);
                if (msnry) {
                    msnry.layout();
                }
            });
        }

        // 方式 2: 查找全局的 Masonry 实例
        if (typeof msnry !== 'undefined' && msnry && typeof msnry.layout === 'function') {
            msnry.layout();
        }

        // 方式 3: 使用 imagesLoaded 触发布局
        if (typeof imagesLoaded === 'function' && typeof Masonry !== 'undefined' && Masonry.data) {
            masonryGrids.forEach(function (grid) {
                const msnry = Masonry.data(grid);
                if (msnry) {
                    imagesLoaded(grid).on('progress', function () {
                        msnry.layout();
                    });
                }
            });
        }
    } catch (error) {
        console.debug('Masonry 布局更新失败（可能页面不使用 Masonry）:', error);
    }
}

/**
 * 为 img 元素设置源
 * @param {HTMLImageElement} imgElement - img 元素
 * @param {string} imageUrl - 图片 URL
 * @returns {Promise<void>}
 */
async function setImageSrc(imgElement, imageUrl) {
    return new Promise((resolve) => {
        // 先检查图片是否已缓存（避免竞态条件）
        if (imgElement.complete && imgElement.src === imageUrl) {
            triggerMasonryLayout();
            resolve();
            return;
        }

        // 设置事件监听器
        imgElement.onload = function () {
            this.onload = null;
            triggerMasonryLayout();
            resolve();
        };

        imgElement.onerror = function (error) {
            console.warn('图片加载失败:', imageUrl, error);
            this.onerror = null;
            this.src = '/icons/404.svg';
            triggerMasonryLayout();
            resolve();
        };

        // 设置图片源
        imgElement.src = imageUrl;

        // 再次检查（处理缓存图片 onload 不触发的情况）
        if (imgElement.complete) {
            imgElement.onload = null;
            triggerMasonryLayout();
            resolve();
        }
    });
}

/**
 * 检测 URL 是否为视频文件
 * @param {string} url - 媒体 URL
 * @returns {boolean} 是否为视频格式
 */
function isVideoFile(url) {
    if (!url || typeof url !== 'string') return false;
    const lowerUrl = url.toLowerCase();
    return VIDEO_EXTENSIONS.some(ext => lowerUrl.endsWith(ext));
}

/**
 * 检测 URL 是否为图片文件
 * @param {string} url - 媒体 URL
 * @returns {boolean} 是否为图片格式
 */
function isImageFile(url) {
    if (!url || typeof url !== 'string') return false;
    const lowerUrl = url.toLowerCase();
    return IMAGE_EXTENSIONS.some(ext => lowerUrl.endsWith(ext));
}

/**
 * 获取媒体类型
 * @param {string} url - 媒体 URL
 * @returns {string} 媒体类型：'image', 'video', 或 'unknown'
 */
function getMediaType(url) {
    if (!url || typeof url !== 'string') return 'unknown';
    if (isVideoFile(url)) return 'video';
    if (isImageFile(url)) return 'image';
    return 'unknown';
}

/**
 * 为 video 元素设置源
 * @param {HTMLVideoElement} videoElement - video 元素
 * @param {string} videoUrl - 视频 URL
 * @returns {Promise<void>}
 */
async function setVideoSrc(videoElement, videoUrl) {
    videoElement.src = videoUrl;

    if (!videoElement.hasAttribute('preload')) {
        videoElement.preload = 'metadata';
    }
    if (!videoElement.hasAttribute('muted')) {
        videoElement.muted = true;
    }
}

/**
 * 智能设置媒体源（自动判断图片或视频）
 * @param {HTMLElement} element - img 或 video 元素
 * @param {string} mediaUrl - 媒体 URL
 * @returns {Promise<void>}
 */
async function setMediaSrc(element, mediaUrl) {
    const mediaType = getMediaType(mediaUrl);
    const tagName = element.tagName.toLowerCase();

    if (mediaType === 'video') {
        if (tagName === 'img') {
            console.warn('检测到视频 URL 但元素是 <img>，建议使用 <video> 元素:', mediaUrl);
            element.alt = '视频内容（需要使用 video 标签）';
            return;
        }
        await setVideoSrc(element, mediaUrl);
    } else if (mediaType === 'image') {
        if (tagName === 'video') {
            console.warn('检测到图片 URL 但元素是 <video>，建议使用 <img> 元素:', mediaUrl);
            return;
        }
        await setImageSrc(element, mediaUrl);
    } else {
        console.warn('未知的媒体类型:', mediaUrl);
        if (tagName === 'img') {
            await setImageSrc(element, mediaUrl);
        } else if (tagName === 'video') {
            await setVideoSrc(element, mediaUrl);
        }
    }
}
