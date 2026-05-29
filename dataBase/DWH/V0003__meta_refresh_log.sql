create schema if not exists meta;

create table meta.refresh_log (
    id                  bigserial                       primary key,
    dag_id              text                            not null,
    dag_run_id          text,
    started_at          timestamp without time zone     not null,
    finished_at         timestamp without time zone,
    status              text                            not null
                                                        check (status in ('running', 'success', 'failure')),
    error               text
);

create index idx_meta_refresh_log_dag_finished
    on meta.refresh_log (dag_id, finished_at desc)
    where status = 'success';
