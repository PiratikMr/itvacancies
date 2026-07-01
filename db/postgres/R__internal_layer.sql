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
    -- join dim_currency dc on f.currency_id = dc.currency_id and dc.is_reference = true
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



create or replace view internal.mv_core_vacancy as
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
        jsonb_agg(distinct jsonb_build_object(
            'location', l.location,
            'country',  coalesce(c.country, '')
        )) as locations
    from bridge_vacancy_location b
    join dim_location l on b.location_id = l.location_id
    left join dim_country c on l.country_id = c.country_id
    -- where l.is_reference = true and c.is_reference = true
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
        vacancy_id,
        array_agg(grade order by sort_order)      as grades,
        array_agg(sort_order order by sort_order) as grades_sort
    from (
        select distinct
            b.vacancy_id,
            d.grade,
            d.sort_order
        from bridge_vacancy_grade b
        join dim_grade d on b.grade_id = d.grade_id
        where d.is_reference = true
    ) x
    group by vacancy_id
),
vacancy_employments as (
    select
        b.vacancy_id,
        array_agg(distinct d.employment) as employments
    from bridge_vacancy_employment b
    join dim_employment d on b.employment_id = d.employment_id
    -- where d.is_reference = true
    group by b.vacancy_id
),
vacancy_languages as (
    select
        b.vacancy_id,
        jsonb_agg(distinct jsonb_build_object(
            'language',    l.language,
            'level',       lvl.language_level,
            'level_sort',  lvl.sort_order
        )) as languages
    from bridge_vacancy_language b
    join dim_language l on b.language_id = l.language_id
    join dim_language_level lvl on b.language_level_id = lvl.language_level_id
    where l.is_reference = true and lvl.is_reference = true
    group by b.vacancy_id
)

select
    f.vacancy_id,
    p.platform                                           as platform,
    coalesce(e.employer, '')                             as employer,
    coalesce(c.currency, '')                             as currency,
    coalesce(exp.experience, 'Не указано')               as experience,
    coalesce(exp.min_years, 255)                         as experience_min_years,
    coalesce(exp.max_years, 255)                         as experience_max_years,
    coalesce(f.latitude, 200)                            as latitude,
    coalesce(f.longitude, 200)                           as longitude,
    coalesce(s.salary, 0)                                as salary,
    coalesce(s.has_range, false)                         as has_range,
    f.published_at,
    f.title,
    f.url,
    coalesce(f.closed_at, '1970-01-01 00:00:00'::timestamp) as closed_at,
    (f.closed_at is null)::int                              as is_active,

    coalesce(v_sk.skills,       ARRAY[]::text[])  as skills,
    coalesce(v_sch.schedules,   ARRAY[]::text[])  as schedules,
    coalesce(v_loc.locations,   '[]'::jsonb)      as locations,
    coalesce(v_fld.fields,      ARRAY[]::text[])  as fields,
    coalesce(v_grd.grades,      ARRAY[]::text[])  as grades,
    coalesce(v_grd.grades_sort, ARRAY[]::smallint[]) as grades_sort,
    coalesce(v_emp.employments, ARRAY[]::text[])  as employments,
    coalesce(v_lng.languages,   '[]'::jsonb)      as languages

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
  -- and (c.currency_id is null or c.is_reference = true)
  and (exp.experience_id is null or exp.is_reference = true);
