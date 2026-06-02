FROM apache/superset:6.1.0

USER root

RUN pip install psycopg2-binary clickhouse-connect --target /app/.venv/lib/python3.10/site-packages

RUN mkdir -p /app/superset_data && chown -R superset:superset /app/superset_data

COPY conf/patch_native_filters.sh /tmp/patch_native_filters.sh
RUN sh /tmp/patch_native_filters.sh

USER superset