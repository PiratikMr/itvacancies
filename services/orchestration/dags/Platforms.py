from config_ETL import PLATFORMS, DEFAULT_ARGS, ETL_MIN_ROWS
from utils import build_spark_etl_task, build_row_check_task, get_config, parse_args
from airflow.decorators import dag
from airflow.models.baseoperator import chain
from airflow.operators.bash import BashOperator

def generate_platform_dag(platform_cfg):
    conf_tree = get_config(platform_cfg.fileName)

    @dag(
        dag_id=f"{platform_cfg.name}_ETL",
        tags=["scala", "etl", platform_cfg.moduleName],
        default_args=DEFAULT_ARGS,
        schedule=conf_tree.get_string("Dags.ETL.schedule") or None,
        catchup=False
    )
    def platform_dag():
        args = parse_args(conf_tree, platform_cfg.args)
        groups = platform_cfg.batch_groups or [[None]]

        tasks = []
        batch_idx = 0

        for group in groups:
            for i, batch_args in enumerate(group):
                suffix = f"_{batch_idx}" if platform_cfg.batch_groups else ""

                tasks += [
                    build_spark_etl_task(
                        platform=platform_cfg,
                        part=part,
                        args=args + (batch_args or []),
                        task_name=f"{part}{suffix}"
                    )
                    for part in platform_cfg.parts
                ]

                if i < len(group) - 1 and platform_cfg.inter_batch_wait_secs > 0:
                    tasks.append(BashOperator(
                        task_id=f"wait_after_idx_{batch_idx}",
                        bash_command=f"sleep {platform_cfg.inter_batch_wait_secs}"
                    ))

                batch_idx += 1

        tasks.append(build_row_check_task(
            platform_name=conf_tree.get_string("FileSystem.platform"),
            min_rows=ETL_MIN_ROWS
        ))

        chain(*tasks)

    return platform_dag()

for platform_config in PLATFORMS:
    dag_instance = generate_platform_dag(platform_config)
    globals()[dag_instance.dag_id] = dag_instance
