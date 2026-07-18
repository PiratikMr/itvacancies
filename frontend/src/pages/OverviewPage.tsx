import type { OverviewResponse } from "../api/types";
import { api } from "../api/client";
import { usePageData, cacheKey } from "../lib/usePageData";
import { Notice } from "../components/shared";
import type { Filters } from "../state/filters";
import { Card, KpiCard, KpiGrid } from "../components/ui";
import { AreaChart } from "../charts/AreaChart";
import { DonutChart, type DonutSlice } from "../charts/DonutChart";
import { DirectionBars, FormatBars, PlatformBars } from "../charts/Bars";
import { tip } from "../lib/tooltip";
import { useIsMobile } from "../lib/useIsMobile";
import { nfmt, salary, salaryFull, weekLabel, monthShort, yearOf, isMonthlyBuckets } from "../lib/format";

const GRADE_COLOR: Record<string, string> = {
  "Сеньор": "#4F46E5", "Миддл": "#6366F1", "Тимлид": "#06B6D4",
  "Джуниор": "#10B981", "Стажер": "#F97316", "Другое": "#9CA3AF",
};

export function OverviewPage({ filters }: { filters: Filters }) {
  const { data, loading, error } = usePageData(() => api.overview(filters), [filters], cacheKey("overview", filters));
  if (error) return <Notice text={`Ошибка загрузки: ${error}`} />;
  if (!data) return <Notice text="Загрузка…" />;
  return <OverviewBody data={data} loading={loading} />;
}

function OverviewBody({ data, loading }: { data: OverviewResponse; loading: boolean }) {
  const mobile = useIsMobile();
  const k = data.kpis;
  const series = data.timeseries;
  const monthly = isMonthlyBuckets(series.map((t) => t.period));
  const areaData = series.map((t) =>
    monthly
      ? { x: monthShort(t.period), y: t.count, year: yearOf(t.period) }
      : { x: weekLabel(t.period), y: t.count }
  );

  const donut: DonutSlice[] = data.grades.map((g) => ({
    name: g.name,
    pct: g.pct,
    color: GRADE_COLOR[g.name] ?? "#9CA3AF",
  }));

  return (
    <div style={{ opacity: loading ? 0.55 : 1, transition: "opacity .15s" }}>
      <KpiGrid>
        <KpiCard label="Всего вакансий" value={nfmt(k.total)} sub="за выбранный период" spark={{ vals: k.spark.total, color: "#4F46E5" }} delay={0} />
        <KpiCard label="Активные" value={nfmt(k.active)} sub="сейчас открыто" spark={{ vals: k.spark.active, color: "#16A34A" }} delay={0.05} />
        <KpiCard label="Медиана ЗП" value={salaryFull(k.median_salary)} sub="по вакансиям с указанной ЗП" spark={{ vals: k.spark.median_salary, color: "#D97706" }} delay={0.1} />
        <KpiCard label="Удалённо" value={`${k.remote_pct}%`} sub="доля вакансий" spark={{ vals: k.spark.remote_pct, color: "#DC2626" }} delay={0.15} />
      </KpiGrid>

      <div className="mob-2col" style={{ display: "grid", gridTemplateColumns: "3fr 2fr", gap: 12, marginBottom: 12 }}>
        <Card title="Динамика публикаций" subtitle={monthly ? "Новые вакансии по месяцам" : "Новые вакансии по неделям"}>
          <AreaChart data={areaData} mobile={mobile} />
        </Card>
        <Card title="Грейды" subtitle="Доля вакансий" style={{ display: "flex", flexDirection: "column" }}>
          <DonutChart data={donut} centerLabel="топ-грейд" mobile={mobile} />
        </Card>
      </div>

      <div className="mob-2col" style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 12 }}>
        <Card title="Направления" subtitle="Вакансии · медиана ЗП">
          <DirectionBars data={data.directions} />
        </Card>
        <Card title="Топ навыков" subtitle="Упоминания · медиана ЗП">
          <SkillsTable data={data.top_skills} />
        </Card>
      </div>

      <div className="mob-2col" style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
        <Card title="Формат работы" subtitle="Доля вакансий · медиана ЗП">
          <FormatBars data={data.formats} />
        </Card>
        <Card title="Площадки" subtitle="Вакансии и срок закрытия">
          <PlatformBars data={data.platforms} />
        </Card>
      </div>
    </div>
  );
}

function SkillsTable({ data }: { data: OverviewResponse["top_skills"] }) {
  return (
    <div>
      <div style={{ display: "grid", gridTemplateColumns: "22px minmax(0, 1fr) 64px 64px", gap: "0 8px", paddingBottom: 8, borderBottom: "1px solid var(--track)", marginBottom: 2 }}>
        <div />
        <div style={{ fontSize: 12, fontWeight: 700, color: "var(--text-4)", textTransform: "uppercase", letterSpacing: ".05em" }}>Навык</div>
        <div style={{ fontSize: 12, fontWeight: 700, color: "var(--text-4)", textAlign: "right", textTransform: "uppercase", letterSpacing: ".05em" }}>Вак.</div>
        <div style={{ fontSize: 12, fontWeight: 700, color: "var(--text-4)", textAlign: "right", textTransform: "uppercase", letterSpacing: ".05em" }}>ЗП</div>
      </div>
      {data.map((s, i) => (
        <div key={s.name} {...tip(`${s.name}: ${nfmt(s.count)} упоминаний · медиана ${salary(s.median_salary)}`)}
          style={{ display: "grid", gridTemplateColumns: "22px minmax(0, 1fr) 64px 64px", gap: "0 8px", padding: "8px 0", borderBottom: "1px solid var(--hover)", alignItems: "center" }}>
          <div style={{ fontSize: 12, fontWeight: 700, color: "var(--text-5)", textAlign: "center" }}>{String(i + 1).padStart(2, "0")}</div>
          <div style={{ fontSize: 15, fontWeight: 600, color: "var(--text)" }}>{s.name}</div>
          <div style={{ fontSize: 14, color: "var(--text-3)", textAlign: "right" }}>{nfmt(s.count)}</div>
          <div style={{ fontSize: 14, fontWeight: 700, color: "#4F46E5", textAlign: "right" }}>{salary(s.median_salary)}</div>
        </div>
      ))}
    </div>
  );
}
