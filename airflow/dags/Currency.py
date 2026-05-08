from config_ETL import DEFAULT_ARGS, Platform
from utils import build_spark_etl_task, get_config, parse_args
from airflow.decorators import dag

config = Platform("currency", "Currency", u=False, module_path="Currency")

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

    for i in range(len(tasks) - 1):
        tasks[i] >> tasks[i + 1]

create_dag()
