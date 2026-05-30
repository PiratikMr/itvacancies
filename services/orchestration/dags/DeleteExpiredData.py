from utils import get_config
from config_ETL import DAGS_CONFIG_PATH, DEFAULT_ARGS
from airflow.decorators import dag
from airflow.operators.bash import BashOperator

conf_tree = get_config(DAGS_CONFIG_PATH)

hdfs_path = conf_tree.get_string("FileSystem.path")
raw_data_expires_in = conf_tree.get_string("Dags.DeleteData.rawData")

def get_delete_data_command() -> str:
    return f"""
        execution_date=$(date -d "{{{{ ds }}}}" +%Y-%m-%d)
        target=$(date -d "$execution_date - {raw_data_expires_in} days" +%s)

        process_path() {{
            local search_path="$1"
            local depth="$2"
            
            if (( depth > 2 )); then return; fi
            
            local root_path="$search_path"
            hdfs ls "$search_path" | while read -r path; do
                if [[ -z "$path" ]]; then
                    continue
                elif [[ $path == /* ]]; then
                    root_path="${{path::-1}}"
                elif [[ $path == ????-??-??* ]]; then
                    local date_parts="${{path:0:10}}"
                    local folder_ts
                    folder_ts=$(date -d "$date_parts" +%s 2>/dev/null) || continue

                    if (( folder_ts < target )); then
                        hdfs rm -r "$root_path$path"
                        echo "deleted $root_path$path"
                    fi
                else
                    process_path "$root_path$path/" $((depth + 1))
                fi
            done
        }}

        process_path "/{hdfs_path}/*/Vacancies/" 0
    """

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
        bash_command=get_delete_data_command()
    )

create_dag()