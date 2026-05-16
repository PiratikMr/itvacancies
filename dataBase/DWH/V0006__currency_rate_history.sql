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
