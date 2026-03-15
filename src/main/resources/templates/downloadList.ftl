<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="referrer" content="no-referrer">
    <title>下载浏览 - 随机图库</title>
    <link rel="preconnect" href="https://fonts.loli.net">
    <link rel="preconnect" href="https://gstatic.loli.net" crossorigin>
    <link href="https://fonts.loli.net/css2?family=Inter:wght@400;500;600;700&family=Poppins:wght@600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.loli.net/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <link rel="stylesheet" href="/css/style.css">
    <link rel="stylesheet" href="/css/web.css" media="(min-width: 769px)">
    <link rel="stylesheet" href="/css/mobile.css" media="(max-width: 768px)">
    <link rel="stylesheet" href="/css/pages/download-list-web.css" media="(min-width: 769px)">
    <link rel="stylesheet" href="/css/pages/download-list-mobile.css" media="(max-width: 768px)">
    <script src="/js/theme.js"></script>
    <script src="/js/heic-converter.js"></script>
    <script src="https://cdnjs.loli.net/ajax/libs/jquery.imagesloaded/5.0.0/imagesloaded.pkgd.min.js"></script>
    <script src="https://cdnjs.loli.net/ajax/libs/masonry/4.2.2/masonry.pkgd.min.js"></script>
</head>
<body>

<header class="navbar">
    <div class="navbar-content">
        <div class="navbar-brand"><i class="fas fa-list"></i> <span>下载浏览</span></div>
        <div class="navbar-actions">
            <button id="viewToggleBtn" class="btn btn-secondary btn-sm" title="切换到双列瀑布流">
                <i class="fas fa-grip-lines"></i>
                <span class="hidden-mobile">单列</span>
            </button>
            <button id="resetFiltersBtn" class="btn btn-secondary btn-sm" title="重置筛选条件">
                <i class="fas fa-rotate-left"></i>
                <span class="hidden-mobile">重置</span>
            </button>
            <button class="btn btn-secondary btn-sm" onclick="window.location.href='/download'"><i class="fas fa-download"></i> <span class="hidden-mobile">图片下载</span></button>
            <button class="btn btn-secondary btn-sm" onclick="window.location.href='/'"><i class="fas fa-home"></i> <span class="hidden-mobile">首页</span></button>
        </div>
    </div>
</header>

<main class="container gallery-container">
    <div id="toast" class="toast"></div>

    <div class="search-section animate-fade-in">
        <div class="search-wrapper">
            <input type="text" id="searchInput" class="search-input" placeholder="搜索作品标题、描述..." autocomplete="off">
            <div class="search-actions">
                <button id="moreBtn" class="more-btn"><i class="fas fa-chevron-down"></i> <span>更多</span></button>
                <button id="searchBtn" class="search-btn"><i class="fas fa-search"></i></button>
            </div>
        </div>
    </div>

    <div id="recommendationsContainer" class="recommendations-container">
        <div class="recommendation-section animate-fade-in">
            <div class="recommendation-header">
                <h3><i class="fas fa-user"></i> 作者推荐</h3>
                <button class="refresh-btn" onclick="refreshAuthors()"><i class="fas fa-sync-alt"></i> <span>下一批</span></button>
            </div>
            <div id="authorRecommendations" class="recommendation-grid"></div>
        </div>

        <div class="recommendation-section animate-fade-in">
            <div class="recommendation-header">
                <h3><i class="fas fa-tag"></i> 标签推荐</h3>
                <button class="refresh-btn" onclick="refreshTags()"><i class="fas fa-sync-alt"></i> <span>下一批</span></button>
            </div>
            <div id="tagRecommendations" class="recommendation-grid"></div>
            <button id="clearFilterBtn" class="clear-filter-btn"><i class="fas fa-times-circle"></i> <span>清除筛选</span></button>
        </div>
    </div>

    <div id="worksGrid" class="masonry-grid animate-fade-in"></div>

    <div id="emptyState" class="empty-state hidden">
        <i class="fas fa-inbox"></i>
        <p>暂无作品数据</p>
    </div>

    <div id="loading" class="loading hidden"><div class="spinner"></div> <span>加载更多作品...</span></div>
    <div id="end" class="text-center hidden end-message"><i class="fas fa-check-circle"></i> <p>已加载全部作品</p></div>
    <div id="sentinel" class="sentinel"></div>
</main>

<script>
    // 变量初始化
    var searchInput = document.getElementById('searchInput');
    var searchBtn = document.getElementById('searchBtn');
    var moreBtn = document.getElementById('moreBtn');
    var recommendationsContainer = document.getElementById('recommendationsContainer');
    var worksGrid = document.getElementById('worksGrid');
    var emptyState = document.getElementById('emptyState');
    var loadingEl = document.getElementById('loading');
    var endEl = document.getElementById('end');
    var clearFilterBtn = document.getElementById('clearFilterBtn');
    var resetFiltersBtn = document.getElementById('resetFiltersBtn');
    var viewToggleBtn = document.getElementById('viewToggleBtn');

    var page = 1, isLoading = false, hasMore = true;
    var currentAuthorId = null, currentTagId = null, currentSearchStr = null;
    var currentSeed = Math.floor(Math.random() * 1000000); // 初始化随机种子
    var masonryInstance = null;
    var allAuthors = [], allTags = [];
    var RECOMMENDATION_SIZE = 8;
    var VIEW_MODE_KEY = 'download-list-view-mode';
    var currentViewMode = 'single';

    // --- 核心布局管理 ---
    function isMobile() { return window.innerWidth <= 768; }

    function manageLayout() {
        if (isMobile()) {
            if (masonryInstance) {
                masonryInstance.destroy();
                masonryInstance = null;
            }
        } else {
            if (!masonryInstance && worksGrid.children.length > 0) {
                masonryInstance = new Masonry(worksGrid, {
                    itemSelector: '.masonry-item',
                    percentPosition: true,
                    gutter: 16
                });
            }
            if (masonryInstance) {
                imagesLoaded(worksGrid).on('progress', function() {
                    masonryInstance.layout();
                });
            }
        }
    }

    function setViewMode(mode) {
        currentViewMode = mode === 'double' ? 'double' : 'single';
        worksGrid.classList.remove('view-single', 'view-double');
        worksGrid.classList.add(currentViewMode === 'double' ? 'view-double' : 'view-single');
        localStorage.setItem(VIEW_MODE_KEY, currentViewMode);
        updateViewToggleButton();
        manageLayout();
    }

    function updateViewToggleButton() {
        if (!viewToggleBtn) return;
        var icon = viewToggleBtn.querySelector('i');
        var text = viewToggleBtn.querySelector('span');
        if (currentViewMode === 'single') {
            if (icon) icon.className = 'fas fa-grip-lines';
            if (text) text.textContent = '单列';
            viewToggleBtn.title = '切换到双列瀑布流';
        } else {
            if (icon) icon.className = 'fas fa-table-columns';
            if (text) text.textContent = '双列';
            viewToggleBtn.title = '切换到单列瀑布流';
        }
    }

    // --- 数据加载核心 (修复 FTL 冲突) ---
    async function loadPage(reset) {
        if (isLoading) return;
        isLoading = true;
        loadingEl.classList.remove('hidden');

        if (reset) {
            page = 1; worksGrid.innerHTML = ''; hasMore = true;
            endEl.classList.add('hidden'); emptyState.classList.add('hidden');
            if (masonryInstance) { masonryInstance.destroy(); masonryInstance = null; }
            // 每次重置列表（刷新/筛选）时，生成新的随机种子
            currentSeed = Math.floor(Math.random() * 1000000);
        }

        try {
            var url = '/api/xhsWork/list?page=' + page + '&size=10';
            if (currentAuthorId) url += '&authorId=' + encodeURIComponent(currentAuthorId);
            if (currentTagId) url += '&tagId=' + encodeURIComponent(currentTagId);
            if (currentTagId) url += '&tagId=' + encodeURIComponent(currentTagId);
            if (currentSearchStr) url += '&str=' + encodeURIComponent(currentSearchStr);
            // 只有在没有特定筛选条件（不是作者也不是标签筛选）且不是搜索时，才使用随机排序
            // 如果用户进行了筛选，通常期望按时间倒序查看相关内容，或者也可以随机，这里先对全列表应用随机
            // 需求是：每次重新点进去 下载浏览 打乱顺序
            if (!currentAuthorId && !currentTagId && !currentSearchStr) {
                 url += '&seed=' + currentSeed;
            }

            const response = await fetch(url);
            const result = await response.json();

            if (result.code === 200 && result.data) {
                var works = result.data.works || [];
                hasMore = result.data.hasMore || false;

                if (works.length === 0 && page === 1) {
                    emptyState.classList.remove('hidden');
                } else {
                    works.forEach(function(work) {
                        worksGrid.appendChild(createWorkCard(work));
                    });

                    imagesLoaded(worksGrid, function() {
                        manageLayout();
                    });

                    page++;
                    if (!hasMore) endEl.classList.remove('hidden');
                }
            }
        } catch (e) { console.error(e); } finally {
            isLoading = false;
            loadingEl.classList.add('hidden');
        }
    }

    function createWorkCard(work) {
        var card = document.createElement('div');
        card.className = 'masonry-item';
        card.onclick = function() { window.location.href = '/downloadDetail?workId=' + encodeURIComponent(work.workId); };

        var title = work.workTitle || '无标题';
        var author = work.authorNickname || '未知作者';
        var imgCount = work.imageCount || 0;
        var coverUrl = work.coverImageUrl || '';

        var html = '';
        if (coverUrl) {
            html += '<img src="" data-src="' + coverUrl + '" alt="' + title + '" onerror="this.onerror=null;this.src=\'/icons/404.svg\';">';
        }
        html += '<div class="masonry-item-info">';
        html += '  <div class="masonry-item-title">' + title + '</div>';
        html += '  <div class="masonry-item-meta">';
        html += '    <div class="masonry-item-author"><i class="fas fa-user"></i> <span>' + author + '</span></div>';
        html += '    <div class="masonry-item-counts">';
        if (imgCount > 0) html += '<span class="count-badge"><i class="fas fa-image"></i> ' + imgCount + '</span>';
        html += '    </div></div></div>';

        card.innerHTML = html;
        var img = card.querySelector('img');
        if (img && typeof setImageSrc === 'function') {
            setImageSrc(img, coverUrl).catch(function(e){});
        }
        return card;
    }

    // --- 推荐及列表刷新逻辑 ---
    window.refreshAuthors = function() { displayAuthorRecommendations(); };
    window.refreshTags = function() { displayTagRecommendations(); };

    function displayAuthorRecommendations() {
        var container = document.getElementById('authorRecommendations');
        var list = allAuthors.slice().sort(function() { return 0.5 - Math.random(); }).slice(0, RECOMMENDATION_SIZE);
        if (!list.length) {
            container.innerHTML = '<div class="recommendation-empty">暂无作者推荐</div>';
            return;
        }
        container.innerHTML = list.map(function(a) {
            var active = currentAuthorId === a.authorId ? 'active' : '';
            return '<div class="recommendation-item type-author ' + active + '" onclick="selectAuthor(\'' + a.authorId + '\')">' +
                '<div class="recommendation-name">' +
                    '<i class="fas fa-user-circle"></i>' +
                    '<span class="recommendation-title">' + (a.authorNickname || a.authorId) + '</span>' +
                    '<span class="recommendation-count-inline"><i class="fas fa-images"></i>' + (a.workCount || 0) + '</span>' +
                '</div>' +
                '</div>';
        }).join('');
    }

    function displayTagRecommendations() {
        var container = document.getElementById('tagRecommendations');
        var list = allTags.slice().sort(function() { return 0.5 - Math.random(); }).slice(0, RECOMMENDATION_SIZE);
        if (!list.length) {
            container.innerHTML = '<div class="recommendation-empty">暂无标签推荐</div>';
            return;
        }
        container.innerHTML = list.map(function(t) {
            var active = currentTagId === t.id ? 'active' : '';
            return '<div class="recommendation-item type-tag ' + active + '" onclick="selectTag(' + t.id + ')">' +
                '<div class="recommendation-name">' +
                    '<i class="fas fa-hashtag"></i>' +
                    '<span class="recommendation-title">' + t.tagName + '</span>' +
                    '<span class="recommendation-count-inline"><i class="fas fa-images"></i>' + (t.workCount || 0) + '</span>' +
                '</div>' +
                '</div>';
        }).join('');
    }

    window.selectAuthor = function(id) {
        currentAuthorId = id; currentTagId = null; currentSearchStr = null;
        searchInput.value = ''; updateUIStates(); loadPage(true); hideRecs();
    };

    window.selectTag = function(id) {
        currentTagId = id; currentAuthorId = null; currentSearchStr = null;
        searchInput.value = ''; updateUIStates(); loadPage(true); hideRecs();
    };

    function updateUIStates() {
        if (currentAuthorId || currentTagId || currentSearchStr) {
            clearFilterBtn.classList.add('show');
        } else {
            clearFilterBtn.classList.remove('show');
        }
        displayAuthorRecommendations(); displayTagRecommendations();
    }

    function hideRecs() {
        recommendationsContainer.classList.remove('show');
        moreBtn.classList.remove('active');
    }

    // --- 初始化及事件绑定 ---
    document.addEventListener('DOMContentLoaded', async function() {
        // 布局模式：默认单列，可切换双列
        var savedMode = localStorage.getItem(VIEW_MODE_KEY);
        currentViewMode = savedMode === 'double' ? 'double' : 'single';
        setViewMode(currentViewMode);

        if (viewToggleBtn) {
            viewToggleBtn.addEventListener('click', function() {
                setViewMode(currentViewMode === 'single' ? 'double' : 'single');
            });
        }

        if (resetFiltersBtn) {
            resetFiltersBtn.addEventListener('click', function() {
                currentAuthorId = null;
                currentTagId = null;
                currentSearchStr = null;
                searchInput.value = '';
                updateUIStates();
                hideRecs();
                loadPage(true);
            });
        }

        // 加载初始数据
        try {
            const [aRes, tRes] = await Promise.all([
                fetch('/api/xhsWork/authors').then(r => r.json()),
                fetch('/api/xhsWork/tags').then(r => r.json())
            ]);
            if(aRes.code === 200) allAuthors = aRes.data;
            if(tRes.code === 200) allTags = tRes.data;
            
            // 解析 URL 参数
            const params = new URLSearchParams(window.location.search);
            const authId = params.get('authorId');
            const tagName = params.get('tag');

            if (authId) {
                currentAuthorId = authId;
            } else if (tagName) {
                // 根据 tagName 查找 tagId
                const tagObj = allTags.find(t => t.tagName === tagName);
                if (tagObj) {
                    currentTagId = tagObj.id;
                }
            }
            
            // 更新UI状态
            updateUIStates(); 

            displayAuthorRecommendations(); displayTagRecommendations();
        } catch(e) {}

        // 搜索按钮
        searchBtn.onclick = function() {
            var v = searchInput.value.trim();
            if(v) { currentSearchStr = v; currentAuthorId = null; currentTagId = null; updateUIStates(); loadPage(true); hideRecs(); }
        };

        // 更多按钮
        moreBtn.onclick = function() {
            recommendationsContainer.classList.toggle('show');
            moreBtn.classList.toggle('active');
        };

        // 清除筛选
        clearFilterBtn.onclick = function() {
            currentAuthorId = null; currentTagId = null; currentSearchStr = null;
            searchInput.value = ''; updateUIStates(); loadPage(true);
        };

        // 滚动监听
        var observer = new IntersectionObserver(function(entries) {
            if (entries[0].isIntersecting && !isLoading && hasMore) { loadPage(false); }
        }, { rootMargin: '400px' });
        observer.observe(document.getElementById('sentinel'));

        // 窗口缩放处理
        var rt;
        window.onresize = function() { clearTimeout(rt); rt = setTimeout(manageLayout, 200); };

        loadPage(true);
    });
</script>
</body>
</html>
