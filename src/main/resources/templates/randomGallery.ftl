<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>随机画廊 - 随机图库</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Poppins:wght@600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <link rel="stylesheet" href="/css/style.css">
    <script src="/js/theme.js"></script>
    <style>
        .back-button {
            position: fixed;
            top: 1.5rem;
            left: 1.5rem;
            z-index: 1030;
        }
        
        .main-content {
            margin-top: 5rem;
            padding-bottom: 2rem;
        }
        
        .gallery-card {
            cursor: pointer;
            transition: all var(--transition-base);
        }
        
        .gallery-card:hover {
            transform: translateY(-4px);
        }
        
        .gallery-card img {
            transition: transform var(--transition-slow);
        }
        
        .gallery-card:hover img {
            transform: scale(1.05);
        }
    </style>
</head>
<body class="min-h-screen p-4">

<button class="btn btn-secondary back-button" onclick="window.location.href='/'">
    <i class="fas fa-arrow-left"></i>
    <span>返回首页</span>
</button>

<div class="container main-content">
    <div class="card animate-fade-in">
        <div class="card-header">
            <h1 class="card-title">
                <i class="fas fa-th" style="margin-right: 0.5rem;"></i>
                随机画廊
            </h1>
            <p class="card-subtitle">浏览精美的图片集合</p>
        </div>

        <div class="text-center" style="margin-bottom: var(--spacing-lg);">
            <button id="btnRefresh" class="btn btn-primary">
                <i class="fas fa-sync-alt"></i>
                <span>刷新</span>
            </button>
        </div>

        <div id="tip" class="toast hidden"></div>

        <div id="gallery" class="gallery-grid"></div>
        <div id="loading" class="text-center" style="padding: var(--spacing-xl); color: var(--color-text-tertiary); display: none;">
            <div class="spinner" style="margin: 0 auto var(--spacing-md);"></div>
            <span>加载中...</span>
        </div>
        <div id="end" class="text-center" style="padding: var(--spacing-xl); color: var(--color-text-tertiary); display: none;">
            <i class="fas fa-check-circle" style="font-size: 2rem; margin-bottom: var(--spacing-sm);"></i>
            <p>已加载全部</p>
        </div>
        <div id="sentinel" style="height: 1px;"></div>
    </div>
</div>

<script>
    const gallery = document.getElementById('gallery');
    const tip = document.getElementById('tip');
    const btnRefresh = document.getElementById('btnRefresh');
    const loadingEl = document.getElementById('loading');
    const endEl = document.getElementById('end');

    let page = 0;
    let isLoading = false;
    let hasMore = true;

    function showTip(text, isError) {
        tip.textContent = text;
        tip.className = 'toast show ' + (isError ? 'error' : 'success');
        setTimeout(() => {
            tip.className = 'toast';
        }, 2000);
    }

    function createCard(item) {
        const div = document.createElement('div');
        div.className = 'gallery-card';
        
        const img = document.createElement('img');
        img.src = item.picUrl || '';
        img.alt = item.groupName || '';
        img.loading = 'lazy';
        
        const meta = document.createElement('div');
        meta.className = 'meta';
        
        const name = document.createElement('div');
        name.className = 'name';
        name.textContent = item.groupName || '未命名分组';
        
        const id = document.createElement('div');
        id.className = 'id';
        id.textContent = 'ID: ' + (item.groupId ?? '-');
        
        meta.appendChild(name);
        meta.appendChild(id);
        div.appendChild(img);
        div.appendChild(meta);
        
        div.onclick = function() {
            if (item.groupId) {
                window.location.href = '/showPicList?groupId=' + item.groupId + '&groupName=' + encodeURIComponent(item.groupName || '');
            }
        };
        
        return div;
    }

    function appendList(list) {
        if (!list || list.length === 0) {
            return;
        }
        const frag = document.createDocumentFragment();
        list.forEach(it => frag.appendChild(createCard(it)));
        gallery.appendChild(frag);
        maybeLoadMoreIfShort();
    }

    async function loadPage(reset) {
        if (isLoading) return;
        isLoading = true;
        loadingEl.style.display = 'block';
        endEl.style.display = 'none';
        
        try {
            if (reset) {
                page = 0;
                gallery.innerHTML = '';
                hasMore = true;
            }

            const url = '/api/group/loadMore?page=' + page;
            const res = await fetch(url);
            const result = await res.json();

            if (result.code === 200 && result.data) {
                const images = result.data.images || [];
                hasMore = result.data.hasMore || false;

                if (images.length === 0) {
                    endEl.style.display = 'block';
                } else {
                    const processedList = images.map(item => ({
                        groupId: item.groupId,
                        groupName: item.groupUrl || '未命名分组',
                        picUrl: extractImageUrl(item.groupName) || ''
                    }));

                    appendList(processedList);
                    page++;
                    
                    if (!hasMore) {
                        endEl.style.display = 'block';
                    }
                }
            } else {
                showTip((result && result.message) || '获取图片失败', true);
                if (reset) {
                    endEl.style.display = 'block';
                }
            }
        } catch (e) {
            console.error('请求失败:', e);
            showTip('网络请求失败', true);
        } finally {
            isLoading = false;
            loadingEl.style.display = 'none';
        }
    }
    
    function extractImageUrl(text) {
        if (!text) return '';
        
        const backtickMatch = text.match(/`([^`]+)`/);
        if (backtickMatch && backtickMatch[1]) {
            return backtickMatch[1];
        }
        
        const urlMatch = text.match(/https?:\/\/[^\s]+/);
        if (urlMatch && urlMatch[0]) {
            return urlMatch[0];
        }
        
        return text;
    }

    function getScrollHeights() {
        const doc = document.documentElement;
        return {
            scrollTop: window.pageYOffset || doc.scrollTop || document.body.scrollTop || 0,
            viewportH: window.innerHeight || doc.clientHeight || 0,
            scrollHeight: Math.max(
                document.body.scrollHeight, doc.scrollHeight,
                document.body.offsetHeight, doc.offsetHeight
            )
        };
    }

    function nearBottomThreshold(threshold) {
        const m = getScrollHeights();
        return (m.scrollTop + m.viewportH) >= (m.scrollHeight - (threshold || 300));
    }

    function maybeLoadMoreIfShort() {
        const m = getScrollHeights();
        if (m.scrollHeight <= m.viewportH + 50 && hasMore && !isLoading) {
            loadPage(false);
        }
    }

    btnRefresh.addEventListener('click', () => {
        loadPage(true);
    });

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
        
        window.addEventListener('scroll', () => {
            if (isLoading || !hasMore) return;
            if (nearBottomThreshold(300)) {
                loadPage(false);
            }
        }, { passive: true });
        
        window.addEventListener('touchmove', () => {
            if (isLoading || !hasMore) return;
            if (nearBottomThreshold(300)) {
                loadPage(false);
            }
        }, { passive: true });
    });
</script>
</body>
</html>
