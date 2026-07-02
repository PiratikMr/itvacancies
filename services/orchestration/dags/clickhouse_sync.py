from airflow.hooks.base import BaseHook
from airflow.providers.postgres.hooks.postgres import PostgresHook

CH_TABLE  = "vacancies"
PG_SOURCE = "internal.mv_core_vacancy"

INSERT_BATCH_SIZE = 50_000

COLUMNS = [
    'vacancy_id', 'platform', 'employer', 'currency', 'experience',
    'experience_min_years', 'experience_max_years',
    'latitude', 'longitude', 'salary', 'has_range', 'published_at',
    'title', 'url', 'closed_at', 'is_active', 'skills', 'schedules', 'locations',
    'fields', 'grades', 'grades_sort', 'employments', 'languages',
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
            (item['language'], item['level'], item['level_sort'])
            for item in (row[LANGUAGES_IDX] or [])
        ]

    staging = f"{CH_TABLE}_new"
    client = _ch_client()
    client.command(f"CREATE TABLE IF NOT EXISTS {staging} AS {CH_TABLE}")
    client.command(f"TRUNCATE TABLE {staging}")

    for start in range(0, len(data), INSERT_BATCH_SIZE):
        client.insert(staging, data[start:start + INSERT_BATCH_SIZE], column_names=COLUMNS)

    client.command(f"EXCHANGE TABLES {CH_TABLE} AND {staging}")
    client.command(f"TRUNCATE TABLE {staging}")


def _latest_rub_per_unit(pg_hook, code):
    row = pg_hook.get_first(
        "SELECT rh.rate "
        "FROM dim_currency c "
        "JOIN dim_currency_rate_history rh ON rh.currency_id = c.currency_id "
        "WHERE c.currency = %s "
        "ORDER BY rh.update_date DESC LIMIT 1",
        parameters=(code,),
    )
    if not row or not row[0]:
        return 0.0
    return round(1.0 / float(row[0]), 2)


def write_meta(finished_at):
    pg_hook = PostgresHook(postgres_conn_id="POSTGRES_CONN")
    usd_rate = _latest_rub_per_unit(pg_hook, "USD")
    eur_rate = _latest_rub_per_unit(pg_hook, "EUR")

    client = _ch_client()
    client.command("CREATE TABLE IF NOT EXISTS meta_new AS meta")
    client.command("TRUNCATE TABLE meta_new")
    client.insert(
        "meta_new",
        [[finished_at, usd_rate, eur_rate]],
        column_names=["finished_at", "usd_rate", "eur_rate"],
    )
    client.command("EXCHANGE TABLES meta AND meta_new")
    client.command("TRUNCATE TABLE meta_new")
