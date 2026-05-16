drop schema if exists marts cascade;

create schema marts;

CREATE MATERIALIZED VIEW marts.dict_platform AS
SELECT platform AS filter_platform, COUNT(*) AS total
FROM internal.mv_core_vacancy
WHERE platform IS NOT NULL
GROUP BY platform;

CREATE MATERIALIZED VIEW marts.dict_employer AS
SELECT employer AS filter_employer, COUNT(*) AS total
FROM internal.mv_core_vacancy
WHERE employer IS NOT NULL
GROUP BY employer;

CREATE MATERIALIZED VIEW marts.dict_currency AS
SELECT currency AS filter_currency, COUNT(*) AS total
FROM internal.mv_core_vacancy
WHERE currency IS NOT NULL
GROUP BY currency;

CREATE MATERIALIZED VIEW marts.dict_experience AS
SELECT experience AS filter_experience, COUNT(*) AS total
FROM internal.mv_core_vacancy
WHERE experience IS NOT NULL
GROUP BY experience;

CREATE MATERIALIZED VIEW marts.dict_skills AS
SELECT unnest(skills) AS filter_skill, COUNT(*) AS total
FROM internal.mv_core_vacancy
GROUP BY filter_skill;

CREATE MATERIALIZED VIEW marts.dict_schedules AS
SELECT unnest(schedules) AS filter_schedule, COUNT(*) AS total
FROM internal.mv_core_vacancy
GROUP BY filter_schedule;

CREATE MATERIALIZED VIEW marts.dict_locations AS
SELECT unnest(locations) AS filter_location, COUNT(*) AS total
FROM internal.mv_core_vacancy
GROUP BY filter_location;

CREATE MATERIALIZED VIEW marts.dict_countries AS
SELECT unnest(countries) AS filter_country, COUNT(*) AS total
FROM internal.mv_core_vacancy
GROUP BY filter_country;

CREATE MATERIALIZED VIEW marts.dict_fields AS
SELECT unnest(fields) AS filter_field, COUNT(*) AS total
FROM internal.mv_core_vacancy
GROUP BY filter_field;

CREATE MATERIALIZED VIEW marts.dict_grades AS
SELECT unnest(grades) AS filter_grade, COUNT(*) AS total
FROM internal.mv_core_vacancy
GROUP BY filter_grade;

CREATE MATERIALIZED VIEW marts.dict_employments AS
SELECT unnest(employments) AS filter_employment, COUNT(*) AS total
FROM internal.mv_core_vacancy
GROUP BY filter_employment;

CREATE MATERIALIZED VIEW marts.dict_languages AS
SELECT unnest(languages) AS filter_language, COUNT(*) AS total
FROM internal.mv_core_vacancy
GROUP BY filter_language;

CREATE MATERIALIZED VIEW marts.dict_language_levels AS
SELECT unnest(language_levels) AS filter_language_level, COUNT(*) AS total
FROM internal.mv_core_vacancy
GROUP BY filter_language_level;

CREATE MATERIALIZED VIEW marts.mv_vacancy_field_salary AS
SELECT
    v.vacancy_id,
    d.field,
    v.salary
FROM internal.mv_core_vacancy v
JOIN bridge_vacancy_field b ON v.vacancy_id = b.vacancy_id
JOIN dim_field d ON b.field_id = d.field_id
WHERE v.salary IS NOT NULL AND d.is_reference = true;

CREATE INDEX if not exists idx_marts_mv_vacancy_field_salary_vacancy_id ON marts.mv_vacancy_field_salary (vacancy_id);



CREATE MATERIALIZED VIEW marts.mv_vacancy_grade_salary AS
SELECT
    v.vacancy_id,
    v.salary,
    v.published_at,
    g.grade
FROM internal.mv_core_vacancy v
JOIN bridge_vacancy_grade b ON v.vacancy_id = b.vacancy_id
JOIN dim_grade g ON b.grade_id = g.grade_id
WHERE g.is_reference = true;

CREATE INDEX if not exists idx_marts_mv_vacancy_grade_salary_vacancy_id ON marts.mv_vacancy_grade_salary (vacancy_id);



CREATE MATERIALIZED VIEW marts.mv_vacancy_grade_schedule AS
SELECT
    bg.vacancy_id,
    g.grade,
    s.schedule
FROM bridge_vacancy_grade bg
JOIN dim_grade g ON bg.grade_id = g.grade_id
JOIN bridge_vacancy_schedule bs ON bg.vacancy_id = bs.vacancy_id
JOIN dim_schedule s ON bs.schedule_id = s.schedule_id
WHERE g.grade IS NOT NULL AND s.schedule IS NOT NULL AND g.is_reference = true AND s.is_reference = true;

CREATE INDEX if not exists idx_marts_mv_vacancy_grade_schedule_vacancy_id ON marts.mv_vacancy_grade_schedule (vacancy_id);



CREATE MATERIALIZED VIEW marts.mv_vacancy_experience AS
SELECT vacancy_id, experience
FROM internal.mv_core_vacancy
WHERE experience IS NOT NULL;

CREATE UNIQUE INDEX if not exists idx_marts_mv_vacancy_experience_vacancy_id ON marts.mv_vacancy_experience (vacancy_id);



CREATE MATERIALIZED VIEW marts.mv_vacancy_grade_duration AS
SELECT
    v.vacancy_id,
    g.grade,
    (v.closed_at::date - v.published_at::date) AS days_open
FROM internal.mv_core_vacancy v
JOIN bridge_vacancy_grade b ON v.vacancy_id = b.vacancy_id
JOIN dim_grade g ON b.grade_id = g.grade_id
WHERE g.grade IS NOT NULL
  AND v.closed_at IS NOT NULL
  AND v.published_at IS NOT NULL
  AND g.is_reference = true;

CREATE INDEX if not exists idx_marts_mv_vacancy_grade_duration_vacancy_id ON marts.mv_vacancy_grade_duration (vacancy_id);



CREATE MATERIALIZED VIEW marts.mv_vacancy_skill_salary AS
SELECT
    v.vacancy_id,
    s.skill,
    v.salary
FROM internal.mv_core_vacancy v
JOIN bridge_vacancy_skill bs ON bs.vacancy_id = v.vacancy_id
JOIN dim_skill s ON bs.skill_id = s.skill_id
WHERE v.salary IS NOT NULL AND s.skill IS NOT NULL AND s.is_reference = true;

CREATE INDEX if not exists idx_marts_mv_vacancy_skill_salary_vacancy_id ON marts.mv_vacancy_skill_salary (vacancy_id);



CREATE MATERIALIZED VIEW marts.mv_vacancy_field_skill AS
SELECT
    bf.vacancy_id,
    f.field,
    s.skill
FROM bridge_vacancy_field bf
JOIN dim_field f ON bf.field_id = f.field_id
JOIN bridge_vacancy_skill bs ON bf.vacancy_id = bs.vacancy_id
JOIN dim_skill s ON bs.skill_id = s.skill_id
WHERE f.field IS NOT NULL AND s.skill IS NOT NULL AND f.is_reference = true AND s.is_reference = true;

CREATE INDEX if not exists idx_marts_mv_vacancy_field_skill_vacancy_id ON marts.mv_vacancy_field_skill (vacancy_id);



CREATE MATERIALIZED VIEW marts.mv_vacancy_schedule AS
SELECT
    bs.vacancy_id,
    s.schedule
FROM bridge_vacancy_schedule bs
JOIN dim_schedule s ON bs.schedule_id = s.schedule_id
WHERE s.schedule IS NOT NULL AND s.is_reference = true;

CREATE INDEX if not exists idx_marts_mv_vacancy_schedule_vacancy_id ON marts.mv_vacancy_schedule (vacancy_id);



CREATE MATERIALIZED VIEW marts.mv_vacancy_location_salary AS
SELECT
    v.vacancy_id,
    l.location,
    c.country,
--  c.iso,
    v.salary
FROM internal.mv_core_vacancy v
JOIN bridge_vacancy_location bl ON v.vacancy_id = bl.vacancy_id
JOIN dim_location l ON bl.location_id = l.location_id
LEFT JOIN dim_country c ON l.country_id = c.country_id
WHERE v.salary IS NOT NULL
  AND l.location IS NOT NULL
--   AND l.is_reference = true
  AND c.is_reference = true;

CREATE INDEX if not exists idx_marts_mv_vacancy_location_salary_vacancy_id ON marts.mv_vacancy_location_salary (vacancy_id);



CREATE MATERIALIZED VIEW marts.mv_vacancy_full_info AS
SELECT
    v.published_at,
    v.vacancy_id,
    v.platform,
    COALESCE(v.employer, 'Не указано') AS employer,
    v.title,
    COALESCE(g.grades, 'Не указано') AS grade,
    COALESCE(v.experience, 'Не указано') AS experience,
    COALESCE(s.schedules, 'Не указано') AS schedule,
    v.salary,
    v.url
FROM internal.mv_core_vacancy v
LEFT JOIN (
    SELECT bg.vacancy_id, STRING_AGG(g.grade, ', ' ORDER BY g.grade) AS grades
    FROM bridge_vacancy_grade bg
    JOIN dim_grade g ON bg.grade_id = g.grade_id
    WHERE g.grade IS NOT NULL AND g.is_reference = true
    GROUP BY bg.vacancy_id
) g ON v.vacancy_id = g.vacancy_id
LEFT JOIN (
    SELECT bs.vacancy_id, STRING_AGG(s.schedule, ', ' ORDER BY s.schedule) AS schedules
    FROM bridge_vacancy_schedule bs
    JOIN dim_schedule s ON bs.schedule_id = s.schedule_id
    WHERE s.schedule IS NOT NULL AND s.is_reference = true
    GROUP BY bs.vacancy_id
) s ON v.vacancy_id = s.vacancy_id
WHERE closed_at IS NULL;

CREATE INDEX if not exists idx_marts_mv_vacancy_full_info_vacancy_id ON marts.mv_vacancy_full_info (vacancy_id);



CREATE MATERIALIZED VIEW marts.mv_vacancy_english_level AS
SELECT
    v.vacancy_id,
    COALESCE(eng.language_level, 'Не требуется') AS language_level,
    v.salary
FROM internal.mv_core_vacancy v
LEFT JOIN (
    SELECT
        bvl.vacancy_id,
        dll.language_level
    FROM bridge_vacancy_language bvl
    JOIN dim_language dl ON bvl.language_id = dl.language_id
    JOIN dim_language_level dll ON bvl.language_level_id = dll.language_level_id
    WHERE dl.language = 'Английский' AND dl.is_reference = true AND dll.is_reference = true
) eng ON v.vacancy_id = eng.vacancy_id
WHERE v.salary IS NOT NULL;

CREATE INDEX if not exists idx_marts_mv_vacancy_english_level_vacancy_id ON marts.mv_vacancy_english_level (vacancy_id);
