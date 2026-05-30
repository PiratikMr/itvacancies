#!/bin/sh
set -eu

TEMPLATE_DIR="/etc/nginx/templates"
OUTPUT_CONF="/etc/nginx/nginx.conf"
SNIPPET_OUT="/etc/nginx/snippets"

VARS='${DOMAIN} ${SUPERSET_UPSTREAM} ${SUPERSET_SUBDOMAIN} ${SUPERSET_DASHBOARD_PATH} ${AIRFLOW_UPSTREAM} ${AIRFLOW_SUBDOMAIN} ${GRAFANA_UPSTREAM} ${GRAFANA_SUBDOMAIN}'

mkdir -p "$SNIPPET_OUT"

for f in "$TEMPLATE_DIR"/snippets/*.conf.template; do
    basename_file=$(basename "$f" .template)
    envsubst "$VARS" < "$f" > "$SNIPPET_OUT/$basename_file"
done

envsubst "$VARS" < "$TEMPLATE_DIR/nginx.conf.template" > "$OUTPUT_CONF"

exec nginx -g 'daemon off;'
