from config_ETL import DEFAULT_ARGS, Platform
from utils import build_spark_etl_task, get_config, parse_args
from airflow.decorators import dag
from airflow.models.baseoperator import chain

config = Platform("currency", "Currency", with_update=False, module_path="Currency")

confTree = get_config(config.fileName)

@dag(
    dag_id="Currency_Updater",
    tags=["scala"],
    default_args=DEFAULT_ARGS,
    schedule=confTree.get_string("Dags.ETL.schedule") or None,
    catchup=False
)
def create_dag():
    args = parse_args(confTree, config.args)

    tasks = [
        build_spark_etl_task(platform=config, part=part, args=args, task_name=part)
        for part in config.parts
    ]

    chain(*tasks)

create_dag()
