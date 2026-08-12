#!/usr/bin/env bash
set -eu

logs_root="$1"
expire_days="$2"

find "$logs_root/scheduler" -mindepth 1 -maxdepth 1 -type d -mtime "+$expire_days" \
    -print -exec rm -rf -- {} +

find "$logs_root/dag_processor_manager" -maxdepth 1 -type f -name '*.log.*' -mtime "+$expire_days" \
    -print -delete

echo "removed scheduler logs older than $expire_days day(s)"
