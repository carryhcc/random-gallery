<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>图片展示 - 随机图库</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="/css/style.css">
</head>
<body class="min-h-screen flex items-center justify-center p-4">

<div class="container pic-container">
    <div class="card animate-fade-in">
        <div class="card-header">
            <h1 class="card-title">精选图片</h1>
            <p class="card-subtitle">希望你喜欢这张随机展示的图片</p>
        </div>
        
        <div class="image-container mb-6">
            <!-- 初始显示加载状态 -->
            <div id="loadingState" class="loading-container">
                <div class="loading-spinner"></div>
                <p>正在加载图片...</p>
            </div>
            
            <!-- 图片显示区域，初始隐藏 -->
            <div id="imageContainer" style="display: none;">
                <img id="displayImage" src="" alt="图片加载失败..." class="image">
                <div class="image-overlay">
                    <div class="image-actions">
                        <button id="downloadBtn" class="btn btn-primary btn-sm download-icon-btn" style="background: rgba(0, 0, 0, 0.6) !important; border: none !important; color: white !important; width: 2.5rem !important; height: 2.5rem !important; border-radius: 50% !important; padding: 0 !important; display: flex !important; align-items: center !important; justify-content: center !important;">
                            <i class="fa fa-download"></i>
                        </button>
                    </div>
                </div>
            </div>
            
            <!-- 错误状态，初始隐藏 -->
            <div id="errorState" class="loading-container" style="display: none;">
                <i class="fa fa-exclamation-circle" style="font-size: 40px; margin-bottom: 16px; color: #ef4444;"></i>
                <p id="errorMessage">加载失败，请重试</p>
            </div>
        </div>
        
        <!-- 图片信息区域，初始隐藏 -->
        <div id="picInfo" class="pic-info mb-4" style="display: none;">
            <div class="info-item">
                <span class="info-label">图片ID：</span>
                <span id="picId" class="info-value">--</span>
            </div>
            <div class="info-item">
                <span class="info-label">图片名称：</span>
                <span id="picName" class="info-value">--</span>
            </div>
        </div>
        
        <div class="button-row">
            <button id="backToHomeBtn" class="btn btn-secondary">
                <i class="fa fa-arrow-left"></i>
                <span>返回首页</span>
            </button>
            <button id="refreshBtn" class="btn btn-secondary">
                <i class="fa fa-sync-alt"></i>
                <span>刷新</span>
            </button>
        </div>
    </div>
</div>

<script>
    document.addEventListener('DOMContentLoaded', function () {
        const backToHomeBtn = document.getElementById('backToHomeBtn');
        const refreshBtn = document.getElementById('refreshBtn');
        const downloadBtn = document.getElementById('downloadBtn');
        const displayImage = document.getElementById('displayImage');
        const picIdElement = document.getElementById('picId');
        const picNameElement = document.getElementById('picName');
        const loadingState = document.getElementById('loadingState');
        const imageContainer = document.getElementById('imageContainer');
        const errorState = document.getElementById('errorState');
        const errorMessage = document.getElementById('errorMessage');
        const picInfo = document.getElementById('picInfo');

        /**
         * 从API获取随机图片数据
         * 通过调用/api/pic/random/one接口获取图片信息
         */
        function fetchRandomPic() {
            // 显示加载状态，隐藏其他状态
            showLoadingState();
            
            // 调用API获取随机图片数据
            fetch('/api/pic/random/one')
                .then(response => {
                    if (!response.ok) {
                        throw new Error('HTTP error! Status: ' + response.status);
                    }
                    return response.json();
                })
                .then(data => {
                    // 成功获取数据后渲染页面
                    renderPicData(data.data);
                })
                .catch(error => {
                    // 处理错误
                    console.error('获取图片数据失败:', error);
                    showErrorState('获取图片失败: ' + error.message);
                });
        }

        /**
         * 渲染图片数据到页面
         * @param {Object} data - 包含图片信息的对象
         */
        function renderPicData(data) {
            try {
                // 设置图片信息
                if (data && data.picUrl) {
                    displayImage.src = data.picUrl;
                } else {
                    throw new Error('无效的图片数据格式');
                }
                
                // 设置图片ID和名称
                picIdElement.textContent = data.groupId || '未知';
                picNameElement.textContent = data.picName || '未知';
                
                // 显示图片和信息，隐藏加载状态
                showImageState();
                
            } catch (error) {
                console.error('渲染图片数据失败:', error);
                showErrorState('渲染失败: ' + error.message);
            }
        }

        /**
         * 显示加载状态
         */
        function showLoadingState() {
            loadingState.style.display = 'flex';
            imageContainer.style.display = 'none';
            errorState.style.display = 'none';
            picInfo.style.display = 'none';
        }

        /**
         * 显示图片状态
         */
        function showImageState() {
            loadingState.style.display = 'none';
            imageContainer.style.display = 'block';
            errorState.style.display = 'none';
            picInfo.style.display = 'block';
        }

        /**
         * 显示错误状态
         * @param {string} message - 错误消息
         */
        function showErrorState(message) {
            errorMessage.textContent = message || '加载失败，请重试';
            loadingState.style.display = 'none';
            imageContainer.style.display = 'none';
            errorState.style.display = 'flex';
            picInfo.style.display = 'none';
        }

        // 返回首页按钮逻辑
        backToHomeBtn.addEventListener('click', function() {
            window.location.href = '/'; // 跳转到根路径
        });

        // 刷新按钮逻辑
        refreshBtn.addEventListener('click', function() {
            fetchRandomPic(); // 重新获取图片数据
        });

        // 下载按钮逻辑
        downloadBtn.addEventListener('click', function() {
            const imageUrl = displayImage.src;
            if (imageUrl) {
                const fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1) || 'downloaded_image';
                const link = document.createElement('a');
                link.href = imageUrl;
                link.download = fileName;
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
            }
        });

        // 图片加载失败处理
        displayImage.addEventListener('error', function() {
            showErrorState('图片加载失败，请点击刷新重试');
        });

        /**
         * 页面初始化
         * 加载页面时立即获取随机图片数据
         */
        function initPage() {
            console.log('页面加载完成，开始获取随机图片');
            fetchRandomPic();
        }

        // 初始化页面
        initPage();
    });
</script>
</body>
</html>