from app.schema.types import Column, ColumnType, Table

VACANCIES = Table(
    full_name="analytics.vacancies",
    columns={
        "vacancy_id":   Column("vacancy_id",   ColumnType.NUMERIC,  filterable=False),
        "title":        Column("title",        ColumnType.SCALAR,   filterable=False),
        "url":          Column("url",          ColumnType.SCALAR,   filterable=False),
        "latitude":     Column("latitude",     ColumnType.NUMERIC,  filterable=False),
        "longitude":    Column("longitude",    ColumnType.NUMERIC,  filterable=False),
        "closed_at":    Column("closed_at",    ColumnType.DATETIME, filterable=False),
        "is_active":    Column("is_active",    ColumnType.NUMERIC,  filterable=False),

        "published_at": Column("published_at", ColumnType.DATETIME),

        "salary":       Column("salary",       ColumnType.NUMERIC),

        "has_range":    Column("has_range",    ColumnType.BOOLEAN),

        "platform":     Column("platform",     ColumnType.SCALAR),
        "employer":     Column("employer",     ColumnType.SCALAR),
        "currency":     Column("currency",     ColumnType.SCALAR),
        "experience":   Column("experience",   ColumnType.SCALAR),

        "skills":       Column("skills",       ColumnType.ARRAY),
        "schedules":    Column("schedules",    ColumnType.ARRAY),
        "fields":       Column("fields",       ColumnType.ARRAY),
        "grades":       Column("grades",       ColumnType.ARRAY),
        "employments":  Column("employments",  ColumnType.ARRAY),

        "location": Column(
            "location", ColumnType.TUPLE_ARRAY,
            parent_column="locations", tuple_field="location",
        ),
        "country": Column(
            "country", ColumnType.TUPLE_ARRAY,
            parent_column="locations", tuple_field="country",
        ),
        "language": Column(
            "language", ColumnType.TUPLE_ARRAY,
            parent_column="languages", tuple_field="language",
        ),
        "language_level": Column(
            "language_level", ColumnType.TUPLE_ARRAY,
            parent_column="languages", tuple_field="level",
        ),
    },
)
