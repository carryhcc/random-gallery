<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>图片展示 - 随机图库</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="/css/style.css">
</head>
<body class="min-h-screen flex items-center justify-center p-4">

<div class="aurora-background"></div>

<div class="w-full max-w-2xl">
    <div class="glass-container">
        <h1 class="text-3xl md:text-4xl font-bold text-white mb-2">精选图片</h1>
        <p class="text-neutral-300 text-base md:text-lg mb-6">希望你喜欢这张随机展示的图片</p>
        <div class="image-wrapper group">
            <img id="displayImage" src="${url}" alt="图片加载失败..." class="single-image">
        </div>
        <div class="mt-8 flex flex-col md:flex-row md:justify-center gap-4">
            <button id="backToHomeBtn" class="btn btn-secondary">
                <i class="fa fa-arrow-left mr-2"></i><span>返回首页</span>
            </button>
            <button id="refreshBtn" class="btn btn-secondary">
                <i class="fa fa-sync-alt mr-2"></i><span>刷新</span>
            </button>
            <button id="downloadBtn" class="btn btn-secondary">
                <i class="fa fa-download mr-2"></i><span>下载</span>
            </button>
        </div>
    </div>
</div>

<script>
    document.addEventListener('DOMContentLoaded', function () {
        const backToHomeBtn = document.getElementById('backToHomeBtn');
        const refreshBtn = document.getElementById('refreshBtn');
        const downloadBtn = document.getElementById('downloadBtn');
        const displayImage = document.getElementById('displayImage');

        // 返回首页按钮逻辑
        backToHomeBtn.addEventListener('click', function() {
            window.location.href = '/'; // 跳转到根路径
        });

        // 刷新按钮逻辑
        refreshBtn.addEventListener('click', function() {
            window.location.reload(); // 重新加载当前页面
        });

        // 下载按钮逻辑
        downloadBtn.addEventListener('click', function() {
            const imageUrl = displayImage.src;
            const fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1) || 'downloaded_image';
            const link = document.createElement('a');
            link.href = imageUrl;
            link.download = fileName;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        });
    });
</script>
</body>
</html>