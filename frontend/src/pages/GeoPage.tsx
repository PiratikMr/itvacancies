import { useMemo, useState } from "react";
import type { GeoResponse } from "../api/types";
import { api } from "../api/client";
import { usePageData } from "../lib/usePageData";
import type { Filters } from "../state/filters";
import { Notice, Track, SortTh, NO_SORT, type Sort } from "../components/shared";
import { Card, KpiGrid, StatCard, TableCard } from "../components/ui";
import { GeoMap } from "../charts/GeoMap";
import { tip } from "../lib/tooltip";
import { nfmt, salary, salaryFull } from "../lib/format";
import { tableTh as th, tableTd as td, capsLabel } from "../lib/tokens";

export function GeoPage({ filters }: { filters: Filters }) {
  const { data, loading, error } = usePageData(() => api.geo(filters), [filters]);
  if (error) return <Notice text={`Ошибка загрузки: ${error}`} />;
  if (!data) return <Notice text="Загрузка…" />;
  return <GeoBody data={data} loading={loading} />;
}

function GeoBody({ data, loading }: { data: GeoResponse; loading: boolean }) {
  const k = data.kpis;
  const dark = document.body.classList.contains("dark");
  const maxV = Math.max(1, ...data.countries.map((c) => c.count));

  const [sort, setSort] = useState<Sort>(NO_SORT);
  const countries = useMemo(() => {
    if (!sort.key || !sort.dir) return data.countries;
    const key = sort.key as keyof (typeof data.countries)[number];
    const arr = [...data.countries];
    arr.sort((a, b) => {
      const av = a[key] as string | number, bv = b[key] as string | number;
      if (typeof av === "string") return sort.dir === "asc" ? av.localeCompare(bv as string) : (bv as string).localeCompare(av);
      return sort.dir === "asc" ? (av as number) - (bv as number) : (bv as number) - (av as number);
    });
    return arr;
  }, [data.countries, sort]);

  return (
    <div style={{ opacity: loading ? 0.55 : 1, transition: "opacity .15s" }}>
      <KpiGrid>
        <StatCard label="Стран" value={nfmt(k.countries)} />
        <StatCard label="Городов" value={nfmt(k.cities)} />
        <StatCard label="Вне России" value={`${k.outside_russia_pct}%`} />
        <StatCard label="Медиана вне РФ" value={salaryFull(k.median_outside)} accent />
      </KpiGrid>

      <Card title="Карта вакансий" style={{ marginBottom: 12 }}>
        <GeoMap points={data.map} dark={dark} />
      </Card>

      <TableCard title="Вакансии по странам">
        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 14 }}>
            <thead>
              <tr style={{ background: "var(--surface-2)", borderBottom: "1px solid var(--track)" }}>
                <th style={{ ...th, ...capsLabel, paddingLeft: 22, textAlign: "left" }}>#</th>
                <th style={th}><SortTh label="Страна" col="name" sort={sort} onSort={setSort} /></th>
                <th style={{ ...th, width: "32%" }}><SortTh label="Вакансий" col="count" sort={sort} onSort={setSort} /></th>
                <th style={th}><SortTh label="Городов" col="cities" sort={sort} onSort={setSort} align="right" /></th>
                <th style={th}><SortTh label="Медиана ЗП" col="median" sort={sort} onSort={setSort} align="right" /></th>
                <th style={{ ...th, paddingRight: 22 }}><SortTh label="Удалённо" col="remote_pct" sort={sort} onSort={setSort} align="right" /></th>
              </tr>
            </thead>
            <tbody>
              {countries.map((c, i) => {
                const ru = c.name === "Россия";
                return (
                  <tr key={c.name} {...tip(`${c.name}: ${nfmt(c.count)} вак · ${c.cities} городов · медиана ${salary(c.median)} · ${c.remote_pct}% удал.`)}
                    style={{ borderBottom: "1px solid var(--hover)" }}>
                    <td style={{ ...td, paddingLeft: 22, fontSize: 12, fontWeight: 700, color: "var(--text-5)" }}>{i + 1}</td>
                    <td style={{ ...td, fontSize: 15, fontWeight: 600, color: ru ? "#4F46E5" : "var(--text)" }}>{c.name}</td>
                    <td style={{ ...td, width: "32%" }}>
                      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                        <div style={{ flex: 1 }}><Track pct={Math.sqrt(c.count / maxV) * 100} color={ru ? "linear-gradient(90deg,#4F46E5,#818CF8)" : "linear-gradient(90deg,#06B6D4,#67E8F9)"} height={6} /></div>
                        <div style={{ width: 56, fontSize: 13, color: "var(--text-3)", textAlign: "right" }}>{nfmt(c.count)}</div>
                      </div>
                    </td>
                    <td style={{ ...td, textAlign: "right", color: "var(--text-2)" }}>{c.cities}</td>
                    <td style={{ ...td, textAlign: "right", fontWeight: 700, color: "var(--text-2)" }}>{salary(c.median)}</td>
                    <td style={{ ...td, paddingRight: 22, textAlign: "right", fontWeight: 600, color: "#059669" }}>{c.remote_pct}%</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </TableCard>
    </div>
  );
}
