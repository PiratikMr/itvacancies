from utils import get_config
from config_ETL import DAGS_CONFIG_PATH, DEFAULT_ARGS
from airflow.decorators import dag
from airflow.operators.bash import BashOperator

conf_tree = get_config(DAGS_CONFIG_PATH)


@dag(
    dag_id="Archive_toDrive",
    default_args=DEFAULT_ARGS,
    tags=["bash", "hdfs", "backup", "gdrive"],
    schedule=conf_tree.get_string("Dags.ArchiveDrive.schedule") or None,
    catchup=False
)
def create_dag():
    BashOperator(
        task_id="archive_raw_to_drive",
        bash_command=(
            'bash /opt/airflow/dags/scripts/archive_hdfs_to_drive.sh '
            '"$(date +%F)" "{{ params.archive_days }}" "{{ params.hdfs_root }}" '
            '"{{ params.remote }}" "{{ params.rclone_conf }}"'
        ),
        env={"TZ": "Asia/Krasnoyarsk"},
        append_env=True,
        params={
            "archive_days": conf_tree.get_string("Dags.ArchiveDrive.archiveDays"),
            "hdfs_root": conf_tree.get_string("FileSystem.path"),
            "remote": conf_tree.get_string("Dags.ArchiveDrive.remote"),
            "rclone_conf": conf_tree.get_string("Dags.ArchiveDrive.rcloneConf")
        }
    )


create_dag()
