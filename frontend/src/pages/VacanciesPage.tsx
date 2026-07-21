import { useEffect, useState } from "react";
import type { VacanciesResponse, VacancyRow } from "../api/types";
import { api } from "../api/client";
import { usePageData, cacheKey } from "../lib/usePageData";
import type { Filters } from "../state/filters";
import { EmptyState, Notice, Pager, SortTh, NO_SORT, type Sort } from "../components/shared";
import { TableCard } from "../components/ui";
import { useIsMobile } from "../lib/useIsMobile";
import { salary } from "../lib/format";
import { tableTh as th, tableTd as td, capsLabel } from "../lib/tokens";

const LIMIT = 20;
const GRADE_COLOR: Record<string, string> = {
  "Сеньор": "#4F46E5", "Миддл": "#6366F1", "Тимлид": "#06B6D4", "Джуниор": "#10B981", "Стажер": "#F97316",
};

const daysSince = (iso: string) => Math.max(0, Math.floor((Date.now() - new Date(iso).getTime()) / 86400000));
const fmtColor = (s: string) =>
  s === "Удалённо" ? "#059669" : s === "Гибрид" ? "#D97706" : s === "Разъездной" ? "#F97316"
    : /^На\s*месте/.test(s) ? "#6366F1" : "var(--text-3)";

function Chips({ items, color }: { items: string[]; color: (s: string) => string }) {
  const arr = (items || []).filter(Boolean);
  if (!arr.length) return <span style={{ color: "var(--text-5)" }}>—</span>;
  return (
    <div style={{ display: "flex", flexWrap: "wrap", gap: 4 }}>
      {arr.map((x) => {
        const c = color(x);
        return <span key={x} style={{ fontSize: 12, fontWeight: 600, color: c, background: c + "1f", padding: "2px 7px", borderRadius: 5 }}>{x}</span>;
      })}
    </div>
  );
}

export function VacanciesPage({ filters }: { filters: Filters }) {
  const [offset, setOffset] = useState(0);
  const [sort, setSort] = useState<Sort>(NO_SORT);
  useEffect(() => setOffset(0), [filters, sort]);
  const { data, loading, error } = usePageData(() => api.vacancies(filters, LIMIT, offset, sort), [filters, offset, sort], cacheKey("vacancies", filters, offset, sort));
  if (error) return <Notice text={`Ошибка загрузки: ${error}`} />;
  if (!data) return <Notice text="Загрузка…" />;
  if (data.total === 0) return <EmptyState />;
  return <VacanciesBody data={data} loading={loading} offset={offset} onOffset={setOffset} sort={sort} onSort={setSort} />;
}

function VacanciesBody({ data, loading, offset, onOffset, sort, onSort }: {
  data: VacanciesResponse; loading: boolean; offset: number; onOffset: (o: number) => void;
  sort: Sort; onSort: (s: Sort) => void;
}) {
  const mobile = useIsMobile();

  if (mobile) {
    return (
      <div style={{ opacity: loading ? 0.55 : 1, transition: "opacity .15s" }}>
        <TableCard title="Актуальные вакансии" subtitle="Только активные">
          <div>
            {data.rows.map((v: VacancyRow) => {
              const days = daysSince(v.published_at);
              const exp = v.experience && v.experience !== "Не указано" ? v.experience : null;
              const grades = (v.grades || []).filter(Boolean);
              const scheds = (v.schedules || []).filter(Boolean);
              return (
                <div key={v.vacancy_id} className="vac-row"
                  onClick={() => v.url && window.open(v.url, "_blank", "noopener")}
                  style={{ padding: "13px 16px", borderBottom: "1px solid var(--hover)", cursor: v.url ? "pointer" : "default" }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", gap: 10 }}>
                    <div className="vac-title" style={{ fontSize: 15, fontWeight: 600, color: "var(--text)", lineHeight: 1.3 }}>{v.title}</div>
                    <div style={{ fontSize: 15, fontWeight: 700, color: "var(--text)", whiteSpace: "nowrap", flexShrink: 0 }}>{salary(v.salary)}</div>
                  </div>
                  <div style={{ fontSize: 13, color: "var(--text-4)", marginTop: 2 }}>{v.employer || "—"}</div>
                  {(grades.length > 0 || scheds.length > 0) && (
                    <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginTop: 10 }}>
                      {grades.length > 0 && <Chips items={grades} color={(g) => GRADE_COLOR[g] || "var(--text-3)"} />}
                      {scheds.length > 0 && <Chips items={scheds} color={fmtColor} />}
                    </div>
                  )}
                  <div style={{ display: "flex", flexWrap: "wrap", gap: "4px 12px", marginTop: 8, fontSize: 12.5, color: "var(--text-4)" }}>
                    {exp && <span>Опыт: {exp}</span>}
                    <span>{v.platform}</span>
                    <span style={{ fontWeight: days === 0 ? 700 : 500, color: days === 0 ? "#059669" : "var(--text-4)" }}>{days === 0 ? "Сегодня" : `${days} дн. назад`}</span>
                  </div>
                </div>
              );
            })}
          </div>
          <Pager total={data.total} limit={data.limit} offset={offset} onOffset={onOffset} />
        </TableCard>
      </div>
    );
  }

  return (
    <div style={{ opacity: loading ? 0.55 : 1, transition: "opacity .15s" }}>
      <TableCard title="Актуальные вакансии" subtitle="Только активные">
        <div style={{ width: "100%", overflowX: "auto" }}>
          <table style={{ width: "100%", minWidth: 760, borderCollapse: "collapse", fontSize: 14 }}>
            <thead>
              <tr style={{ background: "var(--surface-2)", borderBottom: "1px solid var(--track)" }}>
                <th style={{ ...th, ...capsLabel, paddingLeft: 22, textAlign: "left" }}>Вакансия / Компания</th>
                <th style={th}><SortTh label="ЗП" col="salary" sort={sort} onSort={onSort} /></th>
                <th style={th}><SortTh label="Грейд" col="grade" sort={sort} onSort={onSort} /></th>
                <th style={th}><SortTh label="Опыт" col="experience" sort={sort} onSort={onSort} /></th>
                <th style={{ ...th, ...capsLabel, textAlign: "left" }}>Формат</th>
                <th style={{ ...th, ...capsLabel, textAlign: "left" }}>Площадка</th>
                <th style={th}><SortTh label="Дней" col="date" sort={sort} onSort={onSort} /></th>
                <th style={{ ...th, paddingRight: 22 }} />
              </tr>
            </thead>
            <tbody>
              {data.rows.map((v: VacancyRow) => {
                const days = daysSince(v.published_at);
                const exp = v.experience && v.experience !== "Не указано" ? v.experience : "—";
                return (
                  <tr key={v.vacancy_id} className="vac-row"
                    onClick={() => v.url && window.open(v.url, "_blank", "noopener")}
                    style={{ borderBottom: "1px solid var(--hover)", cursor: v.url ? "pointer" : "default" }}>
                    <td style={{ ...td, paddingLeft: 22, minWidth: 200 }}>
                      <div className="vac-title" style={{ fontSize: 15, fontWeight: 600, color: "var(--text)", lineHeight: 1.35 }}>{v.title}</div>
                      <div style={{ fontSize: 13, color: "var(--text-4)" }}>{v.employer || "—"}</div>
                    </td>
                    <td style={{ ...td, whiteSpace: "nowrap", fontSize: 15, fontWeight: 700, color: "var(--text)" }}>{salary(v.salary)}</td>
                    <td style={td}><Chips items={v.grades} color={(g) => GRADE_COLOR[g] || "var(--text-3)"} /></td>
                    <td style={{ ...td, whiteSpace: "nowrap", fontSize: 13, color: exp === "—" ? "var(--text-5)" : "var(--text-2)" }}>{exp}</td>
                    <td style={td}><Chips items={v.schedules} color={fmtColor} /></td>
                    <td style={{ ...td, whiteSpace: "nowrap", fontSize: 14, color: "var(--text-3)" }}>{v.platform}</td>
                    <td style={{ ...td, whiteSpace: "nowrap", fontSize: 14, fontWeight: days === 0 ? 700 : 500, color: days === 0 ? "#059669" : days <= 2 ? "var(--text-2)" : "var(--text-4)" }}>{days === 0 ? "Сег." : `${days}д.`}</td>
                    <td style={{ ...td, paddingRight: 22, width: 28 }}>
                      <div className="vac-go" style={{ display: "flex", justifyContent: "flex-end" }}>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.4} strokeLinecap="round" strokeLinejoin="round"><path d="M7 17L17 7M7 7h10v10" /></svg>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        <Pager total={data.total} limit={data.limit} offset={offset} onOffset={onOffset} />
      </TableCard>
    </div>
  );
}
