<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>随机画廊 - 随机图库</title>
    <link rel="preconnect" href="https://fonts.loli.net">
    <link rel="preconnect" href="https://gstatic.loli.net" crossorigin>
    <link href="https://fonts.loli.net/css2?family=Inter:wght@400;500;600;700&family=Poppins:wght@600;700&display=swap"
          rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.bootcdn.net/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <link rel="stylesheet" href="/css/style.css">
    <script src="/js/theme.js"></script>
</head>
<body>

<!-- 导航栏 -->
<header class="navbar">
    <div class="navbar-content">
        <div class="navbar-brand">
            <i class="fas fa-th"></i>
            <span>随机画廊</span>
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
    <div id="tip" class="toast hidden"></div>

    <div id="gallery" class="gallery-grid animate-fade-in"></div>

    <div id="loading" class="loading hidden">
        <div class="spinner"></div>
        <span>加载更多精彩图片...</span>
    </div>

    <div id="end" class="text-center hidden end-message">
        <i class="fas fa-check-circle end-icon"></i>
        <p>已加载全部图片</p>
    </div>

    <div id="sentinel" class="sentinel"></div>
</main>

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

        const imgContainer = document.createElement('div');
        imgContainer.className = 'gallery-image-container';

        const img = document.createElement('img');
        img.src = item.picUrl || '';
        img.alt = item.groupName || '';
        img.loading = 'lazy';

        const overlay = document.createElement('div');
        overlay.className = 'gallery-overlay';

        const name = document.createElement('div');
        name.className = 'group-name';
        name.textContent = item.groupName || '未命名分组';

        const id = document.createElement('div');
        id.className = 'group-id';
        id.textContent = 'ID: ' + (item.groupId ?? '-');

        overlay.appendChild(name);
        overlay.appendChild(id);
        imgContainer.appendChild(img);
        imgContainer.appendChild(overlay);
        div.appendChild(imgContainer);

        div.onclick = function () {
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
        loadingEl.classList.remove('hidden');
        if (reset) {
            endEl.classList.add('hidden');
        }

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
                    if (!reset) endEl.classList.remove('hidden');
                } else {
                    const processedList = images.map(item => ({
                        groupId: item.groupId,
                        groupName: item.groupName || '未命名分组',
                        picUrl: sanitizeUrl(item.groupUrl) || ''
                    }));

                    appendList(processedList);
                    page++;

                    if (!hasMore) {
                        endEl.classList.remove('hidden');
                    }
                }
            } else {
                showTip((result && result.message) || '获取图片失败', true);
                if (reset) {
                    endEl.classList.remove('hidden');
                }
            }
        } catch (e) {
            console.error('请求失败:', e);
            showTip('网络请求失败', true);
        } finally {
            isLoading = false;
            loadingEl.classList.add('hidden');
        }
    }

    function sanitizeUrl(url) {
        if (!url) return '';
        return String(url).trim().replace(/^[`\s]+|[`\s]+$/g, '');
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
            }, {root: null, rootMargin: '400px', threshold: 0});
            io.observe(sentinel);
        }

        window.addEventListener('scroll', () => {
            if (isLoading || !hasMore) return;
            if (nearBottomThreshold(300)) {
                loadPage(false);
            }
        }, {passive: true});

        window.addEventListener('touchmove', () => {
            if (isLoading || !hasMore) return;
            if (nearBottomThreshold(300)) {
                loadPage(false);
            }
        }, {passive: true});
    });
</script>
</body>
</html>
