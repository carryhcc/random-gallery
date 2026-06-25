#!/usr/bin/env bash
# 在你的 Mac 上构建 debug APK 并安装到已连接的手机。
# 用法：
#   1) 手机用 USB 连接电脑，开启“开发者选项 → USB 调试”，并在手机上点“允许”此电脑调试
#   2) cd android-native && ./build-and-install-debug.sh
set -euo pipefail

cd "$(dirname "$0")"

# 让 gradle 找到 Android SDK（如已配 ANDROID_HOME 可忽略）
if [ ! -f local.properties ]; then
  echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
  echo "已生成 local.properties -> $HOME/Library/Android/sdk"
fi

ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
[ -x "$ADB" ] || ADB="$(command -v adb || true)"

echo "==> 构建 debug APK（首次会下载依赖，请耐心等待）"
./gradlew :app:assembleDebug

APK="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK" ]; then
  echo "构建未产出 APK：$APK" >&2
  exit 1
fi
echo "==> APK 已生成：$APK"

if [ -n "${ADB:-}" ] && [ -x "$ADB" ]; then
  DEVICES="$("$ADB" devices | grep -w device | grep -v '^List' || true)"
  if [ -n "$DEVICES" ]; then
    echo "==> 检测到设备，正在安装"
    "$ADB" install -r "$APK"
    echo "✅ 安装完成，去手机上打开「随机图库」"
  else
    echo "⚠️  未检测到已授权的设备。请确认 USB 调试已开启并在手机上点了“允许”，然后手动执行："
    echo "    $ADB install -r $APK"
  fi
else
  echo "⚠️  未找到 adb。可手动安装：把 $APK 传到手机点击安装，或安装 platform-tools 后执行 adb install -r。"
fi
