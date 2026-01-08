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
    <link rel="stylesheet" href="https://cdn.bootcdn.net/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <link rel="stylesheet" href="https://cdn.bootcdn.net/ajax/libs/choices.js/10.2.0/choices.min.css">
    <link rel="stylesheet" href="/css/style.css">
    <script src="https://cdn.bootcdn.net/ajax/libs/choices.js/10.2.0/choices.min.js"></script>
    <script src="/js/theme.js"></script>
    <style>
        .choices {
            margin-bottom: 0;
            font-size: var(--font-size-base);
            overflow: visible; /* Ensure dropdown is not clipped */
            width: auto;      /* Adaptive width */
            min-width: 250px; /* Minimum width for usability */
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
            z-index: 100; /* Ensure dropdown is on top */
        }
        .choices__item--choice.is-highlighted {
            background-color: var(--color-bg-hover);
            color: var(--color-text-primary);
        }
        /* Fix input visibility in dark mode */
        .choices__input {
            background-color: transparent !important;
            color: var(--color-text-primary) !important;
        }
        /* Custom style for the remove item button provided by Choices.js */
        .choices__button {
            border-left: 1px solid var(--color-border);
            margin: 0 0 0 8px;
            padding-left: 8px;
            opacity: 0.6;
        }
        .choices__button:hover {
            opacity: 1;
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
                <div> <!-- Removed select-wrapper class to allow auto width -->
                    <select id="authorFilter" class="filter-select">
                        <option value="">请选择作者</option>
                    </select>
                </div>
            </div>
            
            <!-- 标签筛选面板 -->
            <div class="filter-panel" data-panel="tag">
                <div> <!-- Removed select-wrapper class to allow auto width -->
                    <select id="tagFilter" class="filter-select">
                        <option value="">请选择标签</option>
                    </select>
                </div>
            </div>
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
    const tabAuthor = document.getElementById('tabAuthor');
    const tabTag = document.getElementById('tabTag');
    const authorFilter = document.getElementById('authorFilter');
    const tagFilter = document.getElementById('tagFilter');
    // Removed old clear buttons references
    const loadingEl = document.getElementById('loading');
    const endEl = document.getElementById('end');

    let page = 0;
    let isLoading = false;
    let hasMore = true;
    let toastTimer;
    let currentAuthorId = null;
    let currentTagId = null;
    let currentTab = 'author';

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
        console.log('开始加载筛选列表...');
        try {
            // 初始化 Choices 实例配置
            const commonConfig = {
                searchEnabled: true,
                itemSelectText: '',
                noResultsText: '无匹配结果',
                noChoicesText: '无可用选项',
                placeholder: true,
                searchPlaceholderValue: '搜索...',
                shouldSort: false,
                loadingText: '加载中...',
                removeItemButton: true, // Enable built-in clear button
            };

            // 初始化作者下拉框
            const authorSelect = document.getElementById('authorFilter');
            window.authorChoices = new Choices(authorSelect, {
                ...commonConfig,
            });

            // 初始化标签下拉框
            const tagSelect = document.getElementById('tagFilter');
            window.tagChoices = new Choices(tagSelect, {
                ...commonConfig,
            });

            // 加载作者列表
            console.log('正在请求作者列表...');
            const authorsRes = await fetch('/api/xhsWork/authors');
            const authorsData = await authorsRes.json();
            console.log('作者列表响应:', authorsData);

            if (authorsData.code === 200 && authorsData.data) {
                const choicesData = authorsData.data.map(author => ({
                    value: String(author.authorId),
                    label: (author.authorNickname || author.authorId) + ' (' + (author.workCount || 0) + ')',
                    selected: false,
                    disabled: false,
                }));
                // 添加默认空选项 (placeholder) - Choices need this to know what is 'empty'
                choicesData.unshift({ 
                    value: '', 
                    label: '请选择作者', 
                    selected: true, 
                    disabled: false, // Must be false to allow re-selection if needed, but placeholder: true handles it
                    placeholder: true 
                });
                
                window.authorChoices.setChoices(choicesData, 'value', 'label', true);
            }

            // 加载标签列表
            console.log('正在请求标签列表...');
            const tagsRes = await fetch('/api/xhsWork/tags');
            const tagsData = await tagsRes.json();
            console.log('标签列表响应:', tagsData);

            if (tagsData.code === 200 && tagsData.data) {
                const choicesData = tagsData.data.map(tag => ({
                    value: String(tag.id), 
                    label: tag.tagName + ' (' + (tag.workCount || 0) + ')',
                    selected: false,
                    disabled: false,
                }));
                // 添加默认空选项 (placeholder)
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

            // 构建URL，添加筛选参数
            let url = '/api/xhsWork/list?page=' + page;
            if (currentAuthorId) {
                url += '&authorId=' + encodeURIComponent(currentAuthorId);
            }
            if (currentTagId) {
                url += '&tagId=' + encodeURIComponent(currentTagId);
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

    // Removed updateClearButtonVisibility function

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
                window.tagChoices.removeActiveItems(); // Use API method specifically for clearing selection
                window.tagChoices.setChoiceByValue(''); // Ensure logic value is also cleared
            }
            // 同时也清空原生值以防万一
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
        // Choices.js 会触发原 select 的 change 事件
        const value = this.value;
        // Removed visibility update
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
        // Removed visibility update
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
    }

    // Removed btnClearAuthor/Tag listeners

    // 无限滚动
    document.addEventListener('DOMContentLoaded', async () => {
        // 加载筛选选项（等待加载完成）
        await loadFilters();
        
        // 读取URL参数
        const urlParams = new URLSearchParams(window.location.search);
        const authorIdParam = urlParams.get('authorId');
        const tagIdParam = urlParams.get('tagId');
        const tagNameParam = urlParams.get('tag'); // 支持通过tag名称筛选
        
        if (authorIdParam) {
            // 通过作者ID筛选
            switchTab('author');
            if (window.authorChoices) {
                window.authorChoices.setChoiceByValue(authorIdParam);
                // 手动触发 change 事件，因为 setChoiceByValue 可能不会触发原生 change
                authorFilter.dispatchEvent(new Event('change'));
            }
        } else if (tagIdParam) {
            // 通过标签ID筛选
            switchTab('tag');
            if (window.tagChoices) {
                window.tagChoices.setChoiceByValue(tagIdParam);
                tagFilter.dispatchEvent(new Event('change'));
            }
        } else if (tagNameParam) {
            // 通过标签名称筛选（详情页跳转时使用）
            switchTab('tag');
            
            // 使用 Choices API 查找匹配的选项
            let found = false;
            // 获取所有 choices (需要 access internal API or recreate logic if strict mapping needed)
            // 简单做法：遍历原始数据或 choices 实例状态看起来比较困难，
            // 但我们在 loadFilters 把数据存进去了。
            // 更稳妥的方式：因为我们已经setChoices了，所以可以直接遍历原生options（Choices会保留它们但隐藏）
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
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                console.warn('未找到匹配的标签:', tagNameParam);
                showToast('未找到标签: ' + tagNameParam, 'error');
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
