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
    <style>
        /* 表格列样式 */
        .count-column {
            width: 120px;
            text-align: center;
        }
        .image-column {
            width: 100px;
            text-align: center;
        }
        .name-column {
            width: 250px;
        }

        
        /* 单元格样式 */
        .count-cell {
            text-align: center;
            font-weight: 500;
            color: #4f46e5;
        }
        .image-cell {
            text-align: center;
            padding: 4px;
        }
        
        /* 分组图片样式 */
        .group-image-container {
            display: inline-block;
            border-radius: 4px;
            overflow: hidden;
            border: 1px solid #e5e7eb;
            transition: all 0.2s;
        }
        .group-image-container:hover {
            border-color: #4f46e5;
            box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
        }
        .group-image {
            width: 60px;
            height: 60px;
            object-fit: cover;
            cursor: pointer;
            transition: transform 0.2s;
        }
        .group-image:hover {
            transform: scale(1.05);
        }
        .no-image {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 60px;
            height: 60px;
            background-color: #f3f4f6;
            color: #9ca3af;
            font-size: 12px;
            border-radius: 4px;
            border: 1px dashed #d1d5db;
        }
        
        /* 图片预览样式 */
        .image-preview-overlay {
            animation: fadeIn 0.2s ease-out;
        }
        .image-preview-content {
            animation: scaleIn 0.2s ease-out;
        }
        @keyframes fadeIn {
            from { opacity: 0; }
            to { opacity: 1; }
        }
        @keyframes scaleIn {
            from { transform: scale(0.9); opacity: 0; }
            to { transform: scale(1); opacity: 1; }
        }
    </style>
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
                    <label for="groupName" class="form-label">名称</label>
                    <input type="text" id="groupName" placeholder="输入名称" class="form-input" onkeyup="if(event.key === 'Enter') queryGroups(1)">
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
                    <th class="count-column">分组条数</th>
                    <th class="image-column">分组图片</th>
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
            const groupNameInput = document.getElementById('groupName').value;
            const groupName = groupNameInput === '' ? null : groupNameInput;
            const requestData = {
                groupName: groupName,
                pageIndex: currentPageIndex,
                pageSize: pageSize
            };
            
            // 显示加载状态
            showLoadingState();
            
            // 使用新的API接口
            postData('/api/group/list', requestData).then(response => {
                // 新的返回结构：response.data 包含 list 和分页信息
                const pageData = response.data || {};
                const groups = pageData.list || [];
                
                if (groups.length > 0) {
                    showTable();
                    const resultsBody = document.getElementById('results-body');
                    resultsBody.innerHTML = '';
                    
                    groups.forEach(function(item) {
                        const newRow = document.createElement('tr');
                        const groupId = item.groupId || '';
                        const groupName = item.groupName || '';
                        const groupCount = item.groupCount || 0;
                        const groupUrl = item.groupUrl ? item.groupUrl.trim().replace(/^`|`$/g, '') : '';
                        
                        // 创建图片展示部分
                        let imageHtml = '';
                        if (groupUrl) {
                            // 在JavaScript中直接拼接字符串，避免FreeMarker模板引擎解析
                            imageHtml = '<div class="group-image-container">' +
                                '<img src="' + groupUrl + '" alt="' + groupName + '" class="group-image" ' +
                                     'onclick="previewImage(\'' + groupUrl + '\')" title="点击预览">' +
                            '</div>';
                        } else {
                            imageHtml = '<div class="no-image">暂无图片</div>';
                        }
                        
                        newRow.innerHTML = '<td class="id-cell">' + groupId + '</td>' +
                            '<td class="name-cell" onclick="navigateToPicPage(\'' + groupId + '\', \'' + groupName.replace(/'/g, "\\'") + '\', this)" title="点击查看套图">' + 
                            '<span class="name-text">' + groupName + '</span>' +
                            '</td>' +
                            '<td class="count-cell">' + groupCount + '</td>' +
                            '<td class="image-cell">' + imageHtml + '</td>';
                        resultsBody.appendChild(newRow);
                    });
                } else {
                    showEmptyState();
                }

                // 使用返回的分页信息
                totalCount = pageData.total || 0;
                totalPages = pageData.pages || 1;
                currentPageIndex = pageData.pageNum || 1;
                
                document.getElementById('totalCount').textContent = totalCount;
                updatePagination();
            }).catch(error => {
                console.error('Fetch Error:', error);
                showTable();
                const resultsBody = document.getElementById('results-body');
                resultsBody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: #dc2626;">查询失败，请重试</td></tr>';
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
        // 使用分页信息判断按钮状态，如果没有分页信息则使用默认逻辑
        prevPageBtn.disabled = currentPageIndex <= 1;
        nextPageBtn.disabled = currentPageIndex >= totalPages || totalPages === 0;

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
        document.getElementById('groupName').value = '';
        queryGroups(1);
    }

    /**
     * 切换名称展开/收起状态
     * @param {HTMLElement} cell - 单元格元素
     */
    function toggleNameExpansion(cell) {
        const nameText = cell.querySelector('.name-text');
        if (nameText.style.whiteSpace === 'normal') {
            // 收起
            nameText.style.whiteSpace = 'nowrap';
            nameText.style.overflow = 'hidden';
            nameText.style.textOverflow = 'ellipsis';
            cell.title = '点击查看套图';
        } else {
            // 展开
            nameText.style.whiteSpace = 'normal';
            nameText.style.overflow = 'visible';
            nameText.style.textOverflow = 'unset';
            cell.title = '点击查看套图';
        }
    }
    
    /**
     * 跳转到图片页面
     * @param {string} groupId - 套图ID
     * @param {string} groupName - 套图名称
     * @param {HTMLElement} cell - 单元格元素
     */
    function navigateToPicPage(groupId, groupName, cell) {
        // 跳转到 picList 页面
        window.location.href = '/showPicList?groupId=' + groupId + '&groupName=' + encodeURIComponent(groupName);
    }
    
    /**
     * 预览图片功能
     * @param {string} imageUrl - 图片URL
     */
    function previewImage(imageUrl) {
        // 创建预览容器
        const previewContainer = document.createElement('div');
        previewContainer.className = 'image-preview-overlay fixed inset-0 z-50 flex items-center justify-center bg-black/80';
        
        // 创建预览内容
        const previewContent = document.createElement('div');
        previewContent.className = 'image-preview-content relative max-w-3xl max-h-[80vh]';
        
        // 创建关闭按钮
        const closeButton = document.createElement('button');
        closeButton.className = 'absolute top-2 right-2 w-10 h-10 bg-white/20 rounded-full flex items-center justify-center text-white hover:bg-white/30 transition-all';
        closeButton.innerHTML = '<i class="fas fa-times"></i>';
        closeButton.onclick = function() {
            document.body.removeChild(previewContainer);
        };
        
        // 创建图片元素
        const img = document.createElement('img');
        img.src = imageUrl;
        img.className = 'max-w-full max-h-[80vh] object-contain';
        img.alt = '预览图片';
        
        // 组装预览内容
        previewContent.appendChild(closeButton);
        previewContent.appendChild(img);
        previewContainer.appendChild(previewContent);
        
        // 添加到页面并添加点击外部关闭功能
        document.body.appendChild(previewContainer);
        previewContainer.addEventListener('click', function(e) {
            if (e.target === previewContainer) {
                document.body.removeChild(previewContainer);
            }
        });
    }

    document.addEventListener('DOMContentLoaded', function() {
        queryGroups(1); // 初始加载第一页
    });
</script>
</body>
</html>