<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>现代图片展示 (可切换)</title>
    <link rel="stylesheet" href="/base.css"> <#-- 假设 base.css 在 static 文件夹中 -->
    <link rel="stylesheet" href="/css/pic-styles.css"> <#-- 假设您已创建此CSS文件 -->
    <style>
        /* 为按钮添加一些基本样式 */
        .controls {
            margin-top: 20px;
            text-align: center;
        }
        .controls button {
            padding: 10px 20px;
            font-size: 1em;
            color: #fff;
            background-color: #007bff;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            transition: background-color 0.3s ease;
        }
        .controls button:hover {
            background-color: #0056b3;
        }
        /* 加载时的占位符样式 (可选) */
        .image-card img[src=""] {
            height: 300px; /* 或者一个适合的占位高度 */
            background-color: #eee;
        }
    </style>
</head>
<body>
<div class="container">
    <header>
        <h1>今日随机图片</h1>
    </header>
    <main>
        <div class="image-card">
            <img id="randomImage" src="${url}" alt="随机加载的图片">
            <div class="image-caption">
                <p>一张精美的随机图片。</p>
            </div>
        </div>
        <div class="controls">
            <button id="changeImageBtn">切换图片</button>
        </div>
    </main>
    <footer>
        <p>&copy; ${.now?string("yyyy")} 您的网站</p>
    </footer>
</div>

<script>
    document.addEventListener('DOMContentLoaded', function () {
        const imageElement = document.getElementById('randomImage');
        const changeButton = document.getElementById('changeImageBtn');

        if (!imageElement) {
            console.error("图片元素 'randomImage' 未找到!");
            return;
        }
        if(!changeButton) {
            console.error("按钮元素 'changeImageBtn' 未找到!");
            return;
        }

        changeButton.addEventListener('click', function () {
            // 可选: 添加加载中的视觉提示，例如禁用按钮
            changeButton.disabled = true;
            changeButton.textContent = '加载中...';

            // 使用 Fetch API 从后端获取新的图片URL
            fetch('/pic') // 这个URL对应 PicController.java 中的 @Mapping("/pic")
                .then(response => {
                    if (!response.ok) {
                        throw new Error('网络响应错误: ' + response.statusText);
                    }
                    return response.text(); // PicController的/pic方法直接返回URL字符串
                })
                .then(newImageUrl => {
                    imageElement.src = newImageUrl;
                })
                .catch(error => {
                    console.error('获取新图片失败:', error);
                    // 可以提示用户获取失败
                    alert('无法加载新图片，请稍后再试。');
                })
                .finally(() => {
                    // 无论成功或失败，都恢复按钮状态
                    changeButton.disabled = false;
                    changeButton.textContent = '切换图片';
                });
        });

        // 可选: 处理图片加载失败的情况
        imageElement.addEventListener('error', function() {
            console.warn('图片加载失败: ' + imageElement.src);
            // 可以在这里设置一个默认的占位图片或者错误提示
            // imageElement.src = '/path/to/default-error-image.png';
            // imageElement.alt = '图片加载失败';
        });
    });
</script>
</body>
</html>