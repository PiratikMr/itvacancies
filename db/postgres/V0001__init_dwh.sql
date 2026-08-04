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
    currency_id                         bigint                  not null references dim_currency(currency_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,

    primary key (currency_id, mapped_value)
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
    employer_id                         bigint                  not null references dim_employer(employer_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,

    primary key (employer_id, mapped_value)
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
    employment_id                         bigint                  not null references dim_employment(employment_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,

    primary key (employment_id, mapped_value)
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
    experience_id                       bigint                  not null references dim_experience(experience_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,

    primary key (experience_id, mapped_value)
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
    field_id                         bigint                  not null references dim_field(field_id),
    mapped_value                     text                    not null,
    is_canonical                     boolean                 not null,

    primary key (field_id, mapped_value)
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
    grade_id                         bigint                  not null references dim_grade(grade_id),
    mapped_value                     text                    not null,
    is_canonical                     boolean                 not null,

    primary key (grade_id, mapped_value)
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
    language_level_id                         bigint                  not null references dim_language_level(language_level_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,

    primary key (language_level_id, mapped_value)
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
    language_id                         bigint                  not null references dim_language(language_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,

    primary key (language_id, mapped_value)
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
    country_id                          bigint                  not null references dim_country(country_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,

    primary key (country_id, mapped_value)
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
    location_id                         bigint                  not null references dim_location(location_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,

    primary key (location_id, mapped_value)
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
    platform_id                         bigint                  not null references dim_platform(platform_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,

    primary key (platform_id, mapped_value)
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
    schedule_id                         bigint                  not null references dim_schedule(schedule_id),
    mapped_value                        text                    not null,
    is_canonical                        boolean                 not null,

    primary key (schedule_id, mapped_value)
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
    skill_id                         bigint                  not null references dim_skill(skill_id),
    mapped_value                     text                    not null,
    is_canonical                     boolean                 not null,

    primary key (skill_id, mapped_value)
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

    created_at                          timestamp without time zone     not null default now(),
    updated_at                          timestamp without time zone     not null default now(),
    closed_at                           timestamp without time zone,

    unique(external_id, platform_id)
);


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