# 随机图库 Android 原生版

本目录是将 Web 端重构后的纯原生 Android App（Kotlin + Jetpack + Retrofit），不使用 WebView。

## 1. 项目目录

```text
android-native
├── app
│   ├── build.gradle.kts
│   ├── src/main
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/example/randomgallery/android
│   │   │   ├── MainActivity.kt                     # 单Activity + 底部导航 + 返回键退出
│   │   │   ├── RandomGalleryApp.kt                 # Application + 图片加载器初始化
│   │   │   ├── AppContainer.kt                     # 依赖容器
│   │   │   ├── data
│   │   │   │   ├── model/ApiModels.kt             # 与后端Result/VO/QO对应
│   │   │   │   ├── network/ApiService.kt          # Retrofit接口封装
│   │   │   │   ├── repository/GalleryRepository.kt# 业务数据层（网络+离线缓存）
│   │   │   │   └── local                           # Room + DataStore
│   │   │   ├── ui
│   │   │   │   ├── home                            # 首页（环境切换/隐私模式）
│   │   │   │   ├── pic                             # 随机单图
│   │   │   │   ├── piclist                         # 套图详情
│   │   │   │   ├── group                           # 分组查询
│   │   │   │   ├── gallery                         # 随机画廊
│   │   │   │   ├── download                        # 下载管理
│   │   │   │   ├── downloadlist                    # 下载浏览
│   │   │   │   ├── downloaddetail                  # 作品详情
│   │   │   │   ├── gif                             # 随机动图
│   │   │   │   └── adapter                         # 列表适配器
│   │   │   ├── worker/DailyReminderWorker.kt       # 推送提醒
│   │   │   └── util                                # 网络检测/下载/提示
│   │   └── res
│   │       ├── navigation/nav_graph.xml            # 全页面导航
│   │       ├── layout/*.xml                        # 所有页面原生布局
│   │       ├── menu/bottom_nav_menu.xml
│   │       ├── drawable/*                          # 卡片、404、loading占位
│   │       └── xml/network_security_config.xml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 2. 核心代码说明

### 2.1 接口封装（Retrofit）
- 文件：`app/src/main/java/.../data/network/ApiService.kt`
- 已覆盖 Web 端核心接口：
  - 图片：`/api/pic/random/one`、`/api/pic/list`
  - 分组：`/api/group/randomGroupInfo`、`/api/group/list`、`/api/group/loadMore`
  - 下载作品：`/api/xhsWork/download`、`/api/xhsWork/list`、`/api/xhsWork/detail/{workId}`、删除接口
  - GIF：`/api/xhsWork/randomGif`
  - 系统：`/api/system/privacy-mode`、`/api/system/env/*`

### 2.2 页面与业务映射（完全原生）
- 首页：环境切换、隐私开关、模块入口、随机套图跳转
- 随机图片页：刷新、下载、点分组名跳套图
- 套图页：分页加载、下拉刷新
- 分组页：关键字查询、翻页、点卡片进套图
- 随机画廊：瀑布流式网格 + 下拉刷新 + 滚动加载
- 下载管理：提交链接下载任务
- 下载浏览：搜索、作者筛选、标签筛选、单双列切换、无限滚动
- 下载详情：作品信息、图片/动图分区、删除作品/媒体、预览、下载
- 随机动图：上下切换历史、循环播放、下载

### 2.3 原生能力
- 本地存储：DataStore 持久化环境、隐私开关、提醒开关、列表视图模式
- 离线缓存：Room 缓存关键接口数据（随机图、随机GIF、下载列表）
- 推送提醒：用户在设置中开启后，通过 WorkManager + Notification 每日提醒
- 权限申请：Android 13+ 仅在开启每日提醒时动态申请通知权限
- 系统返回键：首页双击退出
- 交互：下拉刷新、加载中、友好错误提示、404占位图

## 3. 配置文件

### 3.1 后端地址
- 默认地址文件：`app/build.gradle.kts`
- 运行时配置入口：首页卡片里的“后端地址”
- 默认配置项：
```kotlin
// debug
buildConfigField("String", "DEFAULT_BASE_URL", "\"http://10.0.2.2:8086/\"")

// release
buildConfigField("String", "DEFAULT_BASE_URL", "\"https://example.invalid/\"")
```
- 说明：
  - Debug 默认地址是 `http://10.0.2.2:8086/`，适合 Android 模拟器访问宿主机后端
  - Release 默认地址是占位的 HTTPS 地址，正式发版前需要替换为真实生产地址
  - 真机或其他局域网环境请在 App 首页直接改成实际地址，例如 `http://192.168.10.144:8086/`
  - 地址会持久化保存，后续换网络只需要重新修改，不需要重新打包 APK

### 3.2 明文 HTTP 支持
- 文件：`AndroidManifest.xml` + `res/xml/network_security_config.xml`
- Debug 构建允许明文 HTTP，便于本地和局域网调试。
- Release 构建默认禁止明文 HTTP，应使用 HTTPS 后端。

### 3.3 权限用途（仅必要权限）
- `INTERNET`：访问后端API
- `ACCESS_NETWORK_STATE`：判断网络状态并提示离线
- `POST_NOTIFICATIONS`：每日内容提醒（Android 13+）

## 4. APK 打包步骤（小白版）

### 4.1 准备
1. 安装 Android Studio（建议 Koala 或以上）
2. 打开 Android Studio -> `Open` -> 选择 `android-native` 目录
3. 首次打开等待 Gradle 自动同步完成
4. 命令行构建时，`gradlew` 会在 macOS 下自动尝试查找本机 JDK 21；如果你本机没有 JDK 21，请先安装 Android Studio JBR 或 JDK 21

### 4.2 运行调试版（最快）
1. 连接模拟器或真机
2. 点击顶部绿色 `Run` 按钮
3. App 启动后即可使用

### 4.3 打包 Debug APK
1. 菜单：`Build` -> `Build Bundle(s) / APK(s)` -> `Build APK(s)`，或命令行执行：
   - `./gradlew -p android-native assembleDebug`
2. 构建完成后点击弹窗里的 `locate`
3. APK 路径：
   - `android-native/app/build/outputs/apk/debug/app-debug.apk`
4. 如果已连接设备或模拟器，可直接执行：
   - `./android-native/scripts/install-and-launch.sh debug`
5. 如果要核对包信息、签名摘要和默认后端地址，可执行：
   - `./android-native/scripts/verify-apk.sh debug`

### 4.4 打包 Release APK（可发版）
1. 如需正式签名，先复制 `android-native/keystore.properties.example` 为 `android-native/keystore.properties`，再填入真实值：
```properties
storeFile=your-release-key.jks
storePassword=***
keyAlias=***
keyPassword=***
```
2. 如果没有 `keystore.properties`，Release 构建会直接失败，避免误发 debug 签名包
3. 菜单：`Build` -> `Generate Signed Bundle / APK`，或命令行执行：
   - `./gradlew -p android-native assembleRelease`
4. 完成后 APK 路径：
   - `android-native/app/build/outputs/apk/release/app-release.apk`
5. 如果设备已连接，也可直接安装并启动：
   - `./android-native/scripts/install-and-launch.sh release`
6. 如果要核对 Release 包元数据和签名摘要，可执行：
   - `./android-native/scripts/verify-apk.sh release`

### 4.5 常见问题
- 无法请求后端：检查首页里的“后端地址”是否改成可访问地址
- 真机访问失败：手机和电脑需在同一局域网
- 命令行构建报 JDK 问题：先确认本机存在 JDK 21，或执行 `/usr/libexec/java_home -V` 检查
- 脚本提示没有在线设备：先用 `adb devices` 确认手机已授权，或先启动 Android 模拟器
- 图片加载失败：会自动显示 404 占位图（已做兜底）
