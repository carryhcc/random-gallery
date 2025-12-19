<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>图片分组查询 - 随机图库</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Poppins:wght@600;700&display=swap"
          rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <link rel="stylesheet" href="/css/style.css">
    <script src="/js/theme.js"></script>
</head>
<body>

<!-- 导航栏 -->
<header class="navbar">
    <div class="navbar-content">
        <div class="navbar-brand">
            <i class="fas fa-folder-open"></i>
            <span>分组查询</span>
        </div>
        <div class="navbar-actions">
            <button class="btn btn-secondary btn-sm" onclick="window.location.href='/'">
                <i class="fas fa-arrow-left"></i>
                <span class="hidden-mobile">返回首页</span>
            </button>
        </div>
    </div>
</header>

<!-- 主内容 -->
<main class="container" style="margin-top: 80px; padding-bottom: var(--spacing-xl);">
    <div class="card animate-fade-in">
        <div class="card-header">
        </div>

        <!-- 搜索表单 -->
        <div class="search-form" style="margin-bottom: var(--spacing-lg);">
            <div class="form-group" style="flex: 1;">
                <input type="text" id="groupName" placeholder="输入分组名称关键词..." class="form-input"
                       onkeyup="if(event.key === 'Enter') queryGroups(1)">
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


        <!-- 表格 -->
        <div class="table-container">
            <table class="table" id="results-table">
                <thead>
                <tr>
                    <th style="width: 10%;">ID</th>
                    <th style="width: 50%;">套图名称</th>
                    <th style="width: 15%; text-align: center;">数量</th>
                    <th style="width: 25%; text-align: center;">预览</th>
                </tr>
                </thead>
                <tbody id="results-body">
                </tbody>
            </table>
        </div>

        <!-- 加载状态 -->
        <div id="loadingState" class="loading hidden">
            <div class="spinner"></div>
            <span>正在加载数据...</span>
        </div>

        <!-- 空状态 -->
        <div id="emptyState" class="text-center hidden" style="padding: var(--spacing-2xl);">
            <i class="fas fa-search"
               style="font-size: 3rem; color: var(--color-text-tertiary); margin-bottom: var(--spacing-md);"></i>
            <h3 style="font-size: var(--font-size-lg); font-weight: var(--font-weight-semibold); color: var(--color-text-secondary); margin-bottom: var(--spacing-sm);">
                暂无数据</h3>
            <p style="color: var(--color-text-tertiary);">没有找到匹配的分组数据</p>
        </div>

        <!-- 分页 -->
        <div class="pagination-container hidden" id="paginationContainer">
            <div class="pagination-info">
                <i class="fas fa-list-alt"></i>
                <span>共 <strong id="totalCount" style="color: var(--color-primary);">0</strong> 条记录</span>
            </div>

            <div class="pagination-buttons">
                <button id="prevPage" onclick="changePage(currentPageIndex - 1)" disabled class="pagination-button">
                    <i class="fas fa-chevron-left"></i>
                </button>

                <div id="pageNumbers" class="page-numbers-container"></div>

                <button id="nextPage" onclick="changePage(currentPageIndex + 1)" disabled class="pagination-button">
                    <i class="fas fa-chevron-right"></i>
                </button>
            </div>

            <div style="text-align: center; margin-top: var(--spacing-sm);">
                    <span id="pageInfo" style="font-size: var(--font-size-sm); color: var(--color-text-tertiary);">
                        第 1 页 / 共 0 页
                    </span>
            </div>
        </div>
    </div>
</main>

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

        showLoadingState();

        postData('/api/group/list', requestData).then(response => {
            const pageData = response.data || {};
            const groups = pageData.list || [];

            if (groups.length > 0) {
                showTable();
                const resultsBody = document.getElementById('results-body');
                resultsBody.innerHTML = '';

                groups.forEach(function (item) {
                    const newRow = document.createElement('tr');
                    const groupId = item.groupId || '';
                    const groupName = item.groupName || '';
                    const groupCount = item.groupCount || 0;
                    const groupUrl = item.groupUrl ? item.groupUrl.trim().replace(/^`|`$/g, '') : '';

                    // 添加背景图变量
                    if (groupUrl) {
                        newRow.style.setProperty('--bg-image', 'url(' + groupUrl + ')');
                    }

                    // 设置行点击事件
                    newRow.onclick = function() {
                        navigateToPicPage(groupId, groupName);
                    };
                    newRow.style.cursor = 'pointer';

                    let imageHtml = '';
                    if (groupUrl) {
                        imageHtml = '<div class="group-image-container">' +
                            '<img src="' + groupUrl + '" alt="' + groupName + '" class="group-image" ' +
                            'onclick="event.stopPropagation(); previewImage(\'' + groupUrl + '\')" title="点击预览">' +
                            '</div>';
                    } else {
                        imageHtml = '<div class="no-image">暂无</div>';
                    }

                    newRow.innerHTML = '<td>' + groupId + '</td>' +
                        '<td class="name-cell" title="点击查看套图">' +
                        groupName +
                        '</td>' +
                        '<td style="text-align: center;"><span class="count-cell">' + groupCount + '</span></td>' +
                        '<td class="image-cell">' + imageHtml + '</td>';
                    resultsBody.appendChild(newRow);
                });
            } else {
                showEmptyState();
            }

            totalCount = pageData.total || 0;
            totalPages = pageData.pages || 1;
            currentPageIndex = pageData.pageNum || 1;

            document.getElementById('totalCount').textContent = totalCount;
            updatePagination();
        }).catch(error => {
            console.error('Fetch Error:', error);
            showTable();
            const resultsBody = document.getElementById('results-body');
            resultsBody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: var(--color-error); padding: 2rem;">查询失败，请重试</td></tr>';
            document.getElementById('totalCount').textContent = '0';
            totalPages = 1;
            updatePagination();
        });
    }

    function showLoadingState() {
        document.getElementById('results-table').parentElement.classList.add('hidden');
        document.getElementById('loadingState').classList.remove('hidden');
        document.getElementById('emptyState').classList.add('hidden');
        document.getElementById('paginationContainer').classList.add('hidden');
    }

    function showTable() {
        document.getElementById('results-table').parentElement.classList.remove('hidden');
        document.getElementById('loadingState').classList.add('hidden');
        document.getElementById('emptyState').classList.add('hidden');
        document.getElementById('paginationContainer').classList.remove('hidden');
    }

    function showEmptyState() {
        document.getElementById('results-table').parentElement.classList.add('hidden');
        document.getElementById('loadingState').classList.add('hidden');
        document.getElementById('emptyState').classList.remove('hidden');
        document.getElementById('paginationContainer').classList.add('hidden');
    }

    function updatePagination() {
        const pageInfoSpan = document.getElementById('pageInfo');
        const prevPageBtn = document.getElementById('prevPage');
        const nextPageBtn = document.getElementById('nextPage');

        pageInfoSpan.textContent = '第 ' + currentPageIndex + ' 页 / 共 ' + totalPages + ' 页';
        prevPageBtn.disabled = currentPageIndex <= 1;
        nextPageBtn.disabled = currentPageIndex >= totalPages || totalPages === 0;

        generatePageNumbers();
    }

    function generatePageNumbers() {
        const pageNumbersContainer = document.getElementById('pageNumbers');
        pageNumbersContainer.innerHTML = '';

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
                span.className = 'pagination-button';
                span.style.background = 'transparent';
                span.style.border = 'none';
                span.style.cursor = 'default';
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
                span.className = 'pagination-button';
                span.style.background = 'transparent';
                span.style.border = 'none';
                span.style.cursor = 'default';
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
        button.className = 'pagination-button';

        if (pageNum === currentPageIndex) {
            button.style.background = 'linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%)';
            button.style.color = 'white';
            button.style.borderColor = 'var(--color-primary)';
        }

        button.onclick = function () {
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
            resultsTable.scrollIntoView({behavior: 'smooth', block: 'start'});
        }
    }

    function resetForm() {
        document.getElementById('groupName').value = '';
        queryGroups(1);
    }

    function navigateToPicPage(groupId, groupName) {
        window.location.href = '/showPicList?groupId=' + groupId + '&groupName=' + encodeURIComponent(groupName);
    }

    function previewImage(imageUrl) {
        const previewContainer = document.createElement('div');
        previewContainer.className = 'image-viewer visible';

        const previewContent = document.createElement('div');
        previewContent.style.position = 'relative';
        previewContent.style.maxWidth = '90vw';
        previewContent.style.maxHeight = '90vh';

        const closeButton = document.createElement('button');
        closeButton.className = 'viewer-close';
        closeButton.innerHTML = '<i class="fas fa-times"></i>';
        closeButton.onclick = function () {
            document.body.removeChild(previewContainer);
        };

        const img = document.createElement('img');
        img.src = imageUrl;
        img.className = 'full-size-image';
        img.alt = '预览图片';

        previewContent.appendChild(closeButton);
        previewContent.appendChild(img);
        previewContainer.appendChild(previewContent);

        document.body.appendChild(previewContainer);
        previewContainer.addEventListener('click', function (e) {
            if (e.target === previewContainer) {
                document.body.removeChild(previewContainer);
            }
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        queryGroups(1);
    });
</script>
</body>
</html>
