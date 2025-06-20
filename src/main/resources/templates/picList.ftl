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
                grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
                gap: 1rem;
                grid-auto-flow: dense;
            }
            .image-grid .img-container {
                transition: transform 0.3s ease, box-shadow 0.3s ease;
            }
            .image-grid .img-container:hover {
                transform: translateY(-5px);
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
                background-color: rgba(255, 255, 255, 0.8);
                border-radius: 50%;
                width: 2.25rem;
                height: 2.25rem;
                display: flex;
                align-items: center;
                justify-content: center;
                opacity: 0;
                transition: opacity 0.3s ease;
                color: #4E5969;
                box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
            }
            .img-container:hover .download-btn {
                opacity: 1;
            }
            .nav-btn {
                position: absolute;
                top: 50%;
                transform: translateY(-50%);
                background-color: rgba(0, 0, 0, 0.5);
                color: white;
                border-radius: 50%;
                width: 2.5rem;
                height: 2.5rem;
                display: flex;
                align-items: center;
                justify-content: center;
                cursor: pointer;
                transition: background-color 0.3s ease;
                z-index: 10;
            }
            .nav-btn:hover {
                background-color: rgba(0, 0, 0, 0.7);
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
            .dropdown {
                transform-origin: top right;
                transform: scale(0);
                opacity: 0;
                transition: transform 0.2s ease, opacity 0.2s ease;
            }
            .dropdown.active {
                transform: scale(1);
                opacity: 1;
            }
            .env-item {
                transition: all 0.2s ease;
            }
            .env-item:hover {
                background-color: rgba(22, 93, 255, 0.1);
            }
            .env-item.active {
                background-color: rgba(22, 93, 255, 0.15);
                color: #165DFF;
                font-weight: 500;
            }
        }
    </style>
</head>
<body class="font-inter bg-neutral-100 text-neutral-700 min-h-screen">
<div class="relative min-h-screen">
    <!-- 半透明置顶头部 -->
    <header class="fixed top-0 left-0 right-0 z-50 bg-white/80 backdrop-blur-sm shadow-header transition-all duration-300">
        <div class="container mx-auto px-4 py-3 flex flex-col md:flex-row md:items-center justify-between">
            <div class="flex items-center justify-between mb-3 md:mb-0">
                <div class="flex items-center">
                    <h1 class="text-[clamp(1.5rem,3vw,2.5rem)] font-bold text-primary">
                        <i class="fa fa-images mr-2"></i>套图
                    </h1>
                    <!-- 环境选择下拉菜单 -->
                    <div class="ml-4 relative">
                        <button id="envSelectorBtn" class="flex items-center justify-center bg-gray-100 hover:bg-gray-200 text-gray-700 px-2.5 py-1.5 rounded-lg transition-all duration-200">
                            <i class="fa fa-cog mr-1"></i>
                            <span id="currentEnv">环境</span>
                            <i class="fa fa-chevron-down ml-1 text-xs"></i>
                        </button>
                        <div id="envDropdown" class="dropdown absolute right-0 mt-2 w-36 bg-white rounded-lg shadow-dropdown z-50 overflow-hidden">
                            <div class="env-item active px-4 py-2 cursor-pointer flex items-center" data-env="dev">
                                <i class="fa fa-check-circle mr-2 text-primary"></i>开发环境
                            </div>
                            <div class="env-item px-4 py-2 cursor-pointer flex items-center" data-env="test">
                                <i class="fa fa-circle-o mr-2 text-gray-400"></i>测试环境
                            </div>
                            <div class="env-item px-4 py-2 cursor-pointer flex items-center" data-env="prod">
                                <i class="fa fa-circle-o mr-2 text-gray-400"></i>生产环境
                            </div>
                        </div>
                    </div>
                </div>
                <button id="mobileMenuBtn" class="md:hidden text-neutral-500 focus:outline-none">
                    <i class="fa fa-bars text-xl"></i>
                </button>
            </div>

            <div class="flex items-center space-x-4">
                <button id="refreshImageListBtn" class="bg-primary hover:bg-primary/90 text-white font-medium py-2 px-4 rounded-lg transition-all duration-300 flex items-center shadow-md hover:shadow-lg transform hover:-translate-y-0.5">
                    <i class="fa fa-refresh mr-2"></i>
                    <span>刷新图片</span>
                </button>
                <div id="galleryTitle" class="ml-2 text-neutral-600 font-medium text-lg md:text-xl fade-in">加载中...</div>
            </div>
        </div>
    </header>

    <!-- 主内容区域 -->
    <main class="container mx-auto px-4 pt-24 pb-16">
        <div id="statusMessage" class="mb-6 text-center py-3 rounded-lg hidden fade-in"></div>

        <div id="imageGallery" class="image-grid fade-in">
            <!-- 图片将在这里动态加载 -->
        </div>

        <div id="loadingIndicator" class="loading-indicator hidden">
            <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-primary"></div>
        </div>
    </main>

    <!-- 大图查看器 -->
    <div id="imageViewer" class="image-viewer fixed inset-0 z-50 bg-black/90 flex items-center justify-center">
        <div class="image-viewer-content relative max-w-5xl w-full px-4">
                <span id="closeViewer" class="absolute top-4 right-4 text-white text-3xl cursor-pointer hover:text-neutral-300 transition-colors">
                    <i class="fa fa-times"></i>
                </span>
            <span id="prevImage" class="nav-btn left-4">
                    <i class="fa fa-chevron-left"></i>
                </span>
            <span id="nextImage" class="nav-btn right-4">
                    <i class="fa fa-chevron-right"></i>
                </span>
            <img id="fullsizeImage" src="" alt="大图预览" class="max-h-[85vh] mx-auto rounded-lg shadow-2xl">
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
        const loadingIndicator = document.getElementById('loadingIndicator');

        // 环境选择相关元素
        const envSelectorBtn = document.getElementById('envSelectorBtn');
        const envDropdown = document.getElementById('envDropdown');
        const envItems = document.querySelectorAll('.env-item');
        const currentEnvDisplay = document.getElementById('currentEnv');

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
        const imagesPerLoad = 5;
        let isLoading = false;
        let currentEnv = 'dev';

        function showStatus(message, isError = false) {
            statusMessage.textContent = message;
            statusMessage.className = isError ?
                'mb-6 text-center py-3 rounded-lg bg-red-50 text-red-700 border border-red-200 fade-in' :
                'mb-6 text-center py-3 rounded-lg bg-blue-50 text-blue-700 border border-blue-200 fade-in';
            statusMessage.classList.remove('hidden');
            gallery.innerHTML = '';
        }

        function displayImages(data) {
            gallery.innerHTML = '';
            statusMessage.classList.add('hidden');
            allImageUrls = [];
            displayedImagesCount = 0;

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
            imgContainer.className = 'img-container relative rounded-lg overflow-hidden shadow-card bg-white opacity-0 transform shadow-card bg-white opacity-0 transform translate-y-4 transition-all duration-500';

            const img = document.createElement('img');
            img.src = url;
            img.alt = '套图图片';
            img.onerror = function() {
                this.alt = '图片加载失败';
                this.classList.add('opacity-50');
                console.warn('图片加载失败: ' + this.src);
            };

            img.onload = function() {
                const ratio = this.naturalWidth / this.naturalHeight;

                if (ratio > 1.8) {
                    imgContainer.classList.add('md:col-span-2');
                }
                else if (ratio < 0.6) {
                    imgContainer.classList.add('md:row-span-2');
                }

                // 添加淡入动画
                setTimeout(() => {
                    imgContainer.classList.remove('opacity-0', 'translate-y-4');
                }, 50);
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
            if (isLoading || displayedImagesCount >= allImageUrls.length) return;

            isLoading = true;
            loadingIndicator.classList.remove('hidden');

            const imagesToLoad = Math.min(imagesPerLoad, allImageUrls.length - displayedImagesCount);
            const fragment = document.createDocumentFragment();

            for (let i = 0; i < imagesToLoad; i++) {
                const imgUrl = allImageUrls[displayedImagesCount + i];
                const imgElement = createImageElement(imgUrl);
                fragment.appendChild(imgElement);
            }

            gallery.appendChild(fragment);
            displayedImagesCount += imagesToLoad;

            // 检查是否已加载全部图片
            if (displayedImagesCount >= allImageUrls.length) {
                setTimeout(() => {
                    isLoading = false;
                    loadingIndicator.classList.add('hidden');
                }, 500);
            } else {
                isLoading = false;
                loadingIndicator.classList.add('hidden');
            }
        }

        // 下载图片的函数
        function downloadImage(url) {
            const link = document.createElement('a');
            link.href = url;
            link.download = url.split('/').pop();
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }

        function fetchAndDisplayImages() {
            refreshButton.disabled = true;
            refreshButton.innerHTML = '<i class="fa fa-spinner fa-spin mr-2"></i><span>加载中...</span>';
            gallery.innerHTML = '';
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
        fetchAndDisplayImages();

        // 环境选择器逻辑
        envSelectorBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            envDropdown.classList.toggle('active');
        });

        envItems.forEach(item => {
            item.addEventListener('click', function(e) {
                e.stopPropagation();
                const env = this.dataset.env;

                if (env !== currentEnv) {
                    // 更新UI
                    envItems.forEach(i => {
                        i.classList.remove('active');
                        i.querySelector('i').className = 'fa fa-circle-o mr-2 text-gray-400';
                    });
                    this.classList.add('active');
                    this.querySelector('i').className = 'fa fa-check-circle mr-2 text-primary';
                    currentEnvDisplay.textContent = env === 'dev' ? '开发' : env === 'test' ? '测试' : '生产';

                    // 更新环境
                    currentEnv = env;

                    // 调用环境切换逻辑
                    fetch('/' + env).then(() => {
                        fetchAndDisplayImages();
                    });
                }

                // 关闭下拉菜单
                envDropdown.classList.remove('active');
            });
        });

        // 点击页面其他区域关闭下拉菜单
        document.addEventListener('click', function() {
            envDropdown.classList.remove('active');
        });

        function openImageViewer(imgUrl) {
            fullSizeImage.src = imgUrl;
            imageViewer.classList.add('active');
            document.body.style.overflow = 'hidden';
            updateNavButtons();
        }

        function closeImageViewer() {
            imageViewer.classList.remove('active');
            document.body.style.overflow = '';
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

        imageViewer.addEventListener('click', function(e) {
            if (e.target === imageViewer) {
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
            const menuItems = document.querySelector('.flex.items-center.space-x-4');
            menuItems.classList.toggle('hidden');
            menuItems.classList.toggle('flex');
        });

        // 滚动加载更多图片
        window.addEventListener('scroll', () => {
            if (isLoading) return;

            const { scrollTop, scrollHeight, clientHeight } = document.documentElement;

            // 当滚动到距离底部100px时加载更多图片
            if (scrollTop + clientHeight >= scrollHeight - 100) {
                loadMoreImages();
            }
        });
    });
</script>
</body>
</html>