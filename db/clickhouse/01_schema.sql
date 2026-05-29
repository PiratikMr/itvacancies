CREATE DATABASE IF NOT EXISTS analytics;

CREATE TABLE IF NOT EXISTS analytics.vacancies (
    vacancy_id      UInt64,
    platform        LowCardinality(String),
    employer        LowCardinality(String),
    currency        LowCardinality(String),
    experience      LowCardinality(String),
    latitude        Float32,
    longitude       Float32,
    salary          Float64,
    has_range       UInt8,
    published_at    DateTime,
    title           String,
    url             String,
    closed_at       DateTime,
    skills      Array(String),
    schedules   Array(LowCardinality(String)),
    locations   Array(Tuple(
                    location LowCardinality(String),
                    country  LowCardinality(String)
                )),
    fields      Array(LowCardinality(String)),
    grades      Array(LowCardinality(String)),
    employments Array(LowCardinality(String)),
    languages   Array(Tuple(
                    language LowCardinality(String),
                    level    LowCardinality(String)
                ))
) ENGINE = MergeTree()
PARTITION BY toYYYYMM(published_at)
ORDER BY (published_at, platform);
