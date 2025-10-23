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

        <!-- 统计信息 -->
        <div class="text-center mb-4">
            <span class="text-secondary">总条数: </span>
            <span id="totalCount" class="text-primary font-bold">0</span>
        </div>

        <!-- 表格 -->
        <div class="table-container">
            <table class="table" id="results-table">
                <thead>
                <tr>
                    <th style="width: 100px;">ID</th>
                    <th>套图名称</th>
                    <th style="width: 100px;">操作</th>
                </tr>
                </thead>
                <tbody id="results-body">
                </tbody>
            </table>
        </div>

        <!-- 分页 -->
        <div class="pagination">
            <button id="prevPage" onclick="changePage(currentPageIndex - 1)" disabled class="pagination-btn">
                <i class="fas fa-chevron-left"></i>
            </button>
            <span class="pagination-btn" style="cursor: default; background: transparent; border: none;" id="pageInfo">第 1 页 / 共 0 页</span>
            <div id="pageNumbers" class="flex gap-2"></div>
            <button id="nextPage" onclick="changePage(currentPageIndex + 1)" disabled class="pagination-btn">
                <i class="fas fa-chevron-right"></i>
            </button>
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
        const resultsBody = document.getElementById('results-body');
        resultsBody.innerHTML = '';

        Promise.all([
            postData('/queryGroupList', requestData),
            postData('/queryGroupCount', requestData)
        ]).then(([listData, countData]) => {
            if (listData && listData.length > 0) {
                listData.forEach(function(item) {
                    const newRow = document.createElement('tr');
                    newRow.innerHTML = '<td>' + (item.groupId || '') + '</td>' +
                        '<td>' + (item.picName || '') + '</td>' +
                        '<td><button class="btn-operation" onclick="viewGroup(\'' + (item.groupId || '') + '\')"><i class="fas fa-eye mr-1"></i>查看</button></td>';
                    resultsBody.appendChild(newRow);
                });
            } else {
                resultsBody.innerHTML = '<tr><td colspan="3" style="text-align: center;">未找到匹配的数据</td></tr>';
            }

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

    function viewGroup(groupId) {
        window.location.href = '/showQueryList?groupId=' + groupId;
    }

    document.addEventListener('DOMContentLoaded', function() {
        queryGroups(1); // 初始加载第一页
    });
</script>
</body>
</html>