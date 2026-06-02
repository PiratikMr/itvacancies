#!/bin/sh
set -eu

ASSETS=/app/superset/static/assets
GATE='\?void 0:[A-Za-z0-9_$]+\.userId\)&&[A-Za-z0-9_$]+\('

files=$(grep -rlE "$GATE" "$ASSETS" 2>/dev/null || true)
if [ -z "$files" ]; then
    echo "[patch_native_filters] gate pattern not found — already patched or Superset internals changed, skipping"
    exit 0
fi

echo "$files" | xargs sed -i -E \
    's/\(null==[A-Za-z0-9_$]+\?void 0:[A-Za-z0-9_$]+\.userId\)&&([A-Za-z0-9_$]+\()/(!0)\&\&\1/g'
echo "[patch_native_filters] patched native-filter userId gate in:"
echo "$files" | sed 's/^/  /'
