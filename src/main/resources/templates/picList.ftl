<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>套图详情 - 随机图库</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Poppins:wght@600;700&display=swap"
          rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <link rel="stylesheet" href="/css/style.css">
    <script src="/js/theme.js"></script>
    <style>
        .navbar {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            height: 60px;
            background: rgba(255, 255, 255, 0.9);
            backdrop-filter: blur(10px);
            border-bottom: 1px solid var(--color-border);
            z-index: 1000;
            display: flex;
            align-items: center;
            padding: 0 var(--spacing-lg);
            box-shadow: var(--shadow-sm);
        }

        .dark-mode .navbar {
            background: rgba(30, 30, 30, 0.9);
        }

        .navbar-content {
            width: 100%;
            max-width: 1400px;
            margin: 0 auto;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .navbar-title {
            font-family: 'Poppins', sans-serif;
            font-weight: 600;
            font-size: 1.25rem;
            color: var(--color-text-primary);
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }

        .gallery-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
            gap: var(--spacing-md);
            padding: var(--spacing-md);
            margin-top: 70px; /* Navbar height + spacing */
        }

        .pic-card {
            position: relative;
            border-radius: var(--radius-md);
            overflow: hidden;
            aspect-ratio: 2/3; /* 假设大多是竖图，或者自适应 */
            background: var(--color-bg-tertiary);
            box-shadow: var(--shadow-sm);
            transition: all var(--transition-base);
            cursor: pointer;
        }

        .pic-card:hover {
            transform: translateY(-4px);
            box-shadow: var(--shadow-lg);
        }

        .pic-image {
            width: 100%;
            height: 100%;
            object-fit: cover;
            transition: transform var(--transition-slow);
        }

        .pic-card:hover .pic-image {
            transform: scale(1.05);
        }

        .image-viewer {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0, 0, 0, 0.95);
            z-index: 2000;
            display: flex;
            align-items: center;
            justify-content: center;
            opacity: 0;
            pointer-events: none;
            transition: opacity var(--transition-base);
        }

        .image-viewer.visible {
            opacity: 1;
            pointer-events: auto;
        }

        .viewer-content {
            max-width: 95vw;
            max-height: 95vh;
            position: relative;
        }

        .viewer-content img {
            max-width: 100%;
            max-height: 95vh;
            object-fit: contain;
            border-radius: var(--radius-sm);
            box-shadow: 0 0 20px rgba(0, 0, 0, 0.5);
        }

        .viewer-close {
            position: absolute;
            top: 20px;
            right: 20px;
            background: rgba(255, 255, 255, 0.1);
            border: none;
            color: white;
            width: 40px;
            height: 40px;
            border-radius: 50%;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: background var(--transition-fast);
            z-index: 2010;
        }

        .viewer-close:hover {
            background: rgba(255, 255, 255, 0.2);
        }

        .viewer-nav {
            position: absolute;
            top: 50%;
            transform: translateY(-50%);
            background: rgba(255, 255, 255, 0.1);
            border: none;
            color: white;
            width: 50px;
            height: 50px;
            border-radius: 50%;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: background var(--transition-fast);
            z-index: 2010;
        }

        .viewer-nav:hover {
            background: rgba(255, 255, 255, 0.2);
        }

        .viewer-prev {
            left: 20px;
        }

        .viewer-next {
            right: 20px;
        }

        @media (max-width: 768px) {
            .gallery-grid {
                grid-template-columns: repeat(2, 1fr);
                gap: var(--spacing-sm);
                padding: var(--spacing-sm);
            }

            .viewer-nav {
                width: 40px;
                height: 40px;
            }

            .viewer-prev {
                left: 10px;
            }

            .viewer-next {
                right: 10px;
            }
        }
    </style>
</head>
<body>

<nav class="navbar">
    <div class="navbar-content">
        <div class="navbar-title">
            <button class="btn btn-ghost btn-sm" onclick="window.history.back()" style="margin-right: 1rem;">
                <i class="fas fa-arrow-left"></i>
            </button>
            <span id="pageTitle">套图详情</span>
        </div>
        <div>
            <button class="btn btn-secondary btn-sm" onclick="window.location.href='/'">
                <i class="fas fa-home"></i>
                <span class="hidden-mobile">首页</span>
            </button>
        </div>
    </div>
</nav>

<div id="gallery" class="gallery-grid">
    <!-- 图片将在这里动态加载 -->
</div>

<div id="loading" class="loading-container hidden" style="margin: 2rem auto;">
    <div class="loading-spinner"></div>
    <p>正在加载更多图片...</p>
</div>

<div id="no-more" class="text-center hidden" style="padding: 2rem; color: var(--color-text-tertiary);">
    <p>已经到底了</p>
</div>

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
    let currentPage = 1;
    const pageSize = 6;
    let isLoading = false;
    let hasMore = true;
    let allImages = [];
    let currentImageIndex = 0;
    let currentGroupName = '';
    let totalGroupCount = 0;

    // logic moved to DOMContentLoaded

    function updatePageTitle() {
        const pageTitle = document.getElementById('pageTitle');
        if (pageTitle && currentGroupName) {
            // 显示格式：名称 (总数量/已加载数量)
            pageTitle.textContent = currentGroupName + ' (' + totalGroupCount + '/' + allImages.length + ')';
        }
    }

    function loadImages() {
        if (isLoading || !hasMore) return;

        isLoading = true;
        document.getElementById('loading').classList.remove('hidden');

        const requestData = {
            groupId: window.currentGroupId,
            pageIndex: currentPage,
            pageSize: pageSize
        };

        fetch('/api/pic/list', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestData)
        })
            .then(response => response.json())
            .then(result => {
                if (result.code === 200) {
                    let images = [];
                    // 兼容两种返回格式：data直接是数组，或者data.list是数组
                    if (Array.isArray(result.data)) {
                        images = result.data;
                    } else if (result.data && Array.isArray(result.data.list)) {
                        images = result.data.list;
                    }

                    if (images.length > 0) {
                        renderImages(images);
                        currentPage++;
                        // 检查是否还有更多：如果返回数量小于页大小，说明没有更多了
                        if (images.length < pageSize) {
                            hasMore = false;
                            document.getElementById('no-more').classList.remove('hidden');
                        }
                    } else {
                        hasMore = false;
                        document.getElementById('no-more').classList.remove('hidden');
                    }
                } else {
                    console.error('加载失败:', result.msg);
                }
            })
            .catch(error => {
                console.error('请求错误:', error);
            })
            .finally(() => {
                isLoading = false;
                document.getElementById('loading').classList.add('hidden');
            });
    }

    function renderImages(images) {
        const gallery = document.getElementById('gallery');
        const startIndex = allImages.length;

        images.forEach((img, index) => {
            // 确保图片对象有url属性，兼容 picUrl
            if (!img.url && img.picUrl) {
                img.url = img.picUrl;
            }

            allImages.push(img);
            const globalIndex = startIndex + index;

            const card = document.createElement('div');
            card.className = 'pic-card animate-scale-in';
            card.style.animationDelay = (index * 0.05) + 's';

            const imgElement = document.createElement('img');
            imgElement.src = img.url;
            imgElement.alt = img.picName || img.name || '图片';
            imgElement.className = 'pic-image';
            imgElement.loading = 'lazy';

            card.onclick = () => openViewer(globalIndex);

            card.appendChild(imgElement);
            gallery.appendChild(card);
        });

        updatePageTitle();
    }

    function fetchGroupInfo(id) {
        fetch('/api/group/randomGroupInfo?groupId=' + id)
            .then(response => response.json())
            .then(result => {
                if (result.code === 200 && result.data) {
                    const group = result.data;
                    currentGroupName = group.groupName;
                    totalGroupCount = group.groupCount;
                    document.title = currentGroupName + ' - 随机图库';

                    // 更新页面标题
                    updatePageTitle();
                }
            })
            .catch(error => console.error('获取分组信息失败:', error));
    }

    // 图片查看器功能
    function openViewer(index) {
        currentImageIndex = index;
        updateViewerImage();
        document.getElementById('viewer').classList.add('visible');
        document.body.style.overflow = 'hidden'; // 禁止背景滚动
    }

    function closeViewer() {
        document.getElementById('viewer').classList.remove('visible');
        document.body.style.overflow = '';
    }

    function changeImage(direction) {
        let newIndex = currentImageIndex + direction;
        if (newIndex >= 0 && newIndex < allImages.length) {
            currentImageIndex = newIndex;
            updateViewerImage();
        }
    }

    function updateViewerImage() {
        const img = allImages[currentImageIndex];
        const viewerImg = document.getElementById('viewer-img');

        // 添加淡入效果
        viewerImg.style.opacity = '0.5';
        viewerImg.src = img.url;
        setTimeout(() => {
            viewerImg.style.opacity = '1';
        }, 50);

        // 更新按钮状态
        document.querySelector('.viewer-prev').style.opacity = currentImageIndex === 0 ? '0.3' : '1';
        document.querySelector('.viewer-prev').style.pointerEvents = currentImageIndex === 0 ? 'none' : 'auto';

        document.querySelector('.viewer-next').style.opacity = currentImageIndex === allImages.length - 1 ? '0.3' : '1';
        document.querySelector('.viewer-next').style.pointerEvents = currentImageIndex === allImages.length - 1 ? 'none' : 'auto';

        // 如果接近末尾，预加载更多
        if (currentImageIndex > allImages.length - 5 && hasMore && !isLoading) {
            loadImages();
        }
    }

    // 键盘导航
    document.addEventListener('keydown', (e) => {
        if (!document.getElementById('viewer').classList.contains('visible')) return;

        if (e.key === 'Escape') closeViewer();
        if (e.key === 'ArrowLeft') changeImage(-1);
        if (e.key === 'ArrowRight') changeImage(1);
    });

    // 无限滚动
    window.addEventListener('scroll', () => {
        if ((window.innerHeight + window.scrollY) >= document.body.offsetHeight - 1000) {
            loadImages();
        }
    });

    // 初始化加载
    document.addEventListener('DOMContentLoaded', () => {
        // 获取URL参数
        const urlParams = new URLSearchParams(window.location.search);
        const groupId = urlParams.get('groupId');
        const groupName = urlParams.get('groupName');

        // 初始化页面信息
        if (groupName) {
            document.title = groupName + ' - 随机图库';
            currentGroupName = groupName;
            // 初始时不知道总数，先不显示或显示部分
            updatePageTitle();
        }

        if (groupId) {
            // Make variables available to loadImages
            window.currentGroupId = groupId;
            fetchGroupInfo(groupId);
            loadImages();
        } else {
            document.getElementById('gallery').innerHTML = '<div class="text-center" style="grid-column: 1/-1; padding: 2rem;">无效的分组ID</div>';
        }
    });
</script>
</body>
</html>
