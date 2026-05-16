from config_ETL import PLATFORMS, DEFAULT_ARGS
from utils import build_spark_etl_task, get_config, parse_args
from airflow.decorators import dag
from airflow.operators.bash import BashOperator

def generate_platform_dag(platform_cfg):
    conf_tree = get_config(platform_cfg.fileName)

    raw_schedule = conf_tree.get_string("Dags.ETL.schedule")
    dag_schedule = raw_schedule if raw_schedule else None

    @dag(
        dag_id=f"{platform_cfg.name}_ETL",
        tags=["scala", "etl", platform_cfg.moduleName],
        default_args=DEFAULT_ARGS,
        schedule=dag_schedule,
        catchup=False
    )
    def platform_dag():
        args = parse_args(conf_tree, platform_cfg.args)
        batches = platform_cfg.batch_extra_args or [None]
        prev_task = None

        for i, batch_args in enumerate(batches):
            extra = batch_args or []
            suffix = f"_{i}" if platform_cfg.batch_extra_args else ""

            for part in platform_cfg.parts:
                curr_task = build_spark_etl_task(
                    platform=platform_cfg,
                    part=part,
                    args=args + extra,
                    task_name=f"{part}{suffix}"
                )
                if prev_task:
                    prev_task >> curr_task
                prev_task = curr_task

            is_last = (i == len(batches) - 1)
            if not is_last and platform_cfg.inter_batch_wait_secs > 0:
                wait = BashOperator(
                    task_id=f"wait_after_idx_{i}",
                    bash_command=f"sleep {platform_cfg.inter_batch_wait_secs}"
                )
                prev_task >> wait
                prev_task = wait

    return platform_dag()

for platform_config in PLATFORMS:
    dag_instance = generate_platform_dag(platform_config)
    globals()[dag_instance.dag_id] = dag_instance