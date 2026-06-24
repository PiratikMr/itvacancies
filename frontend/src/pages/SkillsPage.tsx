import { useEffect, useState } from "react";
import type { SkillsResponse } from "../api/types";
import { api } from "../api/client";
import { usePageData } from "../lib/usePageData";
import type { Filters } from "../state/filters";
import { Notice, Track, Pager, SortTh, NO_SORT, type Sort } from "../components/shared";
import { Card, KpiGrid, StatCard, TableCard } from "../components/ui";
import { tip } from "../lib/tooltip";
import { nfmt, salary } from "../lib/format";
import { tableTh as th, tableTd as td, capsLabel } from "../lib/tokens";

const LIMIT = 20;
const LEVELS = ["A1", "A2", "B1", "B2", "C1", "C2"];
const lvlRank = (s: string) => {
  const i = LEVELS.findIndex((c) => s.startsWith(c));
  return i < 0 ? 99 : i;
};

export function SkillsPage({ filters }: { filters: Filters }) {
  const [offset, setOffset] = useState(0);
  const [sort, setSort] = useState<Sort>(NO_SORT);
  useEffect(() => setOffset(0), [filters, sort]);
  const { data, loading, error } = usePageData(() => api.skills(filters, LIMIT, offset, sort), [filters, offset, sort]);
  if (error) return <Notice text={`Ошибка загрузки: ${error}`} />;
  if (!data) return <Notice text="Загрузка…" />;
  return <SkillsBody data={data} loading={loading} offset={offset} onOffset={setOffset} sort={sort} onSort={setSort} />;
}

function SkillsBody({ data, loading, offset, onOffset, sort, onSort }: { data: SkillsResponse; loading: boolean; offset: number; onOffset: (o: number) => void; sort: Sort; onSort: (s: Sort) => void }) {
  const k = data.kpis;
  const market = data.market_median || 1;

  const eng = [...data.english.levels]
    .sort((a, b) => lvlRank(a.level) - lvlRank(b.level))
    .map((l) => ({ lvl: l.level, count: l.count, median: l.median }));
  const engTotal = eng.reduce((s, e) => s + e.count, 0) || 1;
  const engMax = Math.max(1, ...eng.map((e) => e.count));
  const tableMax = Math.max(1, ...data.table.rows.map((r) => r.count));

  return (
    <div style={{ opacity: loading ? 0.55 : 1, transition: "opacity .15s" }}>
      <KpiGrid>
        <StatCard label="Самый дорогой навык" value={k.top_paid.name || "—"} sub={`медиана ${salary(k.top_paid.median)}`} />
        <StatCard label="Самый востребованный" value={k.top_demand.name || "—"} sub={`${nfmt(k.top_demand.count || 0)} упоминаний`} />
        <StatCard label="Ср. навыков в вакансии" value={String(k.avg_skills ?? "—")} />
        <StatCard label="Требуют англ. язык" value={`${k.english_pct ?? 0}%`} accent />
      </KpiGrid>

      <Card title="Английский язык" subtitle={`Требуют английский — ${k.english_pct ?? 0}% вакансий`} style={{ marginBottom: 12 }}>
        <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          {eng.map((e) => {
            const pct = +((e.count / engTotal) * 100).toFixed(1);
            return (
              <div key={e.lvl} {...tip(`${e.lvl}: ${pct}% от требующих английский${e.median ? ` · медиана ${salary(e.median)}` : ""}`)}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 5 }}>
                  <span style={{ fontSize: 14, fontWeight: 500, color: "var(--text-2)" }}>{e.lvl}</span>
                  <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                    <span style={{ fontSize: 13, color: "var(--text-4)" }}>{pct}%</span>
                    {e.median != null && <span style={{ fontSize: 13, fontWeight: 700, color: "#4F46E5" }}>{salary(e.median)}</span>}
                  </div>
                </div>
                <Track pct={(e.count / engMax) * 100} color="#6366F1" height={7} />
              </div>
            );
          })}
        </div>
      </Card>

      <TableCard title="Все навыки">
        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 14 }}>
            <thead>
              <tr style={{ background: "var(--surface-2)", borderBottom: "1px solid var(--track)" }}>
                <th style={{ ...th, ...capsLabel, paddingLeft: 22, textAlign: "left" }}>#</th>
                <th style={th}><SortTh label="Навык" col="name" sort={sort} onSort={onSort} /></th>
                <th style={{ ...th, width: "34%" }}><SortTh label="Упоминаний" col="count" sort={sort} onSort={onSort} /></th>
                <th style={th}><SortTh label="Медиана ЗП" col="median" sort={sort} onSort={onSort} align="right" /></th>
                <th style={{ ...th, ...capsLabel, paddingRight: 22, textAlign: "right" }}>vs рынок</th>
              </tr>
            </thead>
            <tbody>
              {data.table.rows.map((s, i) => {
                const prem = Math.round((s.median / market - 1) * 100);
                return (
                  <tr key={s.name} {...tip(`${s.name} · ${nfmt(s.count)} упоминаний · медиана ${salary(s.median)}`)}
                    style={{ borderBottom: "1px solid var(--hover)" }}>
                    <td style={{ ...td, paddingLeft: 22, fontSize: 12, fontWeight: 700, color: "var(--text-5)" }}>{offset + i + 1}</td>
                    <td style={{ ...td, fontSize: 15, fontWeight: 600, color: "var(--text)" }}>{s.name}</td>
                    <td style={{ ...td, width: "34%" }}>
                      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                        <div style={{ flex: 1 }}><Track pct={(s.count / tableMax) * 100} color="#4F46E5" height={6} /></div>
                        <div style={{ width: 56, fontSize: 13, color: "var(--text-3)", textAlign: "right" }}>{nfmt(s.count)}</div>
                      </div>
                    </td>
                    <td style={{ ...td, textAlign: "right", fontWeight: 700, color: "var(--text)" }}>{salary(s.median)}</td>
                    <td style={{ ...td, paddingRight: 22, textAlign: "right" }}>
                      <span style={{ fontSize: 12, fontWeight: 700, color: prem >= 0 ? "#059669" : "#DC2626", background: prem >= 0 ? "var(--tint-green)" : "var(--tint-red)", padding: "2px 7px", borderRadius: 5 }}>{prem >= 0 ? "+" : ""}{prem}%</span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        <Pager total={data.table.total} limit={data.table.limit} offset={offset} onOffset={onOffset} />
      </TableCard>
    </div>
  );
}
