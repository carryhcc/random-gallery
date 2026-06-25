#!/bin/sh

set -eu

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)

ARG=${1:-release}
case "$ARG" in
  debug)
    APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
    ;;
  release)
    APK_PATH="$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"
    ;;
  *)
    APK_PATH="$ARG"
    ;;
esac

if [ ! -f "$APK_PATH" ]; then
    echo "APK not found: $APK_PATH" >&2
    exit 1
fi

find_sdk_tool() {
    TOOL_NAME=$1
    if command -v "$TOOL_NAME" >/dev/null 2>&1; then
        command -v "$TOOL_NAME"
        return 0
    fi

    ANDROID_SDK_ROOT_DIR=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-"$HOME/Library/Android/sdk"}}
    if [ -d "$ANDROID_SDK_ROOT_DIR/build-tools" ]; then
        FOUND_TOOL=$(find "$ANDROID_SDK_ROOT_DIR/build-tools" -path "*/$TOOL_NAME" | sort | tail -n 1)
        if [ -n "$FOUND_TOOL" ]; then
            printf '%s\n' "$FOUND_TOOL"
            return 0
        fi
    fi

    return 1
}

AAPT_BIN=$(find_sdk_tool aapt) || {
    echo "aapt not found. Install Android build-tools first." >&2
    exit 1
}
APKSIGNER_BIN=$(find_sdk_tool apksigner) || {
    echo "apksigner not found. Install Android build-tools first." >&2
    exit 1
}

BADGING_OUTPUT=$("$AAPT_BIN" dump badging "$APK_PATH")
PACKAGE_LINE=$(printf '%s\n' "$BADGING_OUTPUT" | awk -F"'" '/^package: name=/{print $2; exit}')
VERSION_CODE=$(printf '%s\n' "$BADGING_OUTPUT" | awk -F"'" '/^package: name=/{print $4; exit}')
VERSION_NAME=$(printf '%s\n' "$BADGING_OUTPUT" | awk -F"'" '/^package: name=/{print $6; exit}')
SDK_LINE=$(printf '%s\n' "$BADGING_OUTPUT" | awk '/^sdkVersion:|^targetSdkVersion:/{print}')
SHA256=$(shasum -a 256 "$APK_PATH" | awk '{print $1}')
DEX_ENTRIES=$(zipinfo -1 "$APK_PATH" | rg '^classes[0-9]*\.dex$' || true)
BASE_URLS=""
if [ -n "$DEX_ENTRIES" ]; then
    BASE_URLS=$(
        printf '%s\n' "$DEX_ENTRIES" |
            while IFS= read -r DEX_ENTRY; do
                unzip -p "$APK_PATH" "$DEX_ENTRY" | strings
            done |
            rg -o "https?://[^\"'[:space:]]+" |
            rg "8086/?$" |
            sort -u ||
            true
    )
fi

echo "APK: $APK_PATH"
echo "Package: $PACKAGE_LINE"
echo "VersionCode: $VERSION_CODE"
echo "VersionName: $VERSION_NAME"
printf '%s\n' "$SDK_LINE"
echo "SHA256: $SHA256"
echo "Signing:"
"$APKSIGNER_BIN" verify --print-certs "$APK_PATH" | sed 's/^/  /'
if [ -n "$BASE_URLS" ]; then
    echo "Embedded base URLs:"
    printf '%s\n' "$BASE_URLS" | sed 's/^/  /'
else
    echo "Embedded base URLs: none found"
fi
