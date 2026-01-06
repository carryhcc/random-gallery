<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>图片下载 - 随机图库</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Poppins:wght@600;700&display=swap"
          rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <link rel="stylesheet" href="/css/style.css">
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
            <button class="btn btn-secondary btn-sm" onclick="window.location.href='/'">
                <i class="fas fa-home"></i>
                <span class="hidden-mobile">首页</span>
            </button>
            <button id="btnRefresh" class="btn btn-primary btn-sm">
                <i class="fas fa-sync-alt"></i>
                <span class="hidden-mobile">刷新</span>
            </button>
        </div>
    </div>
</header>

<!-- 主内容 -->
<main class="container gallery-container">
    <div id="toast" class="toast"></div>

    <!-- URL 输入区 -->
    <div class="url-input-section animate-fade-in">
        <div class="url-input-wrapper">
            <input 
                type="text" 
                id="urlInput" 
                class="url-input" 
                placeholder="请输入作品链接..."
                autocomplete="off">
            <button id="parseBtn" class="btn btn-primary parse-btn">
                <i class="fas fa-search"></i>
                <span>解析</span>
            </button>
        </div>
    </div>

    <!-- 作品列表 -->
    <div id="worksGrid" class="works-grid animate-fade-in"></div>

    <!-- 空状态 -->
    <div id="emptyState" class="empty-state hidden">
        <i class="fas fa-inbox"></i>
        <p>暂无解析数据</p>
        <p style="font-size: var(--font-size-sm); margin-top: 0.5rem;">请输入链接并点击解析</p>
    </div>

    <!-- 加载中 -->
    <div id="loading" class="loading hidden">
        <div class="spinner"></div>
        <span>加载更多作品...</span>
    </div>

    <!-- 加载完成 -->
    <div id="end" class="text-center hidden end-message">
        <i class="fas fa-check-circle end-icon"></i>
        <p>已加载全部作品</p>
    </div>

    <div id="sentinel" class="sentinel"></div>
</main>

<script>
    const urlInput = document.getElementById('urlInput');
    const parseBtn = document.getElementById('parseBtn');
    const worksGrid = document.getElementById('worksGrid');
    const emptyState = document.getElementById('emptyState');
    const toast = document.getElementById('toast');
    const btnRefresh = document.getElementById('btnRefresh');
    const loadingEl = document.getElementById('loading');
    const endEl = document.getElementById('end');

    let page = 0;
    let isLoading = false;
    let hasMore = true;
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
                showToast('解析任务已添加，请稍后手动刷新查看', 'success');
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

    // 创建作品卡片
    function createWorkCard(work) {
        const card = document.createElement('div');
        card.className = 'work-card';
        card.onclick = () => {
            window.location.href = '/downloadDetail?workId=' + encodeURIComponent(work.workId);
        };

        const coverUrl = work.coverImageUrl || '';
        const title = work.workTitle || '无标题';
        const author = work.authorNickname || '未知作者';
        const imageCount = work.imageCount || 0;
        const gifCount = work.gifCount || 0;

        card.innerHTML = 
            (coverUrl ? '<img src="' + coverUrl + '" alt="' + title + '" class="work-cover" loading="lazy">' : '<div class="work-cover"></div>') +
            '<div class="work-info">' +
                '<div class="work-title">' + title + '</div>' +
                '<div class="work-meta">' +
                    '<div class="work-author">' +
                        '<i class="fas fa-user"></i>' +
                        '<span>' + author + '</span>' +
                    '</div>' +
                    '<div class="work-badges">' +
                        (imageCount > 0 ? '<span class="badge"><i class="fas fa-image"></i> ' + imageCount + '</span>' : '') +
                        (gifCount > 0 ? '<span class="badge gif"><i class="fas fa-film"></i> ' + gifCount + '</span>' : '') +
                    '</div>' +
                '</div>' +
            '</div>';

        return card;
    }

    // 加载作品列表
    async function loadPage(reset = false) {
        if (isLoading) return;
        isLoading = true;
        loadingEl.classList.remove('hidden');
        if (reset) {
            endEl.classList.add('hidden');
            emptyState.classList.add('hidden');
        }

        try {
            if (reset) {
                page = 0;
                worksGrid.innerHTML = '';
                hasMore = true;
            }

            const response = await fetch('/api/xhsWork/list?page=' + page);
            const result = await response.json();

            if (result.code === 200 && result.data) {
                const works = result.data.works || [];
                hasMore = result.data.hasMore || false;

                if (works.length === 0 && page === 0) {
                    emptyState.classList.remove('hidden');
                } else {
                    emptyState.classList.add('hidden');
                    works.forEach(work => {
                        worksGrid.appendChild(createWorkCard(work));
                    });
                    page++;

                    if (!hasMore) {
                        endEl.classList.remove('hidden');
                    }
                }
            } else {
                showToast(result.message || '加载失败', 'error');
            }
        } catch (error) {
            console.error('加载失败:', error);
            showToast('网络请求失败', 'error');
        } finally {
            isLoading = false;
            loadingEl.classList.add('hidden');
        }
    }

    // 刷新
    btnRefresh.addEventListener('click', () => {
        loadPage(true);
    });

    // 无限滚动
    document.addEventListener('DOMContentLoaded', () => {
        loadPage(true);

        const sentinel = document.getElementById('sentinel');
        if ('IntersectionObserver' in window && sentinel) {
            const io = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting && !isLoading && hasMore) {
                        loadPage(false);
                    }
                });
            }, { root: null, rootMargin: '400px', threshold: 0 });
            io.observe(sentinel);
        }
    });
</script>
</body>
</html>
