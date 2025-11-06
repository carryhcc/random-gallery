<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>随机画廊 - 随机图库</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="/css/style.css">
    <style>
        /* Masonry 瀑布流：使用多列布局，移动端与PC端自适应列数 */
        .gallery-grid{column-count:4;column-gap:1rem}
        .gallery-card{background:#0f172a;border:1px solid rgba(255,255,255,0.08);border-radius:14px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,.25);break-inside:avoid;margin:0 0 1rem}
        .gallery-card img{width:100%;height:auto;display:block}
        .gallery-card .meta{padding:.75rem 1rem}
        .gallery-card .meta .name{color:#e2e8f0;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
        .gallery-card .meta .id{color:#94a3b8;font-size:.85rem;margin-top:.25rem}
        .toolbar{display:flex;gap:.5rem;align-items:center}
        .toolbar .form-input{min-width:220px}
        /* 响应式列数 */
        @media (max-width: 640px){
            .gallery-grid{column-count:3;column-gap:.5rem}
            .gallery-card{margin:0 0 .5rem}
            .gallery-card .meta{padding:.5rem}
            .gallery-card .meta .name{font-size:.85rem}
            .gallery-card .meta .id{font-size:.75rem}
        }
        @media (min-width: 641px) and (max-width: 1024px){
            .gallery-grid{column-count:3}
        }
        @media (min-width: 1025px){
            .gallery-grid{column-count:4}
        }
    </style>
    </head>
<body class="min-h-screen p-4">

<button class="btn btn-secondary" onclick="window.location.href='/'" style="position: fixed; top: 1rem; left: 1rem; z-index: 1000;">
    <i class="fa fa-arrow-left"></i>
    <span>返回首页</span>
    </button>

<div class="container" style="max-width:1200px; margin-top: 3.5rem;">
    <div class="card animate-fade-in">
        <div class="card-header">
            <h1 class="card-title">随机画廊</h1>
        </div>

        <div class="text-center mb-4">
            <button id="btnRefresh" class="btn btn-secondary">
                <i class="fas fa-sync-alt"></i>
                <span>刷新</span>
            </button>
        </div>

        <div id="tip" class="toast hidden"></div>

        <div id="gallery" class="gallery-grid"></div>
        <div id="loading" class="text-center text-slate-400 py-4 hidden">加载中...</div>
        <div id="end" class="text-center text-slate-500 py-6 hidden">已加载全部</div>
        <div id="sentinel" style="height:1px;"></div>
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

    function showTip(text, isError){
        tip.textContent = text;
        tip.className = 'toast show ' + (isError ? 'error' : 'success');
        setTimeout(()=>{ tip.className='toast'; }, 2000);
    }

    function createCard(item){
        const div = document.createElement('div');
        div.className = 'gallery-card';
        const img = document.createElement('img');
        img.src = item.picUrl || '';
        img.alt = item.groupName || '';
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
        return div;
    }

    function appendList(list){
        if(!list || list.length === 0){
            return;
        }
        const frag = document.createDocumentFragment();
        list.forEach(it=> frag.appendChild(createCard(it)) );
        gallery.appendChild(frag);
        // 若内容不足一屏，自动继续加载，提升手机端触发可靠性
        maybeLoadMoreIfShort();
    }

    async function loadPage(reset){
        if (isLoading) return;
        isLoading = true;
        loadingEl.classList.remove('hidden');
        endEl.classList.add('hidden');
        try{
            if (reset){
                page = 0;
                gallery.innerHTML = '';
                hasMore = true;
            }

            // 使用新的接口格式
            const url = '/api/group/loadMore?page=' + page;
            const res = await fetch(url);
            const result = await res.json();

            if (result.code === 200 && result.data){
                // 兼容新的接口返回格式
                const images = result.data.images || [];
                hasMore = result.data.hasMore || false;

                if (images.length === 0) {
                    endEl.classList.remove('hidden');
                } else {
                    // 处理字段映射：提取图片URL和分组名称
                    const processedList = images.map(item => ({
                        groupId: item.groupId,
                        groupName: item.groupUrl || '未命名分组', // 使用groupUrl作为分组名称
                        picUrl: extractImageUrl(item.groupName) || '' // 从groupName中提取图片URL
                    }));

                    appendList(processedList);
                    page++;
                    
                    if (!hasMore) {
                        endEl.classList.remove('hidden');
                    }
                }
            } else {
                showTip((result && result.message) || '获取图片失败', true);
                if (reset){
                    endEl.classList.remove('hidden');
                }
            }
        } catch (e){
            console.error('请求失败:', e);
            showTip('网络请求失败', true);
        } finally {
            isLoading = false;
            loadingEl.classList.add('hidden');
        }
    }
    
    /**
     * 从字符串中提取图片URL
     * @param {string} text - 可能包含URL的文本
     * @returns {string} - 提取的URL或空字符串
     */
    function extractImageUrl(text) {
        if (!text) return '';
        
        // 尝试匹配反引号中的URL
        const backtickMatch = text.match(/`([^`]+)`/);
        if (backtickMatch && backtickMatch[1]) {
            return backtickMatch[1];
        }
        
        // 尝试直接匹配URL格式
        const urlMatch = text.match(/https?:\/\/[^\s]+/);
        if (urlMatch && urlMatch[0]) {
            return urlMatch[0];
        }
        
        return text; // 如果无法提取，返回原始文本
    }

    function getScrollHeights(){
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

    function nearBottomThreshold(threshold){
        const m = getScrollHeights();
        return (m.scrollTop + m.viewportH) >= (m.scrollHeight - (threshold || 300));
    }

    function maybeLoadMoreIfShort(){
        // 如果内容高度不足一屏，且还有更多，则继续加载一页
        const m = getScrollHeights();
        if (m.scrollHeight <= m.viewportH + 50 && hasMore && !isLoading){
            loadPage(false);
        }
    }

    btnRefresh.addEventListener('click', ()=>{ loadPage(true); });

    document.addEventListener('DOMContentLoaded', ()=>{
        loadPage(true);
        // 优先使用 IntersectionObserver，确保手机端可靠触发
        const sentinel = document.getElementById('sentinel');
        if ('IntersectionObserver' in window && sentinel){
            const io = new IntersectionObserver((entries)=>{
                entries.forEach(entry=>{
                    if (entry.isIntersecting && !isLoading && hasMore){
                        loadPage(false);
                    }
                });
            }, { root: null, rootMargin: '400px', threshold: 0 });
            io.observe(sentinel);
        }
        // 兜底方案：滚动事件（部分旧机型）
        window.addEventListener('scroll', ()=>{
            if (isLoading || !hasMore) return;
            if (nearBottomThreshold(300)){
                loadPage(false);
            }
        }, { passive: true });
        // 触摸滚动兜底（部分移动端只触发 touchmove）
        window.addEventListener('touchmove', ()=>{
            if (isLoading || !hasMore) return;
            if (nearBottomThreshold(300)){
                loadPage(false);
            }
        }, { passive: true });
    });
</script>

</body>
</html>


