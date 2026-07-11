import sys
from pathlib import Path

import pytest
from airflow.models import DagBag

DAGS_DIR = str(Path(__file__).resolve().parents[1] / "dags")
sys.path.insert(0, DAGS_DIR)

EXPECTED_DAG_IDS = {
    "Adzuna_ETL",
    "Backup_database",
    "Currency_Updater",
    "Delete_expiredData",
    "Finder_ETL",
    "GeekJob_ETL",
    "GetMatch_ETL",
    "HabrCareer_ETL",
    "HeadHunter_ETL",
    "Refresh_Materialized_views",
}


@pytest.fixture(scope="session")
def dag_bag():
    return DagBag(dag_folder=DAGS_DIR, include_examples=False)


def test_no_import_errors(dag_bag):
    assert dag_bag.import_errors == {}


def test_expected_dags_present(dag_bag):
    assert EXPECTED_DAG_IDS <= set(dag_bag.dag_ids)


def test_platform_dags_end_with_row_check(dag_bag):
    platform_dag_ids = [d for d in dag_bag.dag_ids if d.endswith("_ETL")]
    assert platform_dag_ids

    for dag_id in platform_dag_ids:
        dag = dag_bag.dags[dag_id]
        assert [t.task_id for t in dag.leaves] == ["check_min_rows"]
