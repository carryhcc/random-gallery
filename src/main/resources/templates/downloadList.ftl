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
    <script src="/js/theme.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/heic2any@0.0.4/dist/heic2any.min.js"></script>
    <script src="/js/heic-converter.js"></script>
    <script src="https://cdnjs.loli.net/ajax/libs/jquery.imagesloaded/5.0.0/imagesloaded.pkgd.min.js"></script>
    <script src="https://cdnjs.loli.net/ajax/libs/masonry/4.2.2/masonry.pkgd.min.js"></script>
    <style>
        /* --- [1. 基础布局及 Masonry 桌面端适配] --- */
        .masonry-grid {
            display: block; /* 必须为 block，Masonry 才能计算绝对定位 */
            position: relative;
            width: 100%;
            margin: 0 auto;
        }

        /* --- [2. 核心修复：移动端样式隔离] --- */
        @media (max-width: 768px) {
            .masonry-grid {
                display: grid !important;
                grid-template-columns: repeat(2, 1fr);
                gap: 12px;
                height: auto !important; /* 覆盖 Masonry 计算的高度 */
            }
            .masonry-item {
                position: relative !important;
                top: 0 !important;
                left: 0 !important;
                width: 100% !important;
                margin-bottom: 0 !important;
            }
            .masonry-grid.single-column {
                grid-template-columns: 1fr;
            }
        }

        /* --- [3. 还原您的全部小红书样式细节] --- */
        .masonry-item {
            /* 桌面端默认宽度，Masonry 初始化后会根据 gutter 自动调整 */
            width: calc(33.333% - 16px);
            margin-bottom: 16px;
            background: var(--color-bg-card);
            border-radius: var(--radius-lg);
            overflow: hidden;
            cursor: pointer;
            transition: all var(--transition-fast);
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
            display: inline-block;
            vertical-align: top;
        }

        .masonry-item:hover {
            transform: translateY(-4px);
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
        }

        .masonry-item img {
            width: 100%; height: auto; display: block; object-fit: cover;
        }

        .masonry-item-info { padding: 12px; }

        .masonry-item-title {
            font-size: var(--font-size-base); font-weight: 600;
            color: var(--color-text-primary); margin-bottom: 8px;
            overflow: hidden; text-overflow: ellipsis;
            display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
        }

        .masonry-item-meta {
            display: flex; align-items: center; justify-content: space-between;
            font-size: var(--font-size-sm); color: var(--color-text-secondary);
        }

        .masonry-item-author { display: flex; align-items: center; gap: 4px; }

        .count-badge { display: flex; align-items: center; gap: 4px; }

        /* --- [4. 还原搜索及推荐区域样式] --- */
        .search-section { margin-bottom: 1.5rem; }
        .search-wrapper { position: relative; max-width: 100%; }
        .search-input {
            width: 100%; padding: 14px 100px 14px 16px;
            border: 2px solid var(--color-border); border-radius: var(--radius-lg);
            background: var(--color-bg-primary); color: var(--color-text-primary);
            font-size: var(--font-size-base); transition: all var(--transition-fast);
        }
        .search-input:focus { outline: none; border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1); }
        .search-btn {
            position: absolute; right: 8px; top: 50%; transform: translateY(-50%);
            background: var(--color-primary); border: none; color: white;
            width: 40px; height: 40px; border-radius: var(--radius-md); cursor: pointer;
            display: flex; align-items: center; justify-content: center;
        }
        .more-btn {
            position: absolute; right: 56px; top: 50%; transform: translateY(-50%);
            background: var(--color-bg-secondary); border: 1px solid var(--color-border);
            color: var(--color-text-secondary); padding: 0 16px; height: 40px;
            border-radius: var(--radius-md); cursor: pointer; display: flex; align-items: center; gap: 6px; font-size: var(--font-size-sm);
        }
        .more-btn.active i { transform: rotate(180deg); }

        /* --- [5. 还原推荐列表样式] --- */
        .recommendation-section {
            margin-bottom: 1rem; background: var(--color-bg-card);
            border-radius: var(--radius-lg); padding: 1rem; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
        }
        .recommendation-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; }
        .recommendation-header h3 { font-size: var(--font-size-base); font-weight: 600; color: var(--color-text-primary); margin: 0; display: flex; align-items: center; gap: 0.5rem; }
        .refresh-btn {
            display: flex; align-items: center; gap: 0.5rem; padding: 6px 12px;
            background: var(--color-bg-secondary); border: 1px solid var(--color-border);
            border-radius: var(--radius-md); color: var(--color-text-secondary); cursor: pointer; font-size: var(--font-size-xs);
        }
        .recommendation-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); gap: 8px; }

        @media (max-width: 768px) {
            .recommendation-grid { grid-template-columns: repeat(auto-fill, minmax(80px, 1fr)); gap: 6px; }
        }

        .recommendation-item {
            background: var(--color-bg-secondary); border: 2px solid var(--color-border);
            border-radius: var(--radius-md); padding: 8px 6px; text-align: center;
            cursor: pointer; transition: all var(--transition-fast); overflow: hidden;
            min-height: 60px; display: flex; flex-direction: column; justify-content: center;
        }
        .recommendation-item:hover { border-color: var(--color-primary); background: var(--color-bg-hover); transform: translateY(-2px); }
        .recommendation-item.active { border-color: var(--color-primary); background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%); }
        .recommendation-name { font-weight: 600; font-size: var(--font-size-sm); color: var(--color-text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .recommendation-count { font-size: 11px; color: var(--color-text-secondary); }

        .clear-filter-btn {
            margin-top: 0.75rem; display: none; align-items: center; justify-content: center;
            gap: 0.5rem; padding: 8px 16px; background: var(--color-bg-secondary);
            border: 1px solid var(--color-border); border-radius: var(--radius-md);
            color: var(--color-text-secondary); cursor: pointer; width: 100%; font-size: var(--font-size-sm);
        }
        .clear-filter-btn.show { display: flex; }

        .recommendations-container { max-height: 0; overflow: hidden; transition: max-height 0.3s ease-out; }
        .recommendations-container.show { max-height: 1500px; transition: max-height 0.5s ease-in; }
    </style>
</head>
<body>

<header class="navbar">
    <div class="navbar-content">
        <div class="navbar-brand"><i class="fas fa-list"></i> <span>下载浏览</span></div>
        <div class="navbar-actions">
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
            <button id="moreBtn" class="more-btn"><i class="fas fa-chevron-down"></i> <span>更多</span></button>
            <button id="searchBtn" class="search-btn"><i class="fas fa-search"></i></button>
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

    var page = 1, isLoading = false, hasMore = true;
    var currentAuthorId = null, currentTagId = null, currentSearchStr = null;
    var masonryInstance = null;
    var allAuthors = [], allTags = [];
    var RECOMMENDATION_SIZE = 8;

    // --- 核心布局管理 ---
    function isMobile() { return window.innerWidth <= 768; }

    function manageLayout() {
        if (isMobile()) {
            if (masonryInstance) {
                masonryInstance.destroy();
                masonryInstance = null;
            }
            updateGridMode();
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

    function updateGridMode() {
        if (worksGrid.querySelectorAll('.masonry-item').length <= 1) {
            worksGrid.classList.add('single-column');
        } else {
            worksGrid.classList.remove('single-column');
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
        }

        try {
            var url = '/api/xhsWork/list?page=' + page + '&size=10';
            if (currentAuthorId) url += '&authorId=' + encodeURIComponent(currentAuthorId);
            if (currentTagId) url += '&tagId=' + encodeURIComponent(currentTagId);
            if (currentSearchStr) url += '&str=' + encodeURIComponent(currentSearchStr);

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
        container.innerHTML = list.map(function(a) {
            var active = currentAuthorId === a.authorId ? 'active' : '';
            return '<div class="recommendation-item ' + active + '" onclick="selectAuthor(\'' + a.authorId + '\')">' +
                '<div class="recommendation-name">' + (a.authorNickname || a.authorId) + '</div>' +
                '<div class="recommendation-count">' + (a.workCount || 0) + ' 作品</div>' +
                '</div>';
        }).join('');
    }

    function displayTagRecommendations() {
        var container = document.getElementById('tagRecommendations');
        var list = allTags.slice().sort(function() { return 0.5 - Math.random(); }).slice(0, RECOMMENDATION_SIZE);
        container.innerHTML = list.map(function(t) {
            var active = currentTagId === t.id ? 'active' : '';
            return '<div class="recommendation-item ' + active + '" onclick="selectTag(' + t.id + ')">' +
                '<div class="recommendation-name">' + t.tagName + '</div>' +
                '<div class="recommendation-count">' + (t.workCount || 0) + ' 作品</div>' +
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
            if(v) { currentSearchStr = v; currentAuthorId = null; currentTagId = null; updateUIStates(); loadPage(true); }
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