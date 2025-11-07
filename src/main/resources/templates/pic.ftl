<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>随机图片 - 随机图库</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Poppins:wght@600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <link rel="stylesheet" href="/css/style.css">
    <style>
        .pic-container {
            max-width: 900px;
            margin: 0 auto;
            padding: var(--spacing-xl) var(--spacing-md);
        }
        
        .image-wrapper {
            position: relative;
            border-radius: var(--radius-xl);
            overflow: hidden;
            background: var(--color-bg-tertiary);
            border: 1px solid var(--color-border);
            box-shadow: var(--shadow-xl);
            margin-bottom: var(--spacing-lg);
        }
        
        .image-wrapper img {
            width: 100%;
            height: auto;
            display: block;
        }
        
        .image-overlay {
            position: absolute;
            bottom: 0;
            left: 0;
            right: 0;
            background: linear-gradient(to top, rgba(0, 0, 0, 0.8) 0%, transparent 100%);
            padding: var(--spacing-lg);
            display: flex;
            justify-content: center;
            opacity: 0;
            transition: opacity var(--transition-base);
        }
        
        .image-wrapper:hover .image-overlay {
            opacity: 1;
        }
        
        #groupName {
            transition: all var(--transition-fast);
        }
        
        #groupName:hover {
            opacity: 0.8;
            transform: translateX(2px);
        }
    </style>
</head>
<body class="min-h-screen flex items-center justify-center p-4">

<div class="pic-container">
    <div class="card animate-fade-in">
        <div class="card-header">
            <h1 class="card-title">
                <i class="fas fa-image" style="margin-right: 0.5rem;"></i>
                精选图片
            </h1>
            <p class="card-subtitle">希望你喜欢这张随机展示的图片</p>
        </div>
        
        <div class="image-wrapper" id="imageWrapper" style="display: none;">
            <img id="displayImage" src="" alt="图片加载失败..." class="image">
            <div class="image-overlay">
                <button id="downloadBtn" class="download-icon-btn">
                    <i class="fas fa-download"></i>
                </button>
            </div>
        </div>
        
        <!-- 加载状态 -->
        <div id="loadingState" class="loading-container">
            <div class="loading-spinner"></div>
            <p>正在加载图片...</p>
        </div>
        
        <!-- 错误状态 -->
        <div id="errorState" class="loading-container" style="display: none;">
            <i class="fas fa-exclamation-circle" style="font-size: 3rem; color: var(--color-error); margin-bottom: var(--spacing-md);"></i>
            <p id="errorMessage" style="color: var(--color-text-secondary);">加载失败，请重试</p>
        </div>
        
        <!-- 图片信息 -->
        <div id="picInfo" class="pic-info" style="display: none;">
            <div class="info-item">
                <span class="info-label">分组名称：</span>
                <span id="groupName" class="info-value" style="color: var(--color-primary); cursor: pointer; text-decoration: underline;">--</span>
            </div>
        </div>
        
        <div class="button-row" style="display: flex; gap: var(--spacing-md); flex-wrap: wrap; justify-content: center;">
            <button id="backToHomeBtn" class="btn btn-secondary">
                <i class="fas fa-arrow-left"></i>
                <span>返回首页</span>
            </button>
            <button id="refreshBtn" class="btn btn-primary">
                <i class="fas fa-sync-alt"></i>
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
        const groupNameElement = document.getElementById('groupName');
        const loadingState = document.getElementById('loadingState');
        const imageWrapper = document.getElementById('imageWrapper');
        const errorState = document.getElementById('errorState');
        const errorMessage = document.getElementById('errorMessage');
        const picInfo = document.getElementById('picInfo');
        
        let currentGroupId = null;
        let currentGroupName = '';

        function fetchRandomPic() {
            showLoadingState();
            
            fetch('/api/pic/random/one')
                .then(response => {
                    if (!response.ok) {
                        throw new Error('HTTP error! Status: ' + response.status);
                    }
                    return response.json();
                })
                .then(data => {
                    renderPicData(data.data);
                })
                .catch(error => {
                    console.error('获取图片数据失败:', error);
                    showErrorState('获取图片失败: ' + error.message);
                });
        }

        function renderPicData(data) {
            try {
                if (data && data.picUrl) {
                    displayImage.src = data.picUrl;
                } else {
                    throw new Error('无效的图片数据格式');
                }
                
                currentGroupId = data.groupId;
                
                // 如果有groupId，获取分组信息
                if (currentGroupId) {
                    fetchGroupInfo(currentGroupId);
                } else {
                    groupNameElement.textContent = '未知分组';
                    groupNameElement.style.cursor = 'default';
                    groupNameElement.style.textDecoration = 'none';
                    groupNameElement.style.color = 'var(--color-text-primary)';
                    showImageState();
                }
                
            } catch (error) {
                console.error('渲染图片数据失败:', error);
                showErrorState('渲染失败: ' + error.message);
            }
        }
        
        function fetchGroupInfo(groupId) {
            fetch('/api/group/randomGroupInfo?groupId=' + encodeURIComponent(groupId))
                .then(response => {
                    if (!response.ok) {
                        throw new Error('HTTP error! Status: ' + response.status);
                    }
                    return response.json();
                })
                .then(result => {
                    if (result.code === 200 && result.data) {
                        currentGroupName = result.data.groupName || '未命名分组';
                        groupNameElement.textContent = currentGroupName;
                        groupNameElement.style.cursor = 'pointer';
                        groupNameElement.style.textDecoration = 'underline';
                        groupNameElement.style.color = 'var(--color-primary)';
                    } else {
                        groupNameElement.textContent = '未知分组';
                        groupNameElement.style.cursor = 'default';
                        groupNameElement.style.textDecoration = 'none';
                        groupNameElement.style.color = 'var(--color-text-primary)';
                    }
                    showImageState();
                })
                .catch(error => {
                    console.error('获取分组信息失败:', error);
                    groupNameElement.textContent = '未知分组';
                    groupNameElement.style.cursor = 'default';
                    groupNameElement.style.textDecoration = 'none';
                    groupNameElement.style.color = 'var(--color-text-primary)';
                    showImageState();
                });
        }
        
        function navigateToGroup() {
            if (currentGroupId && currentGroupName) {
                window.location.href = '/showPicList?groupId=' + currentGroupId + '&groupName=' + encodeURIComponent(currentGroupName);
            }
        }
        
        // 为分组名称元素添加点击事件（使用事件委托，确保动态更新后仍能工作）
        groupNameElement.addEventListener('click', function() {
            if (currentGroupId && currentGroupName) {
                navigateToGroup();
            }
        });

        function showLoadingState() {
            loadingState.style.display = 'flex';
            imageWrapper.style.display = 'none';
            errorState.style.display = 'none';
            picInfo.style.display = 'none';
        }

        function showImageState() {
            loadingState.style.display = 'none';
            imageWrapper.style.display = 'block';
            errorState.style.display = 'none';
            picInfo.style.display = 'block';
        }

        function showErrorState(message) {
            errorMessage.textContent = message || '加载失败，请重试';
            loadingState.style.display = 'none';
            imageWrapper.style.display = 'none';
            errorState.style.display = 'flex';
            picInfo.style.display = 'none';
        }

        backToHomeBtn.addEventListener('click', function() {
            window.location.href = '/';
        });

        refreshBtn.addEventListener('click', function() {
            fetchRandomPic();
        });

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

        displayImage.addEventListener('error', function() {
            showErrorState('图片加载失败，请点击刷新重试');
        });

        function initPage() {
            console.log('页面加载完成，开始获取随机图片');
            fetchRandomPic();
        }

        initPage();
    });
</script>
</body>
</html>
