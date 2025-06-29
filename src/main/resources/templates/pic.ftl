<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>图片展示</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.2/css/all.min.css">
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        primary: '#165DFF',
                        secondary: '#36BFFA',
                        neutral: {
                            100: '#F5F7FA',
                            200: '#E4E6EB',
                            300: '#C9CDD4',
                            400: '#86909C',
                            500: '#4E5969',
                            600: '#272E3B',
                            700: '#1D2129',
                        }
                    },
                    fontFamily: {
                        inter: ['Inter', 'sans-serif'],
                    },
                    boxShadow: {
                        'card': '0 4px 20px rgba(0, 0, 0, 0.08)',
                        'header': '0 2px 10px rgba(0, 0, 0, 0.05)',
                        'dropdown': '0 4px 12px rgba(0, 0, 0, 0.15)',
                    }
                },
            }
        }
    </script>
    <style type="text/tailwindcss">
        @layer utilities {
            body {
                @apply font-inter bg-neutral-100 text-neutral-700 min-h-screen flex flex-col;
            }
            .header-main {
                @apply fixed top-0 left-0 right-0 z-50 bg-white/80 backdrop-blur-sm shadow-header transition-all duration-300 py-3 md:py-4;
            }
            .image-card {
                @apply bg-white rounded-xl shadow-card p-6 md:p-8 text-center max-w-full md:max-w-xl lg:max-w-2xl w-full mx-auto my-auto transition-transform duration-300 ease-in-out;
            }
            .image-card:hover {
                @apply transform -translate-y-1;
            }
            .image-wrapper {
                @apply relative overflow-hidden rounded-lg mx-auto mb-4 block leading-none;
            }
            .single-image {
                @apply block max-w-full h-auto rounded-lg transform scale-100 transition-transform duration-300 ease-in-out;
            }
            .image-wrapper:hover .single-image {
                @apply scale-110;
            }
            .download-link {
                @apply absolute bottom-2.5 right-2.5 flex items-center justify-center p-2 px-3 bg-black/60 text-white no-underline rounded-md text-sm transition-all duration-300 ease-in-out backdrop-blur-sm opacity-0 invisible pointer-events-none;
            }
            .image-wrapper:hover .download-link {
                @apply opacity-100 visible pointer-events-auto transform -translate-y-1;
            }
            .download-link:hover {
                @apply bg-black/80 transform -translate-y-1.5;
            }
            .download-link .icon {
                @apply mr-1 text-base;
            }
            /* 响应式调整 */
            @media (max-width: 768px) {
                .image-card {
                    @apply p-5 mx-4 my-auto;
                }
                .download-link {
                    @apply text-xs p-1.5 px-2 bottom-2 right-2;
                }
                .download-link .icon {
                    @apply text-sm;
                }
            }
        }
    </style>
</head>
<body>
<div class="relative min-h-screen flex flex-col">
    <header class="header-main">
        <div class="container mx-auto px-4 flex flex-col md:flex-row md:items-center justify-between">
            <div class="flex items-center justify-between mb-3 md:mb-0 w-full md:w-auto">
                <div class="flex items-center">
                    <h1 class="text-[clamp(1.5rem,3vw,2.25rem)] font-bold text-primary flex items-center">
                        <i class="fa fa-image mr-2"></i>单图
                    </h1>
                </div>
                <button id="mobileMenuBtn" class="md:hidden text-neutral-500 focus:outline-none p-2 rounded-md hover:bg-neutral-200 transition-colors">
                    <i class="fa fa-bars text-xl"></i>
                </button>
            </div>

            <div id="navActions" class="flex flex-col md:flex-row items-center space-y-3 md:space-y-0 md:space-x-4 w-full md:w-auto mt-3 md:mt-0 hidden md:flex">
                <button id="backToHomeBtn" class="bg-gray-500 hover:bg-gray-600 text-white font-medium py-2.5 px-5 rounded-lg transition-all duration-300 flex items-center shadow-md hover:shadow-lg transform hover:-translate-y-0.5 w-full md:w-auto justify-center">
                    <i class="fa fa-arrow-left mr-2"></i>
                    <span>返回首页</span>
                </button>
            </div>
        </div>
    </header>

    <main class="flex-grow flex items-center justify-center px-4 py-28">
        <div class="image-card">
            <h2 class="text-neutral-700 text-2xl md:text-3xl font-bold mb-3">精选图片展示</h2>
            <p class="text-neutral-500 text-base md:text-lg mb-6">欣赏一张高质量的图片</p>
            <div class="image-wrapper">
                <img src="${url}" alt="图片加载失败" class="single-image">
                <a href="${url}" class="download-link" download="${url?substring(url?last_index_of('/') + 1)}">
                    <i class="fa fa-download icon"></i>下载
                </a>
            </div>
            <p class="text-neutral-400 text-sm md:text-base mt-6">如果图片未显示或无法下载，请检查外部网络连接。</p>
        </div>
    </main>
</div>

<script>
    document.addEventListener('DOMContentLoaded', function () {
        const mobileMenuBtn = document.getElementById('mobileMenuBtn');
        const navActions = document.getElementById('navActions');
        const backToHomeBtn = document.getElementById('backToHomeBtn');

        // 移动端菜单处理
        mobileMenuBtn.addEventListener('click', function() {
            navActions.classList.toggle('hidden');
            navActions.classList.toggle('flex');
        });

        // 返回首页按钮逻辑
        backToHomeBtn.addEventListener('click', function() {
            window.location.href = '/'; // 跳转到根路径
        });

        // 头部滚动效果 (与 picList.ftl 保持一致)
        const header = document.querySelector('header');
        window.addEventListener('scroll', () => {
            if (window.scrollY > 10) {
                header.classList.add('py-2', 'shadow-md');
                header.classList.remove('py-3', 'shadow-header');
            } else {
                header.classList.add('py-3', 'shadow-header');
                header.classList.remove('py-2', 'shadow-md');
            }
        });
    });
</script>
</body>
</html>