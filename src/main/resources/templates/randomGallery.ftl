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
        .gallery-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:1rem}
        .gallery-card{background:#0f172a;border:1px solid rgba(255,255,255,0.08);border-radius:14px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,.25)}
        .gallery-card img{width:100%;height:180px;object-fit:cover;display:block}
        .gallery-card .meta{padding:.75rem 1rem}
        .gallery-card .meta .name{color:#e2e8f0;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
        .gallery-card .meta .id{color:#94a3b8;font-size:.85rem;margin-top:.25rem}
        .toolbar{display:flex;gap:.5rem;align-items:center}
        .toolbar .form-input{min-width:220px}
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
            <p class="card-subtitle">按名称模糊查询，随机返回10条分组及代表图</p>
        </div>

        <div class="card mb-4">
            <div class="search-form toolbar">
                <div class="form-group">
                    <label for="groupName" class="form-label">名称</label>
                    <input id="groupName" type="text" placeholder="输入分组名称（模糊）" class="form-input" value="${groupName!''}">
                </div>
                <div class="button-group">
                    <button id="btnQuery" class="btn btn-primary">
                        <i class="fas fa-search"></i>
                        <span>查询</span>
                    </button>
                    <button id="btnRefresh" class="btn btn-secondary">
                        <i class="fas fa-sync-alt"></i>
                        <span>刷新</span>
                    </button>
                </div>
            </div>
        </div>

        <div id="tip" class="toast hidden"></div>

        <div id="gallery" class="gallery-grid"></div>
    </div>
</div>

<script>
    const gallery = document.getElementById('gallery');
    const tip = document.getElementById('tip');
    const inputName = document.getElementById('groupName');
    const btnQuery = document.getElementById('btnQuery');
    const btnRefresh = document.getElementById('btnRefresh');

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

    function render(list){
        gallery.innerHTML = '';
        if(!list || list.length === 0){
            const empty = document.createElement('div');
            empty.className = 'text-center text-slate-400 py-8';
            empty.textContent = '暂无数据';
            gallery.appendChild(empty);
            return;
        }
        const frag = document.createDocumentFragment();
        list.forEach(it=> frag.appendChild(createCard(it)) );
        gallery.appendChild(frag);
    }

    async function loadData(){
        const name = inputName.value && inputName.value.trim() !== '' ? encodeURIComponent(inputName.value.trim()) : '';
        const url = name ? ('/api/group/random-gallery?groupName=' + name + '&limit=10') : '/api/group/random-gallery?limit=10';
        const res = await fetch(url);
        const data = await res.json();
        if(data.code === 200){
            render(data.data || []);
        } else {
            showTip(data.message || '查询失败', true);
            render([]);
        }
    }

    btnQuery.addEventListener('click', ()=>{ loadData(); });
    btnRefresh.addEventListener('click', ()=>{ loadData(); });

    document.addEventListener('DOMContentLoaded', ()=>{
        loadData();
    });
</script>

</body>
</html>


