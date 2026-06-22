import { useEffect, useRef, useState } from "react";

const GROUP: React.CSSProperties = { padding: "0 6px 13px", marginBottom: 13, borderBottom: "1px solid var(--track)" };
const LABEL: React.CSSProperties = { fontSize: 12, fontWeight: 700, color: "var(--text-2)", marginBottom: 7 };

export function Chip({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <div onClick={onClick} style={{
      padding: "4px 9px", borderRadius: 6, fontSize: 12, cursor: "pointer",
      background: active ? "#4F46E5" : "var(--track)",
      color: active ? "var(--on-accent)" : "var(--text-3)",
      fontWeight: active ? 600 : 500, whiteSpace: "nowrap",
    }}>{children}</div>
  );
}

export function ChipGroup({ title, options, value, onChange }: {
  title: string; options: { id: string; label: string }[]; value: string; onChange: (v: string) => void;
}) {
  return (
    <div style={GROUP}>
      <div style={LABEL}>{title}</div>
      <div style={{ display: "flex", flexWrap: "wrap", gap: 4 }}>
        {options.map((o) => <Chip key={o.id} active={value === o.id} onClick={() => onChange(o.id)}>{o.label}</Chip>)}
      </div>
    </div>
  );
}

export function FacetChips({ title, options, selected, onChange, labels }: {
  title: string; options: string[]; selected: string[];
  onChange: (next: string[]) => void; labels?: Record<string, string>;
}) {
  const display = [...options];
  for (const s of selected) if (!display.includes(s)) display.push(s);
  const toggle = (v: string) => onChange(selected.includes(v) ? selected.filter((x) => x !== v) : [...selected, v]);

  return (
    <div style={GROUP}>
      <div style={LABEL}>{title}</div>
      <div style={{ display: "flex", flexWrap: "wrap", gap: 4 }}>
        <Chip active={selected.length === 0} onClick={() => onChange([])}>Все</Chip>
        {display.map((v) => <Chip key={v} active={selected.includes(v)} onClick={() => toggle(v)}>{labels?.[v] ?? v}</Chip>)}
      </div>
    </div>
  );
}

export function FacetSelect({ title, selected, onChange, initialOptions, search, placeholder }: {
  title: string;
  selected: string[];
  onChange: (next: string[]) => void;
  initialOptions: string[];
  search: (q: string) => Promise<string[]>;
  placeholder?: string;
}) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<string[]>(initialOptions);
  const reqId = useRef(0);

  useEffect(() => {
    if (!open) return;
    if (!query.trim()) { setResults(initialOptions); return; }
    const id = ++reqId.current;
    const t = setTimeout(() => {
      search(query.trim()).then((vals) => { if (id === reqId.current) setResults(vals); }).catch(() => {});
    }, 250);
    return () => clearTimeout(t);
  }, [query, open, initialOptions]);

  const toggle = (v: string) => onChange(selected.includes(v) ? selected.filter((x) => x !== v) : [...selected, v]);

  return (
    <div style={GROUP}>
      <div style={LABEL}>{title}</div>

      <div onClick={() => setOpen((v) => !v)} style={{
        display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8,
        padding: "8px 11px", borderRadius: 8, cursor: "pointer",
        border: `1px solid ${open ? "var(--accent)" : "var(--border)"}`,
        background: "var(--surface)",
        boxShadow: open ? "0 0 0 3px var(--tint-indigo)" : "none",
        transition: "border-color .12s, box-shadow .12s",
      }}>
        <span style={{ fontSize: 13, color: selected.length ? "var(--text)" : "var(--text-4)", fontWeight: selected.length ? 600 : 400, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
          {selected.length ? `Выбрано: ${selected.length}` : (placeholder ?? "Выберите…")}
        </span>
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="var(--text-3)" strokeWidth="2.5" strokeLinecap="round" style={{ flexShrink: 0, transform: open ? "rotate(180deg)" : "none", transition: "transform .15s" }}><polyline points="6 9 12 15 18 9" /></svg>
      </div>

      {selected.length > 0 && (
        <div style={{ display: "flex", flexWrap: "wrap", gap: 4, marginTop: 6 }}>
          {selected.map((v) => (
            <div key={v} onClick={() => toggle(v)} style={{ display: "flex", alignItems: "center", gap: 4, padding: "3px 7px", borderRadius: 6, fontSize: 12, fontWeight: 600, background: "var(--tint-indigo)", color: "var(--accent)", cursor: "pointer" }}>
              {v}
              <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round"><line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" /></svg>
            </div>
          ))}
        </div>
      )}

      {open && (
        <div style={{ marginTop: 6, border: "1px solid var(--border)", borderRadius: 8, background: "var(--surface)", overflow: "hidden" }}>
          <input autoFocus value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Поиск…"
            style={{ width: "100%", padding: "8px 11px", border: "none", borderBottom: "1px solid var(--track)", fontSize: 12, color: "var(--text-2)", outline: "none", background: "var(--surface)" }} />
          <div style={{ maxHeight: 184, overflowY: "auto" }}>
            {results.length === 0 && <div style={{ padding: "10px", fontSize: 12, color: "var(--text-4)", textAlign: "center" }}>Ничего не найдено</div>}
            {results.map((v) => {
              const on = selected.includes(v);
              return (
                <div key={v} onClick={() => toggle(v)} style={{ display: "flex", alignItems: "center", gap: 8, padding: "6px 11px", fontSize: 12.5, cursor: "pointer", color: on ? "var(--accent)" : "var(--text-2)", fontWeight: on ? 600 : 400, background: on ? "var(--hover)" : "transparent" }}>
                  <span style={{ width: 14, height: 14, borderRadius: 4, border: `1.5px solid ${on ? "var(--accent)" : "var(--border)"}`, background: on ? "var(--accent)" : "transparent", flexShrink: 0, display: "flex", alignItems: "center", justifyContent: "center" }}>
                    {on && <svg width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="3.5" strokeLinecap="round"><polyline points="20 6 9 17 4 12" /></svg>}
                  </span>
                  <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{v}</span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
