create table dim_currency_rate_history (
    currency_id                         bigint                      not null references dim_currency(currency_id),
    update_date                         timestamp without time zone not null,
    rate                                double precision            not null,

    primary key (currency_id, update_date)
);

create index idx_currency_rate_history_lookup
on dim_currency_rate_history (currency_id, update_date desc);


insert into dim_currency_rate_history (currency_id, update_date, rate)
select currency_id, now(), rate
from dim_currency;

-- TODO: Дропнуть колонку rate в dim_currency


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
