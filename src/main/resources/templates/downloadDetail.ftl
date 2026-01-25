<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="referrer" content="no-referrer">
    <title>作品详情 - 随机图库</title>
    <link rel="preconnect" href="https://fonts.loli.net">
    <link rel="preconnect" href="https://gstatic.loli.net" crossorigin>
    <link href="https://fonts.loli.net/css2?family=Inter:wght@400;500;600;700&family=Poppins:wght@600;700&display=swap"
          rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.loli.net/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <link rel="stylesheet" href="/css/style.css">
    <script src="https://cdnjs.loli.net/ajax/libs/masonry/4.2.2/masonry.pkgd.min.js"></script>
    <script src="https://cdnjs.loli.net/ajax/libs/jquery.imagesloaded/5.0.0/imagesloaded.pkgd.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/heic2any@0.0.4/dist/heic2any.min.js"></script>
    <script src="/js/heic-converter.js"></script>
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
            <button class="btn btn-secondary btn-sm" onclick="window.location.href='/downloadList'">
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

<!-- 确认模态框 -->
<div id="confirmModal" class="confirm-modal">
    <div class="confirm-backdrop" onclick="closeConfirm()"></div>
    <div class="confirm-box">
        <div class="confirm-header">
            <i class="fas fa-exclamation-triangle"></i>
            <span>确认操作</span>
        </div>
        <div id="confirmMessage" class="confirm-body">
            <!-- 消息内容 -->
        </div>
        <div class="confirm-footer">
            <button class="btn btn-secondary btn-sm" onclick="closeConfirm()">取消</button>
            <button id="confirmBtn" class="btn btn-danger btn-sm">删除</button>
        </div>
    </div>
</div>

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
        <img id="viewer-img" src="" alt="大图预览" onerror="this.onerror=null;this.src='/icons/404.svg';">
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
        // 使用 HEIC 转换工具设置图片源
        setImageSrc(viewerImg, imgUrl).then(() => {
            viewerImg.style.opacity = '1';
        });
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
            return '<span class="tag clickable" data-tag-name="' + tag + '">' + tag + '</span>';
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
        
        // 判断是否为移动端 (宽度小于768px 或 支持触摸)
        const isMobile = () => window.innerWidth < 768 || 'ontouchstart' in window;

        // 观察器选项
        const observerOptions = {
            root: null, // Viewport
            rootMargin: '0px',
            threshold: 0.5 // 50% 可见时触发
        };

        // 创建 IntersectionObserver (主要用于移动端滚动自动播放)
        const observer = new IntersectionObserver((entries) => {
            // 如果是桌面端，不执行滚动自动播放逻辑
            if (!isMobile()) return;

            entries.forEach(entry => {
                const livePhoto = entry.target;
                const videoId = livePhoto.getAttribute('data-video-id');
                const video = document.getElementById(videoId);
                
                if (!video) return;

                if (entry.isIntersecting) {
                    // 进入视口：自动播放
                    video.play().catch(err => console.log('自动播放失败:', err));
                    livePhoto.classList.add('playing');
                } else {
                    // 离开视口：暂停并重置
                    video.pause();
                    video.currentTime = 0;
                    livePhoto.classList.remove('playing');
                }
            });
        }, observerOptions);

        livePhotos.forEach(function(livePhoto) {
            const videoId = livePhoto.getAttribute('data-video-id');
            const video = document.getElementById(videoId);
            
            if (!video) return;

            // 1. 加入观察列表 (始终观察，但在 callback 中判断是否执行)
            observer.observe(livePhoto);
            
            // 2. 鼠标交互 (仅桌面端有效)
            livePhoto.addEventListener('mouseenter', function() {
                if (isMobile()) return; // 移动端忽略 hover

                if (video.paused) {
                    video.currentTime = 0;
                    video.play().catch(err => console.log('播放失败:', err));
                    livePhoto.classList.add('playing');
                }
            });
            
            livePhoto.addEventListener('mouseleave', function() {
                if (isMobile()) return; // 移动端忽略 hover leave

                video.pause();
                video.currentTime = 0;
                livePhoto.classList.remove('playing');
            });
            
            // 3. 触摸/点击支持 (通用手动控制)
            // 允许用户手动点击播放/暂停，作为自动逻辑的补充
            livePhoto.addEventListener('touchstart', function(e) {
                // 触摸事件略作防抖或直接处理
                if (video.paused) {
                    video.currentTime = 0;
                    video.play().catch(err => console.log('播放失败:', err));
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
                            '<a href="#" class="detail-author-name" data-author-id="' + (base.authorId || '') + '">' + (base.authorNickname || '未知作者') + '</a>' +
                            '<a href="' + (base.authorUrl || '#') + '" target="_blank" class="btn btn-sm btn-secondary" style="margin-left: 0.5rem;" title="访问作者主页">' +
                                '<i class="fas fa-external-link-alt"></i>' +
                            '</a>' +
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
                    (tagsHtml ? '<div class="work-description">' + tagsHtml + '</div>' : '') +
                    '<div style="margin-top: 20px; padding-top: 20px; border-top: 1px solid var(--border-color);">' +
                        '<button class="btn btn-danger btn-sm" id="deleteWorkBtn" style="width: auto;">' +
                            '<i class="fas fa-trash-alt"></i> 删除作品' +
                        '</button>' +
                    '</div>';
                
                // 绑定删除作品按钮事件
                const deleteWorkBtn = document.getElementById('deleteWorkBtn');
                if (deleteWorkBtn) {
                    deleteWorkBtn.addEventListener('click', function() {
                        confirmDeleteWork();
                    });
                }
                
                // 绑定作者名称点击事件
                const authorNameEl = document.querySelector('.detail-author-name');
                if (authorNameEl) {
                    authorNameEl.addEventListener('click', function(e) {
                        e.preventDefault();
                        const authorId = this.dataset.authorId;
                        if (authorId) {
                            window.location.href = '/downloadList?authorId=' + encodeURIComponent(authorId);
                        }
                    });
                }
                
                // 绑定标签点击事件
                const tagElements = document.querySelectorAll('.tag.clickable');
                tagElements.forEach(function(tagEl) {
                    tagEl.addEventListener('click', function() {
                        const tagName = this.dataset.tagName;
                        if (tagName) {
                            window.location.href = '/downloadList?tag=' + encodeURIComponent(tagName);
                        }
                    });
                });

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
                        // 创建 wrapper
                        const wrapper = document.createElement('div');
                        wrapper.className = 'masonry-item-wrapper';

                        const mediaItem = document.createElement('div');
                        mediaItem.className = 'media-item';
                        
                        const imgElement = document.createElement('img');
                        imgElement.alt = '图片 ' + (index + 1);
                        imgElement.loading = 'lazy';
                        imgElement.onerror = function() {
                            this.onerror = null;
                            this.src = '/icons/404.svg';
                        };
                        // 使用 HEIC 转换工具设置图片源
                        setImageSrc(imgElement, img.mediaUrl);
                        
                        // 使用事件监听器打开新的查看器 (监听整个 item)
                        const openViewerHandler = function() {
                            openViewer(index);
                        };
                        
                        // 绑定到 mediaItem 而不是 img，确保点击遮罩也能触发
                        mediaItem.addEventListener('click', openViewerHandler);
                        
                        const overlay = document.createElement('div');
                        overlay.className = 'media-overlay';
                        
                        const deleteBtn = document.createElement('button');
                        deleteBtn.className = 'download-btn delete-btn'; // 复用样式，添加 delete-btn 类
                        deleteBtn.style.marginRight = '8px'; // 与下载按钮的间距
                        deleteBtn.innerHTML = '<i class="fas fa-trash"></i>';
                        deleteBtn.title = '删除图片';
                        deleteBtn.addEventListener('click', function(e) {
                            e.stopPropagation();
                            confirmDeleteMedia(img.id, 'image', wrapper);
                        });

                        const downloadBtn = document.createElement('button');
                        downloadBtn.className = 'download-btn';
                        downloadBtn.innerHTML = '<i class="fas fa-download"></i>';
                        downloadBtn.addEventListener('click', function(e) {
                            e.stopPropagation(); // 阻止冒泡，防止触发 openViewer
                            downloadMedia(img.mediaUrl, index + 1, 'image');
                        });
                        
                        overlay.appendChild(deleteBtn);
                        overlay.appendChild(downloadBtn);
                        mediaItem.appendChild(imgElement);
                        mediaItem.appendChild(overlay);
                        
                        wrapper.appendChild(mediaItem);
                        mediaGrid.appendChild(wrapper);
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
                    mediaGrid.className = 'masonry-grid'; // 使用新的 瀑布流容器
                    
                    gifs.forEach(function(gif, index) {
                        const videoId = 'live-video-' + index;
                        
                        // 创建 wrapper
                        const wrapper = document.createElement('div');
                        wrapper.className = 'masonry-item-wrapper';
                        
                        const mediaItem = document.createElement('div');
                        mediaItem.className = 'media-item live-photo';
                        mediaItem.setAttribute('data-video-id', videoId);
                        
                        const badge = document.createElement('span');
                        badge.className = 'live-photo-badge';
                        badge.innerHTML = '<span class="icon-live-photo"></span>';
                        
                        const hint = document.createElement('span');
                        hint.className = 'live-photo-overlay-hint';
                        hint.textContent = '悬停播放';
                        
                        const video = document.createElement('video');
                        video.id = videoId;
                        video.src = gif.mediaUrl;
                        video.loop = true;
                        video.muted = true;
                        video.preload = 'metadata';
                        
                        // 监听元数据加载完成事件，触发重新布局
                        video.addEventListener('loadedmetadata', function() {
                            const grid = document.querySelector('#gifsSection .masonry-grid');
                            if (grid) {
                                const msnry = Masonry.data(grid);
                                if (msnry) {
                                    msnry.layout();
                                }
                            }
                        });
                        
                        // 绑定点击事件到 mediaItem
                        mediaItem.addEventListener('click', function(e) {
                             // 预留位置，防止报错
                        });
                        
                        const overlay = document.createElement('div');
                        overlay.className = 'media-overlay';
                        
                        const deleteBtn = document.createElement('button');
                        deleteBtn.className = 'download-btn delete-btn';
                        deleteBtn.style.marginRight = '8px';
                        deleteBtn.innerHTML = '<i class="fas fa-trash"></i>';
                        deleteBtn.title = '删除';
                        deleteBtn.addEventListener('click', function(e) {
                            e.stopPropagation();
                            confirmDeleteMedia(gif.id, 'gif', wrapper);
                        });

                        const downloadBtn = document.createElement('button');
                        downloadBtn.className = 'download-btn';
                        downloadBtn.innerHTML = '<i class="fas fa-download"></i>';
                        downloadBtn.addEventListener('click', function(e) {
                            e.stopPropagation();
                            downloadMedia(gif.mediaUrl, index + 1, 'gif');
                        });
                        
                        overlay.appendChild(deleteBtn);
                        overlay.appendChild(downloadBtn);
                        mediaItem.appendChild(badge);
                        mediaItem.appendChild(hint);
                        mediaItem.appendChild(video);
                        mediaItem.appendChild(overlay);
                        
                        wrapper.appendChild(mediaItem);
                        mediaGrid.appendChild(wrapper);
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
                
                // 触发 Masonry 布局初始化
                // 延迟执行确保 DOM 已插入
                 setTimeout(function() {
                    initMasonry();
                 }, 100);

            } else {
                detailHeader.innerHTML = '<div class="empty-section"><i class="fas fa-exclamation-circle"></i><p>' + (result.message || '加载失败') + '</p></div>';
            }
        } catch (error) {
            console.error('加载失败:', error);
            detailHeader.innerHTML = '<div class="empty-section"><i class="fas fa-exclamation-circle"></i><p>网络请求失败</p></div>';
        }
    }
    
    // 初始化 Masonry 布局 (使用 Masonry.js 库)
    function initMasonry() {
        const grids = document.querySelectorAll('.masonry-grid');
        grids.forEach(function(grid) {
            // 初始化 Masonry
            const msnry = new Masonry(grid, {
                itemSelector: '.masonry-item-wrapper',
                percentPosition: true,
                // gutter 由 wrapper padding 控制，这里设为0或不设
            });
            
            // 使用 imagesLoaded 确保图片加载后重新布局
            if (typeof imagesLoaded === 'function') {
                imagesLoaded(grid).on('progress', function() {
                    msnry.layout();
                });
            }
        });
    }

    // 页面加载时获取详情
    document.addEventListener('DOMContentLoaded', loadDetail);

    // 确认模态框逻辑
    let confirmCallback = null;
    const confirmModal = document.getElementById('confirmModal');
    const confirmMessage = document.getElementById('confirmMessage');
    const confirmBtn = document.getElementById('confirmBtn');

    // 暴露给全局，防止作用域问题
    window.showConfirm = function(message, callback) {
        confirmMessage.textContent = message;
        confirmCallback = callback;
        confirmModal.classList.add('visible');
        document.body.style.overflow = 'hidden';
    };

    window.closeConfirm = function() {
        confirmModal.classList.remove('visible');
        confirmCallback = null;
        document.body.style.overflow = '';
    };

    // 绑定确认按钮事件
    if (confirmBtn) {
        confirmBtn.addEventListener('click', function() {
            if (confirmCallback) {
                confirmCallback();
            }
            closeConfirm();
        });
    }

    // 这里处理一下Esc关闭
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape' && confirmModal && confirmModal.classList.contains('visible')) {
            closeConfirm();
        }
    });

    // 删除逻辑 - 挂载到 window
    window.confirmDeleteWork = function() {
        showConfirm('确定要删除这个作品吗？删除后将无法查看。', function() {
            showToast('正在删除...', 'info');
            fetch('/api/xhsWork/' + workId, {
                method: 'DELETE'
            })
            .then(response => response.json())
            .then(data => {
                if (data.code === 200) {
                    showToast('作品已删除');
                    setTimeout(() => {
                        window.location.href = '/downloadList';
                    }, 1000);
                } else {
                    showToast(data.message || '删除失败', 'error');
                }
            })
            .catch(err => {
                console.error('Delete error:', err);
                showToast('网络请求失败', 'error');
            });
        });
    };

    window.confirmDeleteMedia = function(mediaId, type, itemElement) {
        // mediaId 是数据库ID (Long)
        if (!mediaId) {
            showToast('无法删除：缺少ID', 'error');
            return;
        }
        
        showConfirm('确定要删除这张' + (type==='gif'?'实况照片':'图片') + '吗？', function() {
            // showToast('正在删除...', 'info');
            fetch('/api/xhsWork/media/' + mediaId, {
                method: 'DELETE'
            })
            .then(response => response.json())
            .then(data => {
                if (data.code === 200) {
                    showToast('已删除');
                    
                    // 获取容器和 section
                    const grid = itemElement.parentElement;
                    const section = grid.parentElement;

                    // 先直接从DOM移除元素
                    itemElement.remove();
                    
                    // 然后更新Masonry布局
                    const msnry = Masonry.data(grid);
                    if (msnry) {
                        msnry.layout();
                    }

                    // 使用 setTimeout 确保 DOM 更新完成后再统计数量并更新标题
                    setTimeout(function() {
                        const remainingItems = grid.querySelectorAll('.masonry-item-wrapper');
                        const newCount = remainingItems.length;
                        
                        // 查找标题元素并更新
                        const titleEl = section.querySelector('.section-title');
                        const iconClass = type === 'gif' ? 'fa-film' : 'fa-image';
                        const textType = type === 'gif' ? '实况照片' : '图片';

                        if (titleEl) {
                            titleEl.innerHTML = '<i class="fas ' + iconClass + '"></i> ' + textType + ' (' + newCount + ')';
                        }

                        // 如果数量为0，显示占位图
                        if (newCount === 0) {
                             section.innerHTML = 
                                '<h2 class="section-title">' +
                                    '<i class="fas ' + iconClass + '"></i> ' +
                                    textType +
                                '</h2>' +
                                '<div class="empty-section">' +
                                    '<i class="fas ' + iconClass + '"></i>' +
                                    '<p>暂无' + textType + '</p>' +
                                '</div>';
                        }
                    }, 100);
                    // --- 更新数量逻辑 结束 ---

                } else {
                    showToast(data.message || '删除失败', 'error');
                }
            })
            .catch(err => {
                console.error('Delete media error:', err);
                showToast('网络请求失败', 'error');
            });
        });
    };
</script>
</body>
</html>
