create table dup_members (
    vacancy_id          bigint              references fact_vacancy (vacancy_id) primary key,
    cluster_id          bigint              not null
);

create index on dup_members (cluster_id);
