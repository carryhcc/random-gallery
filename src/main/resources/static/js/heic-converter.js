/**
 * 媒体格式处理工具（简化版）
 * - 后端已处理所有 HEIC 转换，前端直接使用 URL
 * - 支持 MOV/MP4/WebM/LIVP 等视频格式检测和处理
 * - 提供统一的媒体设置接口
 */

/**
 * 为 img 元素设置源
 * @param {HTMLImageElement} imgElement - img 元素
 * @param {string} imageUrl - 图片 URL
 * @returns {Promise<void>}
 */
async function setImageSrc(imgElement, imageUrl) {
    // 创建一个 Promise 来等待图片加载完成
    return new Promise((resolve) => {
        // 添加加载成功事件监听
        imgElement.onload = function () {
            // 图片加载完成后，触发 Masonry 布局更新
            triggerMasonryLayout();
            resolve();
        };

        // 添加加载失败事件监听
        imgElement.onerror = function (error) {
            console.warn('图片加载失败:', imageUrl, error);
            // 加载失败显示默认图
            this.onerror = null;
            this.src = '/icons/404.svg';
            // 即使失败也触发布局更新，避免布局一直错乱
            triggerMasonryLayout();
            resolve(); // 不 reject，避免中断流程
        };

        // 设置图片源
        imgElement.src = imageUrl;

        // 如果图片已经缓存，onload 可能不会触发，需要检查
        if (imgElement.complete) {
            imgElement.onload = null;
            triggerMasonryLayout();
            resolve();
        }
    });
}

/**
 * 触发 Masonry 布局更新
 * 尝试多种方式查找并更新 Masonry 实例
 */
function triggerMasonryLayout() {
    try {
        // 方式 1: 查找页面中所有的 Masonry 网格并更新布局
        const masonryGrids = document.querySelectorAll('.masonry-grid');
        masonryGrids.forEach(function (grid) {
            // 尝试获取 Masonry 实例（通过 Masonry.data）
            if (typeof Masonry !== 'undefined' && Masonry.data) {
                const msnry = Masonry.data(grid);
                if (msnry) {
                    msnry.layout();
                }
            }
        });

        // 方式 2: 查找全局的 Masonry 实例（某些页面可能存储在变量中）
        if (typeof msnry !== 'undefined' && msnry && typeof msnry.layout === 'function') {
            msnry.layout();
        }

        // 方式 3: 如果有 imagesLoaded，使用它触发布局
        if (typeof imagesLoaded === 'function') {
            masonryGrids.forEach(function (grid) {
                if (typeof Masonry !== 'undefined' && Masonry.data) {
                    const msnry = Masonry.data(grid);
                    if (msnry) {
                        imagesLoaded(grid).on('progress', function () {
                            msnry.layout();
                        });
                    }
                }
            });
        }
    } catch (error) {
        // 静默处理错误，避免影响图片加载
        console.debug('Masonry 布局更新失败（可能页面不使用 Masonry）:', error);
    }
}


/**
 * 检测 URL 是否为视频文件
 * @param {string} url - 媒体 URL
 * @returns {boolean} 是否为视频格式
 */
function isVideoFile(url) {
    if (!url || typeof url !== 'string') return false;
    const lowerUrl = url.toLowerCase();
    // 支持常见的视频格式：MOV, MP4, WebM, LIVP
    return lowerUrl.endsWith('.mov') ||
        lowerUrl.endsWith('.mp4') ||
        lowerUrl.endsWith('.webm') ||
        lowerUrl.endsWith('.livp') ||
        lowerUrl.endsWith('.m4v') ||
        lowerUrl.endsWith('.avi');
}

/**
 * 获取媒体类型
 * @param {string} url - 媒体 URL
 * @returns {string} 媒体类型：'image', 'video', 或 'unknown'
 */
function getMediaType(url) {
    if (!url || typeof url !== 'string') return 'unknown';

    if (isVideoFile(url)) {
        return 'video';
    }

    // 检测图片格式
    const lowerUrl = url.toLowerCase();
    const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg'];
    const isImage = imageExtensions.some(ext => lowerUrl.endsWith(ext));

    if (isImage) {
        return 'image';
    }

    return 'unknown';
}

/**
 * 为 video 元素设置源
 * @param {HTMLVideoElement} videoElement - video 元素
 * @param {string} videoUrl - 视频 URL
 * @returns {Promise<void>}
 */
async function setVideoSrc(videoElement, videoUrl) {
    try {
        // 对于视频文件，直接设置源
        // 浏览器原生支持 MOV, MP4, WebM 等格式
        videoElement.src = videoUrl;

        // 设置一些默认属性（如果未设置）
        if (!videoElement.hasAttribute('preload')) {
            videoElement.preload = 'metadata';
        }
        if (!videoElement.hasAttribute('muted')) {
            videoElement.muted = true; // 默认静音，避免自动播放问题
        }

        console.log('视频源已设置:', videoUrl);
    } catch (error) {
        console.error('设置视频源失败:', videoUrl, error);
        throw error;
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

    if (mediaType === 'video') {
        // 如果是视频但元素是 img，发出警告
        if (element.tagName.toLowerCase() === 'img') {
            console.warn('检测到视频 URL 但元素是 <img>，建议使用 <video> 元素:', mediaUrl);
            // 尝试转换为视频元素或显示占位符
            element.alt = '视频内容（需要使用 video 标签）';
            return;
        }

        await setVideoSrc(element, mediaUrl);
    } else if (mediaType === 'image') {
        // 如果是图片但元素是 video，发出警告
        if (element.tagName.toLowerCase() === 'video') {
            console.warn('检测到图片 URL 但元素是 <video>，建议使用 <img> 元素:', mediaUrl);
            return;
        }

        await setImageSrc(element, mediaUrl);
    } else {
        // 未知类型，尝试按元素类型处理
        console.warn('未知的媒体类型:', mediaUrl);
        if (element.tagName.toLowerCase() === 'img') {
            await setImageSrc(element, mediaUrl);
        } else if (element.tagName.toLowerCase() === 'video') {
            await setVideoSrc(element, mediaUrl);
        }
    }
}
