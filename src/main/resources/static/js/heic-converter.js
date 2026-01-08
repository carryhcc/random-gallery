/**
 * 媒体格式转换和处理工具
 * - 支持 HEIC/HEIF 图片格式转换（使用 heic2any 库转换为 JPEG）
 * - 支持 MOV/MP4/WebM/LIVP 等视频格式检测和处理
 * - 提供统一的媒体设置接口
 */

/**
 * 检测 URL 是否为 HEIC 格式图片
 * @param {string} url - 图片 URL
 * @returns {boolean} 是否为 HEIC 格式
 */
function isHeicImage(url) {
    if (!url || typeof url !== 'string') return false;
    const lowerUrl = url.toLowerCase();
    return lowerUrl.endsWith('.heic') || lowerUrl.endsWith('.heif');
}

/**
 * 将 HEIC 图片 URL 转换为可显示的 JPEG 格式
 * @param {string} imageUrl - 原始图片 URL
 * @returns {Promise<string>} 转换后的图片 URL (Blob URL) 或原始 URL
 */
async function convertImageSrc(imageUrl) {
    // 如果不是 HEIC 格式，直接返回原始 URL
    if (!isHeicImage(imageUrl)) {
        return imageUrl;
    }

    // 检查 heic2any 库是否已加载
    if (typeof heic2any === 'undefined') {
        console.warn('heic2any 库未加载，无法转换 HEIC 图片，将使用原始 URL');
        return imageUrl;
    }

    try {
        console.log('检测到 HEIC 格式图片，开始转换:', imageUrl);

        // 下载 HEIC 图片
        const response = await fetch(imageUrl);
        if (!response.ok) {
            throw new Error('图片下载失败: ' + response.status);
        }

        const blob = await response.blob();

        // 使用 heic2any 转换为 JPEG
        const convertedBlob = await heic2any({
            blob: blob,
            toType: 'image/jpeg',
            quality: 0.9
        });

        // 创建 Blob URL
        const blobUrl = URL.createObjectURL(convertedBlob);
        console.log('HEIC 图片转换成功:', imageUrl, '->', blobUrl);

        return blobUrl;
    } catch (error) {
        console.error('HEIC 图片转换失败:', imageUrl, error);
        console.warn('将使用原始 URL 作为回退方案');
        return imageUrl;
    }
}

/**
 * 为 img 元素设置源，自动处理 HEIC 转换
 * @param {HTMLImageElement} imgElement - img 元素
 * @param {string} imageUrl - 图片 URL
 * @returns {Promise<void>}
 */
async function setImageSrc(imgElement, imageUrl) {
    // 如果是 HEIC 格式，在转换期间显示加载占位符
    if (isHeicImage(imageUrl)) {
        // 设置加载中的占位图（使用 SVG data URI）
        imgElement.src = getLoadingPlaceholder();
        // 添加加载中的 CSS 类（如果页面有定义相关样式）
        imgElement.classList.add('heic-loading');
    }

    // 执行转换
    const convertedUrl = await convertImageSrc(imageUrl);

    // 移除加载状态
    imgElement.classList.remove('heic-loading');

    // 创建一个 Promise 来等待图片加载完成
    return new Promise((resolve, reject) => {
        // 添加加载成功事件监听
        imgElement.onload = function () {
            // 图片加载完成后，触发 Masonry 布局更新
            triggerMasonryLayout();
            resolve();
        };

        // 添加加载失败事件监听
        imgElement.onerror = function (error) {
            console.warn('图片加载失败:', convertedUrl, error);
            // 即使失败也触发布局更新，避免布局一直错乱
            triggerMasonryLayout();
            resolve(); // 不 reject，避免中断流程
        };

        // 设置图片源
        imgElement.src = convertedUrl;

        // 如果图片已经缓存，onload 可能不会触发，需要检查
        if (imgElement.complete) {
            imgElement.onload = null;
            triggerMasonryLayout();
            resolve();
        }
    });
}

/**
 * 获取加载中的占位符图片（SVG data URI）
 * @returns {string} 加载中占位符的 data URI
 */
function getLoadingPlaceholder() {
    // 创建一个简单的 SVG 加载动画
    const svg = `
        <svg width="200" height="200" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">
            <rect width="200" height="200" fill="#f0f0f0"/>
            <circle cx="100" cy="100" r="20" fill="none" stroke="#999" stroke-width="3" stroke-dasharray="31.4 31.4">
                <animateTransform 
                    attributeName="transform" 
                    type="rotate" 
                    from="0 100 100" 
                    to="360 100 100" 
                    dur="1s" 
                    repeatCount="indefinite"/>
            </circle>
            <text x="100" y="140" text-anchor="middle" font-family="Arial, sans-serif" font-size="12" fill="#666">
                转换中...
            </text>
        </svg>
    `.trim();

    // 将 SVG 转换为 data URI
    const encoded = encodeURIComponent(svg);
    return `data:image/svg+xml,${encoded}`;
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

    // 检测图片格式（包括 HEIC）
    const lowerUrl = url.toLowerCase();
    const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg', '.heic', '.heif'];
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
