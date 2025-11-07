<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>图片列表展示 - 随机图库</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Poppins:wght@600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <link rel="stylesheet" href="/css/style.css">
    <script src="/js/theme.js"></script>
</head>
<body class="min-h-screen">

<div class="min-h-screen flex flex-col">
    <!-- 导航栏 -->
    <header class="navbar">
        <div class="navbar-content">
            <div class="flex items-center gap-4">
                <h1 class="navbar-brand">
                    <i class="fas fa-images"></i>
                    <span id="galleryTitle">加载中...</span>
                </h1>
            </div>
            <div class="navbar-actions">
                <button id="backToHomeBtn" class="btn btn-secondary btn-sm">
                    <i class="fas fa-arrow-left"></i>
                    <span class="hidden-mobile">返回上级</span>
                </button>
                <button id="refreshImageListBtn" class="btn btn-primary btn-sm">
                    <i class="fas fa-sync-alt"></i>
                    <span class="hidden-mobile">刷新图片</span>
                </button>
            </div>
        </div>
    </header>

    <!-- 主内容 -->
    <main class="container flex-grow piclist-main">
        <div id="statusMessage" class="toast hidden"></div>
        <div id="imageGallery" class="image-grid animate-fade-in"></div>
        <div id="loadingIndicator" class="loading hidden">
            <div class="spinner"></div>
            <span>加载更多图片中...</span>
        </div>
        <div id="noMoreImages" class="hidden text-center" style="padding: var(--spacing-xl); color: var(--color-text-tertiary);">
            <i class="fas fa-check-circle" style="font-size: 2rem; margin-bottom: var(--spacing-sm);"></i>
            <p>所有图片已加载完毕 ✨</p>
        </div>
    </main>

    <!-- 图片查看器 -->
    <div id="imageViewer" class="image-viewer hidden">
        <div class="relative max-w-full lg:max-w-6xl w-full h-full flex items-center justify-center">
            <button id="closeViewer" class="viewer-close">
                <i class="fas fa-times"></i>
            </button>
            <button id="prevImage" class="viewer-prev">
                <i class="fas fa-chevron-left"></i>
            </button>
            <button id="nextImage" class="viewer-next">
                <i class="fas fa-chevron-right"></i>
            </button>
            <img id="fullSizeImage" src="" alt="大图预览" class="full-size-image">
        </div>
    </div>
</div>

<style>
    .hidden-mobile {
        display: inline;
    }
    
    @media (max-width: 640px) {
        .hidden-mobile {
            display: none;
        }
    }
</style>

<script>
    document.addEventListener('DOMContentLoaded', function () {
        const gallery = document.getElementById('imageGallery');
        const refreshButton = document.getElementById('refreshImageListBtn');
        const statusMessage = document.getElementById('statusMessage');
        const galleryTitle = document.getElementById('galleryTitle');
        const loadingIndicator = document.getElementById('loadingIndicator');
        const noMoreImages = document.getElementById('noMoreImages');
        const backToHomeBtn = document.getElementById('backToHomeBtn');
        const imageViewer = document.getElementById('imageViewer');
        const fullSizeImage = document.getElementById('fullSizeImage');
        const closeViewer = document.getElementById('closeViewer');
        const prevImage = document.getElementById('prevImage');
        const nextImage = document.getElementById('nextImage');

        let currentImageIndex = 0;
        let allImageUrls = [];
        let displayedImagesCount = 0;
        const imagesPerLoad = 6;
        let isLoading = false;
        let hasMoreImages = true;
        let currentPage = 1;
        let totalImages = 0; // 总图片数量

        let templateGroupId = ${groupId!'null'};
        let currentGroupId = templateGroupId;
        let currentGroupName = '';

        function showStatus(message, isError = false) {
            statusMessage.textContent = message;
            statusMessage.className = 'toast show';
            statusMessage.classList.add(isError ? 'error' : 'success');
            statusMessage.classList.remove('hidden');
            gallery.innerHTML = '';
            loadingIndicator.classList.add('hidden');
            noMoreImages.classList.add('hidden');
            
            setTimeout(() => {
                statusMessage.classList.remove('show');
            }, 3000);
        }

        function displayPagedImages(data) {
            try {
                const images = data || [];
                const hasMore = images.length === imagesPerLoad;
                const displayName = currentGroupName || '未命名图片组';

                if (images.length === 0) {
                    if (currentPage === 1) {
                        galleryTitle.textContent = displayName + ' (0/' + totalImages + ')';
                        showStatus('图片列表为空。');
                    } else {
                        hasMoreImages = false;
                        noMoreImages.classList.remove('hidden');
                    }
                    return;
                }

                const imageUrls = images.map(item => item.picUrl).filter(Boolean);
                allImageUrls = allImageUrls.concat(imageUrls);

                const fragment = document.createDocumentFragment();
                imageUrls.forEach(imgUrl => {
                    fragment.appendChild(createImageElement(imgUrl));
                });
                gallery.appendChild(fragment);

                displayedImagesCount = allImageUrls.length;
                hasMoreImages = hasMore;
                if (!hasMoreImages) {
                    noMoreImages.classList.remove('hidden');
                }

                galleryTitle.textContent = displayName + ' (' + displayedImagesCount + '/' + totalImages + ')';

            } catch (error) {
                console.error('解析分页图片数据失败:', error);
                showStatus('解析分页图片数据失败: ' + error.message, true);
            }
        }

        function createImageElement(imgUrl) {
            const imgContainer = document.createElement('div');
            imgContainer.className = 'image-item';
            imgContainer.onclick = () => openImageViewer(imgUrl);

            const imageContainer = document.createElement('div');
            imageContainer.className = 'image-container';

            const img = document.createElement('img');
            img.src = imgUrl;
            img.alt = '图片';
            img.className = 'image';
            img.loading = 'lazy';

            const imageOverlay = document.createElement('div');
            imageOverlay.className = 'image-overlay';

            const imageActions = document.createElement('div');
            imageActions.className = 'image-actions';

            const downloadBtn = document.createElement('button');
            downloadBtn.className = 'download-icon-btn';
            downloadBtn.innerHTML = '<i class="fas fa-download"></i>';
            downloadBtn.onclick = (e) => {
                e.stopPropagation();
                downloadImage(imgUrl);
            };

            imageActions.appendChild(downloadBtn);
            imageOverlay.appendChild(imageActions);
            imageContainer.appendChild(img);
            imageContainer.appendChild(imageOverlay);
            imgContainer.appendChild(imageContainer);

            return imgContainer;
        }

        function loadMoreImages() {
            if (isLoading || !hasMoreImages) {
                if (!isLoading && !hasMoreImages && allImageUrls.length > 0) {
                    noMoreImages.classList.remove('hidden');
                }
                return;
            }
            isLoading = true;
            loadingIndicator.classList.remove('hidden');
            noMoreImages.classList.add('hidden');

            fetchPagedImages();
        }

        function fetchPagedImages() {
            const fetchUrl = '/api/pic/list';
            const requestBody = {
                pageIndex: currentPage,
                pageSize: imagesPerLoad
            };

            if (currentGroupId) {
                requestBody.groupId = currentGroupId;
            }

            fetch(fetchUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestBody)
            })
                .then(response => response.json())
                .then(result => {
                    if (result.code === 200 && result.success) {
                        displayPagedImages(result.data);
                        currentPage++;
                    } else {
                        throw new Error(result.message || '获取图片失败');
                    }
                })
                .catch(error => {
                    console.error('获取分页图片失败:', error);
                    showStatus('获取图片失败: ' + error.message, true);
                })
                .finally(() => {
                    isLoading = false;
                    loadingIndicator.classList.add('hidden');
                });
        }

        function refreshWithRandomGroup() {
            refreshButton.disabled = true;
            refreshButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i><span class="hidden-mobile">加载中...</span>';
            gallery.innerHTML = '';
            noMoreImages.classList.add('hidden');
            showStatus('正在获取随机分组...');
            galleryTitle.textContent = '加载中...';

            allImageUrls = [];
            displayedImagesCount = 0;
            currentPage = 1;
            hasMoreImages = true;
            totalImages = 0;
            currentGroupId = null;
            currentGroupName = '';
            templateGroupId = null;

            fetchRandomGroupId()
                .finally(() => {
                    refreshButton.disabled = false;
                    refreshButton.innerHTML = '<i class="fas fa-sync-alt"></i><span class="hidden-mobile">刷新图片</span>';
                });
        }

        function openImageViewer(imgUrl) {
            const imageIndex = allImageUrls.indexOf(imgUrl);
            if (imageIndex !== -1) {
                currentImageIndex = imageIndex;
            }
            fullSizeImage.src = imgUrl;
            imageViewer.classList.remove('hidden');
            imageViewer.classList.add('visible');
            document.body.style.overflow = 'hidden';
        }

        function closeImageViewer() {
            imageViewer.classList.add('hidden');
            imageViewer.classList.remove('visible');
            document.body.style.overflow = '';
        }

        function showPrevImage() {
            if (currentImageIndex > 0) {
                currentImageIndex--;
                fullSizeImage.src = allImageUrls[currentImageIndex];
            }
        }

        function showNextImage() {
            if (currentImageIndex < allImageUrls.length - 1) {
                currentImageIndex++;
                fullSizeImage.src = allImageUrls[currentImageIndex];
            }
        }

        function downloadImage(imgUrl) {
            const fileName = imgUrl.substring(imgUrl.lastIndexOf('/') + 1) || 'downloaded_image';
            const link = document.createElement('a');
            link.href = imgUrl;
            link.download = fileName;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }

        function fetchRandomGroupId() {
            showStatus('正在获取分组信息...');
            galleryTitle.textContent = '加载中...';

            const apiUrl = templateGroupId ? '/api/group/randomGroupInfo?groupId=' + templateGroupId : '/api/group/randomGroupInfo';

            return fetch(apiUrl)
                .then(response => response.json())
                .then(result => {
                    if (result.code === 200 && result.data && result.data.groupId) {
                        currentGroupId = result.data.groupId;
                        currentGroupName = result.data.groupName || '随机套图';
                        totalImages = result.data.groupCount || 0;
                        galleryTitle.textContent = currentGroupName + ' (0/' + totalImages + ')';
                        statusMessage.classList.add('hidden');
                        loadMoreImages();
                    } else {
                        throw new Error(result.message || '获取分组信息失败');
                    }
                })
                .catch(error => {
                    console.error('获取分组信息失败:', error);
                    showStatus('获取分组信息失败: ' + error.message, true);
                });
        }

        refreshButton.addEventListener('click', refreshWithRandomGroup);
        backToHomeBtn.addEventListener('click', () => window.history.back());
        closeViewer.addEventListener('click', closeImageViewer);
        prevImage.addEventListener('click', showPrevImage);
        nextImage.addEventListener('click', showNextImage);

        document.addEventListener('keydown', (e) => {
            if (imageViewer.classList.contains('visible')) {
                if (e.key === 'Escape') {
                    closeImageViewer();
                } else if (e.key === 'ArrowLeft') {
                    showPrevImage();
                } else if (e.key === 'ArrowRight') {
                    showNextImage();
                }
            }
        });

        window.addEventListener('scroll', () => {
            if (isLoading || !hasMoreImages) return;
            if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 300) {
                loadMoreImages();
            }
        });

        fetchRandomGroupId();
    });
</script>
</body>
</html>
