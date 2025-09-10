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

<div class="aurora-background"></div>

<div class="relative min-h-screen flex flex-col">
    <header class="glass-header">
        <div class="container mx-auto px-4 flex flex-col md:flex-row md:items-center justify-between">
            <div class="flex items-center justify-between mb-3 md:mb-0 w-full md:w-auto">
                <h1 class="text-[clamp(1.5rem,3vw,2.25rem)] font-bold text-white flex items-center">
                    <i class="fa fa-images mr-3"></i>套图
                </h1>
                <button id="mobileMenuBtn" class="md:hidden text-white/80 focus:outline-none p-2 rounded-md hover:bg-white/10 transition-colors">
                    <i class="fa fa-bars text-xl"></i>
                </button>
            </div>
            <div id="navActions" class="flex flex-col md:flex-row items-center space-y-3 md:space-y-0 md:space-x-4 w-full md:w-auto mt-3 md:mt-0 hidden md:flex">
                <button id="backToHomeBtn" class="btn-glow">
                    <i class="fa fa-arrow-left mr-2"></i><span>返回首页</span>
                </button>
                <button id="refreshImageListBtn" class="btn-glow">
                    <i class="fa fa-refresh mr-2"></i><span>刷新图片</span>
                </button>
                <div id="galleryTitle" class="ml-2 text-white/80 font-medium text-base md:text-xl fade-in text-center md:text-left w-full md:w-auto">加载中...</div>
            </div>
        </div>
    </header>

    <main class="container mx-auto px-4 pt-32 pb-16 flex-grow">
        <div id="statusMessage" class="mb-6 text-center py-3 px-4 rounded-lg hidden fade-in text-sm md:text-base bg-black/20 border border-white/10"></div>
        <div id="imageGallery" class="image-grid fade-in"></div>
        <div id="loadingIndicator" class="hidden mt-8 flex justify-center items-center p-2">
            <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-cyan-400"></div>
            <p class="ml-4 text-neutral-300">加载更多图片中...</p>
        </div>
        <div id="noMoreImages" class="hidden text-center text-neutral-400 mt-8 py-4 border-t border-white/10">
            所有图片已加载完毕 ✨
        </div>
    </main>

    <div id="imageViewer" class="fixed inset-0 z-50 bg-black/90 flex items-center justify-center p-4 backdrop-blur-sm transition-opacity duration-300 opacity-0 invisible">
        <div class="relative max-w-full lg:max-w-6xl w-full h-full flex items-center justify-center">
            <span id="closeViewer" class="absolute top-4 right-4 text-white/80 text-3xl cursor-pointer hover:text-white transition-colors z-20"><i class="fa fa-times"></i></span>
            <span id="prevImage" class="nav-btn left-4 md:left-8"><i class="fa fa-chevron-left"></i></span>
            <span id="nextImage" class="nav-btn right-4 md:right-8"><i class="fa fa-chevron-right"></i></span>
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
            statusMessage.className = isError ?
                'mb-6 text-center py-3 px-4 rounded-lg bg-red-900/50 text-red-300 border border-red-500/30 fade-in' :
                'mb-6 text-center py-3 px-4 rounded-lg bg-cyan-900/50 text-cyan-300 border border-cyan-500/30 fade-in';
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
            imgContainer.className = 'img-container group';

            const img = document.createElement('img');
            img.className = 'w-full h-full object-cover transition-transform duration-300 group-hover:scale-105';
            img.src = url;
            img.alt = '套图图片';
            img.loading = 'lazy';
            img.onerror = function() { this.alt = '图片加载失败'; };

            img.onload = function() {
                const ratio = this.naturalWidth / this.naturalHeight;
                if (ratio > 1.8) imgContainer.classList.add('md:col-span-2');
                else if (ratio < 0.6) imgContainer.classList.add('md:row-span-2');
                imgContainer.classList.add('loaded');
            };

            const downloadBtn = document.createElement('button');
            downloadBtn.className = 'download-btn';
            downloadBtn.innerHTML = '<i class="fa fa-download"></i>';
            downloadBtn.onclick = (e) => {
                e.stopPropagation();
                downloadImage(url);
            };

            imgContainer.appendChild(img);
            imgContainer.appendChild(downloadBtn);
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