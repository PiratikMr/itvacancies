from utils import get_config
from config_ETL import DAGS_CONFIG_PATH, DEFAULT_ARGS
from airflow.decorators import dag
from airflow.operators.bash import BashOperator

conf_tree = get_config(DAGS_CONFIG_PATH)

keep_dumps = conf_tree.get_int("Dags.BackupDB.keepDumps")


CLEAN_STAGING_CMD = r"""
set -euo pipefail
PGPASSWORD="$PG_PASS" psql -v ON_ERROR_STOP=1 \
  -h "$HOST_APP_POSTGRES" -U "$PG_USER" -d "$PG_DB" <<'SQL' 2>&1
DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT tablename FROM pg_tables
        WHERE schemaname = 'public' AND tablename ~ '^staging_[0-9a-f]{32}$'
    LOOP
        EXECUTE format('DROP TABLE IF EXISTS public.%I', r.tablename);
        RAISE NOTICE 'dropped stale staging table %', r.tablename;
    END LOOP;
END $$;
SQL
"""


DUMP_CMD = r"""
set -euo pipefail
mkdir -p /opt/airflow/dumps
out="/opt/airflow/dumps/itvacancies_data_{{ ds }}.sql.gz"
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
"""


PRUNE_CMD = f"""
set -eu
cd /opt/airflow/dumps
rm -f -- *.tmp
ls -1t -- *.sql.gz 2>/dev/null | tail -n +{keep_dumps + 1} | xargs -r rm -f --
echo "kept newest {keep_dumps} dump(s) in /opt/airflow/dumps"
"""


@dag(
    dag_id="Backup_database",
    default_args=DEFAULT_ARGS,
    tags=["bash", "postgresql", "backup"],
    schedule=conf_tree.get_string("Dags.BackupDB.schedule") or None,
    catchup=False
)
def create_dag():
    clean_staging = BashOperator(task_id="clean_staging_tables", bash_command=CLEAN_STAGING_CMD)
    dump = BashOperator(task_id="dump_database", bash_command=DUMP_CMD)
    prune = BashOperator(task_id="prune_old_dumps", bash_command=PRUNE_CMD)

    clean_staging >> dump >> prune


create_dag()
