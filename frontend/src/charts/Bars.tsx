import { tip } from "../lib/tooltip";
import { nfmt, salary } from "../lib/format";
import type { NamedCount, PlatformStat } from "../api/types";

const GRAD = "linear-gradient(90deg,#4F46E5,#818CF8)";

export function DirectionBars({ data }: { data: NamedCount[] }) {
  const maxC = Math.max(1, ...data.map((d) => d.count));
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
      {data.map((d, i) => (
        <div key={d.name} {...tip(`${d.name}: ${nfmt(d.count)} вак. · медиана ${salary(d.median_salary)}/мес`)}
          style={{ padding: "5px 7px", margin: "0 -7px", borderRadius: 8 }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 5 }}>
            <span style={{ fontSize: 14, fontWeight: 500, color: "var(--text-2)", minWidth: 108 }}>{d.name}</span>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <span style={{ fontSize: 13, color: "var(--text-4)" }}>{nfmt(d.count)}</span>
              <span style={{ fontSize: 14, fontWeight: 700, color: "#4F46E5", minWidth: 38, textAlign: "right" }}>{salary(d.median_salary)}</span>
              {d.trend != null && (
                <span style={{ fontSize: 12, fontWeight: 700, minWidth: 38, textAlign: "right", color: d.trend >= 0 ? "#059669" : "#DC2626", background: d.trend >= 0 ? "var(--tint-green)" : "var(--tint-red)", padding: "1px 6px", borderRadius: 5 }}>
                  {d.trend >= 0 ? "+" : ""}{d.trend}%
                </span>
              )}
            </div>
          </div>
          <div style={{ height: 5, background: "var(--track)", borderRadius: 3, overflow: "hidden" }}>
            <div style={{ height: "100%", width: `${(d.count / maxC) * 100}%`, background: GRAD, borderRadius: 3, transformOrigin: "left", animation: `growX .8s cubic-bezier(.22,1,.36,1) ${i * 0.05}s both` }} />
          </div>
        </div>
      ))}
    </div>
  );
}

const FORMAT_COLOR: Record<string, string> = {
  "На месте работодателя": "#6366F1",
  "Гибрид": "#06B6D4",
  "Удалённо": "#4F46E5",
  "Разъездной": "#F97316",
};

export function FormatBars({ data }: { data: NamedCount[] }) {
  const sum = data.reduce((s, d) => s + d.count, 0) || 1;
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 14, paddingTop: 6 }}>
      {data.map((d, i) => {
        const pct = +((d.count / sum) * 100).toFixed(1);
        return (
          <div key={d.name} {...tip(`${d.name}: ${pct}% вакансий · медиана ${salary(d.median_salary)}/мес`)}
            style={{ padding: "6px 8px", margin: "0 -8px", borderRadius: 8 }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 6 }}>
              <span style={{ fontSize: 15, fontWeight: 600, color: "var(--text-2)" }}>{d.name}</span>
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <span style={{ fontSize: 18, fontWeight: 800, color: "var(--text)", letterSpacing: "-0.02em" }}>{pct}%</span>
                <span style={{ fontSize: 13, fontWeight: 600, color: "#4F46E5", background: "var(--tint-indigo)", padding: "2px 8px", borderRadius: 5 }}>медиана {salary(d.median_salary)}</span>
              </div>
            </div>
            <div style={{ height: 9, background: "var(--track)", borderRadius: 5, overflow: "hidden" }}>
              <div style={{ height: "100%", width: `${pct}%`, background: FORMAT_COLOR[d.name.replace(/\s+/g, " ")] ?? "#4F46E5", borderRadius: 5, transformOrigin: "left", animation: `growX .8s cubic-bezier(.22,1,.36,1) ${i * 0.08}s both` }} />
            </div>
          </div>
        );
      })}
    </div>
  );
}

const PLATFORM_COLORS = ["#4F46E5", "#6366F1", "#06B6D4", "#10B981", "#F97316", "#9CA3AF"];

export function PlatformBars({ data }: { data: PlatformStat[] }) {
  const sum = data.reduce((s, d) => s + d.count, 0) || 1;
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
      {data.map((d, i) => {
        const pct = Math.round((d.count / sum) * 100);
        const days = d.avg_close_days != null ? `${Math.round(d.avg_close_days)}дн.` : "—";
        return (
          <div key={d.name} {...tip(`${d.name}: ${nfmt(d.count)} вак. · закрытие ${days}`)}
            style={{ display: "flex", alignItems: "center", gap: 10, padding: "5px 7px", margin: "0 -7px", borderRadius: 8 }}>
            <div style={{ width: 92, fontSize: 14, fontWeight: 500, color: "var(--text-2)", flexShrink: 0 }}>{d.name}</div>
            <div style={{ flex: 1, height: 8, background: "var(--track)", borderRadius: 4, overflow: "hidden" }}>
              <div style={{ height: "100%", width: `${pct}%`, background: PLATFORM_COLORS[i % PLATFORM_COLORS.length], borderRadius: 4, transformOrigin: "left", animation: `growX .8s cubic-bezier(.22,1,.36,1) ${i * 0.06}s both` }} />
            </div>
            <div style={{ fontSize: 13, color: "var(--text-3)", width: 38, textAlign: "right" }}>{pct}%</div>
            <div style={{ fontSize: 13, fontWeight: 600, color: "var(--text-2)", width: 44, textAlign: "right" }}>{days}</div>
          </div>
        );
      })}
    </div>
  );
}
