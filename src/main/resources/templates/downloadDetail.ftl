<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>作品详情 - 随机图库</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Poppins:wght@600;700&display=swap"
          rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <link rel="stylesheet" href="/css/style.css">
    <script src="/js/theme.js"></script>
</head>
<body>

<!-- 导航栏 -->
<header class="navbar">
    <div class="navbar-content">
        <div class="navbar-brand">
            <i class="fas fa-image"></i>
            <span>作品详情</span>
        </div>
        <div class="navbar-actions">
            <button class="btn btn-secondary btn-sm" onclick="window.location.href='/download'">
                <i class="fas fa-arrow-left"></i>
                <span class="hidden-mobile">返回列表</span>
            </button>
            <button class="btn btn-secondary btn-sm" onclick="window.location.href='/'">
                <i class="fas fa-home"></i>
                <span class="hidden-mobile">首页</span>
            </button>
        </div>
    </div>
</header>

<!-- 主内容 -->
<main class="container gallery-container">
    <div id="toast" class="toast"></div>

    <!-- 作品信息 -->
    <div id="detailHeader" class="detail-header animate-fade-in">
        <div class="loading">
            <div class="spinner"></div>
            <span>加载中...</span>
        </div>
    </div>

    <!-- 静态图片区 -->
    <div id="imagesSection"></div>

    <!-- 动图区 -->
    <div id="gifsSection"></div>
</main>

<!-- 图片预览模态框 -->
<!-- 图片查看器 -->
<div id="viewer" class="image-viewer">
    <button class="viewer-close" onclick="closeViewer()">
        <i class="fas fa-times"></i>
    </button>
    <button class="viewer-nav viewer-prev" onclick="changeImage(-1)">
        <i class="fas fa-chevron-left"></i>
    </button>
    <button class="viewer-nav viewer-next" onclick="changeImage(1)">
        <i class="fas fa-chevron-right"></i>
    </button>
    <div class="viewer-content">
        <img id="viewer-img" src="" alt="大图预览">
    </div>
</div>

<script>
    const workId = '${workId!}';
    const detailHeader = document.getElementById('detailHeader');
    const imagesSection = document.getElementById('imagesSection');
    const gifsSection = document.getElementById('gifsSection');
    const toast = document.getElementById('toast');
    const imageModal = document.getElementById('imageModal');
    const modalImage = document.getElementById('modalImage');
    const modalCounter = document.getElementById('modalCounter');
    let toastTimer;
    
    // 图片轮播相关
    let allImages = []; // 存储所有图片 URL
    let currentImageIndex = 0; // 当前查看的图片索引
    
    // 触摸滑动相关
    let touchStartX = 0;
    let touchEndX = 0;

    function showToast(message, type = 'success') {
        clearTimeout(toastTimer);
        toast.textContent = message;
        toast.className = 'toast show ' + type;
        toastTimer = setTimeout(() => {
            toast.className = 'toast';
        }, 3000);
    }

    // 图片查看器逻辑
    function openViewer(index) {
        currentImageIndex = index;
        updateViewerImage();
        const viewer = document.getElementById('viewer');
        viewer.classList.add('visible');
        document.body.style.overflow = 'hidden'; // 禁止背景滚动
    }

    function closeViewer() {
        const viewer = document.getElementById('viewer');
        viewer.classList.remove('visible');
        document.body.style.overflow = '';
    }

    function changeImage(direction) {
        let newIndex = currentImageIndex + direction;
        // 循环播放
        if (newIndex < 0) newIndex = allImages.length - 1;
        if (newIndex >= allImages.length) newIndex = 0;
        
        currentImageIndex = newIndex;
        updateViewerImage();
    }

    function updateViewerImage() {
        if (allImages.length === 0) return;
        const imgUrl = allImages[currentImageIndex];
        const viewerImg = document.getElementById('viewer-img');
        
        // 淡入效果
        viewerImg.style.opacity = '0.5';
        viewerImg.src = imgUrl;
        setTimeout(() => {
            viewerImg.style.opacity = '1';
        }, 50);
    }

    // 键盘导航
    document.addEventListener('keydown', (e) => {
        const viewer = document.getElementById('viewer');
        if (!viewer.classList.contains('visible')) return;
        
        if (e.key === 'Escape') closeViewer();
        if (e.key === 'ArrowLeft') changeImage(-1);
        if (e.key === 'ArrowRight') changeImage(1);
    });

    // 下载图片
    async function downloadMedia(url, index, type = 'image') {
        try {
            showToast('正在下载...', 'info');
            const response = await fetch(url);
            if (!response.ok) throw new Error('下载失败');
            
            const blob = await response.blob();
            const blobUrl = URL.createObjectURL(blob);
            
            // 根据类型确定文件扩展名
            const ext = type === 'gif' ? '.mp4' : '.jpg';
            const filename = 'media_' + index + ext;
            
            const a = document.createElement('a');
            a.href = blobUrl;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            
            // 释放 Blob URL
            setTimeout(() => URL.revokeObjectURL(blobUrl), 100);
            showToast('下载成功', 'success');
        } catch (error) {
            console.error('下载失败:', error);
            showToast('下载失败，请重试', 'error');
        }
    }

    // 处理标签：用空格分割，生成带颜色的标签
    function processTags(tags) {
        if (!tags || tags.trim() === '') return '';
        const tagArray = tags.trim().split(/\s+/);
        return tagArray.map(function(tag) {
            return '<span class="tag">' + tag + '</span>';
        }).join('');
    }

    // 处理描述:去掉 # 和 [话题]，生成带颜色的标签
    function processDescription(desc) {
        if (!desc || desc.trim() === '') return '';
        // 匹配 #xxx[话题]# 或 ##xxx[话题]# 格式
        const regex = /##+([^#\[]+)\[话题\]#+/g;
        const tags = [];
        let match;
        while ((match = regex.exec(desc)) !== null) {
            tags.push(match[1].trim());
        }
        if (tags.length === 0) return '';
        return tags.map(function(tag) {
            return '<span class="tag">' + tag + '</span>';
        }).join('');
    }

    // 初始化 Live Photo 交互效果
    function initializeLivePhotos() {
        const livePhotos = document.querySelectorAll('.live-photo');
        
        livePhotos.forEach(function(livePhoto) {
            const videoId = livePhoto.getAttribute('data-video-id');
            const video = document.getElementById(videoId);
            
            if (!video) return;
            
            // 鼠标进入时播放
            livePhoto.addEventListener('mouseenter', function() {
                video.currentTime = 0; // 从头开始
                video.play().catch(function(err) {
                    console.log('播放失败:', err);
                });
                livePhoto.classList.add('playing');
            });
            
            // 鼠标离开时暂停并重置
            livePhoto.addEventListener('mouseleave', function() {
                video.pause();
                video.currentTime = 0; // 重置到第一帧
                livePhoto.classList.remove('playing');
            });
            
            // 触摸设备支持
            livePhoto.addEventListener('touchstart', function(e) {
                if (video.paused) {
                    video.currentTime = 0;
                    video.play().catch(function(err) {
                        console.log('播放失败:', err);
                    });
                    livePhoto.classList.add('playing');
                } else {
                    video.pause();
                    video.currentTime = 0;
                    livePhoto.classList.remove('playing');
                }
            });
        });
    }

    // 加载作品详情
    async function loadDetail() {
        if (!workId) {
            detailHeader.innerHTML = '<div class="empty-section"><i class="fas fa-exclamation-circle"></i><p>缺少作品ID</p></div>';
            return;
        }

        try {
            const response = await fetch('/api/xhsWork/detail/' + encodeURIComponent(workId));
            const result = await response.json();

            if (result.code === 200 && result.data) {
                const data = result.data;
                const base = data.baseInfo;
                const images = data.images || [];
                const gifs = data.gifs || [];

                // 处理标签和描述
                const tagsHtml = processTags(base.workTags);
                const descHtml = processDescription(base.workDescription);

                // 渲染头部信息
                detailHeader.innerHTML =
                    '<div class="detail-title">' + (base.workTitle || '无标题') + '</div>' +
                    '<div class="detail-meta">' +
                        '<div class="meta-item">' +
                            '<i class="fas fa-user"></i>' +
                            '<a href="' + (base.authorUrl || '#') + '" target="_blank" class="author-link">' + (base.authorNickname || '未知作者') + '</a>' +
                        '</div>' +
                        '<div class="meta-item">' +
                            '<i class="fas fa-calendar"></i>' +
                            '<span>' + (base.publishTime || '未知时间') + '</span>' +
                        '</div>' +
                        '<div class="meta-item">' +
                            '<i class="fas fa-heart"></i>' +
                            '<span>' + (base.likeCount || '0') + ' 点赞</span>' +
                        '</div>' +
                        '<div class="meta-item">' +
                            '<i class="fas fa-star"></i>' +
                            '<span>' + (base.collectCount || '0') + ' 收藏</span>' +
                        '</div>' +
                        '<div class="meta-item">' +
                            '<i class="fas fa-comment"></i>' +
                            '<span>' + (base.commentCount || '0') + ' 评论</span>' +
                        '</div>' +
                    '</div>' +
                    (descHtml ? '<div class="work-description">' + descHtml + '</div>' : '') +
                    (tagsHtml ? '<div class="work-description">' + tagsHtml + '</div>' : '');

                // 填充图片数组用于轮播
                allImages = images.map(img => img.mediaUrl);
                
                // 渲染图片区
                if (images.length > 0) {
                    // 创建标题
                    const sectionTitle = document.createElement('h2');
                    sectionTitle.className = 'section-title';
                    sectionTitle.innerHTML = '<i class="fas fa-image"></i> 图片 (' + images.length + ')';
                    
                    // 创建图片网格 (改为瀑布流)
                    const mediaGrid = document.createElement('div');
                    mediaGrid.className = 'masonry-grid'; // 使用新的瀑布流类
                    
                    images.forEach(function(img, index) {
                        const mediaItem = document.createElement('div');
                        mediaItem.className = 'media-item';
                        
                        const imgElement = document.createElement('img');
                        imgElement.src = img.mediaUrl;
                        imgElement.alt = '图片 ' + (index + 1);
                        imgElement.loading = 'lazy';
                        
                        // 使用事件监听器打开新的查看器 (监听整个 item)
                        const openViewerHandler = function() {
                            openViewer(index);
                        };
                        
                        // 绑定到 mediaItem 而不是 img，确保点击遮罩也能触发
                        mediaItem.addEventListener('click', openViewerHandler);
                        
                        const overlay = document.createElement('div');
                        overlay.className = 'media-overlay';
                        
                        const downloadBtn = document.createElement('button');
                        downloadBtn.className = 'download-btn';
                        downloadBtn.innerHTML = '<i class="fas fa-download"></i>';
                        downloadBtn.addEventListener('click', function(e) {
                            e.stopPropagation(); // 阻止冒泡，防止触发 openViewer
                            downloadMedia(img.mediaUrl, index + 1, 'image');
                        });
                        
                        overlay.appendChild(downloadBtn);
                        mediaItem.appendChild(imgElement);
                        mediaItem.appendChild(overlay);
                        mediaGrid.appendChild(mediaItem);
                    });
                    
                    imagesSection.innerHTML = '';
                    imagesSection.appendChild(sectionTitle);
                    imagesSection.appendChild(mediaGrid);
                } else {
                    imagesSection.innerHTML =
                        '<h2 class="section-title">' +
                            '<i class="fas fa-image"></i>' +
                            '图片' +
                        '</h2>' +
                        '<div class="empty-section">' +
                            '<i class="fas fa-image"></i>' +
                            '<p>暂无图片</p>' +
                        '</div>';
                }

                // 渲染动图区（Live Photo 效果）
                if (gifs.length > 0) {
                    // 创建标题
                    const sectionTitle = document.createElement('h2');
                    sectionTitle.className = 'section-title';
                    sectionTitle.innerHTML = '<i class="fas fa-film"></i> 实况照片 (' + gifs.length + ')';
                    
                    // 创建图片网格 (改为瀑布流)
                    const mediaGrid = document.createElement('div');
                    mediaGrid.className = 'masonry-grid'; // 使用新的瀑布流类
                    
                    gifs.forEach(function(gif, index) {
                        const videoId = 'live-video-' + index;
                        const mediaItem = document.createElement('div');
                        mediaItem.className = 'media-item live-photo';
                        mediaItem.setAttribute('data-video-id', videoId);
                        
                        const badge = document.createElement('span');
                        badge.className = 'live-photo-badge';
                        badge.innerHTML = '<i class="fas fa-circle"></i> LIVE';
                        
                        const hint = document.createElement('span');
                        hint.className = 'live-photo-overlay-hint';
                        hint.textContent = '悬停播放';
                        
                        const video = document.createElement('video');
                        video.id = videoId;
                        video.src = gif.mediaUrl;
                        video.loop = true;
                        video.muted = true;
                        video.preload = 'metadata';
                        
                        // 为视频添加点击事件也打开查看器 (如果需要支持 GIF 预览)
                        video.parentElement.addEventListener('click', function(e) {
                             // 只有点击不是下载按钮时才打开预览
                             if (!e.target.closest('.download-btn')) {
                                // 注意：这里需要考虑 gifs 和 images 的索引合并问题，
                                // 如果 gifs 单独展示，需要把 gifs 也加入 allImages 或者单独处理
                                // 这里简化处理，暂不预览 GIF 视频，或者需要将 GIF 也加入 allImages array
                                // 根据当前逻辑 allImages 只填充了 images。
                                // 如果用户也想预览 GIF (作为视频或图片帧)，需要扩展逻辑。
                                // 目前 allImages 只包含 images.map，所以这里暂时不添加 click 预览，或者只能预览图片。
                                // 用户的需求主要是"图片可以点击放大"，GIF 已经是播放的视频了，通常不需要"放大查看静态图"。
                                // 如果要全屏播放视频，那是另一个需求。
                                // 所以这里暂时不给 GIF 添加 openViewer，除非用户明确要求。
                             }
                        });
                        
                        const overlay = document.createElement('div');
                        overlay.className = 'media-overlay';
                        
                        const downloadBtn = document.createElement('button');
                        downloadBtn.className = 'download-btn';
                        downloadBtn.innerHTML = '<i class="fas fa-download"></i>';
                        downloadBtn.addEventListener('click', function(e) {
                            e.stopPropagation();
                            downloadMedia(gif.mediaUrl, index + 1, 'gif');
                        });
                        
                        overlay.appendChild(downloadBtn);
                        mediaItem.appendChild(badge);
                        mediaItem.appendChild(hint);
                        mediaItem.appendChild(video);
                        mediaItem.appendChild(overlay);
                        mediaGrid.appendChild(mediaItem);
                    });
                    
                    gifsSection.innerHTML = '';
                    gifsSection.appendChild(sectionTitle);
                    gifsSection.appendChild(mediaGrid);
                    
                    // 初始化 Live Photo 交互
                    initializeLivePhotos();
                } else {
                    gifsSection.innerHTML =
                        '<h2 class="section-title">' +
                            '<i class="fas fa-film"></i>' +
                            '实况照片' +
                        '</h2>' +
                        '<div class="empty-section">' +
                            '<i class="fas fa-film"></i>' +
                            '<p>暂无实况照片</p>' +
                        '</div>';
                }
            } else {
                detailHeader.innerHTML = '<div class="empty-section"><i class="fas fa-exclamation-circle"></i><p>' + (result.message || '加载失败') + '</p></div>';
            }
        } catch (error) {
            console.error('加载失败:', error);
            detailHeader.innerHTML = '<div class="empty-section"><i class="fas fa-exclamation-circle"></i><p>网络请求失败</p></div>';
        }
    }

    // 页面加载时获取详情
    document.addEventListener('DOMContentLoaded', loadDetail);
</script>
</body>
</html>
