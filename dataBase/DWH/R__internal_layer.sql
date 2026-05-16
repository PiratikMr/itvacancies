drop schema if exists internal cascade;

create schema internal;

create or replace view internal.salary as
with calculated_salary as (
    select
        f.vacancy_id,
        case
            when f.salary_from is not null and f.salary_to is not null then
                (f.salary_from + f.salary_to) / (2 * cr.rate)
            when f.salary_from is not null then
                f.salary_from / cr.rate
            when f.salary_to is not null then
                f.salary_to / cr.rate
        end as salary,
        case
            when f.salary_from is not null and f.salary_to is not null then true
            else false
        end as has_range
    from fact_vacancy as f
    join lateral (
        select rate
        from dim_currency_rate_history
        where currency_id = f.currency_id
        order by abs(extract(epoch from (update_date - f.published_at)))
        limit 1
    ) cr on true
    where (f.salary_from is not null or f.salary_to is not null)
)
select * from calculated_salary
where salary between 1000 and 1500000;



create materialized view internal.mv_core_vacancy as
with vacancy_skills as (
    select
        b.vacancy_id,
        array_agg(distinct d.skill) as skills
    from bridge_vacancy_skill b
    join dim_skill d on b.skill_id = d.skill_id
    where d.is_reference = true
    group by b.vacancy_id
),
vacancy_schedules as (
    select
        b.vacancy_id,
        array_agg(distinct d.schedule) as schedules
    from bridge_vacancy_schedule b
    join dim_schedule d on b.schedule_id = d.schedule_id
    where d.is_reference = true
    group by b.vacancy_id
),
vacancy_locations as (
    select
        b.vacancy_id,
        array_agg(distinct l.location) as locations,
        array_agg(distinct c.country) as countries
    from bridge_vacancy_location b
    join dim_location l on b.location_id = l.location_id
    left join dim_country c on l.country_id = c.country_id
    where l.is_reference = true and c.is_reference = true
    group by b.vacancy_id
),
vacancy_fields as (
    select
        b.vacancy_id,
        array_agg(distinct d.field) as fields
    from bridge_vacancy_field b
    join dim_field d on b.field_id = d.field_id
    where d.is_reference = true
    group by b.vacancy_id
),
vacancy_grades as (
    select
        b.vacancy_id,
        array_agg(distinct d.grade) as grades
    from bridge_vacancy_grade b
    join dim_grade d on b.grade_id = d.grade_id
    where d.is_reference = true
    group by b.vacancy_id
),
vacancy_employments as (
    select
        b.vacancy_id,
        array_agg(distinct d.employment) as employments
    from bridge_vacancy_employment b
    join dim_employment d on b.employment_id = d.employment_id
    where d.is_reference = true
    group by b.vacancy_id
),
vacancy_languages as (
    select
        b.vacancy_id,
        array_agg(distinct l.language) as languages,
        array_agg(distinct lvl.language_level) as language_levels
    from bridge_vacancy_language b
    join dim_language l on b.language_id = l.language_id
    join dim_language_level lvl on b.language_level_id = lvl.language_level_id
    where l.is_reference = true and lvl.is_reference = true
    group by b.vacancy_id
)

select
    f.vacancy_id,
    p.platform,
    e.employer,
    c.currency,
    exp.experience,
    f.latitude,
    f.longitude,
    s.salary,
    coalesce(s.has_range, false) as has_range,
    f.published_at,
    f.title,
    f.url,
    f.closed_at,

    coalesce(v_sk.skills, '{}') as skills,
    coalesce(v_sch.schedules, '{}') as schedules,
    coalesce(v_loc.locations, '{}') as locations,
    coalesce(v_loc.countries, '{}') as countries,
    coalesce(v_fld.fields, '{}') as fields,
    coalesce(v_grd.grades, '{}') as grades,
    coalesce(v_emp.employments, '{}') as employments,
    coalesce(v_lng.languages, '{}') as languages,
    coalesce(v_lng.language_levels, '{}') as language_levels

from fact_vacancy f
left join dim_platform p on f.platform_id = p.platform_id
left join dim_employer e on f.employer_id = e.employer_id
left join dim_currency c on f.currency_id = c.currency_id
left join dim_experience exp on f.experience_id = exp.experience_id
left join internal.salary s on f.vacancy_id = s.vacancy_id

left join vacancy_skills v_sk on f.vacancy_id = v_sk.vacancy_id
left join vacancy_schedules v_sch on f.vacancy_id = v_sch.vacancy_id
left join vacancy_locations v_loc on f.vacancy_id = v_loc.vacancy_id
left join vacancy_fields v_fld on f.vacancy_id = v_fld.vacancy_id
left join vacancy_grades v_grd on f.vacancy_id = v_grd.vacancy_id
left join vacancy_employments v_emp on f.vacancy_id = v_emp.vacancy_id
left join vacancy_languages v_lng on f.vacancy_id = v_lng.vacancy_id
where (p.platform_id is null or p.is_reference = true)
--  and (e.employer_id is null or e.is_reference = true)
--  and (c.currency_id is null or c.is_reference = true)
  and (exp.experience_id is null or exp.is_reference = true);

create index if not exists idx_mv_core_vacancy_skills          on internal.mv_core_vacancy using gin (skills);
create index if not exists idx_mv_core_vacancy_schedules       on internal.mv_core_vacancy using gin (schedules);
create index if not exists idx_mv_core_vacancy_locations       on internal.mv_core_vacancy using gin (locations);
create index if not exists idx_mv_core_vacancy_countries       on internal.mv_core_vacancy using gin (countries);
create index if not exists idx_mv_core_vacancy_grades          on internal.mv_core_vacancy using gin (grades);
create index if not exists idx_mv_core_vacancy_employments     on internal.mv_core_vacancy using gin (employments);
create index if not exists idx_mv_core_vacancy_languages       on internal.mv_core_vacancy using gin (languages);
create index if not exists idx_mv_core_vacancy_fields          on internal.mv_core_vacancy using gin (fields);
create index if not exists idx_mv_core_vacancy_language_levels on internal.mv_core_vacancy using gin (language_levels);

create index        if not exists idx_mv_core_vacancy_employer     on internal.mv_core_vacancy (employer);
create index        if not exists idx_mv_core_vacancy_experience   on internal.mv_core_vacancy (experience);
create index        if not exists idx_mv_core_vacancy_published_at on internal.mv_core_vacancy (published_at);
create index        if not exists idx_mv_core_vacancy_platform     on internal.mv_core_vacancy (platform);
create index        if not exists idx_mv_core_vacancy_currency     on internal.mv_core_vacancy (currency);
create index        if not exists idx_mv_core_vacancy_salary       on internal.mv_core_vacancy (salary);
create index        if not exists idx_mv_core_vacancy_has_range    on internal.mv_core_vacancy (has_range);
create unique index if not exists idx_mv_core_vacancy_id           on internal.mv_core_vacancy (vacancy_id);


CREATE OR REPLACE FUNCTION internal.get_filtered_vacancies(
    p_from_dttm        TIMESTAMP DEFAULT NULL,
    p_to_dttm          TIMESTAMP DEFAULT NULL,
    p_salary_min       INTEGER   DEFAULT NULL,
    p_salary_max       INTEGER   DEFAULT NULL,
    p_has_range        BOOLEAN   DEFAULT NULL,
    p_platforms        TEXT[]    DEFAULT NULL,
    p_employers        TEXT[]    DEFAULT NULL,
    p_currencies       TEXT[]    DEFAULT NULL,
    p_experiences      TEXT[]    DEFAULT NULL,
    p_skills           TEXT[]    DEFAULT NULL,
    p_schedules        TEXT[]    DEFAULT NULL,
    p_locations        TEXT[]    DEFAULT NULL,
    p_countries        TEXT[]    DEFAULT NULL,
    p_fields           TEXT[]    DEFAULT NULL,
    p_grades           TEXT[]    DEFAULT NULL,
    p_employments      TEXT[]    DEFAULT NULL,
    p_languages        TEXT[]    DEFAULT NULL,
    p_language_levels  TEXT[]    DEFAULT NULL
)
RETURNS TABLE (vacancy_id BIGINT)
LANGUAGE sql
STABLE PARALLEL SAFE
AS $$
    SELECT v.vacancy_id
    FROM internal.mv_core_vacancy v
    WHERE (p_from_dttm       IS NULL OR v.published_at    >= p_from_dttm)
      AND (p_to_dttm         IS NULL OR v.published_at    <= p_to_dttm)
      AND (p_salary_min      IS NULL OR v.salary          >= p_salary_min)
      AND (p_salary_max      IS NULL OR v.salary          <= p_salary_max)
      AND (p_has_range       IS NULL OR v.has_range        = p_has_range)
      AND (p_platforms       IS NULL OR v.platform        = ANY(p_platforms))
      AND (p_employers       IS NULL OR v.employer        = ANY(p_employers))
      AND (p_currencies      IS NULL OR v.currency        = ANY(p_currencies))
      AND (p_experiences     IS NULL OR v.experience      = ANY(p_experiences))
      AND (p_skills          IS NULL OR v.skills          && p_skills)
      AND (p_schedules       IS NULL OR v.schedules       && p_schedules)
      AND (p_locations       IS NULL OR v.locations       && p_locations)
      AND (p_countries       IS NULL OR v.countries       && p_countries)
      AND (p_fields          IS NULL OR v.fields          && p_fields)
      AND (p_grades          IS NULL OR v.grades          && p_grades)
      AND (p_employments     IS NULL OR v.employments     && p_employments)
      AND (p_languages       IS NULL OR v.languages       && p_languages)
      AND (p_language_levels IS NULL OR v.language_levels && p_language_levels);
$$;
