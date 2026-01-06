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
<div id="imageModal" class="modal">
    <button class="modal-close" onclick="closeModal()">
        <i class="fas fa-times"></i>
    </button>
    <img id="modalImage" class="modal-content" src="" alt="预览">
</div>

<script>
    const workId = '${workId!}';
    const detailHeader = document.getElementById('detailHeader');
    const imagesSection = document.getElementById('imagesSection');
    const gifsSection = document.getElementById('gifsSection');
    const toast = document.getElementById('toast');
    const imageModal = document.getElementById('imageModal');
    const modalImage = document.getElementById('modalImage');
    let toastTimer;

    function showToast(message, type = 'success') {
        clearTimeout(toastTimer);
        toast.textContent = message;
        toast.className = 'toast show ' + type;
        toastTimer = setTimeout(() => {
            toast.className = 'toast';
        }, 3000);
    }

    function openModal(imageUrl) {
        modalImage.src = imageUrl;
        imageModal.classList.add('show');
    }

    function closeModal() {
        imageModal.classList.remove('show');
    }

    // ESC 键关闭模态框
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeModal();
        }
    });

    // 点击背景关闭模态框
    imageModal.addEventListener('click', (e) => {
        if (e.target === imageModal) {
            closeModal();
        }
    });

    // 下载图片
    function downloadMedia(url, index) {
        const a = document.createElement('a');
        a.href = url;
        a.download = 'image_' + index + '.jpg';
        a.target = '_blank';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        showToast('开始下载', 'success');
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

                // 渲染图片区
                if (images.length > 0) {
                    imagesSection.innerHTML =
                        '<h2 class="section-title">' +
                            '<i class="fas fa-image"></i>' +
                            '图片 (' + images.length + ')' +
                        '</h2>' +
                        '<div class="media-grid">' +
                            images.map(function(img, index) {
                                return '<div class="media-item">' +
                                    '<img src="' + img.mediaUrl + '" alt="图片 ' + (index + 1) + '" loading="lazy" onclick="openModal(\'' + img.mediaUrl + '\')">' +
                                    '<div class="media-overlay">' +
                                        '<button class="download-btn" onclick="downloadMedia(\'' + img.mediaUrl + '\', ' + (index + 1) + ')">' +
                                            '<i class="fas fa-download"></i>' +
                                            '下载' +
                                        '</button>' +
                                    '</div>' +
                                '</div>';
                            }).join('') +
                        '</div>';
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
                    gifsSection.innerHTML =
                        '<h2 class="section-title">' +
                            '<i class="fas fa-film"></i>' +
                            '实况照片 (' + gifs.length + ')' +
                        '</h2>' +
                        '<div class="media-grid">' +
                            gifs.map(function(gif, index) {
                                const videoId = 'live-video-' + index;
                                return '<div class="media-item live-photo" data-video-id="' + videoId + '">' +
                                    '<span class="live-photo-badge">' +
                                        '<i class="fas fa-circle"></i>' +
                                        'LIVE' +
                                    '</span>' +
                                    '<span class="live-photo-overlay-hint">悬停播放</span>' +
                                    '<video id="' + videoId + '" src="' + gif.mediaUrl + '" loop muted preload="metadata"></video>' +
                                    '<div class="media-overlay">' +
                                        '<button class="download-btn" onclick="downloadMedia(\'' + gif.mediaUrl + '\', ' + (index + 1) + ')">' +
                                            '<i class="fas fa-download"></i>' +
                                            '下载' +
                                        '</button>' +
                                    '</div>' +
                                '</div>';
                            }).join('') +
                        '</div>';
                    
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
