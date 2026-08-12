from utils import get_config
from config_ETL import DAGS_CONFIG_PATH, DEFAULT_ARGS
from airflow.decorators import dag
from airflow.operators.bash import BashOperator

conf_tree = get_config(DAGS_CONFIG_PATH)

@dag(
    dag_id="Delete_expiredData",
    default_args=DEFAULT_ARGS,
    tags=["bash", "hdfs", "cleanup"],
    schedule=conf_tree.get_string("Dags.DeleteData.schedule") or None,
    catchup=False
)
def create_dag():
    BashOperator(
        task_id="delete_expired_directories",
        bash_command=(
            'bash /opt/airflow/dags/scripts/delete_expired_hdfs.sh '
            '"{{ ds }}" "{{ params.expire_days }}" "{{ params.hdfs_root }}"'
        ),
        params={
            "expire_days": conf_tree.get_string("Dags.DeleteData.rawData"),
            "hdfs_root": conf_tree.get_string("FileSystem.path")
        }
    )

    BashOperator(
        task_id="delete_expired_logs",
        bash_command=(
            'bash /opt/airflow/dags/scripts/delete_expired_logs.sh '
            '"{{ params.logs_root }}" "{{ params.expire_days }}"'
        ),
        params={
            "logs_root": "/opt/airflow/logs",
            "expire_days": conf_tree.get_string("Dags.DeleteData.airflowLogs")
        }
    )

create_dag()
