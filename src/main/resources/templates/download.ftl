<!DOCTYPE html>
<html lang="zh-CN" data-theme="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="referrer" content="no-referrer">
    <title>图片下载</title>
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
        /* Toggle 开关样式 */
        .clipboard-toggle {
            display: flex;
            align-items: center;
            gap: 6px;
            padding: 4px 8px;
            border-radius: var(--radius-sm);
            background: var(--color-bg-card);
            border: 1px solid var(--color-border);
        }

        .clipboard-toggle-label {
            font-size: var(--font-size-sm);
            color: var(--color-text-secondary);
        }

        .toggle-switch {
            position: relative;
            display: inline-block;
            width: 36px;
            height: 20px;
            cursor: pointer;
        }

        .toggle-switch input {
            opacity: 0;
            width: 0;
            height: 0;
        }

        .toggle-slider {
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background-color: var(--color-text-muted);
            border-radius: var(--radius-full);
            transition: var(--transition-fast);
        }

        .toggle-slider::before {
            content: "";
            position: absolute;
            height: 16px;
            width: 16px;
            left: 2px;
            bottom: 2px;
            background-color: white;
            border-radius: 50%;
            transition: var(--transition-fast);
        }

        .toggle-switch input:checked + .toggle-slider {
            background-color: var(--color-primary);
        }

        .toggle-switch input:checked + .toggle-slider::before {
            transform: translateX(16px);
        }

        .toggle-switch input:focus-visible + .toggle-slider {
            outline: 2px solid var(--color-border-focus);
            outline-offset: 2px;
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
            <div class="clipboard-toggle" title="自动读取粘贴板">
                <span class="clipboard-toggle-label">
                    <i class="fas fa-clipboard"></i>
                </span>
                <label class="toggle-switch">
                    <input type="checkbox" id="autoReadClipboard" checked>
                    <span class="toggle-slider"></span>
                </label>
            </div>
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
    const autoReadToggle = document.getElementById('autoReadClipboard');
    const STORAGE_KEY = 'autoReadClipboard';
    let toastTimer;

    function showToast(message, type = 'success') {
        clearTimeout(toastTimer);
        toast.textContent = message;
        toast.className = 'toast show ' + type;
        toastTimer = setTimeout(() => {
            toast.className = 'toast';
        }, 3000);
    }

    // 开关状态持久化
    function loadToggleState() {
        const saved = localStorage.getItem(STORAGE_KEY);
        autoReadToggle.checked = saved !== 'false';
    }

    function saveToggleState() {
        localStorage.setItem(STORAGE_KEY, autoReadToggle.checked);
    }

    autoReadToggle.addEventListener('change', saveToggleState);
    loadToggleState();

    // 检测文本中是否包含 HTTP 链接
    function extractHttpUrl(text) {
        if (!text || typeof text !== 'string') return null;
        const match = text.match(/https?:\/\/\S+/i);
        return match ? match[0] : null;
    }

    // 防抖计时器
    let readClipboardTimer = null;
    const READ_DELAY = 300; // 延迟读取，等待页面完全激活

    // 读取粘贴板并填充链接（带防抖）
    function scheduleReadClipboard() {
        if (readClipboardTimer) clearTimeout(readClipboardTimer);
        readClipboardTimer = setTimeout(readClipboardAndFill, READ_DELAY);
    }

    // 读取粘贴板并填充链接
    async function readClipboardAndFill() {
        if (!autoReadToggle.checked) return;
        if (!navigator.clipboard || !navigator.clipboard.readText) return;

        try {
            const text = await navigator.clipboard.readText();
            const url = extractHttpUrl(text);
            if (url) {
                // 避免重复填充相同内容
                if (urlInput.value !== url) {
                    urlInput.value = url;
                    showToast('已从粘贴板读取链接', 'success');
                }
            }
        } catch (err) {
            // 静默处理权限错误
        }
    }

    // 页面加载时读取
    window.addEventListener('load', readClipboardAndFill);

    // 页面切换回来时读取（多种事件确保移动端兼容）
    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible') {
            scheduleReadClipboard();
        }
    });

    // window focus 事件（比 visibilitychange 更可靠）
    window.addEventListener('focus', scheduleReadClipboard);

    // pageshow 事件（处理浏览器前进后退、从后台恢复）
    window.addEventListener('pageshow', (event) => {
        // persisted 表示页面从 bfcache 恢复
        if (event.persisted) {
            scheduleReadClipboard();
        }
    });

    // 输入框获得焦点时也尝试读取
    urlInput.addEventListener('focus', scheduleReadClipboard);

    // 解析 URL
    parseBtn.addEventListener('click', async () => {
        const rawInput = urlInput.value.trim();
        if (!rawInput) {
            showToast('请输入链接', 'error');
            return;
        }

        // 从输入内容中提取链接
        const url = extractHttpUrl(rawInput) || rawInput;

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
