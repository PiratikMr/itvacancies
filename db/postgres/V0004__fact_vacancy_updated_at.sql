alter table fact_vacancy
    add column updated_at                timestamp without time zone     not null default now();

update fact_vacancy
    set updated_at = coalesce(closed_at, published_at);
