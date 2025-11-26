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
</head>
<body>

<nav class="navbar-fixed">
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

<div id="gallery" class="gallery-grid-pic">
    <!-- 图片将在这里动态加载 -->
</div>

<div id="loading" class="loading-container hidden" style="margin: 2rem auto;">
    <div class="loading-spinner"></div>
    <p>正在加载更多图片...</p>
</div>

<div id="no-more" class="text-center hidden" style="padding: 2rem; color: var(--color-text-tertiary);">
    <p>已经到底了</p>
</div>

<!-- 底部哨兵元素，用于触发 IntersectionObserver -->
<div id="sentinel" style="height: 1px;"></div>

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
    const scrollThreshold = 400;

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
                checkScrollForMore();
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

    function getScrollContainer() {
        return document.scrollingElement || document.body || document.documentElement;
    }

    function checkScrollForMore() {
        if (isLoading || !hasMore) return;
        const scrollContainer = getScrollContainer();
        if (!scrollContainer) return;
        const distanceToBottom = scrollContainer.scrollHeight - (scrollContainer.scrollTop + window.innerHeight);
        if (distanceToBottom <= scrollThreshold) {
            loadImages();
        }
    }

    function setupInfiniteScroll() {
        const sentinel = document.getElementById('sentinel');
        if (sentinel && 'IntersectionObserver' in window) {
            const observer = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        checkScrollForMore();
                    }
                });
            }, {
                root: null,
                rootMargin: '400px 0px 400px 0px',
                threshold: 0
            });
            observer.observe(sentinel);
        }

        window.addEventListener('scroll', checkScrollForMore, {passive: true});
        window.addEventListener('resize', checkScrollForMore, {passive: true});
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
            setupInfiniteScroll();
            loadImages();
        } else {
            document.getElementById('gallery').innerHTML = '<div class="text-center" style="grid-column: 1/-1; padding: 2rem;">无效的分组ID</div>';
        }
    });
</script>
</body>
</html>
