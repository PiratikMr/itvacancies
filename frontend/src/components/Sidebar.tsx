import { useEffect, useState } from "react";
import { CLEARED_FILTERS, hasActiveFilters, type Filters } from "../state/filters";
import { nfmt } from "../lib/format";
import { ChipGroup, FacetChips, FacetSelect } from "./facets";

const PERIODS = [{ id: "1w", label: "Неделя" }, { id: "1m", label: "Месяц" }, { id: "3m", label: "3 мес." }, { id: "6m", label: "6 мес." }, { id: "1y", label: "Год" }, { id: "all", label: "Всё время" }];
const STATUSES = [{ id: "all", label: "Все" }, { id: "active", label: "Активные" }, { id: "closed", label: "Закрытые" }];
const FORMAT_OPTS = ["remote", "office", "hybrid", "travel"];
const FORMAT_LABELS: Record<string, string> = { remote: "Удалённо", office: "Офис", hybrid: "Гибрид", travel: "Разъездной" };

type SalaryField = "min" | "max";
const SALARY_KEY = { min: "salaryMin", max: "salaryMax" } as const;

function fmtDate(iso: string | null): string {
  if (!iso) return "—";
  const d = new Date(iso);
  const m = ["января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"];
  return `${d.getDate()} ${m[d.getMonth()]} ${d.getFullYear()}`;
}

export function Sidebar({
  filters, onFilters, facets, onSearch, dataUpdatedAt, open,
}: {
  filters: Filters;
  onFilters: (f: Filters) => void;
  facets: Record<string, string[]>;
  onSearch: (field: string, q: string) => Promise<string[]>;
  dataUpdatedAt: string | null;
  open: boolean;
}) {
  const inp = { width: "100%", padding: "5px 6px", border: "1px solid var(--border)", borderRadius: 6, fontSize: 12, color: "var(--text-2)", outline: "none", background: "var(--surface)", fontVariantNumeric: "tabular-nums" } as const;

  const [draft, setDraft] = useState<{ field: SalaryField; raw: string } | null>(null);

  const parseNum = (raw: string): number | null => {
    const digits = raw.replace(/\D/g, "");
    return digits === "" ? null : Number(digits);
  };

  const commit = (field: SalaryField, raw: string) => {
    const key = SALARY_KEY[field];
    const next = parseNum(raw);
    if (next !== filters[key]) onFilters({ ...filters, [key]: next } as Filters);
  };

  useEffect(() => {
    if (draft == null) return;
    const t = setTimeout(() => commit(draft.field, draft.raw), 600);
    return () => clearTimeout(t);
  }, [draft]);

  const shown = (field: SalaryField): string => {
    if (draft?.field === field) return draft.raw;
    const v = filters[SALARY_KEY[field]];
    return v == null ? "" : nfmt(v);
  };

  const salaryInput = (field: SalaryField, placeholder: string) => (
    <input
      type="text" inputMode="numeric" autoComplete="off" className="salary-inp"
      placeholder={placeholder}
      value={shown(field)}
      onChange={(e) => setDraft({ field, raw: e.target.value.replace(/\D/g, "") })}
      onFocus={() => setDraft({ field, raw: String(filters[SALARY_KEY[field]] ?? "") })}
      onBlur={() => { if (draft) commit(draft.field, draft.raw); setDraft(null); }}
      onKeyDown={(e) => { if (e.key === "Enter") e.currentTarget.blur(); }}
      style={inp}
    />
  );

  const list = (key: keyof Filters) => (filters[key] as string[]);
  const setList = (key: string, next: string[]) => onFilters({ ...filters, [key]: next } as Filters);
  const opt = (key: string) => facets[key] ?? [];

  return (
    <aside className={"mob-sidebar" + (open ? " open" : "")} style={{ width: 244, background: "var(--surface)", borderRight: "1px solid var(--border)", position: "fixed", top: 0, left: 0, display: "flex", flexDirection: "column", zIndex: 50 }}>
      <div className="desk-hide" style={{ padding: "16px 16px 12px", borderBottom: "1px solid var(--track)", flexShrink: 0 }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 9 }}>
            <div style={{ width: 34, height: 34, background: "#4F46E5", borderRadius: 9, display: "flex", alignItems: "center", justifyContent: "center" }}>
              <svg width="18" height="18" viewBox="0 0 20 20"><rect x="2" y="2" width="6.5" height="6.5" rx="1.5" fill="white" /><rect x="11.5" y="2" width="6.5" height="6.5" rx="1.5" fill="white" opacity="0.55" /><rect x="2" y="11.5" width="6.5" height="6.5" rx="1.5" fill="white" opacity="0.55" /><rect x="11.5" y="11.5" width="6.5" height="6.5" rx="1.5" fill="white" opacity="0.3" /></svg>
            </div>
            <div style={{ lineHeight: 1.15 }}>
              <div style={{ fontSize: 16, letterSpacing: "-0.03em" }}>
                <span style={{ fontWeight: 800, color: "#4F46E5" }}>it</span>
                <span style={{ fontWeight: 700, color: "var(--text)" }}>vacancies</span>
              </div>
              <div style={{ fontSize: 11, color: "var(--text-4)", fontWeight: 500 }}>Аналитика рынка труда</div>
            </div>
          </div>
          <a href="https://github.com/PiratikMr/itvacancies" target="_blank" rel="noopener noreferrer" title="GitHub" style={{ width: 28, height: 28, background: "var(--track)", borderRadius: 7, display: "flex", alignItems: "center", justifyContent: "center", textDecoration: "none", flexShrink: 0 }}>
            <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor" style={{ color: "var(--text-2)" }}><path d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.651 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" /></svg>
          </a>
        </div>
      </div>

      <div className="side-date" style={{ padding: "9px 14px", borderBottom: "1px solid var(--track)", flexShrink: 0 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 9, padding: "7px 10px", background: "var(--surface-2)", border: "1px solid var(--border)", borderRadius: 8 }}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" style={{ color: "var(--text-3)" }} strokeWidth={2} strokeLinecap="round"><rect x="3" y="4" width="18" height="18" rx="2" /><line x1="16" y1="2" x2="16" y2="6" /><line x1="8" y1="2" x2="8" y2="6" /><line x1="3" y1="10" x2="21" y2="10" /></svg>
          <div>
            <div style={{ fontSize: 11, fontWeight: 600, color: "var(--text-3)", textTransform: "uppercase", letterSpacing: "0.05em" }}>Данные актуальны на</div>
            <div style={{ fontSize: 13, fontWeight: 700, color: "var(--text)" }}>{fmtDate(dataUpdatedAt)}</div>
          </div>
        </div>
      </div>

      <div className="mob-filters" style={{ flex: 1, overflowY: "auto", padding: "12px 10px 16px" }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "0 8px 10px" }}>
          <div style={{ fontSize: 12, fontWeight: 700, color: "var(--text-4)", textTransform: "uppercase", letterSpacing: "0.08em" }}>Фильтры</div>
          {hasActiveFilters(filters) && (
            <div onClick={() => onFilters(CLEARED_FILTERS)} title="Очистить все фильтры" style={{ display: "flex", alignItems: "center", gap: 4, fontSize: 12, fontWeight: 600, color: "var(--accent)", cursor: "pointer" }}>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.2} strokeLinecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
              Сбросить
            </div>
          )}
        </div>

        <ChipGroup title="Период публикации" options={PERIODS} value={filters.period} onChange={(v) => onFilters({ ...filters, period: v })} />
        <ChipGroup title="Статус вакансии" options={STATUSES} value={filters.status} onChange={(v) => onFilters({ ...filters, status: v })} />

        <div style={{ padding: "0 6px 13px", marginBottom: 13, borderBottom: "1px solid var(--track)" }}>
          <div style={{ fontSize: 12, fontWeight: 700, color: "var(--text-2)", marginBottom: 7 }}>Зарплата, ₽/мес</div>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 6 }}>
            {salaryInput("min", "от")}
            {salaryInput("max", "до")}
          </div>
        </div>

        <FacetSelect title="Навыки" placeholder="Все навыки" selected={filters.skills} onChange={(n) => setList("skills", n)} initialOptions={opt("skills")} search={(q) => onSearch("skills", q)} />
        <FacetSelect title="Направление" placeholder="Все направления" selected={filters.fields} onChange={(n) => setList("fields", n)} initialOptions={opt("fields")} search={(q) => onSearch("fields", q)} />
        <FacetChips title="Грейд" options={opt("grades")} selected={list("grades")} onChange={(n) => setList("grades", n)} />
        <FacetChips title="Опыт работы" options={opt("experience")} selected={list("experience")} onChange={(n) => setList("experience", n)} />
        <FacetChips title="Формат работы" options={FORMAT_OPTS} labels={FORMAT_LABELS} selected={filters.format} onChange={(n) => setList("format", n)} />
        <FacetChips title="Тип работы" options={opt("employments")} selected={list("employments")} onChange={(n) => setList("employments", n)} />
        <FacetSelect title="Работодатель" placeholder="Любой работодатель" selected={filters.employer} onChange={(n) => setList("employer", n)} initialOptions={opt("employer")} search={(q) => onSearch("employer", q)} />
        <FacetChips title="Валюта ЗП" options={opt("currency")} selected={list("currency")} onChange={(n) => setList("currency", n)} />
        <FacetChips title="Площадка" options={opt("platform")} selected={list("platform")} onChange={(n) => setList("platform", n)} />
      </div>
    </aside>
  );
}
