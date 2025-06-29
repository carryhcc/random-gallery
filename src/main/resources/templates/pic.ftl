<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>图片展示</title>
    <style>
        /* 全局样式 */
        body {
            margin: 0;
            padding: 0;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #f0f2f5; /* 柔和的背景色 */
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh; /* 保证页面至少占满视口高度 */
            overflow: auto; /* 允许滚动 */
            box-sizing: border-box; /* 边框盒模型 */
        }

        /* 图片容器样式 */
        .image-container {
            background-color: #ffffff;
            border-radius: 12px; /* 圆角 */
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1); /* 柔和的阴影 */
            padding: 25px;
            text-align: center;
            max-width: 90%; /* 最大宽度 */
            width: 700px; /* 默认宽度 */
            box-sizing: border-box;
            transition: transform 0.3s ease-in-out; /* 容器悬停动画 */
        }

        .image-container:hover {
            transform: translateY(-5px); /* 容器悬停时向上轻微移动 */
        }

        /* 图片包裹层样式 (用于放大效果和定位下载按钮) */
        .image-wrapper {
            position: relative; /* 为内部的绝对定位元素提供参考 */
            overflow: hidden; /* 裁剪超出容器的部分，实现放大效果 */
            border-radius: 8px;
            margin: 0 auto 15px auto; /* 居中并设置底部外边距 */
            display: block; /* 确保占据一行 */
            max-width: 100%; /* 限制包裹层宽度不超过容器 */
            line-height: 0; /* 消除图片底部的空白间隙 */
        }

        /* 图片本身样式 */
        .single-image {
            display: block; /* 移除图片底部可能存在的空白 */
            max-width: 100%; /* 保证图片不超过其包裹层 */
            height: auto; /* 高度自适应，保持图片比例 */
            border-radius: inherit; /* 继承父元素 .image-wrapper 的圆角 */
            transform: scale(1); /* 默认不放大 */
            transition: transform 0.3s ease-in-out; /* 图片放大动画 */
        }

        /* 鼠标悬停在图片包裹层时，图片放大 */
        .image-wrapper:hover .single-image {
            transform: scale(1.1); /* 鼠标悬停时放大1.1倍 */
        }

        /* 标题样式 */
        h1 {
            color: #333;
            margin-top: 20px;
            font-size: 1.8em;
            letter-spacing: 0.5px;
        }

        /* 描述或提示文字样式 */
        .description {
            color: #777;
            margin-top: 10px;
            font-size: 1em;
            line-height: 1.6;
        }

        /* 下载链接样式 */
        .download-link {
            position: absolute; /* 绝对定位 */
            bottom: 10px; /* 距离底部10px */
            right: 10px; /* 距离右侧10px */
            display: inline-flex; /* 使用 flexbox 居中图标和文本 */
            align-items: center; /* 垂直居中 */
            justify-content: center; /* 水平居中 */
            padding: 8px 12px; /* 调整内边距 */
            background-color: rgba(0, 0, 0, 0.6); /* 半透明黑色背景 */
            color: white; /* 白色文字 */
            text-decoration: none; /* 移除下划线 */
            border-radius: 5px; /* 圆角按钮 */
            font-size: 0.9em; /* 调整字体大小 */
            transition: background-color 0.3s ease, transform 0.3s ease; /* 悬停动画 */
            backdrop-filter: blur(5px); /* 玻璃模糊效果 */
            -webkit-backdrop-filter: blur(5px); /* Safari 兼容 */
            opacity: 0; /* 默认隐藏 */
            visibility: hidden; /* 默认隐藏 */
            pointer-events: none; /* 默认不响应事件 */
        }

        /* 鼠标悬停在图片包裹层时显示下载链接 */
        .image-wrapper:hover .download-link {
            opacity: 1; /* 显示 */
            visibility: visible; /* 显示 */
            pointer-events: auto; /* 响应事件 */
            transform: translateY(-3px); /* 向上轻微移动 */
        }

        .download-link:hover {
            background-color: rgba(0, 0, 0, 0.8); /* 鼠标悬停时背景色变深 */
            transform: translateY(-5px); /* 进一步向上移动 */
        }

        /* 添加下载图标 */
        .download-link::before {
            content: '⬇'; /* 下载图标 Unicode 字符 */
            margin-right: 5px; /* 图标和文字之间的间距 */
            font-size: 1em; /* 图标大小 */
            line-height: 1; /* 确保图标垂直居中 */
        }

        /* 响应式设计 */
        @media (max-width: 768px) {
            .image-container {
                padding: 20px;
                width: 95%; /* 手机端容器更宽 */
                margin: 20px; /* 增加外边距 */
            }

            h1 {
                font-size: 1.5em;
            }

            .description {
                font-size: 0.9em;
            }

            .download-link {
                font-size: 0.8em;
                padding: 6px 10px;
                bottom: 8px;
                right: 8px;
            }
        }

        @media (max-width: 480px) {
            .image-container {
                padding: 15px;
                margin: 15px;
            }

            h1 {
                font-size: 1.3em;
            }

            .description {
                font-size: 0.85em;
            }

            .download-link {
                font-size: 0.75em;
                padding: 5px 8px;
                bottom: 5px;
                right: 5px;
            }
        }
    </style>
</head>
<body>
<div class="image-container">
    <h1>精选图片展示</h1>
    <p class="description">欣赏一张高质量的图片</p>
    <div class="image-wrapper">
        <img src="${url}" alt="图片加载失败" class="single-image">
        <a href="${url}" class="download-link" download="${url?substring(url?last_index_of('/') + 1)}">下载</a>
    </div>
    <p class="description" style="margin-top: 20px;">如果图片未显示或无法下载，请检查外部网络连接。</p>
</div>
</body>
</html>