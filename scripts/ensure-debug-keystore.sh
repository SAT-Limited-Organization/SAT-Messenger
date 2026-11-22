#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEYSTORE_FILE="${ROOT_DIR}/debug.keystore"
BASE64_FILE="${ROOT_DIR}/debug.keystore.base64"

if [ ! -f "$KEYSTORE_FILE" ]; then
  if [ -n "${DEBUG_KEYSTORE_BASE64:-}" ]; then
    printf '%s' "$DEBUG_KEYSTORE_BASE64" | base64 -d > "$KEYSTORE_FILE"
  elif [ -f "$BASE64_FILE" ]; then
    base64 -d "$BASE64_FILE" > "$KEYSTORE_FILE"
  else
    keytool -genkeypair -v \
      -keystore "$KEYSTORE_FILE" \
      -storepass android \
      -alias androiddebugkey \
      -keypass android \
      -keyalg RSA \
      -keysize 2048 \
      -validity 10000 \
      -dname "CN=Android Debug,O=Android,C=US"
  fi
fi

chmod 644 "$KEYSTORE_FILE"
echo "Verified readable debug.keystore at $KEYSTORE_FILE"
