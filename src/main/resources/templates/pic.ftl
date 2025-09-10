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

        @config 'tailwind.config.js';
        @tailwind base;
        @tailwind components;
        @tailwind utilities;

        @layer base {
            body {
                font-family: 'Poppins', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                background-color: #1a1a2e;
                color: #e0e0e0;
            }
        }

        @layer components {
            .btn-glow {
                @apply w-full md:w-auto justify-center text-center font-medium py-3 px-6 rounded-full transition-all duration-300 flex items-center shadow-md hover:shadow-lg transform hover:scale-105;
            }

        }
        .btn-glow-primary {
            @apply btn-glow bg-white/10 text-white border border-white/20;
            padding: 0 12px;
        }
        .btn-glow-primary:hover {
            background-color: rgba(255, 255, 255, 0.2);
            box-shadow: 0 0 15px rgba(0, 170, 255, 0.2), 0 0 20px rgba(0, 170, 255, 0.2);
            border-radius: 5px;
        }
        /* --- 图片展示卡片和下载链接的样式 --- */
        .image-wrapper {
            /* FIX: Use flexbox to center the image inside */
            @apply relative flex justify-center items-center overflow-hidden rounded-lg mx-auto mb-6 shadow-lg;
        }
        .single-image {
            /* max-w-full prevents the image from exceeding the container width */
            @apply block max-w-full h-auto rounded-lg transform scale-100 transition-transform duration-500 ease-in-out;
        }
        .image-wrapper:hover .single-image {
            @apply scale-105;
        }
    </style>
</head>
<body class="min-h-screen flex items-center justify-center p-4">

<div class="aurora-background"></div>

<div class="w-full max-w-2xl">
    <div class="bg-white/10 backdrop-blur-xl border border-white/20 rounded-2xl shadow-2xl p-6 md:p-8 text-center transition-all duration-500 animate-fade-in-up">

        <h1 class="text-3xl md:text-4xl font-bold text-white mb-2 text-shadow">精选图片</h1>
        <p class="text-neutral-300 text-base md:text-lg mb-6 text-shadow-sm">希望你喜欢这张随机展示的图片</p>

        <div class="image-wrapper group">
            <img id="displayImage" src="${url}" alt="图片加载失败..." class="single-image">
        </div>

        <div class="mt-8 flex flex-col md:flex-row md:justify-center gap-4">
            <button id="backToHomeBtn" class="btn-glow-primary">
                <i class="fa fa-arrow-left mr-2"></i>
                <span>返回首页</span>
            </button>
            <button id="refreshBtn" class="btn-glow-primary">
                <i class="fa fa-sync-alt mr-2"></i>
                <span>刷新</span>
            </button>
            <button id="downloadBtn" class="btn-glow-primary">
                <i class="fa fa-download mr-2"></i>
                <span>下载</span>
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
            const fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);

            // 创建一个临时的 a 标签
            const link = document.createElement('a');
            link.href = imageUrl;
            link.download = fileName; // 设置下载的文件名
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        });

        // 添加一个简单的入场动画效果
        const card = document.querySelector('.animate-fade-in-up');
        card.style.opacity = '0';
        card.style.transform = 'translateY(20px)';
        setTimeout(() => {
            card.style.opacity = '1';
            card.style.transform = 'translateY(0)';
        }, 100);
    });
</script>
</body>
</html>