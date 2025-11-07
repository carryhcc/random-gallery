<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>图片分组查询 - 随机图库</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Poppins:wght@600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <link rel="stylesheet" href="/css/style.css">
    <script src="/js/theme.js"></script>
    <style>
        .back-button {
            position: fixed;
            top: 1.5rem;
            left: 1.5rem;
            z-index: 1030;
        }
        
        .main-content {
            margin-top: 5rem;
            padding-bottom: 2rem;
        }
        
        .group-image-container {
            display: inline-block;
            border-radius: var(--radius-md);
            overflow: hidden;
            border: 1px solid var(--color-border);
            transition: all var(--transition-base);
        }
        
        .group-image-container:hover {
            border-color: var(--color-primary);
            box-shadow: var(--shadow-md);
        }
        
        .group-image {
            width: 60px;
            height: 60px;
            object-fit: cover;
            cursor: pointer;
            transition: transform var(--transition-base);
        }
        
        .group-image:hover {
            transform: scale(1.1);
        }
        
        .no-image {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 60px;
            height: 60px;
            background: var(--color-bg-tertiary);
            color: var(--color-text-tertiary);
            font-size: var(--font-size-xs);
            border-radius: var(--radius-md);
            border: 1px dashed var(--color-border);
        }
        
        .name-cell {
            cursor: pointer;
            transition: all var(--transition-fast);
        }
        
        .name-cell:hover {
            color: var(--color-primary);
        }
        
        .count-cell {
            text-align: center;
            font-weight: var(--font-weight-semibold);
            color: var(--color-primary);
        }
        
        .image-cell {
            text-align: center;
            padding: var(--spacing-md);
        }
        
        @media (max-width: 640px) {
            .back-button {
                top: 1rem;
                left: 1rem;
            }
            
            .main-content {
                margin-top: 4rem;
            }
            
            .group-image,
            .no-image {
                width: 50px;
                height: 50px;
            }
        }
    </style>
</head>
<body>

<button class="btn btn-secondary back-button" onclick="window.location.href = '/'">
    <i class="fas fa-arrow-left"></i>
    <span>返回首页</span>
</button>

<div class="container main-content">
    <div class="card animate-fade-in">
        <div class="card-header">
            <h1 class="card-title">
                <i class="fas fa-folder-open" style="margin-right: 0.5rem;"></i>
                图片分组查询
            </h1>
        </div>

        <!-- 搜索表单 -->
        <div class="card" style="margin-bottom: var(--spacing-lg); padding: var(--spacing-lg);">
            <div class="search-form">
                <div class="form-group">
                    <input type="text" id="groupName" placeholder="输入分组名称" class="form-input"
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
        </div>

        <!-- 表格 -->
        <div class="table-container">
            <table class="table" id="results-table">
                <thead>
                <tr>
                    <th style="width: 10%;">ID</th>
                    <th style="width: 50%;">套图名称</th>
                    <th style="width: 15%; text-align: center;">分组条数</th>
                    <th style="width: 25%; text-align: center;">分组图片</th>
                </tr>
                </thead>
                <tbody id="results-body">
                </tbody>
            </table>
        </div>

        <!-- 加载状态 -->
        <div id="loadingState" class="hidden">
            <div class="loading-container">
                <div class="loading-spinner"></div>
                <p>正在加载数据...</p>
            </div>
        </div>

        <!-- 空状态 -->
        <div id="emptyState" class="hidden">
            <div class="loading-container">
                <i class="fas fa-search" style="font-size: 3rem; color: var(--color-text-tertiary); margin-bottom: var(--spacing-md);"></i>
                <h3 style="font-size: var(--font-size-lg); font-weight: var(--font-weight-semibold); color: var(--color-text-secondary); margin-bottom: var(--spacing-sm);">暂无数据</h3>
                <p style="color: var(--color-text-tertiary);">没有找到匹配的分组数据</p>
            </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-container" id="paginationContainer">
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
        
        showLoadingState();
        
        postData('/api/group/list', requestData).then(response => {
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
                    
                    let imageHtml = '';
                    if (groupUrl) {
                        imageHtml = '<div class="group-image-container">' +
                            '<img src="' + groupUrl + '" alt="' + groupName + '" class="group-image" ' +
                                 'onclick="previewImage(\'' + groupUrl + '\')" title="点击预览">' +
                        '</div>';
                    } else {
                        imageHtml = '<div class="no-image">暂无</div>';
                    }
                    
                    newRow.innerHTML = '<td>' + groupId + '</td>' +
                        '<td class="name-cell" onclick="navigateToPicPage(\'' + groupId + '\', \'' + groupName.replace(/'/g, "\\'") + '\')" title="点击查看套图">' + 
                        groupName +
                        '</td>' +
                        '<td class="count-cell">' + groupCount + '</td>' +
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
            resultsBody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: var(--color-error);">查询失败，请重试</td></tr>';
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
    
    function navigateToPicPage(groupId, groupName) {
        window.location.href = '/showPicList?groupId=' + groupId + '&groupName=' + encodeURIComponent(groupName);
    }
    
    function previewImage(imageUrl) {
        const previewContainer = document.createElement('div');
        previewContainer.className = 'image-viewer visible';
        previewContainer.style.position = 'fixed';
        previewContainer.style.top = '0';
        previewContainer.style.left = '0';
        previewContainer.style.right = '0';
        previewContainer.style.bottom = '0';
        previewContainer.style.background = 'rgba(0, 0, 0, 0.95)';
        previewContainer.style.display = 'flex';
        previewContainer.style.alignItems = 'center';
        previewContainer.style.justifyContent = 'center';
        previewContainer.style.zIndex = '9999';
        
        const previewContent = document.createElement('div');
        previewContent.style.position = 'relative';
        previewContent.style.maxWidth = '90vw';
        previewContent.style.maxHeight = '90vh';
        
        const closeButton = document.createElement('button');
        closeButton.className = 'viewer-close';
        closeButton.innerHTML = '<i class="fas fa-times"></i>';
        closeButton.onclick = function() {
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
        previewContainer.addEventListener('click', function(e) {
            if (e.target === previewContainer) {
                document.body.removeChild(previewContainer);
            }
        });
    }

    document.addEventListener('DOMContentLoaded', function() {
        queryGroups(1);
    });
</script>
</body>
</html>
