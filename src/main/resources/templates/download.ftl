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
    <script src="/js/theme.js"></script>
    <style>
        .download-container {
            max-width: 800px;
            margin: 2rem auto;
            padding: 2rem;
        }

        .download-card {
            background: var(--color-bg-card);
            border-radius: var(--radius-xl);
            padding: 2rem;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
        }

        .download-header {
            text-align: center;
            margin-bottom: 2rem;
        }

        .download-title {
            font-size: 2rem;
            font-weight: 700;
            color: var(--color-text-primary);
            margin-bottom: 0.5rem;
        }

        .download-subtitle {
            font-size: var(--font-size-base);
            color: var(--color-text-secondary);
        }

        .url-input-group {
            display: flex;
            gap: 1rem;
            margin-bottom: 1.5rem;
        }

        .url-input {
            flex: 1;
            padding: 14px 18px;
            border: 2px solid var(--color-border);
            border-radius: var(--radius-lg);
            background: var(--color-bg-primary);
            color: var(--color-text-primary);
            font-size: var(--font-size-base);
            transition: all var(--transition-fast);
        }

        .url-input:focus {
            outline: none;
            border-color: var(--color-primary);
            box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
        }

        .parse-button {
            padding: 14px 32px;
            background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
            color: white;
            border: none;
            border-radius: var(--radius-lg);
            font-size: var(--font-size-base);
            font-weight: 600;
            cursor: pointer;
            transition: all var(--transition-fast);
            white-space: nowrap;
        }

        .parse-button:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(102, 126, 234, 0.3);
        }

        .parse-button:disabled {
            opacity: 0.6;
            cursor: not-allowed;
            transform: none;
        }

        .info-section {
            background: var(--color-bg-secondary);
            border-radius: var(--radius-lg);
            padding: 1.5rem;
            margin-top: 2rem;
        }

        .info-title {
            font-size: var(--font-size-lg);
            font-weight: 600;
            color: var(--color-text-primary);
            margin-bottom: 1rem;
            display: flex;
            align-items: center;
            gap: 0.5rem;
        }

        .info-list {
            list-style: none;
            padding: 0;
            margin: 0;
        }

        .info-list li {
            color: var(--color-text-secondary);
            padding: 0.5rem 0;
            display: flex;
            align-items: flex-start;
            gap: 0.5rem;
        }

        .info-list li i {
            color: var(--color-primary);
            margin-top: 0.25rem;
        }

        .quick-links {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 1rem;
            margin-top: 2rem;
        }

        .quick-link-btn {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 0.5rem;
            padding: 1rem;
            background: var(--color-bg-secondary);
            border: 1px solid var(--color-border);
            border-radius: var(--radius-lg);
            color: var(--color-text-primary);
            text-decoration: none;
            transition: all var(--transition-fast);
            font-weight: 500;
        }

        .quick-link-btn:hover {
            background: var(--color-bg-hover);
            border-color: var(--color-primary);
            transform: translateY(-2px);
        }

        @media (max-width: 768px) {
            .download-container {
                padding: 1rem;
            }

            .download-card {
                padding: 1.5rem;
            }

            .download-title {
                font-size: 1.5rem;
            }

            .url-input-group {
                flex-direction: column;
            }

            .parse-button {
                width: 100%;
            }
        }
    </style>
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
                小红书作品下载
            </h1>
            <p class="download-subtitle">输入小红书作品链接，一键解析并保存</p>
        </div>

        <!-- URL 输入区 -->
        <div class="url-input-group">
            <input 
                type="text" 
                id="urlInput" 
                class="url-input" 
                placeholder="请粘贴小红书作品链接..."
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
                    <span>复制小红书作品链接（支持分享链接和网页链接）</span>
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
            const response = await fetch('/api/download/xhs', {
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
