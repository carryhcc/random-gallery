<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="referrer" content="no-referrer">
    <title>图片下载 - 随机图库</title>
    <link rel="preconnect" href="https://fonts.loli.net">
    <link rel="preconnect" href="https://gstatic.loli.net" crossorigin>
    <link href="https://fonts.loli.net/css2?family=Inter:wght@400;500;600;700&family=Poppins:wght@600;700&display=swap"
          rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.loli.net/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <link rel="stylesheet" href="/css/style.css">
    <link rel="stylesheet" href="/css/web.css" media="(min-width: 769px)">
    <link rel="stylesheet" href="/css/mobile.css" media="(max-width: 768px)">
    <link rel="stylesheet" href="/css/pages/download-web.css" media="(min-width: 769px)">
    <link rel="stylesheet" href="/css/pages/download-mobile.css" media="(max-width: 768px)">
    <script src="/js/theme.js"></script>
</head>
<body>

<!-- 导航栏 -->
<header class="navbar">
    <div class="navbar-content">
        <div class="navbar-brand">
            <i class="fas fa-download"></i>
            <span>图片下载</span>
        </div>
        <div class="navbar-actions">
            <button class="btn btn-secondary btn-sm" onclick="window.location.href='/downloadList'">
                <i class="fas fa-list"></i>
                <span class="hidden-mobile">下载浏览</span>
            </button>
            <button class="btn btn-secondary btn-sm" onclick="window.location.href='/'">
                <i class="fas fa-home"></i>
                <span class="hidden-mobile">首页</span>
            </button>
        </div>
    </div>
</header>

<!-- 主内容 -->
<main class="download-container">
    <div id="toast" class="toast"></div>

    <div class="download-card animate-fade-in">
        <div class="download-header">
            <h1 class="download-title">
                <i class="fas fa-cloud-download-alt"></i>
                作品下载
            </h1>
            <p class="download-subtitle">输入作品链接，一键解析并保存</p>
        </div>

        <!-- URL 输入区 -->
        <div class="url-input-group">
            <input 
                type="text" 
                id="urlInput" 
                class="url-input" 
                placeholder="请粘贴作品链接..."
                autocomplete="off">
            <button id="parseBtn" class="parse-button">
                <i class="fas fa-search"></i>
                <span>解析</span>
            </button>
        </div>

        <!-- 使用说明 -->
        <div class="info-section">
            <div class="info-title">
                <i class="fas fa-info-circle"></i>
                使用说明
            </div>
            <ul class="info-list">
                <li>
                    <i class="fas fa-check"></i>
                    <span>复制作品链接（支持分享链接和网页链接）</span>
                </li>
                <li>
                    <i class="fas fa-check"></i>
                    <span>粘贴到上方输入框，点击"解析"按钮</span>
                </li>
                <li>
                    <i class="fas fa-check"></i>
                    <span>系统将自动解析并保存作品的图片和视频</span>
                </li>
                <li>
                    <i class="fas fa-check"></i>
                    <span>解析完成后，前往"下载浏览"页面查看已下载的作品</span>
                </li>
            </ul>
        </div>

        <!-- 快捷链接 -->
        <div class="quick-links">
            <a href="/downloadList" class="quick-link-btn">
                <i class="fas fa-list"></i>
                <span>查看已下载作品</span>
            </a>
            <a href="/" class="quick-link-btn">
                <i class="fas fa-home"></i>
                <span>返回首页</span>
            </a>
        </div>
    </div>
</main>

<script>
    const urlInput = document.getElementById('urlInput');
    const parseBtn = document.getElementById('parseBtn');
    const toast = document.getElementById('toast');
    let toastTimer;

    function showToast(message, type = 'success') {
        clearTimeout(toastTimer);
        toast.textContent = message;
        toast.className = 'toast show ' + type;
        toastTimer = setTimeout(() => {
            toast.className = 'toast';
        }, 3000);
    }

    // 解析 URL
    parseBtn.addEventListener('click', async () => {
        const url = urlInput.value.trim();
        if (!url) {
            showToast('请输入链接', 'error');
            return;
        }

        parseBtn.disabled = true;
        parseBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> <span>解析中...</span>';

        try {
            const response = await fetch('/api/xhsWork/download', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ url: url })
            });

            const result = await response.json();
            if (result.code === 200) {
                showToast('解析任务已添加，请稍后前往"下载浏览"页面查看', 'success');
                urlInput.value = '';

            } else {
                showToast(result.message || '解析失败', 'error');
            }
        } catch (error) {
            console.error('解析失败:', error);
            showToast('网络请求失败', 'error');
        } finally {
            parseBtn.disabled = false;
            parseBtn.innerHTML = '<i class="fas fa-search"></i> <span>解析</span>';
        }
    });

    // 回车键解析
    urlInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            parseBtn.click();
        }
    });

</script>
</body>
</html>
