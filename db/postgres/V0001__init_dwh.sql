create table dim_currency (
    currency_id                         bigserial               primary key,
    currency                            text                    not null,
    is_reference                        boolean                 not null default false,

    unique(currency)
);

create table dim_currency_rate_history (
    currency_id                         bigint                      not null references dim_currency(currency_id),
    update_date                         timestamp without time zone not null,
    rate                                double precision            not null,

    primary key (currency_id, update_date)
);

create index idx_currency_rate_history_lookup
on dim_currency_rate_history (currency_id, update_date desc);

create table mapping_dim_currency (
    mapping_id                          bigserial               primary key,
    currency_id                         bigint                  not null references dim_currency(currency_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,
    is_active                           boolean                 not null default true,
    origin                              text                    not null default 'unknown',
    link_score                          real,

    constraint uq_mapping_dim_currency_value unique (currency_id, mapped_value),
    constraint ch_mapping_dim_currency_origin
        check (origin in ('reference', 'etl', 'nlp_merge', 'manual', 'legacy', 'unknown'))
);

create unique index uq_canonical_per_currency
on mapping_dim_currency (currency_id)
where (is_canonical = true);


create table dim_employer (
    employer_id                         bigserial               primary key,
    employer                            text                    not null,
    is_reference                        boolean                 not null default false,

    unique(employer)
);

create table mapping_dim_employer (
    mapping_id                          bigserial               primary key,
    employer_id                         bigint                  not null references dim_employer(employer_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,
    is_active                           boolean                 not null default true,
    origin                              text                    not null default 'unknown',
    link_score                          real,

    constraint uq_mapping_dim_employer_value unique (employer_id, mapped_value),
    constraint ch_mapping_dim_employer_origin
        check (origin in ('reference', 'etl', 'nlp_merge', 'manual', 'legacy', 'unknown'))
);

create unique index uq_canonical_per_employer
on mapping_dim_employer (employer_id)
where (is_canonical = true);

create table dim_employment (
    employment_id                         bigserial               primary key,
    employment                            text                    not null,
    is_reference                          boolean                 not null default false,

    unique(employment)
);

create table mapping_dim_employment (
    mapping_id                          bigserial               primary key,
    employment_id                       bigint                  not null references dim_employment(employment_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,
    is_active                           boolean                 not null default true,
    origin                              text                    not null default 'unknown',
    link_score                          real,

    constraint uq_mapping_dim_employment_value unique (employment_id, mapped_value),
    constraint ch_mapping_dim_employment_origin
        check (origin in ('reference', 'etl', 'nlp_merge', 'manual', 'legacy', 'unknown'))
);

create unique index uq_canonical_per_employment
on mapping_dim_employment (employment_id)
where (is_canonical = true);

create table dim_experience (
    experience_id                       bigserial               primary key,
    experience                          text                    not null,
    is_reference                        boolean                 not null default false,
    min_years                           smallint,
    max_years                           smallint,

    unique(experience)
);

create table mapping_dim_experience (
    mapping_id                          bigserial               primary key,
    experience_id                       bigint                  not null references dim_experience(experience_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,
    is_active                           boolean                 not null default true,
    origin                              text                    not null default 'unknown',
    link_score                          real,

    constraint uq_mapping_dim_experience_value unique (experience_id, mapped_value),
    constraint ch_mapping_dim_experience_origin
        check (origin in ('reference', 'etl', 'nlp_merge', 'manual', 'legacy', 'unknown'))
);

create unique index uq_canonical_per_experience
on mapping_dim_experience (experience_id)
where (is_canonical = true);

create table dim_field (
    field_id                         bigserial               primary key,
    field                            text                    not null,
    is_reference                     boolean                 not null default false,

    unique(field)
);

create table mapping_dim_field (
    mapping_id                          bigserial               primary key,
    field_id                            bigint                  not null references dim_field(field_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,
    is_active                           boolean                 not null default true,
    origin                              text                    not null default 'unknown',
    link_score                          real,

    constraint uq_mapping_dim_field_value unique (field_id, mapped_value),
    constraint ch_mapping_dim_field_origin
        check (origin in ('reference', 'etl', 'nlp_merge', 'manual', 'legacy', 'unknown'))
);

create unique index uq_canonical_per_field
on mapping_dim_field (field_id)
where (is_canonical = true);

create table dim_grade (
    grade_id                         bigserial               primary key,
    grade                            text                    not null,
    is_reference                     boolean                 not null default false,
    sort_order                       smallint,

    unique(grade)
);

create table mapping_dim_grade (
    mapping_id                          bigserial               primary key,
    grade_id                            bigint                  not null references dim_grade(grade_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,
    is_active                           boolean                 not null default true,
    origin                              text                    not null default 'unknown',
    link_score                          real,

    constraint uq_mapping_dim_grade_value unique (grade_id, mapped_value),
    constraint ch_mapping_dim_grade_origin
        check (origin in ('reference', 'etl', 'nlp_merge', 'manual', 'legacy', 'unknown'))
);

create unique index uq_canonical_per_grade
on mapping_dim_grade (grade_id)
where (is_canonical = true);

create table dim_language_level (
    language_level_id                         bigserial               primary key,
    language_level                            text                    not null,
    is_reference                              boolean                 not null default false,
    sort_order                                smallint,

    unique(language_level)
);

create table mapping_dim_language_level (
    mapping_id                          bigserial               primary key,
    language_level_id                   bigint                  not null references dim_language_level(language_level_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,
    is_active                           boolean                 not null default true,
    origin                              text                    not null default 'unknown',
    link_score                          real,

    constraint uq_mapping_dim_language_level_value unique (language_level_id, mapped_value),
    constraint ch_mapping_dim_language_level_origin
        check (origin in ('reference', 'etl', 'nlp_merge', 'manual', 'legacy', 'unknown'))
);

create unique index uq_canonical_per_language_level
on mapping_dim_language_level (language_level_id)
where (is_canonical = true);

create table dim_language (
    language_id                         bigserial               primary key,
    language                            text                    not null,
    is_reference                        boolean                 not null default false,

    unique(language)
);

create table mapping_dim_language (
    mapping_id                          bigserial               primary key,
    language_id                         bigint                  not null references dim_language(language_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,
    is_active                           boolean                 not null default true,
    origin                              text                    not null default 'unknown',
    link_score                          real,

    constraint uq_mapping_dim_language_value unique (language_id, mapped_value),
    constraint ch_mapping_dim_language_origin
        check (origin in ('reference', 'etl', 'nlp_merge', 'manual', 'legacy', 'unknown'))
);

create unique index uq_canonical_per_language
on mapping_dim_language (language_id)
where (is_canonical = true);

create table dim_country (
    country_id                          bigserial               primary key,
    country                             text                    not null,
    iso                                 text,
    is_reference                        boolean                 not null default false,

    unique (country)
);
-- insert into dim_country (country_id, country) values (0, 'Неизвестно');

create table mapping_dim_country (
    mapping_id                          bigserial               primary key,
    country_id                          bigint                  not null references dim_country(country_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,
    is_active                           boolean                 not null default true,
    origin                              text                    not null default 'unknown',
    link_score                          real,

    constraint uq_mapping_dim_country_value unique (country_id, mapped_value),
    constraint ch_mapping_dim_country_origin
        check (origin in ('reference', 'etl', 'nlp_merge', 'manual', 'legacy', 'unknown'))
);

create unique index uq_canonical_per_country
on mapping_dim_country (country_id)
where (is_canonical = true);



create table dim_location (
    location_id                         bigserial               primary key,
    location                            text                    not null,
    country_id                          bigint                  not null default(0) references dim_country (country_id),
    is_reference                        boolean                 not null default false,

    unique (location, country_id)
);

create table mapping_dim_location (
    mapping_id                          bigserial               primary key,
    location_id                         bigint                  not null references dim_location(location_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,
    is_active                           boolean                 not null default true,
    origin                              text                    not null default 'unknown',
    link_score                          real,

    constraint uq_mapping_dim_location_value unique (location_id, mapped_value),
    constraint ch_mapping_dim_location_origin
        check (origin in ('reference', 'etl', 'nlp_merge', 'manual', 'legacy', 'unknown'))
);

create unique index uq_canonical_per_location
on mapping_dim_location (location_id)
where (is_canonical = true);

create table dim_platform (
    platform_id                         bigserial               primary key,
    platform                            text                    not null,
    is_reference                        boolean                 not null default false,

    unique(platform)
);

create table mapping_dim_platform (
    mapping_id                          bigserial               primary key,
    platform_id                         bigint                  not null references dim_platform(platform_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,
    is_active                           boolean                 not null default true,
    origin                              text                    not null default 'unknown',
    link_score                          real,

    constraint uq_mapping_dim_platform_value unique (platform_id, mapped_value),
    constraint ch_mapping_dim_platform_origin
        check (origin in ('reference', 'etl', 'nlp_merge', 'manual', 'legacy', 'unknown'))
);

create unique index uq_canonical_per_platform
on mapping_dim_platform (platform_id)
where (is_canonical = true);


create table dim_schedule (
    schedule_id                         bigserial               primary key,
    schedule                            text                    not null,
    is_reference                        boolean                 not null default false,

    unique(schedule)
);

create table mapping_dim_schedule (
    mapping_id                          bigserial               primary key,
    schedule_id                         bigint                  not null references dim_schedule(schedule_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,
    is_active                           boolean                 not null default true,
    origin                              text                    not null default 'unknown',
    link_score                          real,

    constraint uq_mapping_dim_schedule_value unique (schedule_id, mapped_value),
    constraint ch_mapping_dim_schedule_origin
        check (origin in ('reference', 'etl', 'nlp_merge', 'manual', 'legacy', 'unknown'))
);

create unique index uq_canonical_per_schedule
on mapping_dim_schedule (schedule_id)
where (is_canonical = true);


create table dim_skill_category (
    category_id                      bigserial               primary key,
    category_name                    text                    not null,
    is_reference                     boolean                 not null default false,

    unique(category_name)
);

create table dim_skill (
    skill_id                         bigserial               primary key,
    skill                            text                    not null,
    category_id                      bigint                  references dim_skill_category(category_id),
    is_reference                     boolean                 not null default false,

    unique(skill)
);

create table mapping_dim_skill (
    mapping_id                          bigserial               primary key,
    skill_id                            bigint                  not null references dim_skill(skill_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,
    is_active                           boolean                 not null default true,
    origin                              text                    not null default 'unknown',
    link_score                          real,

    constraint uq_mapping_dim_skill_value unique (skill_id, mapped_value),
    constraint ch_mapping_dim_skill_origin
        check (origin in ('reference', 'etl', 'nlp_merge', 'manual', 'legacy', 'unknown'))
);

create unique index uq_canonical_per_skill
on mapping_dim_skill (skill_id)
where (is_canonical = true);



create table fact_vacancy (
    vacancy_id                          bigserial                       primary key,

    external_id                         text                            not null,
    platform_id                         bigint                          not null references dim_platform (platform_id),
    employer_id                         bigint                          references dim_employer (employer_id),
    currency_id                         bigint                          references dim_currency (currency_id),
    experience_id                       bigint                          references dim_experience (experience_id),
    
    latitude                            double precision,
    longitude                           double precision,

    salary_from                         double precision,
    salary_to                           double precision,

    published_at                        timestamp without time zone     not null,
    title                               text                            not null,
    url                                 text                            not null,
    description                         text,
    description_hash                    text,

    created_at                          timestamp without time zone     not null default now(),
    updated_at                          timestamp without time zone     not null default now(),
    closed_at                           timestamp without time zone,

    unique(external_id, platform_id)
);

create index on fact_vacancy (description_hash) where description_hash is not null;
alter table fact_vacancy alter column description set compression lz4;


create table bridge_vacancy_skill (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    skill_id                            bigint                          not null references dim_skill (skill_id),

    primary key (vacancy_id, skill_id)
);

create table bridge_vacancy_schedule (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    schedule_id                         bigint                          not null references dim_schedule (schedule_id),

    primary key (vacancy_id, schedule_id)
);

create table bridge_vacancy_location (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    location_id                         bigint                          not null references dim_location (location_id),

    primary key (vacancy_id, location_id)
);

create table bridge_vacancy_field (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    field_id                            bigint                          not null references dim_field (field_id),

    primary key (vacancy_id, field_id)
);

create table bridge_vacancy_grade (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    grade_id                            bigint                          not null references dim_grade (grade_id),

    primary key (vacancy_id, grade_id)
);

create table bridge_vacancy_employment (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    employment_id                       bigint                          not null references dim_employment (employment_id),

    primary key (vacancy_id, employment_id)
);

create table bridge_vacancy_language (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    language_id                         bigint                          not null references dim_language (language_id),
    language_level_id                   bigint                          not null references dim_language_level (language_level_id),

    primary key (vacancy_id, language_id, language_level_id)
);

create table dup_members (
    vacancy_id          bigint              references fact_vacancy (vacancy_id) primary key,
    cluster_id          bigint              not null
);

create index on dup_members (cluster_id);


create table match_log_currency (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    mapping_id                          bigint                          not null references mapping_dim_currency (mapping_id),
    raw_value                           text,
    score                               real,

    unique nulls not distinct (vacancy_id, mapping_id, raw_value)
);

create index on match_log_currency (mapping_id);

create table match_log_employer (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    mapping_id                          bigint                          not null references mapping_dim_employer (mapping_id),
    raw_value                           text,
    score                               real,

    unique nulls not distinct (vacancy_id, mapping_id, raw_value)
);

create index on match_log_employer (mapping_id);

create table match_log_employment (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    mapping_id                          bigint                          not null references mapping_dim_employment (mapping_id),
    raw_value                           text,
    score                               real,

    unique nulls not distinct (vacancy_id, mapping_id, raw_value)
);

create index on match_log_employment (mapping_id);

create table match_log_experience (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    mapping_id                          bigint                          not null references mapping_dim_experience (mapping_id),
    raw_value                           text,
    score                               real,

    unique nulls not distinct (vacancy_id, mapping_id, raw_value)
);

create index on match_log_experience (mapping_id);

create table match_log_field (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    mapping_id                          bigint                          not null references mapping_dim_field (mapping_id),
    raw_value                           text,
    score                               real,

    unique nulls not distinct (vacancy_id, mapping_id, raw_value)
);

create index on match_log_field (mapping_id);

create table match_log_grade (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    mapping_id                          bigint                          not null references mapping_dim_grade (mapping_id),
    raw_value                           text,
    score                               real,

    unique nulls not distinct (vacancy_id, mapping_id, raw_value)
);

create index on match_log_grade (mapping_id);

create table match_log_language_level (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    mapping_id                          bigint                          not null references mapping_dim_language_level (mapping_id),
    raw_value                           text,
    score                               real,

    unique nulls not distinct (vacancy_id, mapping_id, raw_value)
);

create index on match_log_language_level (mapping_id);

create table match_log_language (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    mapping_id                          bigint                          not null references mapping_dim_language (mapping_id),
    raw_value                           text,
    score                               real,

    unique nulls not distinct (vacancy_id, mapping_id, raw_value)
);

create index on match_log_language (mapping_id);

create table match_log_country (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    mapping_id                          bigint                          not null references mapping_dim_country (mapping_id),
    raw_value                           text,
    score                               real,

    unique nulls not distinct (vacancy_id, mapping_id, raw_value)
);

create index on match_log_country (mapping_id);

create table match_log_location (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    mapping_id                          bigint                          not null references mapping_dim_location (mapping_id),
    raw_value                           text,
    score                               real,

    unique nulls not distinct (vacancy_id, mapping_id, raw_value)
);

create index on match_log_location (mapping_id);

create table match_log_platform (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    mapping_id                          bigint                          not null references mapping_dim_platform (mapping_id),
    raw_value                           text,
    score                               real,

    unique nulls not distinct (vacancy_id, mapping_id, raw_value)
);

create index on match_log_platform (mapping_id);

create table match_log_schedule (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    mapping_id                          bigint                          not null references mapping_dim_schedule (mapping_id),
    raw_value                           text,
    score                               real,

    unique nulls not distinct (vacancy_id, mapping_id, raw_value)
);

create index on match_log_schedule (mapping_id);

create table match_log_skill (
    vacancy_id                          bigint                          not null references fact_vacancy (vacancy_id),
    mapping_id                          bigint                          not null references mapping_dim_skill (mapping_id),
    raw_value                           text,
    score                               real,

    unique nulls not distinct (vacancy_id, mapping_id, raw_value)
);

create index on match_log_skill (mapping_id);
