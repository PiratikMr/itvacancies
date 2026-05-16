from pathlib import Path
from pyhocon import ConfigFactory, ConfigTree
from airflow.providers.apache.spark.operators.spark_submit import SparkSubmitOperator
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