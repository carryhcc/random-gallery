<!DOCTYPE html>
<html>
<head>
    <title>图片分组查询</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 20px;
        }
        .search-container {
            margin-bottom: 20px;
        }
        .search-container input {
            padding: 8px;
            margin-right: 10px;
        }
        .search-container button {
            padding: 8px 15px;
            cursor: pointer;
        }
        #results-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        #results-table th, #results-table td {
            border: 1px solid #ddd;
            padding: 12px;
            text-align: left;
        }
        #results-table th {
            background-color: #f2f2f2;
        }
        .total-count {
            margin-top: 10px;
            font-weight: bold;
        }
    </style>
</head>
<body>

<h2>图片分组查询</h2>

<div class="search-container">
    分组名称: <input type="text" id="picName" placeholder="输入分组名称">
    分组ID: <input type="text" id="groupId" placeholder="输入分组ID">
    <button onclick="queryGroups()">查询</button>
    <button onclick="resetForm()">重置</button>
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

<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script>
    function queryGroups() {
        const picNameInput = $('#picName').val();
        const groupIdInput = $('#groupId').val();

        const picName = picNameInput === '' ? null : picNameInput;
        const groupId = groupIdInput === '' ? null : parseInt(groupIdInput);

        const requestData = {
            picName: picName,
            groupId: groupId,
            pageIndex: 1,
            pageSize: 10
        };

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
                            '<td><button onclick="viewGroup(\'' + (item.groupId || '') + '\')">查看</button></td>' +
                            '</tr>';
                        resultsBody.append(newRow);
                    });
                } else {
                    resultsBody.append('<tr><td colspan="3" style="text-align: center;">未找到匹配的数据</td></tr>');
                }
            }
        });

        $.ajax({
            url: '/queryGroupCount',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(requestData),
            success: function(countData) {
                const totalCount = typeof countData === 'object' ? (countData.count || 0) : (countData || 0);
                $('#totalCount').text(totalCount);
            }
        });
    }

    function resetForm() {
        $('#picName').val('');
        $('#groupId').val('');
        $('#results-body').empty();
        $('#totalCount').text('0');
    }

    // 修改 viewGroup 函数的 URL 拼接方式
    function viewGroup(groupId) {
        window.location.href = '/showQueryList?groupId=' + groupId;
    }

    $(document).ready(function() {
        queryGroups();
    });
</script>
</body>
</html>