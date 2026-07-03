import { useEffect, useState } from "react";
import type { EmployersResponse } from "../api/types";
import { api } from "../api/client";
import { usePageData, cacheKey } from "../lib/usePageData";
import type { Filters } from "../state/filters";
import { Notice, Pager, Track, SortTh, NO_SORT, type Sort } from "../components/shared";
import { KpiGrid, StatCard, TableCard } from "../components/ui";
import { ComboChart } from "../charts/ComboChart";
import { tip } from "../lib/tooltip";
import { useIsMobile } from "../lib/useIsMobile";
import { nfmt, salary, weekLabel, monthShort, yearOf, isMonthlyBuckets } from "../lib/format";
import { surfaceCard, panelTitle, panelSub, pad, tableTh as th, tableTd as td, capsLabel } from "../lib/tokens";

const LIMIT = 15;

export function EmployersPage({ filters }: { filters: Filters }) {
  const [offset, setOffset] = useState(0);
  const [sort, setSort] = useState<Sort>(NO_SORT);
  useEffect(() => setOffset(0), [filters, sort]);
  const { data, loading, error } = usePageData(() => api.employers(filters, LIMIT, offset, sort), [filters, offset, sort], cacheKey("employers", filters, offset, sort));
  if (error) return <Notice text={`Ошибка загрузки: ${error}`} />;
  if (!data) return <Notice text="Загрузка…" />;
  return <EmployersBody data={data} loading={loading} offset={offset} onOffset={setOffset} sort={sort} onSort={setSort} />;
}

function EmployersBody({ data, loading, offset, onOffset, sort, onSort }: { data: EmployersResponse; loading: boolean; offset: number; onOffset: (o: number) => void; sort: Sort; onSort: (s: Sort) => void }) {
  const mobile = useIsMobile();
  const k = data.kpis;
  const activePct = k.unique_employers ? Math.round((k.active_employers / k.unique_employers) * 100) : 0;
  const dyn = data.dynamics;
  const monthly = isMonthlyBuckets(dyn.map((t) => t.period));
  const combo = dyn.map((t) =>
    monthly
      ? { x: monthShort(t.period), bar: t.employers, line: t.per_employer, year: yearOf(t.period) }
      : { x: weekLabel(t.period), bar: t.employers, line: t.per_employer }
  );
  const maxActive = Math.max(1, ...data.top_active.map((a) => a.active));

  return (
    <div style={{ opacity: loading ? 0.55 : 1, transition: "opacity .15s" }}>
      <KpiGrid>
        <StatCard label="Уникальных работодателей" value={nfmt(k.unique_employers)} />
        <StatCard label="Активно нанимают" value={nfmt(k.active_employers)} sub={`${activePct}% базы`} />
        <StatCard label="Ср. вакансий на компанию" value={String(k.avg_per_company ?? "—")} />
        <StatCard label="Ср. время закрытия" value={k.avg_close_days != null ? `${k.avg_close_days} дн.` : "—"} accent />
      </KpiGrid>

      <div className="mob-2col" style={{ display: "grid", gridTemplateColumns: "1.4fr 1fr", gap: 12 }}>
        <TableCard title="Топ работодателей">
          {mobile ? (
            <div>
              {data.table.rows.map((e, i) => (
                <div key={e.name + i} {...tip(`${e.name}: ${nfmt(e.count)} вак · ${nfmt(e.active)} активных · медиана ${salary(e.median)}`)}
                  style={{ padding: "12px 16px", borderBottom: "1px solid var(--hover)" }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", gap: 10 }}>
                    <span style={{ fontSize: 15, fontWeight: 600, color: "var(--text)" }}>
                      <span style={{ color: "var(--text-5)", fontWeight: 700, marginRight: 8 }}>{offset + i + 1}</span>{e.name}
                    </span>
                    <span style={{ fontSize: 14, fontWeight: 700, color: "#4F46E5", whiteSpace: "nowrap", flexShrink: 0 }}>{salary(e.median)}</span>
                  </div>
                  <div style={{ marginTop: 6, fontSize: 12.5, color: "var(--text-4)" }}>
                    {nfmt(e.count)} вакансий · {nfmt(e.active)} активных
                  </div>
                </div>
              ))}
            </div>
          ) : (
          <div style={{ overflowX: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 14 }}>
              <thead>
                <tr style={{ background: "var(--surface-2)", borderBottom: "1px solid var(--track)" }}>
                  <th style={{ ...th, ...capsLabel, paddingLeft: 22, textAlign: "left" }}>#</th>
                  <th style={th}><SortTh label="Компания" col="name" sort={sort} onSort={onSort} /></th>
                  <th style={th}><SortTh label="Вакансий" col="count" sort={sort} onSort={onSort} align="right" /></th>
                  <th style={th}><SortTh label="Активных" col="active" sort={sort} onSort={onSort} align="right" /></th>
                  <th style={{ ...th, paddingRight: 22 }}><SortTh label="Медиана ЗП" col="median" sort={sort} onSort={onSort} align="right" /></th>
                </tr>
              </thead>
              <tbody>
                {data.table.rows.map((e, i) => (
                  <tr key={e.name + i} {...tip(`${e.name}: ${nfmt(e.count)} вак · ${nfmt(e.active)} активных · медиана ${salary(e.median)}`)}
                    style={{ borderBottom: "1px solid var(--hover)" }}>
                    <td style={{ ...td, paddingLeft: 22, fontSize: 12, fontWeight: 700, color: "var(--text-5)" }}>{offset + i + 1}</td>
                    <td style={{ ...td, fontSize: 15, fontWeight: 600, color: "var(--text)" }}>{e.name}</td>
                    <td style={{ ...td, textAlign: "right", color: "var(--text-2)" }}>{nfmt(e.count)}</td>
                    <td style={{ ...td, textAlign: "right", color: "var(--text-2)" }}>{nfmt(e.active)}</td>
                    <td style={{ ...td, paddingRight: 22, textAlign: "right", fontWeight: 700, color: "#4F46E5" }}>{salary(e.median)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          )}
          <Pager total={data.table.total} limit={data.table.limit} offset={offset} onOffset={onOffset} />
        </TableCard>

        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          <div style={{ ...surfaceCard, padding: pad.card }}>
            <div style={panelTitle}>Динамика найма</div>
            <div style={panelSub}>{monthly ? "По месяцам" : "По неделям"}</div>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 14, margin: "12px 0 10px", fontSize: 12, color: "var(--text-3)" }}>
              <span style={{ display: "flex", alignItems: "center", gap: 6 }}><span style={{ width: 11, height: 11, borderRadius: 2, background: "#C7D2FE" }} />Активные работодатели</span>
              <span style={{ display: "flex", alignItems: "center", gap: 6 }}><span style={{ width: 15, height: 3, borderRadius: 2, background: "#4F46E5" }} />Вакансий на работодателя</span>
            </div>
            <ComboChart data={combo} barName="работодателей" lineName="вак./раб." mobile={mobile} />
          </div>

          <div style={{ ...surfaceCard, padding: pad.card, flex: 1, display: "flex", flexDirection: "column", minHeight: 0 }}>
            <div style={panelTitle}>Активнее всего нанимают</div>
            <div style={panelSub}>Открытые вакансии сейчас</div>
            <div style={{ marginTop: 14, flex: 1, display: "flex", flexDirection: "column", justifyContent: "space-between", gap: 12, minHeight: 0 }}>
              {data.top_active.map((a) => (
                <div key={a.name} {...tip(`${a.name}: ${nfmt(a.active)} активных вакансий`)}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", gap: 10, marginBottom: 5 }}>
                    <span style={{ fontSize: 14, fontWeight: 500, color: "var(--text-2)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{a.name}</span>
                    <span style={{ fontSize: 14, fontWeight: 700, color: "var(--text)" }}>{nfmt(a.active)}</span>
                  </div>
                  <Track pct={(a.active / maxActive) * 100} color="linear-gradient(90deg,#4F46E5,#818CF8)" height={7} />
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
