#!/usr/bin/env bash
set -euo pipefail

ds="$1"

mkdir -p /opt/airflow/dumps
out="/opt/airflow/dumps/itvacancies_data_${ds}.sql.gz"
tmp="$out.tmp"

PGPASSWORD="$PG_PASS" pg_dump \
  -h "$HOST_APP_POSTGRES" -U "$PG_USER" -d "$PG_DB" \
  -n public \
  --data-only --no-owner --no-privileges \
  --exclude-table=flyway_schema_history \
  | sed -E '/^\\(un)?restrict [A-Za-z0-9]+$/d' \
  | gzip > "$tmp"
mv "$tmp" "$out"
echo "wrote $out ($(du -h "$out" | cut -f1))"
