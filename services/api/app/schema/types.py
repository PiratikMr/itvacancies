from __future__ import annotations

from dataclasses import dataclass
from enum import Enum


class ColumnType(str, Enum):

    SCALAR = "scalar"
    """Скалярное строковое значение: LowCardinality(String), String.
    Фильтр: column IN ('a', 'b')."""

    NUMERIC = "numeric"
    """Числовое значение: UInt64, Float64 и т.д.
    Фильтр: column >= X, column <= Y (суффиксы _from/_to)."""

    DATETIME = "datetime"
    """Дата/время: DateTime.
    Фильтр: column >= '...', column <= '...' (суффиксы _from/_to)."""

    BOOLEAN = "boolean"
    """Булево (UInt8 в ClickHouse).
    Фильтр: column = 0/1."""

    ARRAY = "array"
    """Массив скаляров: Array(String), Array(LowCardinality(String)).
    Фильтр: hasAny(column, ['a', 'b'])."""

    TUPLE_ARRAY = "tuple_array"
    """Массив кортежей: Array(Tuple(field1, field2, ...)).
    Фильтруется по отдельным полям кортежа через arrayExists.
    Требует parent_column и tuple_field."""


@dataclass(frozen=True)
class Column:

    name: str
    """Имя колонки в БД (как в CREATE TABLE)."""

    type: ColumnType
    """Тип для определения логики фильтрации."""

    filterable: bool = True
    """Можно ли использовать эту колонку как фильтр."""

    parent_column: str | None = None
    """Имя колонки-массива, содержащей кортежи (locations, languages)."""

    tuple_field: str | None = None
    """Имя поля внутри кортежа (location, country, language, level)."""


@dataclass(frozen=True)
class Table:

    full_name: str
    """Полное имя таблицы: database.table."""

    columns: dict[str, Column]
    """Словарь: имя_фильтра → Column.
    Ключ может отличаться от Column.name для TUPLE_ARRAY-полей."""

    def get_filterable(self) -> dict[str, Column]:
        return {k: v for k, v in self.columns.items() if v.filterable}

    def get_by_type(self, col_type: ColumnType) -> dict[str, Column]:
        return {k: v for k, v in self.columns.items() if v.type == col_type}
