CREATE DATABASE IF NOT EXISTS analytics;

CREATE TABLE IF NOT EXISTS analytics.vacancies (
    vacancy_id      UInt64,
    platform        LowCardinality(String),
    employer        LowCardinality(String),
    currency        LowCardinality(String),
    experience      LowCardinality(String),
    experience_min_years UInt8,
    experience_max_years UInt8,
    latitude        Float32,
    longitude       Float32,
    salary          Float64,
    has_range       UInt8,
    published_at    DateTime,
    title           String,
    url             String,
    closed_at       DateTime,
    is_active       UInt8,
    skills      Array(String),
    schedules   Array(LowCardinality(String)),
    locations   Array(Tuple(
                    location LowCardinality(String),
                    country  LowCardinality(String)
                )),
    fields      Array(LowCardinality(String)),
    grades      Array(LowCardinality(String)),
    grades_sort Array(UInt8),
    employments Array(LowCardinality(String)),
    languages   Array(Tuple(
                    language   LowCardinality(String),
                    level      LowCardinality(String),
                    level_sort UInt8
                ))
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(published_at)
ORDER BY (published_at, platform);

CREATE TABLE IF NOT EXISTS analytics.meta (
    finished_at DateTime,
    usd_rate    Float64,
    eur_rate    Float64
) ENGINE = MergeTree()
ORDER BY tuple();
