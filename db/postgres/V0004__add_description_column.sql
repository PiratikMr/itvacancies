alter table fact_vacancy
add description text;

alter table fact_vacancy
add description_hash text;

create index on fact_vacancy (description_hash) where description_hash is not null;
alter table fact_vacancy alter column description set compression lz4;