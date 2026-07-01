alter table dim_experience
    add column min_years                        smallint,
    add column max_years                        smallint;

alter table dim_grade
    add column sort_order                       smallint;

alter table dim_language_level
    add column sort_order                       smallint;
