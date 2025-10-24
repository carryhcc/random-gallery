<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>图片分组查询 - 随机图库</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="/css/style.css">
</head>
<body class="min-h-screen p-4">

<!-- 返回按钮 -->
<button id="backToHomeBtn" class="btn btn-secondary" onclick="window.location.href = '/'" style="position: fixed; top: 1rem; left: 1rem; z-index: 1000;">
    <i class="fa fa-arrow-left"></i>
    <span>返回首页</span>
</button>

<div class="container" style="max-width: 1000px; margin-top: 4rem;">
    <!-- 主卡片 -->
    <div class="card animate-fade-in">
        <div class="card-header">
            <h1 class="card-title">图片分组查询</h1>
            <p class="card-subtitle">查询、浏览和管理图片分组</p>
        </div>

        <!-- 搜索表单 -->
        <div class="card mb-6">
            <div class="search-form">
                <div class="form-group">
                    <label for="picName" class="form-label">名称</label>
                    <input type="text" id="picName" placeholder="输入名称" class="form-input">
                </div>
                <div class="button-group">
                    <button class="btn btn-primary" onclick="queryGroups(1)">
                        <i class="fas fa-search"></i>
                        <span>查询</span>
                    </button>
                    <button class="btn btn-secondary" onclick="resetForm()">
                        <i class="fas fa-redo"></i>
                        <span>重置</span>
                    </button>
                </div>
            </div>
        </div>

        <!-- 表格 -->
        <div class="table-container">
            <table class="table" id="results-table">
                <thead>
                <tr>
                    <th class="id-column">ID</th>
                    <th class="name-column">套图名称</th>
                    <th class="action-column">操作</th>
                </tr>
                </thead>
                <tbody id="results-body">
                </tbody>
            </table>
        </div>

        <!-- 加载状态 -->
        <div id="loadingState" class="hidden text-center py-8">
            <div class="inline-flex items-center gap-2 text-gray-500">
                <div class="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-600"></div>
                <span>正在加载数据...</span>
            </div>
        </div>

        <!-- 空状态 -->
        <div id="emptyState" class="hidden text-center py-12">
            <div class="text-gray-400 text-6xl mb-4">
                <i class="fas fa-search"></i>
            </div>
            <h3 class="text-lg font-medium text-gray-600 mb-2">暂无数据</h3>
            <p class="text-gray-500">没有找到匹配的分组数据</p>
        </div>

        <!-- 分页和统计信息 -->
        <div class="pagination-container flex flex-col gap-4 mt-6 p-4 sm:p-6 bg-gradient-to-r from-slate-800/90 to-slate-700/90 backdrop-blur-sm rounded-xl border border-slate-600/50 shadow-xl">
            <!-- 统计信息 -->
            <div class="pagination-info flex items-center justify-center sm:justify-start gap-3 text-sm text-slate-300">
                <div class="flex items-center justify-center w-6 h-6 sm:w-8 sm:h-8 bg-gradient-to-br from-blue-500 to-blue-600 rounded-full shadow-lg">
                    <i class="fas fa-list-alt text-white text-xs sm:text-sm"></i>
                </div>
                <div class="flex items-center gap-2">
                    <span>共</span>
                    <span id="totalCount" class="font-bold text-blue-400 text-lg sm:text-xl">0</span>
                    <span>条记录</span>
                </div>
            </div>
            
            <!-- 分页组件 -->
            <div class="flex flex-col items-center gap-3">
                <!-- 页码信息 - 单独一行 -->
                <div class="flex items-center justify-center w-full">
                    <span id="pageInfo" class="px-3 py-2 text-xs sm:text-sm text-slate-200 bg-slate-700/80 rounded-lg font-medium backdrop-blur-sm">
                        第 1 页 / 共 0 页
                    </span>
                </div>
                
                <!-- 分页按钮组 - 所有按钮在同一行 -->
                <div class="pagination-buttons flex items-center justify-center gap-1 sm:gap-2 w-full">
                    <!-- 上一页按钮 -->
                    <button id="prevPage" onclick="changePage(currentPageIndex - 1)" disabled 
                            class="pagination-button px-2 py-1.5 sm:px-4 sm:py-2.5 text-xs sm:text-sm font-medium text-slate-300 bg-slate-600/80 rounded-lg hover:bg-slate-500 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed transition-all duration-200 hover:shadow-lg hover:scale-105">
                        <i class="fas fa-chevron-left"></i>
                    </button>
                    
                    <!-- 页码按钮容器 - 直接包含所有页码按钮 -->
                    <div id="pageNumbers" class="page-numbers-container flex gap-1 justify-center items-center flex-1"></div>
                    
                    <!-- 下一页按钮 -->
                    <button id="nextPage" onclick="changePage(currentPageIndex + 1)" disabled 
                            class="pagination-button px-2 py-1.5 sm:px-4 sm:py-2.5 text-xs sm:text-sm font-medium text-slate-300 bg-slate-600/80 rounded-lg hover:bg-slate-500 hover:text-white disabled:opacity-40 disabled:cursor-not-allowed transition-all duration-200 hover:shadow-lg hover:scale-105">
                        <i class="fas fa-chevron-right"></i>
                    </button>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    let currentPageIndex = 1;
    const pageSize = 10;
    let totalPages = 0;
    let totalCount = 0;

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
        currentPageIndex = pageIndex || 1;
        const picNameInput = document.getElementById('picName').value;
        const picName = picNameInput === '' ? null : picNameInput;
        const requestData = {
            picName: picName,
            pageIndex: currentPageIndex,
            pageSize: pageSize
        };
        
        // 显示加载状态
        showLoadingState();
        
        // 使用新的分页接口，一次调用获取所有分页信息
        postData('/api/group/list/paged', requestData).then(response => {
            // 处理统一响应格式
            const pageResult = response.data;
            if (pageResult && pageResult.list && pageResult.list.length > 0) {
                showTable();
                const resultsBody = document.getElementById('results-body');
                resultsBody.innerHTML = '';
                
                pageResult.list.forEach(function(item) {
                    const newRow = document.createElement('tr');
                    const groupId = item.groupId || '';
                    const picName = item.picName || '';
                    
                    newRow.innerHTML = '<td class="id-cell">' + groupId + '</td>' +
                        '<td class="name-cell" onclick="toggleNameExpansion(this)" title="点击查看完整名称">' + 
                        '<span class="name-text">' + picName + '</span>' +
                        '</td>' +
                        '<td class="action-cell"><button class="btn-operation" onclick="viewGroup(\'' + groupId + '\', \'' + picName.replace(/'/g, "\\'") + '\')"><i class="fas fa-eye mr-1"></i>查看</button></td>';
                    resultsBody.appendChild(newRow);
                });
            } else {
                showEmptyState();
            }

            // 从分页结果中获取统计信息
            totalCount = pageResult.total || 0;
            totalPages = pageResult.pages || 1;
            currentPageIndex = pageResult.pageNum || 1;
            
            document.getElementById('totalCount').textContent = totalCount;
            updatePagination();
        }).catch(error => {
            console.error('Fetch Error:', error);
            showTable();
            const resultsBody = document.getElementById('results-body');
            resultsBody.innerHTML = '<tr><td colspan="3" style="text-align: center; color: #dc2626;">查询失败，请重试</td></tr>';
            document.getElementById('totalCount').textContent = '0';
            totalPages = 1;
            updatePagination();
        });
    }

    function showLoadingState() {
        document.getElementById('results-table').parentElement.classList.add('hidden');
        document.getElementById('loadingState').classList.remove('hidden');
        document.getElementById('emptyState').classList.add('hidden');
    }

    function showTable() {
        document.getElementById('results-table').parentElement.classList.remove('hidden');
        document.getElementById('loadingState').classList.add('hidden');
        document.getElementById('emptyState').classList.add('hidden');
    }

    function showEmptyState() {
        document.getElementById('results-table').parentElement.classList.add('hidden');
        document.getElementById('loadingState').classList.add('hidden');
        document.getElementById('emptyState').classList.remove('hidden');
    }

    function updatePagination() {
        const pageInfoSpan = document.getElementById('pageInfo');
        const prevPageBtn = document.getElementById('prevPage');
        const nextPageBtn = document.getElementById('nextPage');

        pageInfoSpan.textContent = '第 ' + currentPageIndex + ' 页 / 共 ' + totalPages + ' 页';
        prevPageBtn.disabled = currentPageIndex <= 1;
        nextPageBtn.disabled = currentPageIndex >= totalPages;

        generatePageNumbers();
    }

    function generatePageNumbers() {
        const pageNumbersContainer = document.getElementById('pageNumbers');
        pageNumbersContainer.innerHTML = '';
        
        // 手机端显示更少的页码，桌面端显示更多
        const isMobile = window.innerWidth <= 640;
        const maxVisiblePages = isMobile ? 3 : 5;
        
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
                span.className = 'px-0.5 py-0.5 sm:px-2 sm:py-2 text-xs sm:text-sm text-slate-400 flex items-center flex-shrink-0';
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
                span.className = 'px-0.5 py-0.5 sm:px-2 sm:py-2 text-xs sm:text-sm text-slate-400 flex items-center flex-shrink-0';
                span.textContent = '...';
                pageNumbersContainer.appendChild(span);
            }
            addPageButton(totalPages);
        }
    }

    function addPageButton(pageNum) {
        const pageNumbersContainer = document.getElementById('pageNumbers');
        const button = document.createElement('button');
        button.textContent = pageNum;
        button.className = 'px-1 py-0.5 sm:px-3 sm:py-2.5 text-xs sm:text-sm font-medium rounded-lg hover:bg-slate-500 hover:text-white transition-all duration-200 hover:shadow-lg hover:scale-105 min-w-[20px] sm:min-w-[44px] flex-shrink-0';
        
        if (pageNum === currentPageIndex) {
            button.classList.add('bg-gradient-to-br', 'from-blue-500', 'to-blue-600', 'text-white', 'shadow-lg', 'scale-105');
        } else {
            button.classList.add('bg-slate-600/80', 'text-slate-300', 'backdrop-blur-sm');
        }
        
        button.onclick = function() {
            changePage(pageNum);
        };
        pageNumbersContainer.appendChild(button);
    }

    function changePage(pageNum) {
        if (pageNum < 1 || pageNum > totalPages || pageNum === currentPageIndex) {
            return;
        }
        queryGroups(pageNum);
        const resultsTable = document.getElementById('results-table');
        if (resultsTable) {
            resultsTable.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    }

    function resetForm() {
        document.getElementById('picName').value = '';
        queryGroups(1);
    }

    function viewGroup(groupId, groupName) {
        window.location.href = '/showPicList?groupId=' + groupId + '&groupName=' + encodeURIComponent(groupName);
    }

    function toggleNameExpansion(cell) {
        const nameText = cell.querySelector('.name-text');
        if (nameText.style.whiteSpace === 'normal') {
            // 收起
            nameText.style.whiteSpace = 'nowrap';
            nameText.style.overflow = 'hidden';
            nameText.style.textOverflow = 'ellipsis';
            cell.title = '点击查看完整名称';
        } else {
            // 展开
            nameText.style.whiteSpace = 'normal';
            nameText.style.overflow = 'visible';
            nameText.style.textOverflow = 'unset';
            cell.title = '点击收起名称';
        }
    }

    document.addEventListener('DOMContentLoaded', function() {
        queryGroups(1); // 初始加载第一页
    });
</script>
</body>
</html>