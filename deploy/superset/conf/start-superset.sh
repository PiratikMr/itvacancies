#!/bin/bash
set -euo pipefail

: "${SUPERSET_ADMIN_USERNAME:?SUPERSET_ADMIN_USERNAME must be set}"
: "${SUPERSET_ADMIN_FIRSTNAME:?SUPERSET_ADMIN_FIRSTNAME must be set}"
: "${SUPERSET_ADMIN_LASTNAME:?SUPERSET_ADMIN_LASTNAME must be set}"
: "${SUPERSET_ADMIN_EMAIL:?SUPERSET_ADMIN_EMAIL must be set}"
: "${SUPERSET_ADMIN_PASSWORD:?SUPERSET_ADMIN_PASSWORD must be set}"


MARKER_FILE="/app/superset_data/.superset_initialized"
ZIP_PATH="/superset-mount/dashboards/*.zip"

if [ ! -f "$MARKER_FILE" ]; then
    superset fab create-admin \
                --username "$SUPERSET_ADMIN_USERNAME" \
                --firstname "$SUPERSET_ADMIN_FIRSTNAME" \
                --lastname "$SUPERSET_ADMIN_LASTNAME" \
                --email "$SUPERSET_ADMIN_EMAIL" \
                --password "$SUPERSET_ADMIN_PASSWORD"
    superset db upgrade
    superset init

    if ls $ZIP_PATH 1> /dev/null 2>&1; then
        for dash in $ZIP_PATH; do
            python3 /superset-mount/conf/import_dashboard.py "$dash"
        done
    fi

    python3 /superset-mount/conf/setup_roles.py

    touch "$MARKER_FILE"
fi

sh /superset-mount/conf/patch_native_filters.sh || echo "[start-superset] frontend patch failed (non-fatal)"

/usr/bin/run-server.sh
