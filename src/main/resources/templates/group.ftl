<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>图片分组查询 - 随机图库</title>

    <!-- 外部资源引用（保留原链接） -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <script src="https://cdn.tailwindcss.com"></script>

    <!-- 引用独立 CSS 文件（路径需根据实际项目目录调整） -->
    <link rel="stylesheet" href="/css/style.css">
</head>
<body class="min-h-screen p-4 flex justify-center items-start">

<div class="aurora-background"></div>

<button id="backToHomeBtn" class="btn-glow back-to-home-btn" onclick="window.location.href = '/'">
    <i class="fa fa-arrow-left mr-2"></i><span>返回首页</span>
</button>

<div class="w-full max-w-4xl pt-12 md:pt-24">
    <div class="bg-white/10 backdrop-blur-xl border border-white/20 rounded-2xl shadow-2xl p-6 md:p-8 text-center transition-all duration-500 animate-fade-in-up">

        <h1 class="text-3xl md:text-4xl font-bold text-white mb-2 text-shadow">图片分组查询</h1>
        <p class="text-neutral-300 text-base md:text-lg mb-6 text-shadow-sm">查询、浏览和管理图片分组</p>

        <div class="bg-white/5 border border-white/10 rounded-xl p-4 md:p-6 mb-6">
            <div class="flex flex-col md:flex-row items-center justify-center gap-4">
                <div class="flex-grow flex items-center gap-2 w-full md:w-auto">
                    <label for="picName" class="text-neutral-300 whitespace-nowrap">名称:</label>
                    <input type="text" id="picName" placeholder="输入名称" class="flex-grow w-full bg-white/5 text-white placeholder-neutral-500 border border-white/10 rounded-lg px-3 py-2 focus:outline-none focus:border-sky-500 transition-colors duration-200">
                </div>
                <div class="flex-shrink-0 flex gap-2 w-full md:w-auto">
                    <button class="btn-glow-primary flex-grow" onclick="queryGroups(1)">
                        <i class="fas fa-search mr-2"></i>
                        <span>查询</span>
                    </button>
                    <button class="btn-glow-reset flex-grow" onclick="resetForm()">
                        <i class="fas fa-redo mr-2"></i>
                        <span>重置</span>
                    </button>
                </div>
            </div>
        </div>

        <div class="text-center font-bold text-neutral-300 mb-4">
            总条数: <span id="totalCount">0</span>
        </div>

        <div class="table-container">
            <table style="width:100%;" id="results-table">
                <thead>
                <tr>
                    <th style="width:100px;">ID</th>
                    <th style="width:calc(100% - 200px)">套图名称</th>
                    <th style="width:100px;">操作</th>
                </tr>
                </thead>
                <tbody id="results-body">
                </tbody>
            </table>
        </div>

        <div class="pagination flex flex-col md:flex-row justify-center items-center gap-2 mt-6 text-xs md:text-sm">
            <button id="prevPage" onclick="changePage(currentPageIndex - 1)" disabled class="flex-shrink-0">
                <i class="fas fa-chevron-left"></i>
            </button>
            <span class="page-info text-neutral-400 whitespace-nowrap" id="pageInfo">第 1 页 / 共 0 页</span>
            <div id="pageNumbers" class="flex flex-wrap justify-center gap-2"></div>
            <button id="nextPage" onclick="changePage(currentPageIndex + 1)" disabled class="flex-shrink-0">
                <i class="fas fa-chevron-right"></i>
            </button>
        </div>

    </div>
</div>

<!-- 保留原 JS 脚本（与页面交互强相关，暂不拆分） -->
<script>
    // 分页相关变量
    let currentPageIndex = 1; // 当前页码
    const pageSize = 10;      // 每页条数
    let totalPages = 0;       // 总页数
    let totalCount = 0;       // 总条数

    // 辅助函数：使用 fetch 发送 POST 请求
    function postData(url, data) {
        return fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data)
        }).then(response => {
            if (!response.ok) {
                throw new Error('HTTP Error, status: ' + response.status);
            }
            return response.json();
        });
    }

    function queryGroups(pageIndex) {
        // 更新当前页码
        currentPageIndex = pageIndex || 1;

        const picNameInput = document.getElementById('picName').value;
        const picName = picNameInput === '' ? null : picNameInput;

        const requestData = {
            picName: picName,
            pageIndex: currentPageIndex,
            pageSize: pageSize
        };

        const resultsBody = document.getElementById('results-body');
        resultsBody.innerHTML = ''; // 清空表格

        // 使用 Promise.all 并行发送请求
        Promise.all([
            postData('/queryGroupList', requestData),
            postData('/queryGroupCount', requestData)
        ]).then(([listData, countData]) => {
            // 渲染列表数据
            if (listData && listData.length > 0) {
                listData.forEach(function(item) {
                    const newRow = document.createElement('tr');
                    newRow.innerHTML = '<td>' + (item.groupId || '') + '</td>' +
                        '<td>' + (item.picName || '') + '</td>' +
                        '<td><button class="btn-glow-operation" onclick="viewGroup(\'' + (item.groupId || '') + '\')"><i class="fas fa-eye mr-1"></i>查看</button></td>';
                    resultsBody.appendChild(newRow);
                });
            } else {
                resultsBody.innerHTML = '<tr><td colspan="3" style="text-align: center;">未找到匹配的数据</td></tr>';
            }

            // 更新总条数和分页
            totalCount = typeof countData === 'object' ? (countData.count || 0) : (countData || 0);
            document.getElementById('totalCount').textContent = totalCount;

            totalPages = Math.ceil(totalCount / pageSize);
            if (totalPages === 0) totalPages = 1;

            updatePagination();
        }).catch(error => {
            console.error('Fetch Error:', error);
            resultsBody.innerHTML = '<tr><td colspan="3" style="text-align: center; color: #dc2626;">查询失败，请重试</td></tr>';
            document.getElementById('totalCount').textContent = '0';
            updatePagination();
        });
    }

    // 更新分页控件状态
    function updatePagination() {
        const pageInfoSpan = document.getElementById('pageInfo');
        const prevPageBtn = document.getElementById('prevPage');
        const nextPageBtn = document.getElementById('nextPage');

        pageInfoSpan.textContent = '第 ' + currentPageIndex + ' 页 / 共 ' + totalPages + ' 页';

        prevPageBtn.disabled = currentPageIndex <= 1;
        nextPageBtn.disabled = currentPageIndex >= totalPages;

        generatePageNumbers();
    }

    // 生成页码按钮
    function generatePageNumbers() {
        const pageNumbersContainer = document.getElementById('pageNumbers');
        pageNumbersContainer.innerHTML = '';

        const maxVisiblePages = 5;
        let startPage = Math.max(1, currentPageIndex - Math.floor(maxVisiblePages / 2));
        let endPage = startPage + maxVisiblePages - 1;

        if (endPage > totalPages) {
            endPage = totalPages;
            startPage = Math.max(1, endPage - maxVisiblePages + 1);
        }

        if (startPage > 1) {
            addPageButton(1);
            if (startPage > 2) {
                const span = document.createElement('span');
                span.className = 'text-neutral-500';
                span.textContent = '...';
                pageNumbersContainer.appendChild(span);
            }
        }

        for (let i = startPage; i <= endPage; i++) {
            addPageButton(i);
        }

        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                const span = document.createElement('span');
                span.className = 'text-neutral-500';
                span.textContent = '...';
                pageNumbersContainer.appendChild(span);
            }
            addPageButton(totalPages);
        }
    }

    // 添加单个页码按钮
    function addPageButton(pageNum) {
        const pageNumbersContainer = document.getElementById('pageNumbers');
        const button = document.createElement('button');
        button.textContent = pageNum;

        if (pageNum === currentPageIndex) {
            button.classList.add('active');
        }

        button.onclick = function() {
            changePage(pageNum);
        };
        pageNumbersContainer.appendChild(button);
    }

    // 切换页码
    function changePage(pageNum) {
        if (pageNum < 1 || pageNum > totalPages || pageNum === currentPageIndex) {
            return;
        }

        queryGroups(pageNum);

        // 滚动到表格顶部
        const resultsTable = document.getElementById('results-table');
        if (resultsTable) {
            resultsTable.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    }

    function resetForm() {
        document.getElementById('picName').value = '';
        queryGroups(1);
    }

    function viewGroup(groupId) {
        window.location.href = '/showQueryList?groupId=' + groupId;
    }

    // 页面加载后执行
    document.addEventListener('DOMContentLoaded', function() {
        queryGroups(1); // 初始加载第一页
    });
</script>
</body>
</html>