<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>随机图库</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            margin: 0;
            background-color: #f4f7f6;
            color: #333;
            padding: 20px;
            box-sizing: border-box;
        }

        .container {
            background-color: #ffffff;
            padding: 30px;
            border-radius: 12px;
            box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
            text-align: center;
            width: 100%;
            max-width: 500px;
            box-sizing: border-box;
        }

        h1 {
            color: #2c3e50;
            margin-bottom: 30px;
            font-size: 2em;
        }

        .button-group {
            display: flex;
            flex-direction: column;
            gap: 15px;
            margin-bottom: 30px;
        }

        .button-group button {
            padding: 15px 25px;
            font-size: 1.1em;
            font-weight: bold;
            border: none;
            border-radius: 8px;
            cursor: pointer;
            transition: background-color 0.3s ease, transform 0.2s ease;
            color: white;
            text-decoration: none; /* For anchor tags if used */
            display: block; /* For anchor tags if used */
            width: 100%;
            box-sizing: border-box;
        }

        .button-group button.primary {
            background-color: #3498db;
        }

        .button-group button.primary:hover {
            background-color: #2980b9;
            transform: translateY(-2px);
        }

        .env-buttons {
            display: flex;
            justify-content: center;
            gap: 10px;
            flex-wrap: wrap; /* Allow wrapping on small screens */
        }

        .env-buttons button {
            padding: 10px 15px;
            font-size: 0.9em;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            background-color: #95a5a6;
            color: white;
            transition: background-color 0.3s ease, transform 0.2s ease;
        }

        .env-buttons button:hover {
            background-color: #7f8c8d;
            transform: translateY(-2px);
        }

        /* 响应式调整 */
        @media (max-width: 600px) {
            .container {
                padding: 20px;
                margin: 10px;
            }

            h1 {
                font-size: 1.8em;
            }

            .button-group button {
                font-size: 1em;
                padding: 12px 20px;
            }

            .env-buttons {
                flex-direction: row; /* Keep row for small buttons */
            }

            .env-buttons button {
                flex: 1; /* Distribute space evenly */
                min-width: 80px; /* Ensure minimum width */
                font-size: 0.85em;
            }
        }
    </style>
</head>
<body>
<div class="container">
    <h1>欢迎来到随机图库</h1>
    <div class="button-group">
        <button class="primary" onclick="location.href='/showPic'">获取随机图片</button>
        <button class="primary" onclick="location.href='/showPicList'">获取随机套图</button>
    </div>
    <h3>切换环境</h3>
    <div class="env-buttons">
        <button onclick="location.href='/dev'">开发环境</button>
        <button onclick="location.href='/test'">测试环境</button>
        <button onclick="location.href='/prod'">生产环境</button>
    </div>
</div>
</body>
</html>