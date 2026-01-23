<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="referrer" content="no-referrer">
    <title>下载浏览 - 随机图库</title>
    <link rel="preconnect" href="https://fonts.loli.net">
    <link rel="preconnect" href="https://gstatic.loli.net" crossorigin>
    <link href="https://fonts.loli.net/css2?family=Inter:wght@400;500;600;700&family=Poppins:wght@600;700&display=swap"
          rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.bootcdn.net/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <link rel="stylesheet" href="https://cdn.bootcdn.net/ajax/libs/choices.js/10.2.0/choices.min.css">
    <link rel="stylesheet" href="/css/style.css">
    <script src="https://cdn.bootcdn.net/ajax/libs/choices.js/10.2.0/choices.min.js"></script>
    <script src="https://cdn.bootcdn.net/ajax/libs/masonry/4.2.2/masonry.pkgd.min.js"></script>
    <script src="https://cdn.bootcdn.net/ajax/libs/jquery.imagesloaded/5.0.0/imagesloaded.pkgd.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/heic2any@0.0.4/dist/heic2any.min.js"></script>
    <script src="/js/heic-converter.js"></script>
    <script src="/js/theme.js"></script>
    <style>
        .choices {
            margin-bottom: 0;
            font-size: var(--font-size-base);
            overflow: visible;
            width: auto;
            min-width: 250px;
            max-width: 100%;
        }
        .choices__inner {
            background-color: var(--color-bg-primary);
            border: 1px solid var(--color-border);
            border-radius: var(--radius-md);
            min-height: 44px;
            color: var(--color-text-primary);
            display: flex;
            align-items: center;
        }
        .choices__list--dropdown {
            background-color: var(--color-bg-card);
            border: 1px solid var(--color-border);
            color: var(--color-text-primary);
            z-index: 100;
        }
        .choices__item--choice.is-highlighted {
            background-color: var(--color-bg-hover);
            color: var(--color-text-primary);
        }
        .choices__input {
            background-color: transparent !important;
            color: var(--color-text-primary) !important;
        }
        .choices__button {
            border-left: 1px solid var(--color-border);
            margin: 0 0 0 8px;
            padding-left: 8px;
            opacity: 0.6;
        }
        .choices__button:hover {
            opacity: 1;
        }

        /* 小红书风格的瀑布流布局 */
        .masonry-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
            gap: 16px;
            width: 100%;
        }

        @media (max-width: 768px) {
            .masonry-grid {
                grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
                gap: 12px;
            }
        }

        .masonry-item {
            break-inside: avoid;
            margin-bottom: 16px;
            background: var(--color-bg-card);
            border-radius: var(--radius-lg);
            overflow: hidden;
            cursor: pointer;
            transition: all var(--transition-fast);
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
        }

        .masonry-item:hover {
            transform: translateY(-4px);
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
        }

        .masonry-item img {
            width: 100%;
            height: auto;
            display: block;
            object-fit: cover;
        }

        .masonry-item-info {
            padding: 12px;
        }

        .masonry-item-title {
            font-size: var(--font-size-base);
            font-weight: 600;
            color: var(--color-text-primary);
            margin-bottom: 8px;
            overflow: hidden;
            text-overflow: ellipsis;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
        }

        .masonry-item-meta {
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-size: var(--font-size-sm);
            color: var(--color-text-secondary);
        }

        .masonry-item-author {
            display: flex;
            align-items: center;
            gap: 4px;
        }

        .masonry-item-counts {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .count-badge {
            display: flex;
            align-items: center;
            gap: 4px;
        }

        /* 搜索框样式 */
        .search-section {
            margin-bottom: 1.5rem;
        }

        .search-wrapper {
            position: relative;
            max-width: 600px;
            margin: 0 auto;
        }

        .search-input {
            width: 100%;
            padding: 12px 48px 12px 16px;
            border: 1px solid var(--color-border);
            border-radius: var(--radius-lg);
            background: var(--color-bg-primary);
            color: var(--color-text-primary);
            font-size: var(--font-size-base);
            transition: all var(--transition-fast);
        }

        .search-input:focus {
            outline: none;
            border-color: var(--color-primary);
            box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
        }

        .search-btn {
            position: absolute;
            right: 8px;
            top: 50%;
            transform: translateY(-50%);
            background: var(--color-primary);
            border: none;
            color: white;
            width: 36px;
            height: 36px;
            border-radius: var(--radius-md);
            cursor: pointer;
            transition: all var(--transition-fast);
        }

        .search-btn:hover {
            background: var(--color-primary-dark);
        }
    </style>
</head>
<body>

<!-- 导航栏 -->
<header class="navbar">
    <div class="navbar-content">
        <div class="navbar-brand">
            <i class="fas fa-list"></i>
            <span>下载浏览</span>
        </div>
        <div class="navbar-actions">
            <button class="btn btn-secondary btn-sm" onclick="window.location.href='/download'">
                <i class="fas fa-download"></i>
                <span class="hidden-mobile">图片下载</span>
            </button>
            <button class="btn btn-secondary btn-sm" onclick="window.location.href='/'">
                <i class="fas fa-home"></i>
                <span class="hidden-mobile">首页</span>
            </button>
        </div>
    </div>
</header>

<!-- 主内容 -->
<main class="container gallery-container">
    <div id="toast" class="toast"></div>

    <!-- 搜索区 -->
    <div class="search-section animate-fade-in">
        <div class="search-wrapper">
            <input 
                type="text" 
                id="searchInput" 
                class="search-input" 
                placeholder="搜索作品标题、描述..."
                autocomplete="off">
            <button id="searchBtn" class="search-btn">
                <i class="fas fa-search"></i>
            </button>
        </div>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-section animate-fade-in">
        <!-- Tab切换头部 -->
        <div class="filter-tabs">
            <button class="filter-tab active" data-tab="author" id="tabAuthor">
                <i class="fas fa-user"></i>
                <span>按作者筛选</span>
            </button>
            <button class="filter-tab" data-tab="tag" id="tabTag">
                <i class="fas fa-tag"></i>
                <span>按标签筛选</span>
            </button>
        </div>
        
        <!-- Tab内容区 -->
        <div class="filter-content">
            <!-- 作者筛选面板 -->
            <div class="filter-panel active" data-panel="author">
                <div>
                    <select id="authorFilter" class="filter-select">
                        <option value="">请选择作者</option>
                    </select>
                </div>
            </div>
            
            <!-- 标签筛选面板 -->
            <div class="filter-panel" data-panel="tag">
                <div>
                    <select id="tagFilter" class="filter-select">
                        <option value="">请选择标签</option>
                    </select>
                </div>
            </div>
        </div>
    </div>

    <!-- 作品列表 -->
    <div id="worksGrid" class="masonry-grid animate-fade-in"></div>

    <!-- 空状态 -->
    <div id="emptyState" class="empty-state hidden">
        <i class="fas fa-inbox"></i>
        <p>暂无作品数据</p>
        <p style="font-size: var(--font-size-sm); margin-top: 0.5rem;">请选择筛选条件或输入搜索关键词</p>
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
    const searchInput = document.getElementById('searchInput');
    const searchBtn = document.getElementById('searchBtn');
    const worksGrid = document.getElementById('worksGrid');
    const emptyState = document.getElementById('emptyState');
    const toast = document.getElementById('toast');
    const tabAuthor = document.getElementById('tabAuthor');
    const tabTag = document.getElementById('tabTag');
    const authorFilter = document.getElementById('authorFilter');
    const tagFilter = document.getElementById('tagFilter');
    const loadingEl = document.getElementById('loading');
    const endEl = document.getElementById('end');

    let page = 0;
    let isLoading = false;
    let hasMore = true;
    let toastTimer;
    let currentAuthorId = null;
    let currentTagId = null;
    let currentSearchStr = null;
    let currentTab = 'author';
    let masonryInstance = null;

    function showToast(message, type = 'success') {
        clearTimeout(toastTimer);
        toast.textContent = message;
        toast.className = 'toast show ' + type;
        toastTimer = setTimeout(() => {
            toast.className = 'toast';
        }, 3000);
    }

    // 加载作者和标签筛选列表
    async function loadFilters() {
        try {
            const commonConfig = {
                searchEnabled: true,
                itemSelectText: '',
                noResultsText: '无匹配结果',
                noChoicesText: '无可用选项',
                placeholder: true,
                searchPlaceholderValue: '搜索...',
                shouldSort: false,
                loadingText: '加载中...',
                removeItemButton: true,
            };

            // 初始化作者下拉框
            window.authorChoices = new Choices(authorFilter, {
                ...commonConfig,
            });

            // 初始化标签下拉框
            window.tagChoices = new Choices(tagFilter, {
                ...commonConfig,
            });

            // 加载作者列表
            const authorsRes = await fetch('/api/xhsWork/authors');
            const authorsData = await authorsRes.json();

            if (authorsData.code === 200 && authorsData.data) {
                const choicesData = authorsData.data.map(author => ({
                    value: String(author.authorId),
                    label: (author.authorNickname || author.authorId) + ' (' + (author.workCount || 0) + ')',
                    selected: false,
                    disabled: false,
                }));
                choicesData.unshift({ 
                    value: '', 
                    label: '请选择作者', 
                    selected: true, 
                    disabled: false,
                    placeholder: true 
                });
                
                window.authorChoices.setChoices(choicesData, 'value', 'label', true);
            }

            // 加载标签列表
            const tagsRes = await fetch('/api/xhsWork/tags');
            const tagsData = await tagsRes.json();

            if (tagsData.code === 200 && tagsData.data) {
                const choicesData = tagsData.data.map(tag => ({
                    value: String(tag.id), 
                    label: tag.tagName + ' (' + (tag.workCount || 0) + ')',
                    selected: false,
                    disabled: false,
                }));
                choicesData.unshift({ 
                    value: '', 
                    label: '请选择标签', 
                    selected: true, 
                    disabled: false,
                    placeholder: true 
                });
                
                window.tagChoices.setChoices(choicesData, 'value', 'label', true);
            }
        } catch (error) {
            console.error('加载筛选条件失败:', error);
            showToast('加载筛选条件失败: ' + error.message, 'error');
        }
    }

    // 创建作品卡片（小红书风格）
    function createWorkCard(work) {
        const card = document.createElement('div');
        card.className = 'masonry-item';
        card.onclick = () => {
            window.location.href = '/downloadDetail?workId=' + encodeURIComponent(work.workId);
        };

        const coverUrl = work.coverImageUrl || '';
        const title = work.workTitle || '无标题';
        const author = work.authorNickname || '未知作者';
        const imageCount = work.imageCount || 0;
        const gifCount = work.gifCount || 0;

        // 创建封面图片元素
        if (coverUrl) {
            const coverImg = document.createElement('img');
            coverImg.alt = title;
            coverImg.loading = 'lazy';
            card.appendChild(coverImg);
            // 使用 HEIC 转换工具异步设置图片源
            setImageSrc(coverImg, coverUrl).catch(err => {
                console.warn('封面图片加载失败:', coverUrl, err);
            });
        }
        
        // 创建作品信息区
        const workInfo = document.createElement('div');
        workInfo.className = 'masonry-item-info';
        workInfo.innerHTML = 
            '<div class="masonry-item-title">' + title + '</div>' +
            '<div class="masonry-item-meta">' +
                '<div class="masonry-item-author">' +
                    '<i class="fas fa-user"></i>' +
                    '<span>' + author + '</span>' +
                '</div>' +
                '<div class="masonry-item-counts">' +
                    (imageCount > 0 ? '<span class="count-badge"><i class="fas fa-image"></i> ' + imageCount + '</span>' : '') +
                    (gifCount > 0 ? '<span class="count-badge"><i class="fas fa-film"></i> ' + gifCount + '</span>' : '') +
                '</div>' +
            '</div>';
        card.appendChild(workInfo);

        return card;
    }

    // 初始化 Masonry
    function initMasonry() {
        if (!masonryInstance && worksGrid.children.length > 0) {
            masonryInstance = new Masonry(worksGrid, {
                itemSelector: '.masonry-item',
                percentPosition: true,
                gutter: 16
            });
            
            // 使用 imagesLoaded 确保图片加载后重新布局
            if (typeof imagesLoaded === 'function') {
                imagesLoaded(worksGrid).on('progress', function() {
                    masonryInstance.layout();
                });
            }
        }
    }

    // 更新 Masonry 布局
    function updateMasonry() {
        if (masonryInstance) {
            imagesLoaded(worksGrid).on('progress', function() {
                masonryInstance.reloadItems();
                masonryInstance.layout();
            });
        }
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
                masonryInstance = null;
            }

            // 构建URL，添加筛选参数
            let url = '/api/xhsWork/list?page=' + page + '&size=5';
            if (currentAuthorId) {
                url += '&authorId=' + encodeURIComponent(currentAuthorId);
            }
            if (currentTagId) {
                url += '&tagId=' + encodeURIComponent(currentTagId);
            }
            if (currentSearchStr) {
                url += '&str=' + encodeURIComponent(currentSearchStr);
            }
            const response = await fetch(url);
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
                    
                    // 初始化或更新 Masonry
                    if (!masonryInstance) {
                        setTimeout(() => initMasonry(), 100);
                    } else {
                        setTimeout(() => updateMasonry(), 100);
                    }
                    
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

    // 搜索功能
    searchBtn.addEventListener('click', () => {
        const str = searchInput.value.trim();
        currentSearchStr = str || null;
        loadPage(true);
    });

    // 回车键搜索
    searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            searchBtn.click();
        }
    });

    // Tab切换
    function switchTab(tabName) {
        currentTab = tabName;
        
        // 更新Tab按钮状态
        document.querySelectorAll('.filter-tab').forEach(tab => {
            tab.classList.remove('active');
        });
        document.querySelector('[data-tab="' + tabName + '"]').classList.add('active');
        
        // 更新面板显示
        document.querySelectorAll('.filter-panel').forEach(panel => {
            panel.classList.remove('active');
        });
        document.querySelector('[data-panel="' + tabName + '"]').classList.add('active');
        
        // 切换Tab时，清空另一个Tab的选择状态
        if (tabName === 'author') {
            if (window.tagChoices) {
                window.tagChoices.removeActiveItems();
                window.tagChoices.setChoiceByValue('');
            }
            if (tagFilter) tagFilter.value = '';
            currentTagId = null;
        } else if (tabName === 'tag') {
            if (window.authorChoices) {
                window.authorChoices.removeActiveItems();
                window.authorChoices.setChoiceByValue('');
            }
            if (authorFilter) authorFilter.value = '';
            currentAuthorId = null;
        }
    }

    // Tab点击事件
    tabAuthor.addEventListener('click', () => {
        switchTab('author');
    });

    tabTag.addEventListener('click', () => {
        switchTab('tag');
    });

    // 作者筛选自动查询
    authorFilter.addEventListener('change', function() {
        const value = this.value;
        if (value) {
            currentAuthorId = value;
            currentTagId = null;
            loadPage(true);
        } else {
            currentAuthorId = null;
            showEmptyState();
        }
    });

    // 标签筛选自动查询
    tagFilter.addEventListener('change', function() {
        const value = this.value;
        if (value) {
            currentTagId = value;
            currentAuthorId = null;
            loadPage(true);
        } else {
            currentTagId = null;
            showEmptyState();
        }
    });
    
    function showEmptyState() {
        worksGrid.innerHTML = '';
        loadingEl.classList.add('hidden'); 
        emptyState.classList.remove('hidden');
        endEl.classList.add('hidden');
        masonryInstance = null;
    }

    // 无限滚动
    document.addEventListener('DOMContentLoaded', async () => {
        // 加载筛选选项
        await loadFilters();
        
        // 读取URL参数
        const urlParams = new URLSearchParams(window.location.search);
        const authorIdParam = urlParams.get('authorId');
        const tagIdParam = urlParams.get('tagId');
        const tagNameParam = urlParams.get('tag');
        
        if (authorIdParam) {
            switchTab('author');
            if (window.authorChoices) {
                window.authorChoices.setChoiceByValue(authorIdParam);
                authorFilter.dispatchEvent(new Event('change'));
            }
        } else if (tagIdParam) {
            switchTab('tag');
            if (window.tagChoices) {
                window.tagChoices.setChoiceByValue(tagIdParam);
                tagFilter.dispatchEvent(new Event('change'));
            }
        } else if (tagNameParam) {
            switchTab('tag');
            const options = tagFilter.options;
            for (let i = 0; i < options.length; i++) {
                const optionText = options[i].textContent.trim();
                const tagName = optionText.split(' (')[0];
                if (tagName === tagNameParam) {
                    const matchedTagId = options[i].value;
                    if (window.tagChoices) {
                        window.tagChoices.setChoiceByValue(matchedTagId);
                        tagFilter.dispatchEvent(new Event('change'));
                    }
                    break;
                }
            }
        } else {
            // 默认显示空状态，不自动查询
            emptyState.classList.remove('hidden');
        }

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
