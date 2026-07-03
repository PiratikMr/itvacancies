import { useEffect, useState } from "react";
import type { FieldSkillRow, SkillsResponse } from "../api/types";
import { api } from "../api/client";
import { usePageData, cacheKey } from "../lib/usePageData";
import type { Filters } from "../state/filters";
import { Notice, Track, Pager, SortTh, NO_SORT, type Sort } from "../components/shared";
import { Card, KpiGrid, StatCard, TableCard } from "../components/ui";
import { tip } from "../lib/tooltip";
import { useIsMobile } from "../lib/useIsMobile";
import { nfmt, salary } from "../lib/format";
import { tableTh as th, tableTd as td, capsLabel } from "../lib/tokens";

const LIMIT = 15;
const LEVELS = ["A1", "A2", "B1", "B2", "C1", "C2"];
const lvlRank = (s: string) => {
  const i = LEVELS.findIndex((c) => s.startsWith(c));
  return i < 0 ? 99 : i;
};

const lum = (hex: string) => {
  const n = parseInt(hex.slice(1), 16);
  const f = (c: number) => { const v = c / 255; return v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4; };
  return 0.2126 * f((n >> 16) & 255) + 0.7152 * f((n >> 8) & 255) + 0.0722 * f(n & 255);
};
const inkOn = (hex: string) => (lum(hex) > 0.35 ? "#1F2733" : "#FFFFFF");

const hslHex = (h: number, s: number, l: number) => {
  s /= 100; l /= 100;
  const k = (n: number) => (n + h / 30) % 12;
  const a = s * Math.min(l, 1 - l);
  const f = (n: number) => l - a * Math.max(-1, Math.min(k(n) - 3, Math.min(9 - k(n), 1)));
  const to = (x: number) => Math.round(255 * x).toString(16).padStart(2, "0");
  return `#${to(f(0))}${to(f(8))}${to(f(4))}`;
};

const HUES = [215, 42, 140, 330, 187, 22, 262, 88, 2, 168, 232, 302];

function FieldSkillsChart({ rows }: { rows: FieldSkillRow[] }) {
  const mobile = useIsMobile();
  const dark = document.body.classList.contains("dark");

  const fields: { field: string; total: number; skills: { skill: string; count: number }[] }[] = [];
  for (const r of rows) {
    let f = fields.find((x) => x.field === r.field);
    if (!f) { f = { field: r.field, total: r.field_total, skills: [] }; fields.push(f); }
    f.skills.push({ skill: r.skill, count: r.count });
  }

  const totals = new Map<string, number>();
  for (const r of rows) totals.set(r.skill, (totals.get(r.skill) || 0) + r.count);
  const rank = new Map([...totals.entries()]
    .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
    .map(([s], i) => [s, i] as const));

  const skillColor = (name: string) => {
    const i = rank.get(name) ?? 0;
    const hue = (HUES[i % HUES.length] + Math.floor(i / 24) * 16) % 360;
    const pale = Math.floor(i / HUES.length) % 2 === 1;
    return dark
      ? (pale ? hslHex(hue, 36, 62) : hslHex(hue, 46, 49))
      : (pale ? hslHex(hue, 52, 73) : hslHex(hue, 56, 56));
  };

  return (
    <div>
      <div style={{ display: "flex", flexDirection: "column", gap: mobile ? 14 : 10 }}>
        {fields.map((f) => {
          const sum = f.skills.reduce((s, x) => s + x.count, 0) || 1;
          const segs = [...f.skills].sort((a, b) => b.count - a.count);
          const bar = (
            <div style={{ display: "flex", gap: 2, height: 24, minWidth: 0, flex: 1 }}>
              {segs.map((sg, i) => {
                const pct = (sg.count / sum) * 100;
                const fill = skillColor(sg.skill);
                return (
                  <div key={sg.skill} {...tip(`${f.field} · ${sg.skill}: ${pct.toFixed(0)}% топ-5 · ${nfmt(sg.count)} вакансий`)}
                    style={{ width: `${pct}%`, minWidth: 3, background: fill, display: "flex", alignItems: "center", justifyContent: "center",
                      borderRadius: i === 0 ? "4px 1px 1px 4px" : i === segs.length - 1 ? "1px 4px 4px 1px" : 1 }}>
                    {!mobile && pct >= 4 && (
                      <span style={{ fontSize: 12, fontWeight: 600, color: inkOn(fill), whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", maxWidth: "100%", padding: "0 6px" }}>{sg.skill}</span>
                    )}
                  </div>
                );
              })}
            </div>
          );
          return mobile ? (
            <div key={f.field}>
              <div style={{ fontSize: 13, fontWeight: 600, color: "var(--text-2)", marginBottom: 5 }}>{f.field}</div>
              {bar}
              <div style={{ display: "flex", flexWrap: "wrap", gap: "3px 10px", marginTop: 6 }}>
                {segs.map((sg) => (
                  <span key={sg.skill} style={{ display: "inline-flex", alignItems: "center", gap: 5, fontSize: 11.5, color: "var(--text-3)" }}>
                    <span style={{ width: 7, height: 7, borderRadius: 2, background: skillColor(sg.skill), flexShrink: 0 }} />
                    {sg.skill}
                  </span>
                ))}
              </div>
            </div>
          ) : (
            <div key={f.field} style={{ display: "grid", gridTemplateColumns: "260px 1fr", gap: 14, alignItems: "center" }}>
              <div {...tip(`${f.field}: ${nfmt(f.total)} вакансий`)} style={{ fontSize: 13.5, fontWeight: 500, color: "var(--text-2)", textAlign: "right", lineHeight: 1.25 }}>{f.field}</div>
              {bar}
            </div>
          );
        })}
      </div>
    </div>
  );
}

export function SkillsPage({ filters }: { filters: Filters }) {
  const [offset, setOffset] = useState(0);
  const [sort, setSort] = useState<Sort>(NO_SORT);
  useEffect(() => setOffset(0), [filters, sort]);
  const { data, loading, error } = usePageData(() => api.skills(filters, LIMIT, offset, sort), [filters, offset, sort], cacheKey("skills", filters, offset, sort));
  if (error) return <Notice text={`Ошибка загрузки: ${error}`} />;
  if (!data) return <Notice text="Загрузка…" />;
  return <SkillsBody data={data} loading={loading} offset={offset} onOffset={setOffset} sort={sort} onSort={setSort} />;
}

function SkillsBody({ data, loading, offset, onOffset, sort, onSort }: { data: SkillsResponse; loading: boolean; offset: number; onOffset: (o: number) => void; sort: Sort; onSort: (s: Sort) => void }) {
  const mobile = useIsMobile();
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

      {data.by_field.length > 0 && (
        <Card title="Навыки по направлениям" subtitle="Топ-5 навыков в самых популярных направлениях" style={{ marginBottom: 12 }}>
          <FieldSkillsChart rows={data.by_field} />
        </Card>
      )}

      <div className="mob-2col" style={{ display: "grid", gridTemplateColumns: "1.55fr 1fr", gap: 12 }}>
      <TableCard title="Все навыки">
        {mobile ? (
          <div>
            {data.table.rows.map((s, i) => {
              const prem = Math.round((s.median / market - 1) * 100);
              return (
                <div key={s.name} {...tip(`${s.name} · ${nfmt(s.count)} упоминаний · медиана ${salary(s.median)}`)}
                  style={{ padding: "12px 16px", borderBottom: "1px solid var(--hover)" }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", gap: 10 }}>
                    <span style={{ fontSize: 15, fontWeight: 600, color: "var(--text)" }}>
                      <span style={{ color: "var(--text-5)", fontWeight: 700, marginRight: 8 }}>{offset + i + 1}</span>{s.name}
                    </span>
                    <span style={{ fontSize: 12, fontWeight: 700, color: prem >= 0 ? "#059669" : "#DC2626", background: prem >= 0 ? "var(--tint-green)" : "var(--tint-red)", padding: "2px 7px", borderRadius: 5, flexShrink: 0 }}>{prem >= 0 ? "+" : ""}{prem}%</span>
                  </div>
                  <div style={{ marginTop: 7 }}>
                    <Track pct={(s.count / tableMax) * 100} color="#4F46E5" height={6} />
                  </div>
                  <div style={{ marginTop: 8, fontSize: 12.5, color: "var(--text-4)" }}>
                    {nfmt(s.count)} упоминаний · медиана <span style={{ fontWeight: 700, color: "var(--text-2)" }}>{salary(s.median)}</span>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 14 }}>
            <thead>
              <tr style={{ background: "var(--surface-2)", borderBottom: "1px solid var(--track)" }}>
                <th style={{ ...th, ...capsLabel, paddingLeft: 22, textAlign: "left" }}>#</th>
                <th style={th}><SortTh label="Навык" col="name" sort={sort} onSort={onSort} /></th>
                <th style={{ ...th, width: "34%" }}><SortTh label="Упоминаний" col="count" sort={sort} onSort={onSort} /></th>
                <th style={th}><SortTh label="Медиана ЗП" col="median" sort={sort} onSort={onSort} align="right" /></th>
                <th style={{ ...th, paddingRight: 22 }}><SortTh label="vs рынок" col="median" sort={sort} onSort={onSort} align="right" /></th>
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
        )}
        <Pager total={data.table.total} limit={data.table.limit} offset={offset} onOffset={onOffset} />
      </TableCard>

      <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
      {data.pairs.length > 0 && (
        <Card title="Связки навыков" subtitle="Часто требуются вместе">
          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            {data.pairs.map((p, i) => (
              <div key={`${p.skill_a}+${p.skill_b}`} {...tip(`${p.skill_a} + ${p.skill_b} · ${nfmt(p.count)} вакансий${p.median ? ` · медиана ${salary(p.median)}` : ""}`)}
                style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 10 }}>
                <span style={{ fontSize: 13.5, fontWeight: 600, color: "var(--text)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                  <span style={{ color: "var(--text-5)", fontWeight: 700, marginRight: 8 }}>{i + 1}</span>
                  {p.skill_a} + {p.skill_b}
                </span>
                <span style={{ display: "flex", alignItems: "center", gap: 8, flexShrink: 0 }}>
                  <span style={{ fontSize: 12.5, color: "var(--text-4)" }}>{nfmt(p.count)}</span>
                  {p.median > 0 && <span style={{ fontSize: 13, fontWeight: 700, color: "#4F46E5" }}>{salary(p.median)}</span>}
                </span>
              </div>
            ))}
          </div>
        </Card>
      )}

      <Card title="Английский язык" subtitle={`Требуют английский — ${k.english_pct ?? 0}% вакансий`} style={{ flex: 1, display: "flex", flexDirection: "column" }}>
        <div style={{ display: "flex", flexDirection: "column", gap: 14, flex: 1, justifyContent: "space-between" }}>
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
      </div>
      </div>
    </div>
  );
}
