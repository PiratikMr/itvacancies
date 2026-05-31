from utils import get_config
from config_ETL import DAGS_CONFIG_PATH, DEFAULT_ARGS
from airflow.decorators import dag
from airflow.operators.bash import BashOperator

conf_tree = get_config(DAGS_CONFIG_PATH)

keep_dumps = conf_tree.get_int("Dags.BackupDB.keepDumps")


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
    dump = BashOperator(task_id="dump_database", bash_command=DUMP_CMD)
    prune = BashOperator(task_id="prune_old_dumps", bash_command=PRUNE_CMD)

    dump >> prune


create_dag()
