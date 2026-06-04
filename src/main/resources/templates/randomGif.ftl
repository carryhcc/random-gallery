<!DOCTYPE html>
<html lang="zh-CN" data-theme="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
    <meta name="referrer" content="no-referrer">
    <title>随机动图</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Poppins:wght@600;700&display=swap"
          rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <link rel="stylesheet" href="/css/style.css">
    <link rel="stylesheet" href="/css/web.css" media="(min-width: 769px)">
    <link rel="stylesheet" href="/css/mobile.css" media="(max-width: 768px)">
    <script src="/js/theme.js"></script>
    <style>
        /* Full-screen immersive video player */
        body { overflow: hidden; background: #000; }
        body::before, body::after { display: none; }

        .app-container {
            position: relative;
            width: 100vw;
            height: var(--app-height, 100vh);
            overflow: hidden;
            background: #000;
        }

        .video-wrapper {
            position: absolute;
            inset: 0;
            display: flex;
            align-items: center;
            justify-content: center;
            opacity: 0;
            transition: opacity .4s ease;
        }
        .video-wrapper.show { opacity: 1; }

        .video-wrapper video {
            width: 100%;
            height: 100%;
            object-fit: contain;
            background: #000;
        }
        .video-wrapper video.cover {
            object-fit: cover;
        }

        .overlay-top {
            position: absolute;
            top: 0; left: 0; right: 0;
            height: 120px;
            background: linear-gradient(to bottom, rgba(0,0,0,.5), transparent);
            pointer-events: none;
            z-index: 2;
        }
        .overlay-bottom {
            position: absolute;
            bottom: 0; left: 0; right: 0;
            height: 200px;
            background: linear-gradient(to top, rgba(0,0,0,.65), transparent);
            pointer-events: none;
            z-index: 2;
        }

        .back-btn {
            position: absolute;
            top: max(16px, env(safe-area-inset-top));
            left: 16px;
            z-index: 10;
            width: 40px; height: 40px;
            border-radius: 50%;
            background: rgba(255,255,255,.1);
            border: 1px solid rgba(255,255,255,.12);
            color: #fff;
            display: flex; align-items: center; justify-content: center;
            font-size: .9rem;
            cursor: pointer;
            backdrop-filter: blur(8px);
            transition: all .2s;
        }
        .back-btn:hover { background: rgba(255,255,255,.2); }

        .loader {
            position: absolute;
            top: 50%; left: 50%;
            transform: translate(-50%, -50%);
            z-index: 5;
        }
        .loader .spinner {
            width: 36px; height: 36px;
            border: 3px solid rgba(255,255,255,.15);
            border-top-color: #fff;
            border-radius: 50%;
            animation: spin .8s linear infinite;
        }
        @keyframes spin { to { transform: rotate(360deg); } }

        .error-toast {
            position: absolute;
            top: 50%; left: 50%;
            transform: translate(-50%, -50%);
            z-index: 5;
            padding: 10px 20px;
            border-radius: 20px;
            background: rgba(0,0,0,.7);
            color: #fff;
            font-size: .82rem;
            display: flex; align-items: center; gap: 8px;
            cursor: pointer;
            backdrop-filter: blur(8px);
        }

        .info-panel {
            position: absolute;
            bottom: max(24px, env(safe-area-inset-bottom));
            left: 16px;
            right: 80px;
            z-index: 10;
            display: flex;
            flex-direction: column;
            gap: 8px;
        }
        .author-info {
            display: flex; align-items: center; gap: 6px;
            color: #fff;
            font-size: .88rem; font-weight: 600;
            cursor: pointer;
        }
        .work-title {
            color: rgba(255,255,255,.8);
            font-size: .78rem;
            line-height: 1.5;
            max-height: 3em;
            overflow: hidden;
            cursor: pointer;
        }

        .action-bar {
            position: absolute;
            right: 12px;
            bottom: max(80px, calc(env(safe-area-inset-bottom) + 60px));
            z-index: 10;
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 20px;
        }
        .action-btn {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 4px;
            cursor: pointer;
        }
        .action-icon-bg {
            width: 44px; height: 44px;
            border-radius: 50%;
            background: rgba(255,255,255,.1);
            border: 1px solid rgba(255,255,255,.12);
            display: flex; align-items: center; justify-content: center;
            color: #fff;
            font-size: .9rem;
            backdrop-filter: blur(8px);
            transition: all .2s;
        }
        .action-btn:hover .action-icon-bg {
            background: rgba(255,255,255,.2);
            transform: scale(1.05);
        }
        .action-label {
            font-size: .65rem;
            color: rgba(255,255,255,.7);
        }

        .gesture-hint {
            position: absolute;
            bottom: max(40%, env(safe-area-inset-bottom));
            left: 50%;
            transform: translateX(-50%);
            z-index: 10;
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 8px;
            color: rgba(255,255,255,.6);
            font-size: .78rem;
            opacity: 0;
            transition: opacity .4s;
            pointer-events: none;
        }
        .gesture-hint.show { opacity: 1; }
        .gesture-icon {
            font-size: 1.4rem;
            animation: gestureBounce 1.2s ease-in-out infinite;
        }
        @keyframes gestureBounce {
            0%, 100% { transform: translateY(0); }
            50% { transform: translateY(-8px); }
        }

        /* Desktop: constrain aspect ratio */
        @media (min-width: 769px) {
            .app-container {
                max-width: 480px;
                margin: 0 auto;
                border-left: 1px solid rgba(255,255,255,.05);
                border-right: 1px solid rgba(255,255,255,.05);
            }
        }
    </style>
</head>
<body>

<div class="app-container" id="app">
    <!-- 视频层 -->
    <div class="video-wrapper" id="videoWrapper">
        <video id="player" playsinline loop poster="/icons/loading.svg"></video>
    </div>

    <!-- 遮罩层 -->
    <div class="overlay-top"></div>
    <div class="overlay-bottom"></div>

    <!-- 顶部返回 -->
    <div class="back-btn" onclick="goHome()">
        <i class="fas fa-arrow-left"></i>
    </div>

    <!-- 加载中 -->
    <div class="loader" id="loader">
        <div class="spinner"></div>
    </div>

    <!-- 加载失败提示（已隐藏，改为自动加载下一个） -->
    <div class="error-toast" id="errorToast" style="display: none;">
        <i class="fas fa-exclamation-circle"></i>
        <span>加载失败，正在切换下一个...</span>
    </div>

    <!-- 底部信息 -->
    <div class="info-panel" id="infoPanel" style="display: none;">
        <div class="author-info" onclick="goToAuthor()">
            <i class="fas fa-at"></i>
            <span id="authorName">@Author</span>
        </div>
        <div class="work-title" id="workTitle" onclick="goToDetail()">
            Video Description...
        </div>
    </div>

    <!-- 右侧操作栏 -->
    <div class="action-bar">
        <div class="action-btn" onclick="toggleFill()">
            <div class="action-icon-bg">
                <i class="fas fa-expand" id="fillIcon"></i>
            </div>
            <span class="action-label">填充</span>
        </div>
        <div class="action-btn" onclick="goToDetail()">
            <div class="action-icon-bg">
                <i class="fas fa-info"></i>
            </div>
            <span class="action-label">详情</span>
        </div>
        <div class="action-btn" onclick="downloadFile()">
            <div class="action-icon-bg">
                <i class="fas fa-download"></i>
            </div>
            <span class="action-label">下载</span>
        </div>
    </div>

    <!-- 操作提示 -->
    <div class="gesture-hint" id="gestureHint">
        <i class="fas fa-hand-pointer gesture-icon"></i>
        <div>上滑查看更多</div>
    </div>
</div>

<script>
    // 状态管理
    const state = {
        history: [],          // 历史记录栈
        currentIndex: -1,     // 当前历史指针
        isLoading: false,
        isFillMode: false,
        touchStart: { x: 0, y: 0, time: 0 },
        currentData: null,
        preloadVideo: new Audio() // 用于预加载资源（Audio也可以加载视频资源缓存）
    };

    // DOM 元素
    const dom = {
        player: document.getElementById('player'),
        videoWrapper: document.getElementById('videoWrapper'),
        loader: document.getElementById('loader'),
        infoPanel: document.getElementById('infoPanel'),
        authorName: document.getElementById('authorName'),
        workTitle: document.getElementById('workTitle'),
        errorToast: document.getElementById('errorToast'),
        fillIcon: document.getElementById('fillIcon'),
        gestureHint: document.getElementById('gestureHint')
    };

    // 初始化
    function init() {
        setAppHeight();
        window.addEventListener('resize', setAppHeight);
        bindEvents();
        
        // 首次加载检查是否有历史（例如刷新页面），如果没有则加载新数据
        loadNextGif();

        // 首次提示
        if (!localStorage.getItem('gif_tutorial_shown')) {
            setTimeout(() => {
                dom.gestureHint.classList.add('show');
                setTimeout(() => dom.gestureHint.classList.remove('show'), 2000);
                localStorage.setItem('gif_tutorial_shown', 'true');
            }, 1000);
        }
    }

    // 设置视口高度（解决移动端地址栏问题）
    function setAppHeight() {
        document.documentElement.style.setProperty('--app-height', window.innerHeight + 'px');
    }

    // 事件绑定
    function bindEvents() {
        // 播放器事件
        dom.player.addEventListener('waiting', () => dom.loader.style.display = 'block');
        dom.player.addEventListener('playing', () => dom.loader.style.display = 'none');
        dom.player.addEventListener('click', togglePlay);
        dom.player.addEventListener('error', handleError);

        // 触摸手势
        const app = document.getElementById('app');
        app.addEventListener('touchstart', handleTouchStart, { passive: false });
        app.addEventListener('touchend', handleTouchEnd, { passive: false });
        
        // 错误处理（自动加载下一个，无需点击重试）
        dom.errorToast.addEventListener('click', () => {
            dom.errorToast.style.display = 'none';
            loadNextGif();
        });

        // 键盘支持
        document.addEventListener('keydown', (e) => {
            if (e.key === 'ArrowUp') loadNextGif();
            if (e.key === 'ArrowDown') loadPreviousGif();
            if (e.key === ' ') togglePlay();
        });
    }

    // 加载下一个GIF（真正随机逻辑）
    async function loadNextGif() {
        if (state.isLoading) return;

        // 如果当前指针不是在最后，说明在查看历史，直接前进一步
        if (state.currentIndex < state.history.length - 1) {
            state.currentIndex++;
            renderGif(state.history[state.currentIndex]);
            return;
        }

        // 否则请求新数据
        state.isLoading = true;
        dom.loader.style.display = 'block';
        
        try {
            const res = await fetch('/api/xhsWork/randomGif');
            const json = await res.json();
            
            if (json.code === 200 && json.data) {
                // 加入历史
                state.history.push(json.data);
                state.currentIndex = state.history.length - 1;
                
                // 限制历史长度
                if (state.history.length > 50) {
                    state.history.shift();
                    state.currentIndex--;
                }

                renderGif(json.data);
            } else {
                throw new Error(json.msg || '无法获取数据');
            }
        } catch (err) {
            console.error(err);
            showError();
        } finally {
            state.isLoading = false;
        }
    }

    // 加载上一个GIF
    function loadPreviousGif() {
        if (state.currentIndex > 0) {
            state.currentIndex--;
            renderGif(state.history[state.currentIndex]);
        } else {
             // 已经在第一个了，提示用户
             showToast('已经是第一个了');
        }
    }

    // 渲染GIF
    function renderGif(data) {
        state.currentData = data;
        
        // 重置UI
        dom.videoWrapper.classList.remove('show');
        dom.errorToast.style.display = 'none';
        dom.loader.style.display = 'block';
        
        // 设置信息
        dom.authorName.textContent = (data.authorNickname || '未知作者');
        dom.workTitle.textContent = data.workTitle || '无标题';
        dom.infoPanel.style.display = 'flex';

        // 设置视频
        dom.player.src = data.mediaUrl;
        dom.player.load();
        
        const playPromise = dom.player.play();
        if (playPromise !== undefined) {
            playPromise.then(() => {
                dom.videoWrapper.classList.add('show');
            }).catch(err => {
                console.log("Autoplay prevented:", err);
                // 显示播放按钮或提示用户点击
            });
        }
    }

    // 触摸开始
    function handleTouchStart(e) {
        state.touchStart.x = e.touches[0].clientX;
        state.touchStart.y = e.touches[0].clientY;
        state.touchStart.time = Date.now();
    }

    // 触摸结束
    function handleTouchEnd(e) {
        const touchEnd = {
            x: e.changedTouches[0].clientX,
            y: e.changedTouches[0].clientY
        };
        
        const diffX = touchEnd.x - state.touchStart.x;
        const diffY = touchEnd.y - state.touchStart.y;
        const timeDiff = Date.now() - state.touchStart.time;

        // 忽略误触
        if (timeDiff > 500) return;

        // 垂直滑动判断
        if (Math.abs(diffY) > Math.abs(diffX) && Math.abs(diffY) > 50) {
            if (diffY < 0) {
                // 上滑 -> 下一个
                loadNextGif();
            } else {
                // 下滑 -> 上一个
                loadPreviousGif();
            }
        } 
        // 水平滑动判断 (右滑返回)
        else if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 80) {
             if (diffX > 0) {
                 goHome();
             }
        }
    }

    // 切换播放
    function togglePlay() {
        if (dom.player.paused) {
            dom.player.play();
        } else {
            dom.player.pause();
        }
    }

    // 切换填充模式
    function toggleFill() {
        state.isFillMode = !state.isFillMode;
        if (state.isFillMode) {
            dom.player.classList.add('cover');
            dom.fillIcon.className = 'fas fa-compress';
        } else {
            dom.player.classList.remove('cover');
            dom.fillIcon.className = 'fas fa-expand';
        }
        event.stopPropagation();
    }

    // 操作函数
    function goHome() {
        window.location.href = '/';
    }

    function goToAuthor() {
        if (state.currentData && state.currentData.authorId) {
            window.location.href = '/downloadList?authorId=' + state.currentData.authorId;
        }
        event.stopPropagation();
    }

    function goToDetail() {
        if (state.currentData && state.currentData.workId) {
            window.location.href = '/downloadDetail?workId=' + state.currentData.workId;
        }
        event.stopPropagation();
    }

    function downloadFile() {
        if (state.currentData && state.currentData.mediaUrl) {
            const a = document.createElement('a');
            a.href = state.currentData.mediaUrl;
            a.download = 'gif_' + state.currentData.id + '.gif';
            a.target = '_blank';
            a.click();
        }
        event.stopPropagation();
    }

    function handleError() {
        dom.loader.style.display = 'none';
        // 自动加载下一个，不显示错误提示
        setTimeout(() => loadNextGif(), 500);
    }

    function showError() {
        dom.loader.style.display = 'none';
        // 自动加载下一个，不显示错误提示
        setTimeout(() => loadNextGif(), 500);
    }
    
    function showToast(msg) {
        // 简单Toast实现
        const toast = document.createElement('div');
        toast.style.cssText = `
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: rgba(0,0,0,0.7);
            color: #fff;
            padding: 10px 20px;
            border-radius: 20px;
            z-index: 100;
            pointer-events: none;
            backdrop-filter: blur(5px);
        `;
        toast.textContent = msg;
        document.body.appendChild(toast);
        setTimeout(() => toast.remove(), 1500);
    }

    // 启动
    window.addEventListener('app:page-resumed', function() {
        setAppHeight();
    });

    init();

</script>
</body>
</html>
