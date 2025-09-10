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
    <style>
        /* --- 动态极光背景 --- */
        .aurora-background {
            position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: -1;
            overflow: hidden;
        }
        .aurora-background::before, .aurora-background::after {
            content: ''; position: absolute; width: 800px; height: 800px; border-radius: 50%;
            filter: blur(150px); opacity: 0.4; mix-blend-mode: screen;
        }
        .aurora-background::before {
            background: radial-gradient(circle, #ff3cac, #784ba0, #2b86c5);
            top: -25%; left: -25%; animation: move-aurora-1 25s infinite alternate ease-in-out;
        }
        .aurora-background::after {
            background: radial-gradient(circle, #f7b733, #fc4a1a);
            bottom: -25%; right: -25%; animation: move-aurora-2 25s infinite alternate ease-in-out;
        }
        @keyframes move-aurora-1 { 0% { transform: translate(0, 0) rotate(0deg); } 100% { transform: translate(100px, 200px) rotate(180deg); } }
        @keyframes move-aurora-2 { 0% { transform: translate(0, 0) rotate(0deg); } 100% { transform: translate(-150px, -100px) rotate(-180deg); } }

        @layer base {
            body {
                font-family: 'Poppins', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                background-color: #1a1a2e;
                color: #e0e0e0;
            }
        }

        .btn-glow {
            @apply justify-center text-center font-medium py-2 px-4 rounded-full transition-all duration-300 flex items-center shadow-md hover:shadow-lg transform hover:scale-105;
        }
        .btn-glow-primary {
            @apply btn-glow bg-white/10 text-white border border-white/20;
            padding: 0 3px;
        }
        .btn-glow-primary:hover {
            box-shadow: 0 0 15px rgba(0, 170, 255, 0.6), 0 0 20px rgba(0, 170, 255, 0.4);
            border-radius: 5px;
        }
        .btn-glow-reset {
            @apply btn-glow bg-gray-500/10 text-gray-400 border border-gray-500/20;
            padding: 0 3px;
        }
        .btn-glow-reset:hover {
            background-color: rgba(100, 116, 139, 0.2);
            box-shadow: 0 0 15px rgba(100, 116, 139, 0.6), 0 0 20px rgba(100, 116, 139, 0.4);
            border-radius: 5px;
        }
        .btn-glow-operation {
            @apply btn-glow text-xs px-2 py-1;
            color: #10b981;
        }
        .btn-glow-operation:hover {
            box-shadow: 0 0 15px rgba(16, 185, 129, 0.2), 0 0 20px rgba(16, 185, 129, 0.2);
            border-radius: 5px;
            padding: 0 5px;
        }
        .pagination button {
            @apply btn-glow-primary py-1 px-3;
        }
        .pagination button.active {
            @apply bg-sky-500/50 border-sky-500;
        }
        .pagination button:disabled {
            @apply opacity-50 cursor-not-allowed transform-none shadow-none hover:bg-white/10;
        }
        /* 自定义表格样式 - 增加边框和间距 */
        #results-table {
            @apply w-full border-collapse mt-6 text-sm text-gray-300 rounded-lg overflow-hidden mx-auto;
            border: 1px solid rgba(255, 255, 255, 0.1); /* 表格外边框 */
        }
        #results-table th, #results-table td {
            @apply p-4 text-center; /* 增加内边距 */
            border: 1px solid rgba(255, 255, 255, 0.1); /* 单元格边框 */
        }
        #results-table thead th {
            @apply bg-white/5 font-semibold text-gray-200;
        }
        #results-table tbody tr {
            @apply border-t border-white/10;
        }
        #results-table tbody tr:hover {
            @apply bg-white/5;
        }
        /* 表格容器居中 */
        .table-container {
            @apply flex justify-center;
        }
        /* 新增的按钮定位样式 */
        .back-to-home-btn {
            position: fixed;
            top: 1rem; /* 距离顶部 1rem */
            left: 1rem; /* 距离左侧 1rem */
            z-index: 1000; /* 确保它在最上层 */
        }
    </style>
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

<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script>
    // 分页相关变量
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
            pageIndex: currentPageIndex,
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
                            '<td>' + (item.groupId || '') + '</td>' +
                            '<td>' + (item.picName || '') + '</td>' +
                            '<td><button class="btn-glow-operation" onclick="viewGroup(\'' + (item.groupId || '') + '\')"><i class="fas fa-eye mr-1"></i>查看</button></td>' +
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
                if (totalPages === 0) totalPages = 1;

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

        // 最多显示5个页码按钮
        const maxVisiblePages = 5;
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
                pageNumbersContainer.append('<span class="text-neutral-500">...</span>');
            }
        }

        // 添加中间页码按钮
        for (let i = startPage; i <= endPage; i++) {
            addPageButton(i);
        }

        // 添加最后一页按钮
        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                pageNumbersContainer.append('<span class="text-neutral-500">...</span>');
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