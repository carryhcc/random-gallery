<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <meta name="referrer" content="no-referrer">
    <title>随机动图 - 随机图库</title>
    <link rel="stylesheet" href="https://cdn.bootcdn.net/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            width: 100vw;
            height: 100vh;
            overflow: hidden;
            background: #000;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            touch-action: pan-y;
        }

        .gif-viewer {
            width: 100%;
            height: 100%;
            position: relative;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        #currentGif {
            max-width: 100%;
            max-height: 100%;
            width: auto;
            height: auto;
            object-fit: contain;
            display: block;
            transition: opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1), 
                        transform 0.3s cubic-bezier(0.4, 0, 0.2, 1),
                        object-fit 0.3s ease;
            cursor: pointer;
        }

        #currentGif.fill-mode {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }

        #currentGif.fade-out {
            opacity: 0;
            transform: scale(0.95);
        }

        #currentGif.fade-in {
            animation: fadeInScale 0.4s cubic-bezier(0.4, 0, 0.2, 1) forwards;
        }

        @keyframes fadeInScale {
            from {
                opacity: 0;
                transform: scale(0.95);
            }
            to {
                opacity: 1;
                transform: scale(1);
            }
        }

        .loading {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            color: #fff;
            font-size: 18px;
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 15px;
            animation: fadeIn 0.3s ease-in;
        }

        .loading.hidden {
            display: none;
        }

        @keyframes fadeIn {
            from { opacity: 0; }
            to { opacity: 1; }
        }

        .spinner {
            width: 50px;
            height: 50px;
            border: 4px solid rgba(255, 255, 255, 0.2);
            border-top-color: #fff;
            border-radius: 50%;
            animation: spin 0.8s linear infinite;
        }

        @keyframes spin {
            to { transform: rotate(360deg); }
        }

        .hint {
            position: absolute;
            bottom: 30px;
            left: 50%;
            transform: translateX(-50%);
            background: rgba(0, 0, 0, 0.8);
            color: #fff;
            padding: 14px 28px;
            border-radius: 30px;
            font-size: 14px;
            display: flex;
            align-items: center;
            gap: 10px;
            backdrop-filter: blur(10px);
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
            animation: hintSlideUp 0.5s cubic-bezier(0.4, 0, 0.2, 1);
        }

        @keyframes hintSlideUp {
            from {
                opacity: 0;
                transform: translate(-50%, 20px);
            }
            to {
                opacity: 1;
                transform: translate(-50%, 0);
            }
        }

        .hint.fade-out {
            animation: hintFadeOut 0.4s ease-out forwards;
        }

        @keyframes hintFadeOut {
            to {
                opacity: 0;
                transform: translate(-50%, 10px);
            }
        }

        .error {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            color: #fff;
            font-size: 16px;
            text-align: center;
            padding: 20px;
            animation: fadeIn 0.3s ease-in;
        }

        .error i {
            font-size: 48px;
            margin-bottom: 10px;
            display: block;
            animation: pulse 2s ease-in-out infinite;
        }

        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.5; }
        }

        /* 底部信息栏 */
        .bottom-info {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            background: rgba(0, 0, 0, 0.3);
            backdrop-filter: blur(10px);
            padding: 15px 20px;
            z-index: 100;
            display: flex;
            flex-direction: column;
            gap: 8px;
        }

        .info-title {
            color: #fff;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: color 0.3s ease;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
            text-overflow: ellipsis;
            -webkit-tap-highlight-color: transparent;
            user-select: none;
        }

        .info-title:hover {
            color: #667eea;
        }

        .info-author {
            color: rgba(255, 255, 255, 0.8);
            font-size: 14px;
            cursor: pointer;
            transition: color 0.3s ease;
            display: flex;
            align-items: center;
            gap: 6px;
            -webkit-tap-highlight-color: transparent;
            user-select: none;
        }

        .info-author:hover {
            color: #667eea;
        }

        .info-author i {
            font-size: 12px;
        }

        /* 滑动指示器 */
        .swipe-indicator {
            position: absolute;
            color: rgba(255, 255, 255, 0.8);
            font-size: 40px;
            pointer-events: none;
            z-index: 5;
            opacity: 0;
            transition: opacity 0.2s ease;
        }

        .swipe-indicator.up {
            top: 20%;
            left: 50%;
            transform: translateX(-50%);
            animation: swipeUp 0.6s ease-out;
        }

        .swipe-indicator.down {
            bottom: 20%;
            left: 50%;
            transform: translateX(-50%);
            animation: swipeDown 0.6s ease-out;
        }

        .swipe-indicator.left {
            top: 50%;
            left: 20%;
            transform: translateY(-50%);
            animation: swipeLeft 0.6s ease-out;
        }

        .swipe-indicator.right {
            top: 50%;
            right: 20%;
            transform: translateY(-50%);
            animation: swipeRight 0.6s ease-out;
        }

        @keyframes swipeUp {
            0% { opacity: 0; transform: translate(-50%, 20px); }
            50% { opacity: 1; }
            100% { opacity: 0; transform: translate(-50%, -40px); }
        }

        @keyframes swipeDown {
            0% { opacity: 0; transform: translate(-50%, -20px); }
            50% { opacity: 1; }
            100% { opacity: 0; transform: translate(-50%, 40px); }
        }

        @keyframes swipeLeft {
            0% { opacity: 0; transform: translate(20px, -50%); }
            50% { opacity: 1; }
            100% { opacity: 0; transform: translate(-40px, -50%); }
        }

        @keyframes swipeRight {
            0% { opacity: 0; transform: translate(-20px, -50%); }
            50% { opacity: 1; }
            100% { opacity: 0; transform: translate(40px, -50%); }
        }

        /* 播放/暂停指示器 */
        .play-pause-indicator {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            font-size: 80px;
            color: rgba(255, 255, 255, 0.9);
            pointer-events: none;
            opacity: 0;
            transition: opacity 0.2s ease;
            z-index: 15;
        }

        .play-pause-indicator.show {
            animation: playPauseFlash 0.6s ease-out;
        }

        @keyframes playPauseFlash {
            0% { opacity: 0; transform: translate(-50%, -50%) scale(0.8); }
            50% { opacity: 1; transform: translate(-50%, -50%) scale(1.2); }
            100% { opacity: 0; transform: translate(-50%, -50%) scale(1); }
        }

        /* 缩放指示器 */
        .zoom-indicator {
            position: absolute;
            bottom: 110px;
            left: 50%;
            transform: translateX(-50%);
            background: rgba(0, 0, 0, 0.8);
            color: #fff;
            padding: 10px 20px;
            border-radius: 20px;
            font-size: 14px;
            backdrop-filter: blur(10px);
            opacity: 0;
            transition: opacity 0.3s ease;
            pointer-events: none;
            z-index: 15;
        }

        .zoom-indicator.show {
            opacity: 1;
        }

        /* 进度条动画 */
        .progress-bar {
            position: absolute;
            top: 0;
            left: 0;
            width: 0;
            height: 3px;
            background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
            transition: width 0.3s ease;
            z-index: 20;
        }

        .progress-bar.loading {
            animation: progressLoading 1.5s ease-in-out infinite;
        }

        @keyframes progressLoading {
            0% { width: 0%; }
            50% { width: 70%; }
            100% { width: 100%; }
        }
    </style>
</head>
<body>

<div class="progress-bar" id="progressBar"></div>

<div class="gif-viewer">
    <video id="currentGif" autoplay loop muted playsinline></video>
    
    <div id="loading" class="loading">
        <div class="spinner"></div>
        <span>加载中...</span>
    </div>

    <div id="error" class="error" style="display: none;">
        <i class="fas fa-exclamation-circle"></i>
        <p>暂无可用的动图</p>
    </div>

    <!-- 滑动方向指示器 -->
    <div id="swipeIndicator" class="swipe-indicator"></div>
    
    <!-- 播放/暂停指示器 -->
    <div id="playPauseIndicator" class="play-pause-indicator">
        <i class="fas fa-play"></i>
    </div>
    
    <!-- 缩放指示器 -->
    <div id="zoomIndicator" class="zoom-indicator">
        双击切换填充模式
    </div>
</div>

<div id="hint" class="hint" style="display: none;">
    <i class="fas fa-hand-point-up"></i>
    <span>上下滑动切换 | 左滑详情 | 右滑返回</span>
</div>

<!-- 底部信息栏 -->
<div class="bottom-info" id="bottomInfo" style="display: none;">
    <div class="info-title" id="infoTitle" onclick="goToDetail()">加载中...</div>
    <div class="info-author" id="infoAuthor" onclick="goToAuthor()">
        <i class="fas fa-user"></i>
        <span>加载中...</span>
    </div>
</div>

<script>
    const video = document.getElementById('currentGif');
    const loading = document.getElementById('loading');
    const error = document.getElementById('error');
    const hint = document.getElementById('hint');
    const progressBar = document.getElementById('progressBar');
    const swipeIndicator = document.getElementById('swipeIndicator');
    const playPauseIndicator = document.getElementById('playPauseIndicator');
    const zoomIndicator = document.getElementById('zoomIndicator');

    let currentGifData = null;
    let touchStartX = 0;
    let touchStartY = 0;
    let touchEndX = 0;
    let touchEndY = 0;
    let isLoading = false;
    let isSwiping = false;
    let isFillMode = false; // 是否为填充模式
    let lastTapTime = 0; // 用于检测双击
    const bottomInfo = document.getElementById('bottomInfo');
    const infoTitle = document.getElementById('infoTitle');
    const infoAuthor = document.getElementById('infoAuthor');

    // 显示滑动指示器
    function showSwipeIndicator(direction) {
        swipeIndicator.className = 'swipe-indicator ' + direction;
        
        // 根据方向显示不同图标
        const icons = {
            'up': 'fa-arrow-up',
            'down': 'fa-arrow-down',
            'left': 'fa-arrow-left',
            'right': 'fa-arrow-right'
        };
        
        swipeIndicator.innerHTML = '<i class="fas ' + icons[direction] + '"></i>';
        
        // 动画结束后移除
        setTimeout(() => {
            swipeIndicator.className = 'swipe-indicator';
        }, 600);
    }

    // 加载随机GIF（带动画）
    async function loadRandomGif() {
        if (isLoading) return;
        isLoading = true;
        
        // 显示进度条
        progressBar.className = 'progress-bar loading';
        
        // 淡出当前视频
        video.classList.add('fade-out');
        
        loading.classList.remove('hidden');
        error.classList.add('hidden');

        try {
            const response = await fetch('/api/xhsWork/randomGif');
            const result = await response.json();

            if (result.code === 200 && result.data) {
                currentGifData = result.data;
                
                // 更新底部信息栏
                if (result.data.workTitle && result.data.authorNickname) {
                    infoTitle.textContent = result.data.workTitle || '未知作品';
                    infoAuthor.querySelector('span').textContent = result.data.authorNickname || '未知作者';
                    bottomInfo.style.display = 'flex';
                } else {
                    bottomInfo.style.display = 'none';
                }
                
                // 等待淡出动画完成
                await new Promise(resolve => setTimeout(resolve, 300));
                
                video.src = result.data.mediaUrl;
                
                // 等待视频加载
                video.onloadeddata = () => {
                    loading.classList.add('hidden');
                    progressBar.style.width = '100%';
                    progressBar.className = 'progress-bar';
                    setTimeout(() => {
                        progressBar.style.width = '0';
                    }, 300);
                    
                    // 添加或移除填充模式类
                    if (isFillMode) {
                        video.classList.add('fill-mode');
                    } else {
                        video.classList.remove('fill-mode');
                    }
                    
                    // 淡入新视频
                    video.classList.remove('fade-out');
                    video.classList.add('fade-in');
                    setTimeout(() => {
                        video.classList.remove('fade-in');
                    }, 400);
                    
                    isLoading = false;
                    
                    // 显示提示（仅首次）
                    if (!sessionStorage.getItem('gifHintShown')) {
                        hint.style.display = 'flex';
                        setTimeout(() => {
                            hint.classList.add('fade-out');
                            setTimeout(() => {
                                hint.style.display = 'none';
                                hint.classList.remove('fade-out');
                            }, 400);
                        }, 3000);
                        sessionStorage.setItem('gifHintShown', 'true');
                    }
                };

                video.onerror = () => {
                    console.error('视频加载失败');
                    loading.classList.add('hidden');
                    progressBar.className = 'progress-bar';
                    progressBar.style.width = '0';
                    showError('动图加载失败，请重试');
                    video.classList.remove('fade-out');
                    isLoading = false;
                };
            } else {
                showError(result.message || '暂无可用的动图');
                progressBar.className = 'progress-bar';
                progressBar.style.width = '0';
                video.classList.remove('fade-out');
                isLoading = false;
            }
        } catch (err) {
            console.error('请求失败:', err);
            showError('网络请求失败');
            progressBar.className = 'progress-bar';
            progressBar.style.width = '0';
            video.classList.remove('fade-out');
            isLoading = false;
        }
    }

    function showError(message) {
        loading.classList.add('hidden');
        error.style.display = 'block';
        error.querySelector('p').textContent = message;
        video.style.display = 'none';
    }

    function goBack() {
        // 添加退出动画
        video.classList.add('fade-out');
        setTimeout(() => {
            window.location.href = '/';
        }, 200);
    }

    function goToDetail() {
        if (currentGifData && currentGifData.workId) {
            video.classList.add('fade-out');
            setTimeout(() => {
                window.location.href = '/downloadDetail?workId=' + encodeURIComponent(currentGifData.workId);
            }, 200);
        }
    }

    function goToAuthor() {
        if (currentGifData && currentGifData.authorId) {
            video.classList.add('fade-out');
            setTimeout(() => {
                window.location.href = '/download?authorId=' + encodeURIComponent(currentGifData.authorId);
            }, 200);
        }
    }

    // 显示播放/暂停指示器
    function showPlayPauseIndicator(isPlaying) {
        const icon = playPauseIndicator.querySelector('i');
        icon.className = isPlaying ? 'fas fa-play' : 'fas fa-pause';
        playPauseIndicator.classList.remove('show');
        // 强制重绘
        void playPauseIndicator.offsetWidth;
        playPauseIndicator.classList.add('show');
    }

    // 切换播放/暂停
    function togglePlayPause() {
        if (video.paused) {
            video.play();
            showPlayPauseIndicator(false); // 显示播放图标
        } else {
            video.pause();
            showPlayPauseIndicator(true); // 显示暂停图标
        }
    }

    // 切换填充模式
    function toggleFillMode() {
        isFillMode = !isFillMode;
        
        if (isFillMode) {
            video.classList.add('fill-mode');
            zoomIndicator.textContent = '填充模式：已启用';
        } else {
            video.classList.remove('fill-mode');
            zoomIndicator.textContent = '填充模式：已关闭';
        }
        
        // 显示提示
        zoomIndicator.classList.add('show');
        setTimeout(() => {
            zoomIndicator.classList.remove('show');
        }, 1500);
    }

    // 视频点击事件（区分单击和双击）
    video.addEventListener('click', (e) => {
        const currentTime = new Date().getTime();
        const tapGap = currentTime - lastTapTime;
        
        if (tapGap < 300 && tapGap > 0) {
            // 双击
            toggleFillMode();
            lastTapTime = 0; // 重置，防止三击被识别为双击
        } else {
            // 单击，延迟执行以区分双击
            lastTapTime = currentTime;
            setTimeout(() => {
                if (new Date().getTime() - lastTapTime >= 300) {
                    togglePlayPause();
                }
            }, 300);
        }
    });

    // 触摸事件处理（增强版）
    document.addEventListener('touchstart', (e) => {
        touchStartX = e.changedTouches[0].screenX;
        touchStartY = e.changedTouches[0].screenY;
        isSwiping = false;
    }, { passive: true });

    document.addEventListener('touchmove', (e) => {
        if (!isSwiping) {
            const currentX = e.changedTouches[0].screenX;
            const currentY = e.changedTouches[0].screenY;
            const deltaX = currentX - touchStartX;
            const deltaY = currentY - touchStartY;
            
            // 实时视觉反馈
            if (Math.abs(deltaX) > 30 || Math.abs(deltaY) > 30) {
                isSwiping = true;
            }
        }
    }, { passive: true });

    document.addEventListener('touchend', (e) => {
        touchEndX = e.changedTouches[0].screenX;
        touchEndY = e.changedTouches[0].screenY;
        handleSwipe();
    }, { passive: true });

    function handleSwipe() {
        const deltaX = touchEndX - touchStartX;
        const deltaY = touchEndY - touchStartY;
        const threshold = 50;

        // 判断主要滑动方向
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            // 水平滑动
            if (Math.abs(deltaX) > threshold) {
                if (deltaX > 0) {
                    // 从左往右滑：返回
                    showSwipeIndicator('left');
                    setTimeout(() => goBack(), 300);
                } else {
                    // 从右往左滑：详情
                    showSwipeIndicator('right');
                    setTimeout(() => goToDetail(), 300);
                }
            }
        } else {
            // 垂直滑动
            if (Math.abs(deltaY) > threshold) {
                if (deltaY > 0) {
                    showSwipeIndicator('down');
                } else {
                    showSwipeIndicator('up');
                }
                // 加载下一张
                setTimeout(() => loadRandomGif(), 100);
            }
        }
    }

    // 键盘支持（桌面端）
    document.addEventListener('keydown', (e) => {
        switch(e.key) {
            case 'ArrowUp':
                showSwipeIndicator('up');
                loadRandomGif();
                break;
            case 'ArrowDown':
                showSwipeIndicator('down');
                loadRandomGif();
                break;
            case 'ArrowLeft':
            case 'Escape':
                showSwipeIndicator('left');
                setTimeout(() => goBack(), 300);
                break;
            case 'ArrowRight':
            case 'Enter':
                showSwipeIndicator('right');
                setTimeout(() => goToDetail(), 300);
                break;
        }
    });

    // 页面加载时获取第一张GIF
    document.addEventListener('DOMContentLoaded', () => {
        loadRandomGif();
    });
</script>
</body>
</html>
