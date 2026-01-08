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
                <div class="select-wrapper">
                    <select id="authorFilter" class="filter-select">
                        <option value="">请选择作者</option>
                    </select>
                    <button id="btnClearAuthor" class="btn-clear" title="清空筛选">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
            </div>
            
            <!-- 标签筛选面板 -->
            <div class="filter-panel" data-panel="tag">
                <div class="select-wrapper">
                    <select id="tagFilter" class="filter-select">
                        <option value="">请选择标签</option>
                    </select>
                    <button id="btnClearTag" class="btn-clear" title="清空筛选">
                        <i class="fas fa-times"></i>
                    </button>
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
    const btnClearAuthor = document.getElementById('btnClearAuthor');
    const btnClearTag = document.getElementById('btnClearTag');
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
        try {
            // 加载作者列表
            const authorsRes = await fetch('/api/xhsWork/authors');
            const authorsData = await authorsRes.json();
            if (authorsData.code === 200 && authorsData.data) {
                const authorSelect = document.getElementById('authorFilter');
                authorsData.data.forEach(author => {
                    const option = document.createElement('option');
                    option.value = author.authorId;
                    const displayName = author.authorNickname || author.authorId;
                    const count = author.workCount || 0;
                    option.textContent = displayName + ' (' + count + ')';
                    authorSelect.appendChild(option);
                });
            }

            // 加载标签列表
            const tagsRes = await fetch('/api/xhsWork/tags');
            const tagsData = await tagsRes.json();
            if (tagsData.code === 200 && tagsData.data) {
                const tagSelect = document.getElementById('tagFilter');
                tagsData.data.forEach(tag => {
                    const option = document.createElement('option');
                    option.value = tag.id;
                    const count = tag.workCount || 0;
                    option.textContent = tag.tagName + ' (' + count + ')';
                    tagSelect.appendChild(option);
                });
            }
        } catch (error) {
            console.error('加载筛选条件失败:', error);
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

    // 辅助函数：更新清除按钮可见性
    function updateClearButtonVisibility(selectId, btnId) {
        const select = document.getElementById(selectId);
        const btn = document.getElementById(btnId);
        if (select && btn) {
            if (select.value) {
                btn.classList.add('show');
            } else {
                btn.classList.remove('show');
            }
        }
    }

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
            if (tagFilter) {
                tagFilter.value = '';
                updateClearButtonVisibility('tagFilter', 'btnClearTag');
                currentTagId = null;
            }
        } else if (tabName === 'tag') {
            if (authorFilter) {
                authorFilter.value = '';
                updateClearButtonVisibility('authorFilter', 'btnClearAuthor');
                currentAuthorId = null;
            }
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
        updateClearButtonVisibility('authorFilter', 'btnClearAuthor');
        if (this.value) {
            currentAuthorId = this.value;
            currentTagId = null;
            loadPage(true);
        } else {
             currentAuthorId = null;
             showEmptyState();
        }
    });

    // 标签筛选自动查询
    tagFilter.addEventListener('change', function() {
        updateClearButtonVisibility('tagFilter', 'btnClearTag');
        if (this.value) {
            currentTagId = this.value;
            currentAuthorId = null;
            loadPage(true);
        } else {
            currentTagId = null;
            showEmptyState();
        }
    });
    
    function showEmptyState() {
        worksGrid.innerHTML = '';
        loadMoreBtn.classList.add('hidden');
        emptyState.classList.remove('hidden');
        endEl.classList.add('hidden');
    }

    // 清除按钮点击事件
    btnClearAuthor.addEventListener('click', function(e) {
        e.stopPropagation();
        authorFilter.value = '';
        updateClearButtonVisibility('authorFilter', 'btnClearAuthor');
        currentAuthorId = null;
        showEmptyState();
    });

    btnClearTag.addEventListener('click', function(e) {
        e.stopPropagation();
        tagFilter.value = '';
        updateClearButtonVisibility('tagFilter', 'btnClearTag');
        currentTagId = null;
        showEmptyState();
    });

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
            authorFilter.value = authorIdParam;
            updateClearButtonVisibility('authorFilter', 'btnClearAuthor');
            currentAuthorId = authorIdParam;
            loadPage(true);
        } else if (tagIdParam) {
            // 通过标签ID筛选
            switchTab('tag');
            tagFilter.value = tagIdParam;
            updateClearButtonVisibility('tagFilter', 'btnClearTag');
            currentTagId = tagIdParam;
            loadPage(true);
        } else if (tagNameParam) {
            // 通过标签名称筛选（详情页跳转时使用）
            switchTab('tag');
            // 数据已经加载完成，直接查找
            const options = tagFilter.options;
            let found = false;
            console.log('查找标签:', tagNameParam, '总选项数:', options.length);
            
            for (let i = 0; i < options.length; i++) {
                const optionText = options[i].textContent.trim();
                // 提取标签名称（去掉数量部分）
                const tagName = optionText.split(' (')[0];
                
                console.log('比对选项:', optionText, '提取标签名:', tagName);
                
                if (tagName === tagNameParam) {
                    tagFilter.value = options[i].value;
                    updateClearButtonVisibility('tagFilter', 'btnClearTag');
                    currentTagId = options[i].value;
                    console.log('找到匹配标签, tagId:', currentTagId);
                    loadPage(true);
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
