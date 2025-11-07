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
    <main class="container flex-grow piclist-main">
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
    <div id="imageViewer" class="image-viewer hidden">
        <div class="relative max-w-full lg:max-w-6xl w-full h-full flex items-center justify-center">
            <button id="closeViewer" class="viewer-close">
                <i class="fa fa-times"></i>
            </button>
            <button id="prevImage" class="btn btn-secondary viewer-prev">
                <i class="fa fa-chevron-left"></i>
            </button>
            <button id="nextImage" class="btn btn-secondary viewer-next">
                <i class="fa fa-chevron-right"></i>
            </button>
            <img id="fullSizeImage" src="" alt="大图预览" class="full-size-image">
        </div>
    </div>
</div>

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
        const imagesPerLoad = 6; // 每次加载6张图片
        let isLoading = false;
        let hasMoreImages = true;
        let currentPage = 1;
        let totalPages = 1;
        
        // 从模板获取初始数据
        const templateGroupId = ${groupId!'null'};
        const templateGroupName = '${groupName!""}';
        const isFromGroupList = ${(isFromGroupList?c)!'false'};
        let currentGroupId = templateGroupId;
        let currentGroupName = templateGroupName;

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
            currentPage = 1;
            hasMoreImages = true;
            totalPages = 1;

            // 重置状态并开始流式加载
            galleryTitle.textContent = '随机图库';
            loadMoreImages();
        }

        function displayPagedImages(data) {
            try {
                // 新API直接返回图片数组，不需要再从data.images中获取
                const images = data || [];
                
                // 根据返回数据判断是否还有更多
                // 如果返回数据少于pageSize，认为没有更多了
                const hasMore = images.length === imagesPerLoad;
                
                // 显示套图名称（保持现有逻辑）
                const displayName = currentGroupName || '未命名图片组';
                galleryTitle.textContent = displayName;
                
                if (images.length === 0) {
                    if (currentPage === 1) {
                        showStatus('图片列表为空。');
                    } else {
                        hasMoreImages = false;
                        noMoreImages.classList.remove('hidden');
                    }
                    return;
                }
                
                // 将新图片添加到现有图片列表中
                // 从图片对象中提取picUrl作为显示URL
                const imageUrls = images.map(item => item.picUrl).filter(Boolean);
                allImageUrls = allImageUrls.concat(imageUrls);
                
                // 显示新加载的图片
                const fragment = document.createDocumentFragment();
                imageUrls.forEach(imgUrl => {
                    fragment.appendChild(createImageElement(imgUrl));
                });
                gallery.appendChild(fragment);
                
                displayedImagesCount = allImageUrls.length;
                
                // 更新状态
                hasMoreImages = hasMore;
                if (!hasMoreImages) {
                    noMoreImages.classList.remove('hidden');
                }
                
                // 更新标题显示当前数量（由于没有totalImages，只显示已加载数量）
                galleryTitle.textContent = displayName + ' (' + displayedImagesCount + ')';
                
            } catch (error) {
                console.error('解析分页图片数据失败:', error);
                showStatus('解析分页图片数据失败: ' + error.message, true);
            }
        }

        function createImageElement(imgUrl) {
            const imgContainer = document.createElement('div');
            imgContainer.className = 'image-item group cursor-pointer transform transition-all duration-300 hover:scale-105 hover:shadow-xl';
            imgContainer.onclick = () => openImageViewer(imgUrl);
            
            // 创建图片容器（参考showPic页面结构）
            const imageContainer = document.createElement('div');
            imageContainer.className = 'image-container';
            
            const img = document.createElement('img');
            img.src = imgUrl;
            img.alt = '图片';
            img.className = 'image';
            img.loading = 'lazy';
            
            // 创建图片遮罩层
            const imageOverlay = document.createElement('div');
            imageOverlay.className = 'image-overlay';
            
            // 创建操作按钮区域
            const imageActions = document.createElement('div');
            imageActions.className = 'image-actions';
            
            // 创建下载按钮
            const downloadBtn = document.createElement('button');
            downloadBtn.className = 'btn btn-primary btn-sm download-icon-btn';
            downloadBtn.innerHTML = '<i class="fa fa-download"></i>';
            downloadBtn.style.cssText = 'background: rgba(0, 0, 0, 0.6) !important; border: none !important; color: white !important; width: 2.5rem !important; height: 2.5rem !important; border-radius: 50% !important; padding: 0 !important; display: flex !important; align-items: center !important; justify-content: center !important;';
            downloadBtn.onclick = (e) => {
                e.stopPropagation(); // 阻止触发图片查看器
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
            // 使用新的API接口格式和参数
            const fetchUrl = '/api/pic/list';
            
            // 准备请求体数据
            const requestBody = {
                pageIndex: currentPage,
                pageSize: imagesPerLoad
            };
            
            // 如果有分组ID，则添加到请求体
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
                        // 新接口直接使用result.data作为图片数组
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

        function fetchAndDisplayImages() {
            refreshButton.disabled = true;
            refreshButton.innerHTML = '<i class="fa fa-spinner fa-spin mr-2"></i><span>加载中...</span>';
            gallery.innerHTML = '';
            noMoreImages.classList.add('hidden');
            showStatus('正在加载图片列表...');
            galleryTitle.textContent = '加载中...';

            // 重置状态
            allImageUrls = [];
            displayedImagesCount = 0;
            currentPage = 1;
            hasMoreImages = true;
            totalPages = 1;

            loadMoreImages();

            refreshButton.disabled = false;
            refreshButton.innerHTML = '<i class="fa fa-refresh mr-2"></i><span>刷新图片</span>';
        }

        // 刷新时改为获取一个新的随机分组
        function refreshWithRandomGroup() {
            refreshButton.disabled = true;
            refreshButton.innerHTML = '<i class="fa fa-spinner fa-spin mr-2"></i><span>加载中...</span>';
            gallery.innerHTML = '';
            noMoreImages.classList.add('hidden');
            showStatus('正在获取随机分组...');
            galleryTitle.textContent = '加载中...';

            // 重置状态并清空当前分组
            allImageUrls = [];
            displayedImagesCount = 0;
            currentPage = 1;
            hasMoreImages = true;
            totalPages = 1;
            currentGroupId = null;
            currentGroupName = '';

            // 获取随机分组后加载
            fetchRandomGroupId()
                .finally(() => {
                    refreshButton.disabled = false;
                    refreshButton.innerHTML = '<i class=\"fa fa-refresh mr-2\"></i><span>刷新图片</span>';
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

        // 事件监听器
        // 刷新按钮改为获取新的随机分组
        refreshButton.addEventListener('click', refreshWithRandomGroup);
        backToHomeBtn.addEventListener('click', () => window.location.href = '/');
        closeViewer.addEventListener('click', closeImageViewer);
        prevImage.addEventListener('click', showPrevImage);
        nextImage.addEventListener('click', showNextImage);

        // 键盘事件
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

        // 滚动加载
        window.addEventListener('scroll', () => {
            if (isLoading || !hasMoreImages) return;
            if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 300) {
                loadMoreImages();
            }
        });

        // 根据跳转场景初始化
        if (isFromGroupList && currentGroupId && currentGroupName) {
            // 从分组列表跳转，直接显示套图名称并开始加载
            galleryTitle.textContent = currentGroupName;
            loadMoreImages();
        } else if (currentGroupId && currentGroupName) {
            // 从主页跳转，有groupId和groupName，直接显示套图名称并开始加载
            galleryTitle.textContent = currentGroupName;
            loadMoreImages();
        } else if (currentGroupId) {
            // 从主页跳转，有groupId但没有groupName，需要先获取套图信息
            fetchRandomGroupInfo();
        } else {
            // 随机套图，没有groupId，先获取随机分组ID
            fetchRandomGroupId();
        }
        
        // 获取随机分组信息的函数
        function fetchRandomGroupId() {
            showStatus('正在获取随机分组...');
            galleryTitle.textContent = '加载中...';
            
            return fetch('/api/group/randomGroupInfo')
                .then(response => response.json())
                .then(result => {
                    if (result.code === 200 && result.data && result.data.groupId) {
                        currentGroupId = result.data.groupId;
                        currentGroupName = result.data.groupName || '随机套图';
                        galleryTitle.textContent = currentGroupName;
                        loadMoreImages();
                    } else {
                        throw new Error(result.message || '获取随机分组信息失败');
                    }
                })
                .catch(error => {
                    console.error('获取随机分组信息失败:', error);
                    showStatus('获取随机分组信息失败: ' + error.message, true);
                });
        }
        
        // 获取随机套图信息的函数
        function fetchRandomGroupInfo() {
            showStatus('正在获取随机套图信息...');
            galleryTitle.textContent = '加载中...';
            
            fetch('/api/pic/group?groupId=' + currentGroupId)
                .then(response => response.json())
                .then(result => {
                    if (result.code === 200 && result.data) {
                        currentGroupName = result.data.groupName || '随机套图';
                        galleryTitle.textContent = currentGroupName;
                        loadMoreImages();
                    } else {
                        throw new Error(result.message || '获取套图信息失败');
                    }
                })
                .catch(error => {
                    console.error('获取随机套图信息失败:', error);
                    showStatus('获取套图信息失败: ' + error.message, true);
                    // 失败时仍然尝试加载图片
                    loadMoreImages();
                });
        }
    });
</script>

</body>
</html>