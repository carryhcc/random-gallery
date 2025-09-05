<!DOCTYPE html>
<html>
<head>
    <title>图片分组查询</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://cdn.jsdelivr.net/npm/font-awesome@4.7.0/css/font-awesome.min.css" rel="stylesheet">
    <style>
        /* 保持原有样式不变 */
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
        }
        .search-container {
            margin-bottom: 20px;
            padding: 15px;
            background-color: #f8fafc;
            border-radius: 6px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }
        .search-container input {
            padding: 8px 12px;
            margin-right: 10px;
            border: 1px solid #ddd;
            border-radius: 4px;
        }
        .search-container button {
            padding: 8px 15px;
            cursor: pointer;
            border: none;
            border-radius: 4px;
            background-color: #3b82f6;
            color: white;
            transition: background-color 0.2s;
        }
        .search-container button:hover {
            background-color: #2563eb;
        }
        .search-container button.reset-btn {
            background-color: #64748b;
        }
        .search-container button.reset-btn:hover {
            background-color: #475569;
        }
        #results-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }
        #results-table th, #results-table td {
            border: 1px solid #ddd;
            padding: 12px;
            text-align: left;
        }
        #results-table th {
            background-color: #f1f5f9;
            font-weight: bold;
        }
        #results-table tr:hover {
            background-color: #f8fafc;
        }
        .total-count {
            margin-top: 10px;
            font-weight: bold;
            color: #334155;
        }
        .pagination {
            margin-top: 20px;
            display: flex;
            justify-content: center;
            align-items: center;
            gap: 8px;
        }
        .pagination button {
            padding: 6px 12px;
            border: 1px solid #ddd;
            border-radius: 4px;
            background-color: white;
            cursor: pointer;
            transition: all 0.2s;
        }
        .pagination button:hover:not(.active):not(:disabled) {
            background-color: #f1f5f9;
            border-color: #94a3b8;
        }
        .pagination button.active {
            background-color: #3b82f6;
            color: white;
            border-color: #3b82f6;
        }
        .pagination button:disabled {
            opacity: 0.5;
            cursor: not-allowed;
        }
        .page-info {
            margin: 0 10px;
            color: #64748b;
        }
        .operation-btn {
            padding: 5px 10px;
            background-color: #10b981;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            transition: background-color 0.2s;
        }
        .operation-btn:hover {
            background-color: #059669;
        }
    </style>
</head>
<body>

<h2 class="text-2xl font-bold text-gray-800 mb-6">图片分组查询</h2>

<div class="search-container">
    分组名称: <input type="text" id="picName" placeholder="输入分组名称">
    分组ID: <input type="text" id="groupId" placeholder="输入分组ID">
    <button onclick="queryGroups(1)">查询</button>
    <button class="reset-btn" onclick="resetForm()">重置</button>
</div>

<div class="total-count">总条数: <span id="totalCount">0</span></div>

<table id="results-table">
    <thead>
    <tr>
        <th>分组名称</th>
        <th>分组ID</th>
        <th>操作</th>
    </tr>
    </thead>
    <tbody id="results-body">
    </tbody>
</table>

<!-- 分页控件 -->
<div class="pagination" id="pagination">
    <button id="prevPage" onclick="changePage(currentPageIndex - 1)" disabled>
        <i class="fa fa-chevron-left"></i> 上一页
    </button>
    <span class="page-info" id="pageInfo">第 1 页 / 共 0 页</span>
    <div id="pageNumbers"></div>
    <button id="nextPage" onclick="changePage(currentPageIndex + 1)" disabled>
        下一页 <i class="fa fa-chevron-right"></i>
    </button>
</div>

<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script>
    // 分页相关变量（JavaScript变量，而非FTL模板变量）
    let currentPageIndex = 1; // 当前页码
    const pageSize = 10;      // 每页条数
    let totalPages = 0;       // 总页数
    let totalCount = 0;       // 总条数

    function queryGroups(pageIndex) {
        // 更新当前页码
        currentPageIndex = pageIndex || 1;

        const picNameInput = $('#picName').val();
        const groupIdInput = $('#groupId').val();

        const picName = picNameInput === '' ? null : picNameInput;
        const groupId = groupIdInput === '' ? null : parseInt(groupIdInput);

        const requestData = {
            picName: picName,
            groupId: groupId,
            pageIndex: currentPageIndex,  // 使用当前页码
            pageSize: pageSize
        };

        // 查询数据列表
        $.ajax({
            url: '/queryGroupList',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(requestData),
            success: function(listData) {
                const resultsBody = $('#results-body');
                resultsBody.empty();

                if (listData && listData.length > 0) {
                    listData.forEach(function(item) {
                        const newRow = '<tr>' +
                            '<td>' + (item.picName || '') + '</td>' +
                            '<td>' + (item.groupId || '') + '</td>' +
                            '<td><button class="operation-btn" onclick="viewGroup(\'' + (item.groupId || '') + '\')">查看</button></td>' +
                            '</tr>';
                        resultsBody.append(newRow);
                    });
                } else {
                    resultsBody.append('<tr><td colspan="3" style="text-align: center;">未找到匹配的数据</td></tr>');
                }
            },
            error: function() {
                $('#results-body').html('<tr><td colspan="3" style="text-align: center; color: #dc2626;">查询失败，请重试</td></tr>');
            }
        });

        // 查询总条数并更新分页
        $.ajax({
            url: '/queryGroupCount',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(requestData),
            success: function(countData) {
                totalCount = typeof countData === 'object' ? (countData.count || 0) : (countData || 0);
                $('#totalCount').text(totalCount);

                // 计算总页数
                totalPages = Math.ceil(totalCount / pageSize);
                if (totalPages === 0) totalPages = 1; // 至少显示一页

                // 更新分页控件状态
                updatePagination();
            },
            error: function() {
                alert('获取总条数失败，请重试');
            }
        });
    }

    // 更新分页控件状态
    function updatePagination() {
        // 更新页码信息（使用JavaScript动态更新，而非FTL渲染时静态生成）
        $('#pageInfo').text(`第` + currentPageIndex + `页 / 共`+ totalPages + `页`);

        // 更新上一页/下一页按钮状态
        $('#prevPage').prop('disabled', currentPageIndex <= 1);
        $('#nextPage').prop('disabled', currentPageIndex >= totalPages);

        // 生成页码按钮
        generatePageNumbers();
    }

    // 生成页码按钮
    function generatePageNumbers() {
        const pageNumbersContainer = $('#pageNumbers');
        pageNumbersContainer.empty();

        // 最多显示10个页码按钮
        const maxVisiblePages = 10;
        let startPage = Math.max(1, currentPageIndex - Math.floor(maxVisiblePages / 2));
        let endPage = startPage + maxVisiblePages - 1;

        // 调整结束页
        if (endPage > totalPages) {
            endPage = totalPages;
            startPage = Math.max(1, endPage - maxVisiblePages + 1);
        }

        // 添加第一页按钮
        if (startPage > 1) {
            addPageButton(1);
            if (startPage > 2) {
                pageNumbersContainer.append('<span>...</span>');
            }
        }

        // 添加中间页码按钮
        for (let i = startPage; i <= endPage; i++) {
            addPageButton(i);
        }

        // 添加最后一页按钮
        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                pageNumbersContainer.append('<span>...</span>');
            }
            addPageButton(totalPages);
        }
    }

    // 添加单个页码按钮
    function addPageButton(pageNum) {
        const pageNumbersContainer = $('#pageNumbers');
        const button = $('<button></button>')
            .text(pageNum)
            .click(function() {
                changePage(pageNum);
            });

        // 当前页按钮高亮
        if (pageNum === currentPageIndex) {
            button.addClass('active');
        }

        pageNumbersContainer.append(button);
    }

    // 切换页码
    function changePage(pageNum) {
        // 验证页码有效性
        if (pageNum < 1 || pageNum > totalPages || pageNum === currentPageIndex) {
            return;
        }

        // 跳转到指定页码
        queryGroups(pageNum);

        // 滚动到表格顶部
        $('html, body').animate({
            scrollTop: $('#results-table').offset().top - 20
        }, 300);
    }

    function resetForm() {
        $('#picName').val('');
        $('#groupId').val('');
        $('#results-body').empty();
        $('#totalCount').text('0');
        // 重置分页状态
        currentPageIndex = 1;
        totalPages = 0;
        totalCount = 0;
        updatePagination();
    }

    function viewGroup(groupId) {
        window.location.href = '/showQueryList?groupId=' + groupId;
    }

    $(document).ready(function() {
        queryGroups(1); // 初始加载第一页
    });
</script>
</body>
</html>
