from datetime import timezone
from pathlib import Path
from pyhocon import ConfigFactory, ConfigTree
from airflow.exceptions import AirflowException
from airflow.operators.python import PythonOperator
from airflow.providers.apache.spark.operators.spark_submit import SparkSubmitOperator
from airflow.providers.postgres.hooks.postgres import PostgresHook
from config_ETL import ArgDef, Platform

def get_config(file_path: str) -> ConfigTree:
    return ConfigFactory.parse_file(Path(file_path))

def parse_args(config: ConfigTree, args: list[ArgDef]) -> list[str]:
    cli_args = []

    for arg in args:
        cli_args.append(f"--{arg.name}")
        if arg.is_static:
            cli_args.append(arg.value)
        else:
            cli_args.append(str(config.get(arg.value)))

    return cli_args

ROW_CHECK_SQL = """
    SELECT count(*)
    FROM fact_vacancy f
    JOIN dim_platform p USING (platform_id)
    WHERE p.platform = %(platform)s
      AND f.updated_at >= %(run_start)s - interval '1 minute'
"""

def _check_min_rows(platform: str, min_rows: int, **context):
    run_start = context["dag_run"].start_date.astimezone(timezone.utc).replace(tzinfo=None)

    pg_hook = PostgresHook(postgres_conn_id="POSTGRES_CONN")
    rows = pg_hook.get_first(
        ROW_CHECK_SQL,
        parameters={"platform": platform, "run_start": run_start}
    )[0]

    print(f"{platform}: {rows} row(s) touched this run, required >= {min_rows}")
    if rows < min_rows:
        raise AirflowException(f"{platform}: за прогон затронуто {rows} вакансий (минимум {min_rows})")

def build_row_check_task(platform_name: str, min_rows: int) -> PythonOperator:
    return PythonOperator(
        task_id="check_min_rows",
        python_callable=_check_min_rows,
        op_kwargs={"platform": platform_name, "min_rows": min_rows}
    )


def build_spark_etl_task(platform: Platform, part: str, args: list[str], task_name: str = None) -> SparkSubmitOperator:
    log_conf_path = '/opt/airflow/scalaProject/Core/src/main/resources/log4j2.properties'
    
    return SparkSubmitOperator(
        task_id=task_name or part,
        conn_id="SPARK_CONN",
        application=(
            f'/opt/airflow/scalaProject/{platform.moduleName}/'
            f"target/scala-2.13/{platform.name}-etl.jar"
        ),
        files=log_conf_path,
        application_args=args + ["--etlpart", part],
        conf={
            'spark.driver.extraJavaOptions': f'-Dlog4j.configurationFile=file://{log_conf_path}',
            'spark.executor.extraJavaOptions': '-Dlog4j.configurationFile=log4j2.properties'
        }
    )