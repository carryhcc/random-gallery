#!/usr/bin/env bash
# 构建 APK 并安装到已连接的 Android 设备并自动拉起。
# 用法：
#   1) 手机用 USB 或无线调试连接电脑
#   2) cd android-native && ./build-and-install.sh [debug|release]
set -euo pipefail

cd "$(dirname "$0")"

MODE="${1:-debug}"
if [ "$MODE" != "debug" ] && [ "$MODE" != "release" ]; then
  echo "用法: $0 [debug|release] (默认: debug)"
  exit 1
fi

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
  exit 1
fi

ADB="$SDK_PATH/platform-tools/adb"
[ -x "$ADB" ] || ADB="$(command -v adb || true)"

PACKAGE_NAME="io.github.randomgallery.app"
MAIN_ACTIVITY="com.example.randomgallery.android.MainActivity"

if [ "$MODE" = "release" ]; then
  echo "==> 构建 Release APK（带 R8 优化和 ABI 架构分包）"
  ./gradlew :app:assembleRelease
  APK_DIR="app/build/outputs/apk/release"
else
  echo "==> 构建 Debug APK"
  ./gradlew :app:assembleDebug
  APK_DIR="app/build/outputs/apk/debug"
fi

if [ ! -d "$APK_DIR" ]; then
  echo "❌ 构建未产出目录：$APK_DIR" >&2
  exit 1
fi

if [ -n "${ADB:-}" ] && [ -x "$ADB" ]; then
  # 获取已授权设备序列号列表
  DEVICES=$("$ADB" devices | grep -w device | grep -v '^List' | awk '{print $1}' || true)
  if [ -n "$DEVICES" ]; then
    for dev in $DEVICES; do
      MODEL=$("$ADB" -s "$dev" shell getprop ro.product.model 2>/dev/null | tr -d '\r\n' || echo "$dev")
      ABI=$("$ADB" -s "$dev" shell getprop ro.product.cpu.abi 2>/dev/null | tr -d '\r\n' || echo "arm64-v8a")
      
      # 寻找匹配该设备架构的最佳 APK
      TARGET_APK=""
      if [ -f "$APK_DIR/app-$ABI-$MODE.apk" ]; then
        TARGET_APK="$APK_DIR/app-$ABI-$MODE.apk"
      elif [ -f "$APK_DIR/app-arm64-v8a-$MODE.apk" ]; then
        TARGET_APK="$APK_DIR/app-arm64-v8a-$MODE.apk"
      elif [ -f "$APK_DIR/app-universal-$MODE.apk" ]; then
        TARGET_APK="$APK_DIR/app-universal-$MODE.apk"
      elif [ -f "$APK_DIR/app-$MODE.apk" ]; then
        TARGET_APK="$APK_DIR/app-$MODE.apk"
      else
        TARGET_APK=$(find "$APK_DIR" -name "*.apk" | head -n 1)
      fi

      if [ -z "$TARGET_APK" ] || [ ! -f "$TARGET_APK" ]; then
        echo "❌ 未找到对应 APK 文件" >&2
        exit 1
      fi

      APK_SIZE=$(ls -lh "$TARGET_APK" | awk '{print $5}')
      echo "==> 正在安装到设备 [$MODEL ($dev)] [ABI: $ABI] [包体大小: $APK_SIZE]"
      echo "    APK: $TARGET_APK"
      "$ADB" -s "$dev" install -r -d "$TARGET_APK"

      echo "==> 启动应用「随机图库」..."
      "$ADB" -s "$dev" shell am start -n "$PACKAGE_NAME/$MAIN_ACTIVITY" >/dev/null 2>&1 || true
      echo "✅ 设备 [$MODEL] 安装并拉起完成！"
    done
  else
    echo "⚠️  未检测到已授权的设备。请确认 USB 调试已开启并在手机上点了“允许”。"
  fi
else
  echo "⚠️  未找到 adb。"
fi
