from jinja2 import pass_context


def _quote(value):
    """Экранирует одинарные кавычки для SQL-литерала ClickHouse."""
    return "'" + str(value).replace("'", "''") + "'"


def _sql_in_list(values):
    """[a, b, c] -> "'a', 'b', 'c'" — для IN (...) по скалярной колонке."""
    return ", ".join(_quote(v) for v in values)


def _sql_array(values):
    """[a, b, c] -> "['a', 'b', 'c']" — литерал массива ClickHouse для hasAny()."""
    return "[" + ", ".join(_quote(v) for v in values) + "]"


@pass_context
def get_ch_vacancy_filters(context):
    get_filter_values = context.get('filter_values')
    get_filters       = context.get('get_filters')

    conditions = []

    published_from = context.get('from_dttm')
    published_to   = context.get('to_dttm')
    if published_from:
        conditions.append(f"published_at >= {_quote(published_from)}")
    if published_to:
        conditions.append(f"published_at <= {_quote(published_to)}")


    has_range_vals = get_filter_values('has_range')
    if has_range_vals:
        v = str(has_range_vals[0]).lower()
        if v in ('true', 'false'):
            conditions.append(f"has_range = {1 if v == 'true' else 0}")


    for f in get_filters('salary') or []:
        try:
            val = int(float(f.get('val')))
        except (ValueError, TypeError):
            continue
        op = f.get('op')
        if op == '>=':
            conditions.append(f"salary >= {val}")
        elif op == '<=':
            conditions.append(f"salary <= {val}")


    scalar_mappings = {
        'filter_platform':   'platform',
        'filter_employer':   'employer',
        'filter_currency':   'currency',
        'filter_experience': 'experience',
    }
    for filter_name, column in scalar_mappings.items():
        vals = get_filter_values(filter_name)
        if vals:
            conditions.append(f"{column} IN ({_sql_in_list(vals)})")


    array_mappings = {
        'filter_skill':      'skills',
        'filter_schedule':   'schedules',
        'filter_field':      'fields',
        'filter_grade':      'grades',
        'filter_employment': 'employments',
    }
    for filter_name, column in array_mappings.items():
        vals = get_filter_values(filter_name)
        if vals:
            conditions.append(f"hasAny({column}, {_sql_array(vals)})")


    #   locations: Array(Tuple(location, country))
    #   languages: Array(Tuple(language, level))
    tuple_mappings = {
        'filter_location':       ('locations', 'location'),
        'filter_country':        ('locations', 'country'),
        'filter_language':       ('languages', 'language'),
        'filter_language_level': ('languages', 'level'),
    }
    for filter_name, (array_col, tuple_field) in tuple_mappings.items():
        vals = get_filter_values(filter_name)
        if vals:
            in_list = _sql_in_list(vals)
            conditions.append(
                f"arrayExists(x -> x.{tuple_field} IN ({in_list}), {array_col})"
            )

    if not conditions:
        return ""
    return " AND " + " AND ".join(conditions)
