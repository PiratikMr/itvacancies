# itvacancies

ETL-платформа для агрегации и анализа IT-вакансий с шести площадок: Airflow + Spark + PostgreSQL + Superset + NLP-матчер.

🌐 **Live:** [itvacancies.tech](https://itvacancies.tech/)

---

## Что это

`itvacancies` ежедневно собирает вакансии из шести IT-площадок (HeadHunter, GeekJob, GetMatch, Habr Career, Finder, Adzuna), нормализует и дедуплицирует данные через NLP-матчер, складывает в PostgreSQL DWH со star-схемой и отдаёт интерактивные дашборды в Apache Superset.

Цель — дать соискателям и аналитикам актуальную картину рынка IT-труда: зарплаты по специализациям, востребованные навыки, тренды по городам и грейдам.

## Возможности

- Ежедневный сбор вакансий по расписанию через **Apache Airflow**.
- 6 источников: hh.ru, geekjob.ru, getmatch.ru, career.habr.com, finder.work, adzuna.com.
- Сырые данные складываются в **HDFS** в формате Parquet, обработанные — в **PostgreSQL DWH** (star-схема: dim/fact/bridge таблицы + materialized views).
- **NLP-матчер** для нормализации справочников (skills, employers, locations, fields и т.д.) — гибрид fuzzy-matching (rapidfuzz) и семантического поиска (sentence-transformers). Доступен как HTTP API.
- Дашборды в **Apache Superset** для аналитики, **Grafana + Prometheus** для мониторинга инфраструктуры.
- Production-развёртывание с **nginx** в роли reverse-proxy и SSL-терминации.

## Технологический стек

| Слой | Инструмент |
|---|---|
| Оркестрация | Apache Airflow 2.10 |
| ETL | Apache Spark 3.5 + Scala 2.12 |
| Сырое хранилище | Hadoop HDFS 3.3 |
| DWH | PostgreSQL 17 |
| NLP-матчер | Python 3.11 + Flask + sentence-transformers + rapidfuzz |
| Аналитика | Apache Superset |
| Мониторинг | Grafana 11 + Prometheus |
| Reverse-proxy (prod) | nginx |
| Контейнеризация | Docker Compose |

## Quick start

**Требования:** Docker, Docker Compose, ~10–12 GB свободной RAM. Для production-режима — SSL-сертификаты в `docker-compose/nginx/ssl/`.

1. Клонировать репо:
   ```bash
   git clone git@github.com:PiratikMr/itvacancies.git
   cd itvacancies
   ```

2. Создать `.env` файлы для каждого сервиса (см. раздел [Конфигурация](#конфигурация)):
   ```
   docker-compose/postgres/.env
   docker-compose/airflow/.env
   docker-compose/nlp/.env
   docker-compose/visualisation/.env
   docker-compose/nginx/.env       # только для prod
   ```

3. Положить API-ключи и креды площадок в `conf/secrets/` (HOCON-формат):
   ```
   conf/secrets/local_hh.conf            # OAuth-токен hh.ru
   conf/secrets/local_az.conf            # API-ключ Adzuna
   conf/secrets/local_exchangerate.conf  # API-ключ exchangerate.host
   conf/secrets/local_airflow.conf       # SMTP для алертов
   ```

4. Поднять dev-стек (без nginx):
   ```bash
   docker-compose up -d
   ```

   Или production-стек (с nginx + SSL):
   ```bash
   docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
   ```

После запуска (dev-режим, всё проброшено только на `127.0.0.1`):

| Сервис | URL |
|---|---|
| Airflow UI | http://localhost:11080 |
| Superset | http://localhost:16088 |
| Grafana | http://localhost:16000 |
| NLP-матчер | http://localhost:15000 |
| Spark Master UI | http://localhost:12080 |
| HDFS NameNode | http://localhost:13870 |
| Prometheus | http://localhost:15090 |
| PostgreSQL | localhost:14432 |

## Конфигурация

### `.env` файлы

Шаблонов в репозитории нет — нужно создать вручную. Минимально необходимые переменные по сервисам:

- **`docker-compose/postgres/.env`** — `PG_USER`, `PG_PASS`, `PG_DB`
- **`docker-compose/airflow/.env`** — `AIRFLOW_UID`, `AIRFLOW__CORE__FERNET_KEY`, `AIRFLOW__WEBSERVER__SECRET_KEY`, `AIRFLOW_CONN_POSTGRES_CONN`, `AIRFLOW_CONN_SPARK_CONN`, SMTP-креды, `_AIRFLOW_WWW_USER_USERNAME` / `_AIRFLOW_WWW_USER_PASSWORD`
- **`docker-compose/nlp/.env`** — `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASS`, `DB_NAME`, `WEB_HOST`, `WEB_PORT`
- **`docker-compose/visualisation/.env`** — креды Postgres для Superset, `SUPERSET_ADMIN_*`, `GF_SECURITY_ADMIN_*`, `MAPBOX_API_KEY`
- **`docker-compose/nginx/.env`** *(prod)* — `DOMAIN`, поддомены сервисов, апстримы

### HOCON-конфиги в `conf/`

- `conf/base/` — общие настройки ETL (Spark, сеть, нормализация, инфраструктура).
- `conf/dags/` — параметры расписаний Airflow.
- `conf/platforms/` — настройки парсеров для каждой площадки.
- `conf/secrets/` — API-токены и креды (gitignored, нужно создать самостоятельно).

## Лицензия

Released under the [MIT License](LICENSE).

