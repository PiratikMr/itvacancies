#!/usr/bin/env bash
set -eu

keep="$1"

cd /opt/airflow/dumps
rm -f -- *.tmp
ls -1t -- *.sql.gz 2>/dev/null | tail -n +$((keep + 1)) | xargs -r rm -f --
echo "kept newest $keep dump(s) in /opt/airflow/dumps"
