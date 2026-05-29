CREATE TABLE IF NOT EXISTS analytics.refresh_log (
    finished_at DateTime
) ENGINE = MergeTree()
ORDER BY tuple();
