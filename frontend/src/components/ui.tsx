import type { CSSProperties, ReactNode } from "react";
import { Sparkline } from "../charts/Sparkline";
import { surfaceCard, panelTitle, panelSub, font, weight, pad } from "../lib/tokens";

export function Card({
  title, subtitle, right, children, style,
}: {
  title?: string;
  subtitle?: string;
  right?: ReactNode;
  children: ReactNode;
  style?: CSSProperties;
}) {
  return (
    <div style={{ ...surfaceCard, padding: pad.card, ...style }}>
      {(title || right) && (
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: subtitle ? 2 : 16 }}>
          <div>
            {title && <div style={panelTitle}>{title}</div>}
            {subtitle && <div style={panelSub}>{subtitle}</div>}
          </div>
          {right}
        </div>
      )}
      {!title && subtitle && <div style={{ ...panelSub, marginTop: 0, marginBottom: 12 }}>{subtitle}</div>}
      {(title && subtitle) ? <div style={{ marginTop: 16 }}>{children}</div> : children}
    </div>
  );
}

export function TableCard({ title, subtitle, toolbar, children }: {
  title: string; subtitle?: string; toolbar?: ReactNode; children: ReactNode;
}) {
  return (
    <div className="mob-table" style={{ ...surfaceCard, overflow: "hidden" }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, flexWrap: "wrap", padding: "18px 22px 12px", borderBottom: "1px solid var(--track)" }}>
        <div>
          <div style={panelTitle}>{title}</div>
          {subtitle && <div style={panelSub}>{subtitle}</div>}
        </div>
        {toolbar}
      </div>
      {children}
    </div>
  );
}

export function KpiGrid({ children }: { children: ReactNode }) {
  return (
    <div className="mob-kpi" style={{ display: "grid", gridTemplateColumns: "repeat(4,1fr)", gap: 12, marginBottom: 14 }}>
      {children}
    </div>
  );
}

export function KpiCard({
  label, value, sub, spark, delta, accent, delay = 0,
}: {
  label: string;
  value: string;
  sub?: string;
  spark?: { vals: number[]; color: string };
  delta?: number | null;
  accent?: boolean;
  delay?: number;
}) {
  const up = delta != null && delta >= 0;
  return (
    <div style={{ ...surfaceCard, padding: pad.kpi, borderTop: accent ? "3px solid var(--accent)" : undefined, animation: `fadeUp .3s ease-out ${delay}s both` }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 12 }}>
        <div style={{ fontSize: font.small, fontWeight: weight.semi, color: "var(--text-3)", textTransform: "uppercase", letterSpacing: ".04em" }}>{label}</div>
        {spark && spark.vals.length > 1 && <Sparkline vals={spark.vals} color={spark.color} />}
      </div>
      <div className="mob-kpi-val" style={{ fontSize: font.kpi, fontWeight: weight.heavy, color: accent ? "var(--accent)" : "var(--text)", letterSpacing: "-.04em", marginBottom: 10 }}>{value}</div>
      <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
        {delta != null && (
          <span style={{ display: "inline-flex", alignItems: "center", gap: 3, fontSize: font.small, fontWeight: weight.bold, color: up ? "#059669" : "#DC2626", background: up ? "var(--tint-green)" : "var(--tint-red)", padding: "2px 8px", borderRadius: 6 }}>
            {up ? "▲" : "▼"} {Math.abs(delta)}%
          </span>
        )}
        {sub && <span style={{ fontSize: font.small, color: "var(--text-4)" }}>{sub}</span>}
      </div>
    </div>
  );
}

export const StatCard = KpiCard;
