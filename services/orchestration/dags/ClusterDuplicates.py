from utils import get_config
from config_ETL import DAGS_CONFIG_PATH, DEFAULT_ARGS
from airflow.decorators import dag
from airflow.providers.common.sql.operators.sql import SQLExecuteQueryOperator

conf_tree = get_config(DAGS_CONFIG_PATH)

REFRESH_CLUSTERS_SQL = """
    truncate dup_members;

    insert into dup_members (vacancy_id, cluster_id)
    select vacancy_id, cluster_id
    from clustering.clusters;
"""


@dag(
    dag_id="Cluster_duplicates",
    default_args=DEFAULT_ARGS,
    tags=["postgresql", "clustering"],
    schedule=conf_tree.get_string("Dags.Clustering.schedule") or None,
    catchup=False
)
def create_dag():
    SQLExecuteQueryOperator(
        task_id="refresh_clusters",
        conn_id="POSTGRES_CONN",
        sql=REFRESH_CLUSTERS_SQL
    )


create_dag()
