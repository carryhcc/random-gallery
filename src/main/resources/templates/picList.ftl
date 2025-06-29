<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>图片列表展示</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        primary: '#165DFF',
                        secondary: '#36BFFA',
                        neutral: {
                            100: '#F5F7FA',
                            200: '#E4E6EB',
                            300: '#C9CDD4',
                            400: '#86909C',
                            500: '#4E5969',
                            600: '#272E3B',
                            700: '#1D2129',
                        }
                    },
                    fontFamily: {
                        inter: ['Inter', 'sans-serif'],
                    },
                    boxShadow: {
                        'card': '0 4px 20px rgba(0, 0, 0, 0.08)',
                        'header': '0 2px 10px rgba(0, 0, 0, 0.05)',
                        'dropdown': '0 4px 12px rgba(0, 0, 0, 0.15)',
                    }
                },
            }
        }
    </script>
    <style type="text/tailwindcss">
        @layer utilities {
            .content-auto {
                content-visibility: auto;
            }
            .backdrop-blur-sm {
                backdrop-filter: blur(8px);
            }
            .image-grid {
                display: grid;
                grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); /* 更小的最小宽度适应手机 */
                gap: 1rem;
                grid-auto-flow: dense;
            }
            @media (min-width: 768px) {
                .image-grid {
                    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
                }
            }
            .image-grid .img-container {
                transition: transform 0.3s ease, box-shadow 0.3s ease, opacity 0.5s ease, transform 0.5s ease; /* 添加 opacity 和 transform 过渡 */
                opacity: 0; /* 默认隐藏用于动画 */
                transform: translateY(20px); /* 默认向下移动用于动画 */
            }
            .image-grid .img-container.loaded {
                opacity: 1; /* 加载完成后显示 */
                transform: translateY(0); /* 加载完成后回到原位 */
            }
            .image-grid .img-container:hover {
                transform: translateY(-5px);
                box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12); /* 更明显的阴影效果 */
            }
            .image-grid img {
                object-fit: cover;
                width: 100%;
                height: 100%;
                border-radius: 0.5rem;
            }
            .download-btn {
                position: absolute;
                bottom: 0.75rem;
                right: 0.75rem;
                background-color: rgba(255, 255, 255, 0.9); /* 更不透明 */
                border-radius: 50%;
                width: 2.5rem; /* 稍大一点 */
                height: 2.5rem; /* 稍大一点 */
                display: flex;
                align-items: center;
                justify-content: center;
                opacity: 0;
                transition: opacity 0.3s ease, transform 0.3s ease; /* 添加 transform 过渡 */
                color: #4E5969;
                box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15); /* 更明显的阴影 */
            }
            .img-container:hover .download-btn {
                opacity: 1;
                transform: translateY(-3px); /* 悬停时向上轻微移动 */
            }
            .nav-btn {
                position: absolute;
                top: 50%;
                transform: translateY(-50%);
                background-color: rgba(0, 0, 0, 0.6); /* 稍深 */
                color: white;
                border-radius: 50%;
                width: 3rem; /* 增大点击区域 */
                height: 3rem; /* 增大点击区域 */
                display: flex;
                align-items: center;
                justify-content: center;
                cursor: pointer;
                transition: background-color 0.3s ease, transform 0.2s ease;
                z-index: 10;
                font-size: 1.25rem; /* 增大图标 */
            }
            .nav-btn:hover {
                background-color: rgba(0, 0, 0, 0.8);
                transform: translateY(-50%) scale(1.05); /* 悬停时轻微放大 */
            }
            .image-viewer {
                opacity: 0;
                visibility: hidden;
                transition: opacity 0.3s ease, visibility 0.3s ease;
            }
            .image-viewer.active {
                opacity: 1;
                visibility: visible;
            }
            .fade-in {
                animation: fadeIn 0.5s ease-in-out;
            }
            @keyframes fadeIn {
                from { opacity: 0; }
                to { opacity: 1; }
            }
            .loading-indicator {
                display: flex;
                justify-content: center;
                align-items: center;
                padding: 2rem;
                color: #165DFF;
            }
            /* 以下是为被移除的环境选择器留下的样式，可以根据需要清除 */
            .dropdown {
                transform-origin: top right;
                transform: scale(0.95) translateY(-5px); /* 初始状态微调 */
                opacity: 0;
                transition: transform 0.2s ease-out, opacity 0.2s ease-out; /* 调整过渡效果 */
            }
            .dropdown.active {
                transform: scale(1) translateY(0);
                opacity: 1;
            }
            .env-item {
                transition: all 0.2s ease;
            }
            .env-item:hover {
                background-color: rgba(22, 93, 255, 0.08); /* 稍浅的hover效果 */
            }
            .env-item.active {
                background-color: rgba(22, 93, 255, 0.12); /* 稍浅的active效果 */
                color: #165DFF;
                font-weight: 500;
            }
        }
    </style>
</head>
<body class="font-inter bg-neutral-100 text-neutral-700 min-h-screen">
<div class="relative min-h-screen flex flex-col">
    <header class="fixed top-0 left-0 right-0 z-50 bg-white/80 backdrop-blur-sm shadow-header transition-all duration-300 py-3 md:py-4">
        <div class="container mx-auto px-4 flex flex-col md:flex-row md:items-center justify-between">
            <div class="flex items-center justify-between mb-3 md:mb-0 w-full md:w-auto">
                <div class="flex items-center">
                    <h1 class="text-[clamp(1.5rem,3vw,2.25rem)] font-bold text-primary flex items-center">
                        <i class="fa fa-images mr-2"></i>套图
                    </h1>
                </div>
                <button id="mobileMenuBtn" class="md:hidden text-neutral-500 focus:outline-none p-2 rounded-md hover:bg-neutral-200 transition-colors">
                    <i class="fa fa-bars text-xl"></i>
                </button>
            </div>

            <div id="navActions" class="flex flex-col md:flex-row items-center space-y-3 md:space-y-0 md:space-x-4 w-full md:w-auto mt-3 md:mt-0 hidden md:flex">
                <button id="backToHomeBtn" class="bg-gray-500 hover:bg-gray-600 text-white font-medium py-2.5 px-5 rounded-lg transition-all duration-300 flex items-center shadow-md hover:shadow-lg transform hover:-translate-y-0.5 w-full md:w-auto justify-center">
                    <i class="fa fa-arrow-left mr-2"></i>
                    <span>返回首页</span>
                </button>
                <button id="refreshImageListBtn" class="bg-primary hover:bg-primary/90 text-white font-medium py-2.5 px-5 rounded-lg transition-all duration-300 flex items-center shadow-md hover:shadow-lg transform hover:-translate-y-0.5 w-full md:w-auto justify-center">
                    <i class="fa fa-refresh mr-2"></i>
                    <span>刷新图片</span>
                </button>
                <div id="galleryTitle" class="ml-2 text-neutral-600 font-medium text-base md:text-xl fade-in text-center md:text-left w-full md:w-auto">加载中...</div>
            </div>
        </div>
    </header>

    <main class="container mx-auto px-4 pt-28 pb-16 flex-grow">
        <div id="statusMessage" class="mb-6 text-center py-3 px-4 rounded-lg hidden fade-in text-sm md:text-base"></div>

        <div id="imageGallery" class="image-grid fade-in">
        </div>

        <div id="loadingIndicator" class="loading-indicator hidden mt-8">
            <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-primary"></div>
            <p class="ml-3 text-neutral-500">加载更多图片中...</p>
        </div>
        <div id="noMoreImages" class="hidden text-center text-neutral-400 mt-8 py-4 border-t border-neutral-200">
            所有图片已加载完毕
        </div>
    </main>

    <div id="imageViewer" class="image-viewer fixed inset-0 z-50 bg-black/90 flex items-center justify-center p-4">
        <div class="image-viewer-content relative max-w-full lg:max-w-5xl w-full h-full flex items-center justify-center">
                <span id="closeViewer" class="absolute top-4 right-4 text-white text-3xl cursor-pointer hover:text-neutral-300 transition-colors z-20">
                    <i class="fa fa-times"></i>
                </span>
            <span id="prevImage" class="nav-btn left-4 md:left-8">
                    <i class="fa fa-chevron-left"></i>
                </span>
            <span id="nextImage" class="nav-btn right-4 md:right-8">
                    <i class="fa fa-chevron-right"></i>
                </span>
            <img id="fullsizeImage" src="" alt="大图预览" class="max-h-[85vh] max-w-[90vw] mx-auto rounded-lg shadow-2xl object-contain transition-transform duration-300 ease-out">
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
        const backToHomeBtn = document.getElementById('backToHomeBtn'); // 新增：返回首页按钮

        // 大图查看器相关元素
        const imageViewer = document.getElementById('imageViewer');
        const fullSizeImage = document.getElementById('fullsizeImage');
        const closeViewer = document.getElementById('closeViewer');
        const prevImage = document.getElementById('prevImage');
        const nextImage = document.getElementById('nextImage');

        // 页面滚动效果
        const header = document.querySelector('header');
        window.addEventListener('scroll', () => {
            if (window.scrollY > 10) {
                header.classList.add('py-2', 'shadow-md');
                header.classList.remove('py-3', 'shadow-header');
            } else {
                header.classList.add('py-3', 'shadow-header');
                header.classList.remove('py-2', 'shadow-md');
            }
        });

        let currentImageIndex = 0;
        let allImageUrls = [];
        let displayedImagesCount = 0;
        const imagesPerLoad = 5; // 每次加载更多图片数量
        let isLoading = false;


        function showStatus(message, isError = false) {
            statusMessage.textContent = message;
            statusMessage.className = isError ?
                'mb-6 text-center py-3 px-4 rounded-lg bg-red-50 text-red-700 border border-red-200 fade-in' :
                'mb-6 text-center py-3 px-4 rounded-lg bg-blue-50 text-blue-700 border border-blue-200 fade-in';
            statusMessage.classList.remove('hidden');
            gallery.innerHTML = ''; // 清空画廊
            loadingIndicator.classList.add('hidden'); // 隐藏加载指示器
            noMoreImages.classList.add('hidden'); // 隐藏“无更多图片”信息
        }

        function displayImages(data) {
            gallery.innerHTML = ''; // 清空现有图片
            statusMessage.classList.add('hidden');
            allImageUrls = [];
            displayedImagesCount = 0;
            noMoreImages.classList.add('hidden'); // 每次加载新数据时隐藏

            try {
                const parsedData = typeof data === 'string' ? JSON.parse(data) : data;

                const galleryName = Object.keys(parsedData)[0];
                let imageUrls = parsedData[galleryName];

                if (typeof imageUrls === 'string') {
                    if (imageUrls.startsWith('[') && imageUrls.endsWith(']')) {
                        const urlsString = imageUrls.substring(1, imageUrls.length - 1);
                        imageUrls = urlsString.split(',').map(url => url.trim());
                    } else {
                        imageUrls = [imageUrls.trim()];
                    }
                }

                galleryTitle.textContent = galleryName || '未命名图片组';

                if (!imageUrls || imageUrls.length === 0) {
                    showStatus('图片列表为空。');
                    return;
                }

                imageUrls.forEach(url => {
                    if (url && url.trim() !== '') {
                        allImageUrls.push(url.trim());
                    }
                });

                if (allImageUrls.length > 0) {
                    loadMoreImages(); // 加载第一批图片
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
            imgContainer.className = 'img-container relative rounded-lg overflow-hidden shadow-card bg-white opacity-0 transform translate-y-4'; // 初始状态用于动画

            const img = document.createElement('img');
            img.src = url;
            img.alt = '套图图片';
            img.loading = 'lazy'; // 懒加载图片
            img.onerror = function() {
                this.alt = '图片加载失败';
                this.classList.add('opacity-50', 'bg-neutral-200'); // 加载失败时背景变灰
                console.warn('图片加载失败: ' + this.src);
            };

            img.onload = function() {
                const ratio = this.naturalWidth / this.naturalHeight;

                if (ratio > 1.8) { // 宽图
                    imgContainer.classList.add('md:col-span-2');
                } else if (ratio < 0.6) { // 长图
                    imgContainer.classList.add('md:row-span-2');
                }
                // 图片加载完成后添加 loaded 类以触发动画
                imgContainer.classList.add('loaded');
            };


            // 创建下载按钮
            const downloadBtn = document.createElement('button');
            downloadBtn.className = 'download-btn';
            downloadBtn.innerHTML = '<i class="fa fa-download"></i>';
            downloadBtn.onclick = function(e) {
                e.stopPropagation(); // 防止触发图片点击事件
                downloadImage(url);
            };

            imgContainer.appendChild(img);
            imgContainer.appendChild(downloadBtn);

            // 为图片添加点击事件
            imgContainer.addEventListener('click', function() {
                currentImageIndex = allImageUrls.indexOf(url);
                openImageViewer(url);
            });

            return imgContainer;
        }

        function loadMoreImages() {
            if (isLoading || displayedImagesCount >= allImageUrls.length) {
                if (!isLoading && displayedImagesCount === allImageUrls.length && allImageUrls.length > 0) {
                    noMoreImages.classList.remove('hidden'); // 显示“所有图片已加载”
                }
                return;
            }

            isLoading = true;
            loadingIndicator.classList.remove('hidden');
            noMoreImages.classList.add('hidden'); // 隐藏“无更多图片”信息

            const imagesToLoad = Math.min(imagesPerLoad, allImageUrls.length - displayedImagesCount);
            const fragment = document.createDocumentFragment();

            // 使用 setTimeout 模拟网络延迟和分批加载效果
            setTimeout(() => {
                for (let i = 0; i < imagesToLoad; i++) {
                    const imgUrl = allImageUrls[displayedImagesCount + i];
                    const imgElement = createImageElement(imgUrl);
                    fragment.appendChild(imgElement);
                }

                gallery.appendChild(fragment);
                displayedImagesCount += imagesToLoad;

                isLoading = false;
                loadingIndicator.classList.add('hidden');

                if (displayedImagesCount >= allImageUrls.length && allImageUrls.length > 0) {
                    noMoreImages.classList.remove('hidden'); // 所有图片加载完毕
                }
            }, 300); // 模拟一点加载时间
        }

        // 下载图片的函数
        function downloadImage(url) {
            const link = document.createElement('a');
            link.href = url;
            link.download = url.split('/').pop() || 'downloaded_image'; // 从URL获取文件名，如果为空则给默认名
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }

        function fetchAndDisplayImages() {
            refreshButton.disabled = true;
            refreshButton.innerHTML = '<i class="fa fa-spinner fa-spin mr-2"></i><span>加载中...</span>';
            gallery.innerHTML = ''; // 清空画廊
            noMoreImages.classList.add('hidden'); // 隐藏“无更多图片”信息
            showStatus('正在加载图片列表...');
            galleryTitle.textContent = '加载中...';

            fetch('/pic/list')
                .then(response => {
                    if (!response.ok) {
                        throw new Error('网络响应错误: ' + response.statusText + ' (状态码: ' + response.status + ')');
                    }
                    return response.text();
                })
                .then(dataString => {
                    displayImages(dataString);
                })
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
        fetchAndDisplayImages(); // 页面加载时立即获取图片

        // 返回首页按钮逻辑
        backToHomeBtn.addEventListener('click', function() {
            window.location.href = '/'; // 跳转到根路径
        });

        // 大图查看器逻辑
        function openImageViewer(imgUrl) {
            fullSizeImage.src = imgUrl;
            imageViewer.classList.add('active');
            document.body.style.overflow = 'hidden'; // 禁止背景滚动
            updateNavButtons();
        }

        function closeImageViewer() {
            imageViewer.classList.remove('active');
            document.body.style.overflow = ''; // 恢复背景滚动
            fullSizeImage.src = ''; // 清空图片，防止内存占用
        }

        function updateNavButtons() {
            if (allImageUrls.length <= 1) {
                prevImage.style.display = 'none';
                nextImage.style.display = 'none';
            } else {
                prevImage.style.display = 'flex';
                nextImage.style.display = 'flex';
            }
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

        // 点击大图查看器背景关闭
        imageViewer.addEventListener('click', function(e) {
            if (e.target === imageViewer || e.target === fullSizeImage) {
                closeImageViewer();
            }
        });

        document.addEventListener('keydown', function(e) {
            if (!imageViewer.classList.contains('active')) return;

            if (e.key === 'Escape') {
                closeImageViewer();
            } else if (e.key === 'ArrowLeft') {
                showPreviousImage();
            } else if (e.key === 'ArrowRight') {
                showNextImage();
            }
        });

        // 移动端菜单处理
        mobileMenuBtn.addEventListener('click', function() {
            navActions.classList.toggle('hidden');
            navActions.classList.toggle('flex');
        });

        // 滚动加载更多图片
        window.addEventListener('scroll', () => {
            if (isLoading) return;

            const { scrollTop, scrollHeight, clientHeight } = document.documentElement;

            // 当滚动到距离底部 300px 时加载更多图片
            if (scrollTop + clientHeight >= scrollHeight - 300) {
                loadMoreImages();
            }
        });
    });
</script>
</body>
</html>