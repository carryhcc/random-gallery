#!/usr/bin/env bash
# 在你的 Mac 上构建 debug APK 并安装到已连接的手机。
# 用法：
#   1) 手机用 USB 连接电脑，开启“开发者选项 → USB 调试”，并在手机上点“允许”此电脑调试
#   2) cd android-native && ./build-and-install-debug.sh
set -euo pipefail

cd "$(dirname "$0")"

# 让 gradle 找到 Android SDK（检测常见 macOS 路径）
SDK_PATH=""
if [ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME" ]; then
  SDK_PATH="$ANDROID_HOME"
elif [ -n "${ANDROID_SDK_ROOT:-}" ] && [ -d "$ANDROID_SDK_ROOT" ]; then
  SDK_PATH="$ANDROID_SDK_ROOT"
else
  POSSIBLE_PATHS=(
    "$HOME/Library/Android/sdk"
    "/opt/homebrew/share/android-commandlinetools"
    "/opt/homebrew/share/android-sdk"
    "/usr/local/share/android-sdk"
  )
  for p in "${POSSIBLE_PATHS[@]}"; do
    if [ -d "$p" ]; then
      SDK_PATH="$p"
      break
    fi
  done
fi

if [ -n "$SDK_PATH" ]; then
  echo "sdk.dir=$SDK_PATH" > local.properties
  echo "==> 自动识别并配置 Android SDK 路径：$SDK_PATH"
elif [ -f local.properties ]; then
  EXISTING_DIR="$(grep '^sdk.dir=' local.properties | cut -d'=' -f2- || true)"
  if [ -n "$EXISTING_DIR" ] && [ -d "$EXISTING_DIR" ]; then
    SDK_PATH="$EXISTING_DIR"
  fi
fi

if [ -z "$SDK_PATH" ] || [ ! -d "$SDK_PATH" ]; then
  echo "❌ 构建失败：未在你的 Mac 本地上检测到 Android SDK 路径。" >&2
  echo "原因是系统尚未安装 Android SDK，或者 SDK 目录不存在。" >&2
  echo "" >&2
  echo "💡 解决方法（请任选一种）：" >&2
  echo "  方式 1（推荐）：下载并安装 Android Studio" >&2
  echo "     官网下载：https://developer.android.com/studio" >&2
  echo "     或使用终端命令：brew install --cask android-studio" >&2
  echo "     安装后打开 Android Studio 完成初始化 SDK 组件下载即可。" >&2
  echo "" >&2
  echo "  方式 2：使用 Homebrew 直接安装 Command Line Tools" >&2
  echo "     终端执行：brew install --cask android-commandlinetools" >&2
  echo "" >&2
  echo "安装完成之后，重新运行本脚本即可自动识别并完成构建！" >&2
  exit 1
fi

ADB="$SDK_PATH/platform-tools/adb"
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
