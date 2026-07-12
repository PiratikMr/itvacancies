from utils import get_config
from config_ETL import DAGS_CONFIG_PATH, DEFAULT_ARGS
from airflow.decorators import dag
from airflow.operators.bash import BashOperator
from airflow.providers.common.sql.operators.sql import SQLExecuteQueryOperator

conf_tree = get_config(DAGS_CONFIG_PATH)

keep_dumps = conf_tree.get_int("Dags.BackupDB.keepDumps")

CLEAN_STAGING_SQL = """
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
"""


@dag(
    dag_id="Backup_database",
    default_args=DEFAULT_ARGS,
    tags=["bash", "postgresql", "backup"],
    schedule=conf_tree.get_string("Dags.BackupDB.schedule") or None,
    catchup=False
)
def create_dag():
    clean_staging = SQLExecuteQueryOperator(
        task_id="clean_staging_tables",
        conn_id="POSTGRES_CONN",
        sql=CLEAN_STAGING_SQL
    )

    dump = BashOperator(
        task_id="dump_database",
        bash_command='bash /opt/airflow/dags/scripts/dump_database.sh "{{ ds }}"'
    )

    prune = BashOperator(
        task_id="prune_old_dumps",
        bash_command='bash /opt/airflow/dags/scripts/prune_dumps.sh "{{ params.keep }}"',
        params={"keep": keep_dumps}
    )

    clean_staging >> dump >> prune


create_dag()
