import type { CSSProperties } from "react";

export const radius = { chip: 6, ctl: 8, card: 14 } as const;

export const font = { micro: 12, small: 13, base: 14, item: 15, h: 20, kpi: 34 } as const;

export const weight = { reg: 400, med: 500, semi: 600, bold: 700, heavy: 800 } as const;

export const pad = { card: "22px 24px", kpi: "20px 22px", tableX: 22, cell: "13px 12px" } as const;

export const surfaceCard: CSSProperties = {
  background: "var(--surface)",
  border: "1px solid var(--border)",
  borderRadius: radius.card,
};

export const panelTitle: CSSProperties = {
  fontSize: font.h, fontWeight: weight.bold, color: "var(--text)", letterSpacing: "-0.02em",
};
export const panelSub: CSSProperties = { fontSize: font.small, color: "var(--text-4)", marginTop: 4 };

export const capsLabel: CSSProperties = {
  fontSize: font.micro, fontWeight: weight.bold, color: "var(--text-4)",
  textTransform: "uppercase", letterSpacing: ".05em",
};

export const tableTh: CSSProperties = { padding: "11px 12px", verticalAlign: "middle", whiteSpace: "nowrap" };
export const tableTd: CSSProperties = { padding: "12px", verticalAlign: "middle" };

export const valueChip = (color: string): CSSProperties => ({
  fontSize: font.micro, fontWeight: weight.semi, color,
  background: color + "1f", padding: "2px 7px", borderRadius: radius.chip,
});
