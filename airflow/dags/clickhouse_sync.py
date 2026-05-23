from airflow.hooks.base import BaseHook
from airflow.providers.postgres.hooks.postgres import PostgresHook

CH_TABLE  = "vacancies"
PG_SOURCE = "internal.mv_core_vacancy"

COLUMNS = [
    'vacancy_id', 'platform', 'employer', 'currency', 'experience',
    'latitude', 'longitude', 'salary', 'has_range', 'published_at',
    'title', 'url', 'closed_at', 'skills', 'schedules', 'locations',
    'fields', 'grades', 'employments', 'languages',
]

HAS_RANGE_IDX = COLUMNS.index('has_range')
LOCATIONS_IDX = COLUMNS.index('locations')
LANGUAGES_IDX = COLUMNS.index('languages')


def _ch_client():
    import clickhouse_connect
    conn = BaseHook.get_connection("CLICKHOUSE_CONN")
    return clickhouse_connect.get_client(
        host=conn.host,
        port=conn.port,
        username=conn.login,
        password=conn.password,
        database=conn.schema,
    )


def sync_mv_core_vacancy():
    pg_hook = PostgresHook(postgres_conn_id="POSTGRES_CONN")
    rows = pg_hook.get_records(f"SELECT * FROM {PG_SOURCE}")

    client = _ch_client()
    client.command(f"TRUNCATE TABLE {CH_TABLE}")

    if not rows:
        return

    data = [list(row) for row in rows]
    for row in data:
        row[HAS_RANGE_IDX] = int(row[HAS_RANGE_IDX])

        row[LOCATIONS_IDX] = [
            (item['location'], item['country'])
            for item in (row[LOCATIONS_IDX] or [])
        ]
        row[LANGUAGES_IDX] = [
            (item['language'], item['level'])
            for item in (row[LANGUAGES_IDX] or [])
        ]

    client.insert(CH_TABLE, data, column_names=COLUMNS)


def write_refresh_log(finished_at):
    client = _ch_client()
    client.command("TRUNCATE TABLE refresh_log")
    client.insert(
        "refresh_log",
        [[finished_at]],
        column_names=["finished_at"],
    )
