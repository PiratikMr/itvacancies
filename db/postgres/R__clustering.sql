drop schema if exists clustering cascade;
create schema clustering;

create or replace view clustering.clusters as
with vacs as (
    select
        vacancy_id,
        published_at,
        coalesce(closed_at, '9999-01-01') as closed_at,
        coalesce(employer_id, -vacancy_id) as employer_id,
        title,
        coalesce(description_hash, '~') as desc_hash
    from fact_vacancy

), lay_1 as (
    select
        *,
        case
            when published_at - max(closed_at) over (
                    partition by title, employer_id
                    order by published_at, vacancy_id
                    rows between unbounded preceding and 1 preceding
                ) >= interval '60 day' then 1
            else 0
        end as flag
    from vacs

), lay_2 as (
    select
        *,
        sum(flag) over (
            partition by title, employer_id
            order by published_at, vacancy_id
        ) as island_no
    from lay_1

) select
    vacancy_id,
    min(vacancy_id) over (
        partition by employer_id, title, island_no, desc_hash
    ) as cluster_id
from lay_2;
