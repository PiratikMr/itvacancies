import type { SalaryResponse } from "../api/types";
import { api } from "../api/client";
import { usePageData, cacheKey } from "../lib/usePageData";
import type { Filters } from "../state/filters";
import { Notice, Track } from "../components/shared";
import { Card, KpiGrid, StatCard } from "../components/ui";
import { tip } from "../lib/tooltip";
import { nfmt, salary, salaryFull, salaryNum } from "../lib/format";

const GRADE_ORDER = ["Стажер", "Джуниор", "Миддл", "Сеньор", "Тимлид"];
const GRADE_COLOR: Record<string, string> = {
  "Стажер": "#F97316", "Джуниор": "#10B981", "Миддл": "#6366F1", "Сеньор": "#4F46E5", "Тимлид": "#06B6D4",
};

export function SalaryPage({ filters }: { filters: Filters }) {
  const { data, loading, error } = usePageData(() => api.salary(filters), [filters], cacheKey("salary", filters));
  if (error) return <Notice text={`Ошибка загрузки: ${error}`} />;
  if (!data) return <Notice text="Загрузка…" />;
  return <SalaryBody data={data} loading={loading} />;
}

function SalaryBody({ data, loading }: { data: SalaryResponse; loading: boolean }) {
  const k = data.kpis;

  const bands = [...data.grade_band].sort(
    (a, b) => GRADE_ORDER.indexOf(a.name) - GRADE_ORDER.indexOf(b.name)
  );
  const bandMax = Math.max(1, ...bands.map((b) => b.max));

  const dirMax = Math.max(1, ...data.by_direction.map((d) => d.median));
  const expSum = data.by_experience.reduce((s, e) => s + e.count, 0) || 1;
  const expMax = Math.max(1, ...data.by_experience.map((e) => e.count));
  const curSum = data.by_currency.reduce((s, c) => s + c.count, 0) || 1;

  return (
    <div style={{ opacity: loading ? 0.55 : 1, transition: "opacity .15s" }}>
      <KpiGrid>
        <StatCard label="Junior медиана" value={salaryFull(k.junior_median ?? 0)} />
        <StatCard label="Middle медиана" value={salaryFull(k.middle_median ?? 0)} />
        <StatCard label="Senior медиана" value={salaryFull(k.senior_median ?? 0)} />
        <StatCard label="Разрыв Sr / Jr" value={k.sr_jr_ratio ? `${k.sr_jr_ratio}×` : "—"} accent />
      </KpiGrid>

      <Card title="Диапазон зарплат по грейдам" subtitle="Разброс зарплат, тыс. ₽" style={{ marginBottom: 12 }}>
        <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
          {bands.map((g) => {
            const color = GRADE_COLOR[g.name] ?? "#9CA3AF";
            const minPct = (g.min / bandMax) * 100;
            const medPct = (g.median / bandMax) * 100;
            const maxPct = (g.max / bandMax) * 100;
            return (
              <div key={g.name} {...tip(`${g.name}: ${salaryNum(g.min)}–${salary(g.max)} · медиана ${salary(g.median)}`)}
                style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <div style={{ width: 60, fontSize: 14, fontWeight: 600, color: "var(--text-2)" }}>{g.name}</div>
                <div style={{ flex: 1, height: 18, background: "var(--track)", borderRadius: 5, position: "relative" }}>
                  <div style={{ position: "absolute", left: `${minPct}%`, right: `${100 - maxPct}%`, top: 0, height: "100%", background: color, opacity: 0.2, borderRadius: 5 }} />
                  <div style={{ position: "absolute", left: `${medPct}%`, top: -2, width: 3, height: 22, background: color, borderRadius: 2 }} />
                </div>
                <div style={{ display: "flex", gap: 6, fontSize: 13, color: "var(--text-4)", width: 150, justifyContent: "flex-end" }}>
                  <span>{salaryNum(g.min)}</span>
                  <span style={{ color, fontWeight: 700 }}>{salaryNum(g.median)}</span>
                  <span>{salaryNum(g.max)}</span>
                </div>
              </div>
            );
          })}
        </div>
      </Card>

      <div className="mob-2col" style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 12 }}>
        <Card title="Требуемый опыт" subtitle="Доля и медиана ЗП">
          <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
            {data.by_experience.map((e) => {
              const pct = +((e.count / expSum) * 100).toFixed(1);
              return (
                <div key={e.name} {...tip(`${e.name}: ${pct}% вакансий · медиана ${salary(e.median)}`)}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 6 }}>
                    <span style={{ fontSize: 14, fontWeight: 500, color: "var(--text-2)" }}>{e.name}</span>
                    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                      <span style={{ fontSize: 13, color: "var(--text-4)" }}>{pct}%</span>
                      <span style={{ fontSize: 13, fontWeight: 700, color: "#4F46E5" }}>{salary(e.median)}</span>
                    </div>
                  </div>
                  <Track pct={(e.count / expMax) * 100} color="#6366F1" />
                </div>
              );
            })}
          </div>
        </Card>

        <Card title="Прозрачность зарплат" subtitle="Сумма и валюта">
          <div style={{ display: "flex", alignItems: "baseline", gap: 12, marginBottom: 10 }}>
            <div style={{ fontSize: 36, fontWeight: 800, color: "#4F46E5", letterSpacing: "-0.03em" }}>{data.transparency.with_salary_pct}%</div>
            <div style={{ fontSize: 13, color: "var(--text-3)" }}>вакансий с указанной зарплатой</div>
          </div>
          <div style={{ marginBottom: 18 }}><Track pct={data.transparency.with_salary_pct} color="#4F46E5" /></div>
          <div style={{ fontSize: 12, fontWeight: 700, color: "var(--text-4)", textTransform: "uppercase", letterSpacing: ".05em", marginBottom: 11 }}>Валюта зарплаты</div>
          <div style={{ display: "flex", flexDirection: "column", gap: 11 }}>
            {data.by_currency.map((c) => {
              const pct = +((c.count / curSum) * 100).toFixed(1);
              return (
                <div key={c.name} {...tip(`${c.name}: ${pct}% вакансий · медиана ${salary(c.median)}`)}
                  style={{ display: "flex", alignItems: "center", gap: 11 }}>
                  <div style={{ width: 46, fontSize: 14, fontWeight: 600, color: "var(--text-2)" }}>{c.name}</div>
                  <div style={{ flex: 1 }}><Track pct={pct} color="#4F46E5" height={6} /></div>
                  <div style={{ fontSize: 13, color: "var(--text-4)", width: 44, textAlign: "right" }}>{pct}%</div>
                </div>
              );
            })}
          </div>
        </Card>
      </div>

      <Card title="Медиана ЗП по направлениям" subtitle="Тыс. ₽">
        <div style={{ display: "flex", flexDirection: "column", gap: 13 }}>
          {data.by_direction.map((d) => (
            <div key={d.name} {...tip(`${d.name}: медиана ${salary(d.median)} · ${nfmt(d.count)} вак.`)}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", gap: 12, marginBottom: 5 }}>
                <span style={{ fontSize: 14, fontWeight: 500, color: "var(--text-2)" }}>{d.name}</span>
                <span style={{ fontSize: 14, fontWeight: 700, color: "var(--text)", whiteSpace: "nowrap" }}>{salary(d.median)}</span>
              </div>
              <Track pct={(d.median / dirMax) * 100} color="linear-gradient(90deg,#4F46E5,#818CF8)" height={7} />
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
