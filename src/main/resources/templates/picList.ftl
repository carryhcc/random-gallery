<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>图片列表展示 - 随机图库</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="/css/style.css">
</head>
<body class="min-h-screen">

<div class="min-h-screen flex flex-col">
    <!-- 导航栏 -->
    <header class="navbar">
        <div class="navbar-content">
            <div class="flex items-center gap-4">
                <h1 class="navbar-brand">
                    <i class="fa fa-images"></i>
                    <span>套图</span>
                </h1>
                <div id="galleryTitle" class="text-secondary">加载中...</div>
            </div>
            <div class="navbar-actions">
                <button id="backToHomeBtn" class="btn btn-secondary">
                    <i class="fa fa-arrow-left"></i>
                    <span>返回首页</span>
                </button>
                <button id="refreshImageListBtn" class="btn btn-secondary">
                    <i class="fa fa-refresh"></i>
                    <span>刷新图片</span>
                </button>
            </div>
        </div>
    </header>

    <!-- 主内容 -->
    <main class="container flex-grow" style="margin-top: 5rem; padding-bottom: 2rem;">
        <div id="statusMessage" class="toast hidden"></div>
        <div id="imageGallery" class="image-grid animate-fade-in"></div>
        <div id="loadingIndicator" class="loading hidden">
            <div class="spinner"></div>
            <span>加载更多图片中...</span>
        </div>
        <div id="noMoreImages" class="hidden text-center text-muted mt-8 py-4">
            所有图片已加载完毕 ✨
        </div>
    </main>

    <!-- 图片查看器 -->
    <div id="imageViewer" class="fixed inset-0 bg-overlay flex items-center justify-center p-4 transition-opacity duration-300 opacity-0 invisible" style="z-index: 1000;">
        <div class="relative max-w-full lg:max-w-6xl w-full h-full flex items-center justify-center">
            <button id="closeViewer" class="absolute top-4 right-4 text-white text-3xl cursor-pointer hover:text-gray-300 transition-colors z-20">
                <i class="fa fa-times"></i>
            </button>
            <button id="prevImage" class="btn btn-secondary" style="position: absolute; left: 1rem; z-index: 10;">
                <i class="fa fa-chevron-left"></i>
            </button>
            <button id="nextImage" class="btn btn-secondary" style="position: absolute; right: 1rem; z-index: 10;">
                <i class="fa fa-chevron-right"></i>
            </button>
            <img id="fullSizeImage" src="" alt="大图预览" class="max-h-[90vh] max-w-[95vw] mx-auto rounded-lg shadow-2xl object-contain">
        </div>
    </div>
</div>

<script>
    document.addEventListener('DOMContentLoaded', function () {
        const gallery = document.getElementById('imageGallery');
        const refreshButton = document.getElementById('refreshImageListBtn');
        const statusMessage = document.getElementById('statusMessage');
        const galleryTitle = document.getElementById('galleryTitle');
        const mobileMenuBtn = document.getElementById('mobileMenuBtn');
        const navActions = document.getElementById('navActions');
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
        const imagesPerLoad = 5;
        let isLoading = false;

        function showStatus(message, isError = false) {
            statusMessage.textContent = message;
            statusMessage.className = 'toast';
            statusMessage.classList.add(isError ? 'error' : 'success');
            statusMessage.classList.remove('hidden');
            gallery.innerHTML = '';
            loadingIndicator.classList.add('hidden');
            noMoreImages.classList.add('hidden');
        }

        function displayImages(data) {
            gallery.innerHTML = '';
            statusMessage.classList.add('hidden');
            allImageUrls = [];
            displayedImagesCount = 0;
            noMoreImages.classList.add('hidden');
            try {
                const parsedData = typeof data === 'string' ? JSON.parse(data) : data;
                const galleryName = Object.keys(parsedData)[0];
                let imageUrls = parsedData[galleryName];
                if (typeof imageUrls === 'string') {
                    imageUrls = imageUrls.replace(/^\[|\]$/g, '').split(',').map(url => url.trim());
                }
                galleryTitle.textContent = galleryName || '未命名图片组';
                if (!imageUrls || imageUrls.length === 0) {
                    showStatus('图片列表为空。');
                    return;
                }
                allImageUrls = imageUrls.filter(url => url && url.trim() !== '');
                if (allImageUrls.length > 0) {
                    loadMoreImages();
                } else {
                    showStatus('没有有效的图片URL。');
                }
            } catch (error) {
                console.error('解析图片数据失败:', error);
                showStatus('解析图片数据失败: ' + error.message, true);
            }
        }

        function createImageElement(url) {
            const imgContainer = document.createElement('div');
            imgContainer.className = 'image-container';

            const img = document.createElement('img');
            img.className = 'image';
            img.src = url;
            img.alt = '套图图片';
            img.loading = 'lazy';
            img.onerror = function() { this.alt = '图片加载失败'; };
            img.onload = function() {
                const ratio = this.naturalWidth / this.naturalHeight;
                if (ratio > 1.8) imgContainer.style.gridColumn = 'span 2';
                else if (ratio < 0.6) imgContainer.style.gridRow = 'span 2';
            };

            const overlay = document.createElement('div');
            overlay.className = 'image-overlay';

            const actions = document.createElement('div');
            actions.className = 'image-actions';

            const downloadBtn = document.createElement('button');
            downloadBtn.className = 'btn btn-primary btn-sm';
            downloadBtn.innerHTML = '<i class="fa fa-download"></i><span>下载</span>';
            downloadBtn.onclick = (e) => {
                e.stopPropagation();
                downloadImage(url);
            };

            actions.appendChild(downloadBtn);
            overlay.appendChild(actions);
            imgContainer.appendChild(img);
            imgContainer.appendChild(overlay);
            
            imgContainer.addEventListener('click', () => {
                currentImageIndex = allImageUrls.indexOf(url);
                openImageViewer(url);
            });
            return imgContainer;
        }

        function loadMoreImages() {
            if (isLoading || displayedImagesCount >= allImageUrls.length) {
                if (!isLoading && displayedImagesCount === allImageUrls.length && allImageUrls.length > 0) {
                    noMoreImages.classList.remove('hidden');
                }
                return;
            }
            isLoading = true;
            loadingIndicator.classList.remove('hidden');
            noMoreImages.classList.add('hidden');
            const imagesToLoad = allImageUrls.slice(displayedImagesCount, displayedImagesCount + imagesPerLoad);
            const fragment = document.createDocumentFragment();
            setTimeout(() => {
                imagesToLoad.forEach(imgUrl => {
                    fragment.appendChild(createImageElement(imgUrl));
                });
                gallery.appendChild(fragment);
                displayedImagesCount += imagesToLoad.length;
                isLoading = false;
                loadingIndicator.classList.add('hidden');
                if (displayedImagesCount >= allImageUrls.length) {
                    noMoreImages.classList.remove('hidden');
                }
            }, 300);
        }

        function downloadImage(url) {
            const link = document.createElement('a');
            link.href = url;
            link.download = url.split('/').pop() || 'downloaded_image';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }

        function fetchAndDisplayImages() {
            refreshButton.disabled = true;
            refreshButton.innerHTML = '<i class="fa fa-spinner fa-spin mr-2"></i><span>加载中...</span>';
            gallery.innerHTML = '';
            noMoreImages.classList.add('hidden');
            showStatus('正在加载图片列表...');
            galleryTitle.textContent = '加载中...';
            const urlParams = new URLSearchParams(window.location.search);
            const groupId = urlParams.get('groupId');
            const fetchUrl = groupId ? `/pic/list?groupId=` + groupId : '/pic/list';
            fetch(fetchUrl)
                .then(response => {
                    return response.text();
                })
                .then(displayImages)
                .catch(error => {
                    console.error('获取图片列表失败:', error);
                    showStatus('获取图片列表失败: ' + error.message, true);
                    galleryTitle.textContent = '加载失败';
                })
                .finally(() => {
                    refreshButton.disabled = false;
                    refreshButton.innerHTML = '<i class="fa fa-refresh mr-2"></i><span>刷新图片</span>';
                });
        }

        refreshButton.addEventListener('click', fetchAndDisplayImages);
        fetchAndDisplayImages();
        backToHomeBtn.addEventListener('click', () => window.location.href = '/');

        function openImageViewer(imgUrl) {
            fullSizeImage.src = imgUrl;
            imageViewer.classList.remove('invisible');
            imageViewer.classList.add('opacity-100');
            document.body.style.overflow = 'hidden';
            updateNavButtons();
        }

        function closeImageViewer() {
            imageViewer.classList.add('invisible');
            imageViewer.classList.remove('opacity-100');
            document.body.style.overflow = '';
        }

        function updateNavButtons() {
            const display = allImageUrls.length <= 1 ? 'none' : 'flex';
            prevImage.style.display = display;
            nextImage.style.display = display;
        }

        function showPreviousImage() {
            if (allImageUrls.length > 0) {
                currentImageIndex = (currentImageIndex - 1 + allImageUrls.length) % allImageUrls.length;
                fullSizeImage.src = allImageUrls[currentImageIndex];
            }
        }

        function showNextImage() {
            if (allImageUrls.length > 0) {
                currentImageIndex = (currentImageIndex + 1) % allImageUrls.length;
                fullSizeImage.src = allImageUrls[currentImageIndex];
            }
        }

        closeViewer.addEventListener('click', closeImageViewer);
        prevImage.addEventListener('click', showPreviousImage);
        nextImage.addEventListener('click', showNextImage);
        imageViewer.addEventListener('click', e => {
            if (e.target === imageViewer) closeImageViewer();
        });
        document.addEventListener('keydown', e => {
            if (imageViewer.classList.contains('invisible')) return;
            if (e.key === 'Escape') closeImageViewer();
            if (e.key === 'ArrowLeft') showPreviousImage();
            if (e.key === 'ArrowRight') showNextImage();
        });
        mobileMenuBtn.addEventListener('click', () => navActions.classList.toggle('hidden'));
        window.addEventListener('scroll', () => {
            if (isLoading || displayedImagesCount >= allImageUrls.length) return;
            if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 500) {
                loadMoreImages();
            }
        });
    });
</script>
</body>
</html>