# 安卓小红书 Cookie 管理

下载与解析管理页新增 Cookie 卡片：

- 登录获取：打开小红书网页，在网页完成登录后点击“获取 Cookie 并校验”。只读取小红书网站的 Cookie，检测到非空 web_session 后保存并校验。
- 手动更新：粘贴完整 Cookie，保存后自动校验。
- 校验：使用当前保存值请求后端，不创建任务，也不下载文件。
- 清除：删除用于入库的本地 Cookie。之后新任务传 null，下载器继续使用服务端默认配置。网页登录状态单独由 WebView 管理。

每次新建入库任务都会读取 DataStore 最新 Cookie，并写入任务的 params_cookie；后台消费时传给下载器。已经创建的任务（包括对旧任务的重试）仍使用该任务原有 Cookie。

## 服务配置与启动

后端需要配置 `other.downloader.probe-url`，可以通过环境变量 `OTHER_DOWNLOADER_PROBE_URL` 指定一个可访问的小红书作品完整链接（保留必要的 xsec_token 等参数）。不要提交真实 Cookie 到代码仓库。

```sh
# 项目根目录；使用 Java 21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
# OTHER_DOWNLOADER_PROBE_URL 设为实际可访问的完整作品链接后启动
mvn spring-boot:run

# 安卓工程目录
cd android-native
./gradlew :app:assembleDebug
# 安装到已连接的 arm64 设备
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

## 测试

```sh
# 项目根目录
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn -Dtest=CookieValidationTest,CookieValidationRequestTest,CookieLoggingTest test

cd android-native
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

自动测试覆盖：Cookie 格式、持久化、入库传参、空值回退、更新期间旧校验响应、保存失败、保存期间入库保护、保存后校验、HTTP 错误、不创建下载任务以及后端日志脱敏。

真机验收：进入管理页 → 登录获取或手动更新 → 观察校验状态 → 提交作品入库 → 检查任务完成 → 清除 Cookie 后再次提交。网络失败应提示并允许重试，不把服务错误推断为 Cookie 失效。

## 校验范围与限制

校验沿用下载器的元数据探测：`download=false`，能获得非空作品 ID 就表示此次下载器请求可用。这不是独立的账号身份认证；探测作品删除、私密或链接签名失效也会导致未通过。应配置稳定、可访问的探测作品。

登录页面可能受小红书网页登录方式、验证码、风控和手机 WebView 版本影响；无法网页登录时可以手动更新。页面加载失败有错误提示和刷新入口。Cookie 不显示在管理卡片里，编辑框默认遮挡；安卓入库/校验请求不打印网络正文，后端请求参数日志对 Cookie 脱敏。DataStore 和 WebView 数据均排除备份与设备迁移。

## 本次续改文件

- `app/src/main/java/com/example/randomgallery/android/ui/download/DownloadManageScreen.kt`
- `app/src/main/java/com/example/randomgallery/android/ui/download/DownloadManageViewModel.kt`
- `app/src/main/java/com/example/randomgallery/android/ui/download/XhsCookieCard.kt`（新增）
- `app/src/main/java/com/example/randomgallery/android/ui/download/XhsCookieLoginDialog.kt`（新增）
- `app/src/main/java/com/example/randomgallery/android/data/network/NetworkModule.kt`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/test/java/com/example/randomgallery/android/ui/download/DownloadManageViewModelTest.kt`
- `../src/main/java/com/example/randomGallery/service/Impl/DownloadApiImpl.java`
- `../src/main/java/com/example/randomGallery/config/ApiLoggingAspect.java`
- `../src/test/java/com/example/randomGallery/service/Impl/CookieValidationRequestTest.java`（新增）
- `../src/test/java/com/example/randomGallery/config/CookieLoggingTest.java`（新增）
- 本说明文件（新增）

登录读取使用 Android [CookieManager 官方 API](https://developer.android.com/reference/android/webkit/CookieManager)。

## 本次自检结果（2026-10-09）

- 安卓 debug 编译、打包成功；产物 `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk`。
- 安卓全部 70 个 JVM 单测通过（其中管理 ViewModel 23 个、Cookie 持久化 2 个）。
- 后端 Java 21 编译成功；Cookie 校验及日志脱敏相关 8 个测试通过。
- `git diff --check` 通过；复用已有任务 Cookie 字段，无新增表或依赖。
- lint 未完成：离线缓存缺少 lint 32.2.1 工具依赖；联网重试等待 HTTPS 依赖下载超过数分钟，线程栈停在 Gradle artifact download。网络恢复后重跑 `./gradlew :app:lintDebug`。
- 无已连接安卓设备，因此真实网页登录、真实 Cookie 和下载器联调尚未验证；未填入探测作品地址或实际账号凭据。
- GitNexus 完整工作区检测为 CRITICAL，覆盖此前已有 43 个已跟踪文件、299 个符号的未提交改动；不是仅本次 Cookie 续改范围。未执行提交。
