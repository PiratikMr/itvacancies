import { nfmt } from "../lib/format";

export function Notice({ text }: { text: string }) {
  return <div style={{ padding: "60px 0", textAlign: "center", color: "var(--text-4)", fontSize: 14 }}>{text}</div>;
}

export type SortDir = "asc" | "desc" | null;
export interface Sort { key: string | null; dir: SortDir }
export const NO_SORT: Sort = { key: null, dir: null };

export function nextSort(cur: Sort, key: string): Sort {
  if (cur.key !== key) return { key, dir: "desc" };
  if (cur.dir === "desc") return { key, dir: "asc" };
  return NO_SORT;
}

export function SortTh({ label, col, sort, onSort, align = "left" }: {
  label: string; col: string; sort: Sort; onSort: (s: Sort) => void;
  align?: "left" | "center" | "right";
}) {
  const active = sort.key === col;
  const arrow = !active ? "↕" : sort.dir === "asc" ? "↑" : "↓";
  const justify = align === "right" ? "flex-end" : align === "center" ? "center" : "flex-start";
  return (
    <div onClick={() => onSort(nextSort(sort, col))} title="Сортировать"
      style={{ display: "flex", alignItems: "center", gap: 4, justifyContent: justify, cursor: "pointer", userSelect: "none",
        fontSize: 12, fontWeight: 700, color: active ? "var(--accent)" : "var(--text-4)", textTransform: "uppercase", letterSpacing: ".05em" }}>
      <span>{label}</span>
      <span style={{ fontSize: 11, opacity: active ? 1 : 0.4 }}>{arrow}</span>
    </div>
  );
}

export function Track({ pct, color = "var(--accent)", height = 8 }: { pct: number; color?: string; height?: number }) {
  return (
    <div style={{ height, background: "var(--track)", borderRadius: 99, overflow: "hidden" }}>
      <div style={{ height: "100%", width: `${Math.max(0, Math.min(100, pct))}%`, background: color, borderRadius: 99, transformOrigin: "left", animation: "growX .7s cubic-bezier(.22,1,.36,1) both" }} />
    </div>
  );
}

export function Pager({ total, limit, offset, onOffset }: { total: number; limit: number; offset: number; onOffset: (o: number) => void }) {
  const pages = Math.max(1, Math.ceil(total / limit));
  const cur = Math.floor(offset / limit);
  const from = total === 0 ? 0 : cur * limit + 1;
  const to = Math.min(total, (cur + 1) * limit);
  const go = (p: number) => onOffset(Math.max(0, Math.min(pages - 1, p)) * limit);

  const list: (number | "…")[] = [];
  if (pages <= 7) {
    for (let i = 0; i < pages; i++) list.push(i);
  } else {
    list.push(0);
    const st = Math.max(1, cur - 1), en = Math.min(pages - 2, cur + 1);
    if (st > 1) list.push("…");
    for (let j = st; j <= en; j++) list.push(j);
    if (en < pages - 2) list.push("…");
    list.push(pages - 1);
  }

  const btn = (label: string, p: number, active: boolean, dis: boolean) => (
    <div key={label + p} onClick={dis ? undefined : () => go(p)}
      style={{ minWidth: 32, height: 32, padding: "0 10px", display: "flex", alignItems: "center", justifyContent: "center", borderRadius: 8, fontSize: 13, fontWeight: active ? 700 : 500, cursor: dis ? "default" : "pointer", color: active ? "var(--on-accent)" : dis ? "var(--text-5)" : "var(--text-2)", background: active ? "var(--accent)" : "var(--surface)", border: "1px solid", borderColor: active ? "var(--accent)" : "var(--border)" }}>
      {label}
    </div>
  );

  return (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "12px 18px", borderTop: "1px solid var(--track)", flexWrap: "wrap", gap: 10 }}>
      <div style={{ fontSize: 13, color: "var(--text-4)" }}>Показано {from}–{to} из {nfmt(total)}</div>
      <div style={{ display: "flex", gap: 5, alignItems: "center" }}>
        {btn("‹", cur - 1, false, cur === 0)}
        {list.map((p, i) => p === "…"
          ? <div key={"e" + i} style={{ padding: "0 4px", color: "var(--text-4)", fontSize: 13 }}>…</div>
          : btn(String(p + 1), p, p === cur, false))}
        {btn("›", cur + 1, false, cur === pages - 1)}
      </div>
    </div>
  );
}
