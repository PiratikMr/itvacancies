from config_ETL import DAGS_CONFIG_PATH, DEFAULT_ARGS
from airflow.decorators import dag, task
from airflow.providers.postgres.hooks.postgres import PostgresHook
from utils import get_config
from clickhouse_sync import sync_mv_core_vacancy, write_refresh_log
from datetime import datetime, timezone

config = get_config(DAGS_CONFIG_PATH)
dag_schedule = config.get('Dags.RefreshMatViews.schedule')

@dag(
    dag_id="Refresh_Materialized_views",
    default_args=DEFAULT_ARGS,
    tags=["python", "postgresql"],
    schedule=dag_schedule or None,
    catchup=False
)
def create_dag():
    @task
    def start_refresh_log(**context) -> int:
        pg_hook = PostgresHook(postgres_conn_id="POSTGRES_CONN")
        record = pg_hook.get_first(
            "INSERT INTO meta.refresh_log (dag_id, dag_run_id, started_at, status) "
            "VALUES (%s, %s, now(), 'running') RETURNING id",
            parameters=(context['dag'].dag_id, context['dag_run'].run_id)
        )
        return record[0]

    @task(trigger_rule='all_done')
    def finish_refresh_log(log_id: int, **context):
        dag_run = context['dag_run']
        failed = [
            ti for ti in dag_run.get_task_instances()
            if ti.state in ('failed', 'upstream_failed')
            and ti.task_id != 'finish_refresh_log'
        ]
        status = 'failure' if failed else 'success'
        error = '; '.join(f"{ti.task_id}: {ti.state}" for ti in failed) if failed else None
        finished_at = datetime.now(timezone.utc).replace(tzinfo=None)
        pg_hook = PostgresHook(postgres_conn_id="POSTGRES_CONN")
        pg_hook.run(
            "UPDATE meta.refresh_log SET finished_at = %s, status = %s, error = %s WHERE id = %s",
            parameters=(finished_at, status, error, log_id)
        )
        if status == 'success':
            write_refresh_log(finished_at)

    @task
    def sync_to_clickhouse():
        sync_mv_core_vacancy()

    log_id = start_refresh_log()

    sync_ch = sync_to_clickhouse()

    log_id >> sync_ch >> finish_refresh_log(log_id)

create_dag()
