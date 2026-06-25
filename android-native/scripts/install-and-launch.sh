#!/bin/sh

set -eu

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)

VARIANT=${1:-debug}
case "$VARIANT" in
  debug|release) ;;
  *)
    echo "Usage: $0 [debug|release]" >&2
    exit 1
    ;;
esac

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found. Install Android Platform Tools first." >&2
  exit 1
fi

DEVICE_COUNT=$(adb devices | awk 'NR>1 && $2=="device" {count++} END {print count+0}')
if [ "$DEVICE_COUNT" -eq 0 ]; then
  echo "No online Android device or emulator found." >&2
  exit 1
fi

if [ "$VARIANT" = "debug" ]; then
  TASK=assembleDebug
  APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
else
  TASK=assembleRelease
  APK_PATH="$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"
fi

"$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" "$TASK"

if [ ! -f "$APK_PATH" ]; then
  echo "APK not found: $APK_PATH" >&2
  exit 1
fi

adb install -r "$APK_PATH"
adb shell am start -n com.example.randomgallery.android/.MainActivity

echo "Installed and launched $VARIANT APK: $APK_PATH"
