from config_ETL import DAGS_CONFIG_PATH, DEFAULT_ARGS
from airflow.decorators import dag, task, task_group
from airflow.providers.postgres.hooks.postgres import PostgresHook
from utils import get_config

config = get_config(DAGS_CONFIG_PATH)
dag_schedule = config.get('Dags.RefreshMatViews.schedule')

schemas = [
    "marts"
]

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
        pg_hook = PostgresHook(postgres_conn_id="POSTGRES_CONN")
        pg_hook.run(
            "UPDATE meta.refresh_log SET finished_at = now(), status = %s, error = %s WHERE id = %s",
            parameters=(status, error, log_id)
        )

    @task
    def get_matviews_list(schema: str):
        pg_hook = PostgresHook(postgres_conn_id="POSTGRES_CONN")
        sql = "SELECT matviewname FROM pg_matviews WHERE schemaname = %s"

        records = pg_hook.get_records(sql, parameters=(schema,))
        mv_list = [row[0] for row in records]
        return mv_list

    @task
    def refresh_matview(mv_name: str, schema: str):
        pg_hook = PostgresHook(postgres_conn_id="POSTGRES_CONN")
        sql = f'REFRESH MATERIALIZED VIEW {schema}.{mv_name}; ANALYZE {schema}.{mv_name};'
        pg_hook.run(sql)

    log_id = start_refresh_log()

    refresh_core = refresh_matview.override(task_id="refresh_internal_core_vacancy")(
        mv_name="mv_core_vacancy",
        schema="internal"
    )

    log_id >> refresh_core
    prev = refresh_core

    for schema in schemas:

        @task_group(group_id=f"{schema}")
        def refresh_schema(schema: str):
            mv_list = get_matviews_list(schema)
            refresh_matview.partial(schema=schema).expand(mv_name=mv_list)

        curr = refresh_schema(schema)
        if prev:
            prev >> curr
        prev = curr

    prev >> finish_refresh_log(log_id)

create_dag()
