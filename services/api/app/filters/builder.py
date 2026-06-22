from __future__ import annotations

from typing import Any

from app.queries.common import ACTIVE, CLOSED
from app.schema.types import Column, ColumnType, Table


def _quote(value: Any) -> str:
    return "'" + str(value).replace("'", "''") + "'"


def _in_list(values: list[str]) -> str:
    return ", ".join(_quote(v) for v in values)


def _array_literal(values: list[str]) -> str:
    return "[" + ", ".join(_quote(v) for v in values) + "]"


class FilterBuilder:

    def __init__(self, table: Table) -> None:
        self._table = table
        self._filterable = table.get_filterable()

    def build(self, filters: dict[str, Any]) -> str:
        conditions: list[str] = []

        for key, value in filters.items():
            if value is None:
                continue
            if isinstance(value, list) and not value:
                continue

            condition = self._resolve(key, value)
            if condition:
                conditions.append(condition)

        if not conditions:
            return ""
        return "WHERE " + " AND ".join(conditions)


    def _resolve(self, key: str, value: Any) -> str | None:
        if key == "status":
            if value == "active":
                return ACTIVE
            if value == "closed":
                return CLOSED
            return None

        if key in self._filterable:
            return self._build_match(self._filterable[key], value)

        if key.endswith("_from"):
            col_name = key.removesuffix("_from")
            return self._build_range(col_name, ">=", value)

        if key.endswith("_to"):
            col_name = key.removesuffix("_to")
            return self._build_range(col_name, "<=", value)

        return None


    def _build_range(self, col_name: str, op: str, value: Any) -> str | None:
        col = self._filterable.get(col_name)
        if col is None:
            return None

        if col.type == ColumnType.DATETIME:
            return f"{col.name} {op} {_quote(value)}"

        if col.type == ColumnType.NUMERIC:
            return f"{col.name} {op} {value}"

        return None

    def _build_match(self, col: Column, value: Any) -> str | None:
        builder = self._MATCH_BUILDERS.get(col.type)
        if builder is None:
            return None
        return builder(self, col, value)

    def _match_boolean(self, col: Column, value: Any) -> str:
        return f"{col.name} = {1 if value else 0}"

    def _match_scalar(self, col: Column, value: Any) -> str:
        if isinstance(value, list):
            return f"{col.name} IN ({_in_list(value)})"
        return f"{col.name} = {_quote(value)}"

    def _match_array(self, col: Column, value: Any) -> str:
        if isinstance(value, list):
            return f"hasAny({col.name}, {_array_literal(value)})"
        return f"has({col.name}, {_quote(value)})"

    def _match_tuple_array(self, col: Column, value: Any) -> str | None:
        if not col.parent_column or not col.tuple_field:
            return None
        vals = value if isinstance(value, list) else [value]
        return (
            f"arrayExists("
            f"x -> x.{col.tuple_field} IN ({_in_list(vals)}), "
            f"{col.parent_column})"
        )

    _MATCH_BUILDERS: dict[ColumnType, Any] = {
        ColumnType.BOOLEAN: _match_boolean,
        ColumnType.SCALAR: _match_scalar,
        ColumnType.ARRAY: _match_array,
        ColumnType.TUPLE_ARRAY: _match_tuple_array,
    }
