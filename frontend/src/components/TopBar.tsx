import { type TabId } from "../state/filters";

export interface Fx { usd: number; eur: number; live: boolean; date: string }

const I = (d: string) => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round"><path d={d} /></svg>
);

const NAV: { id: TabId; label: string; icon: JSX.Element }[] = [
  { id: "overview", label: "Обзор рынка", icon: <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></svg> },
  { id: "salary", label: "Зарплаты и грейды", icon: I("M12 1v22M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6") },
  { id: "skills", label: "Навыки", icon: I("M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5") },
  { id: "employers", label: "Работодатели", icon: I("M3 21h18M5 21V7l8-4v18M19 21V11l-6-4") },
  { id: "geo", label: "География", icon: <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}><circle cx="12" cy="12" r="10" /><line x1="2" y1="12" x2="22" y2="12" /><path d="M12 2a15.3 15.3 0 014 10 15.3 15.3 0 01-4 10 15.3 15.3 0 01-4-10 15.3 15.3 0 014-10z" /></svg> },
  { id: "vacancies", label: "Вакансии", icon: I("M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8zM14 2v6h6M16 13H8") },
];

export function TopBar({
  title, tab, onTab, fx, dark, onToggleTheme, onToggleSidebar,
}: {
  title: string;
  tab: TabId;
  onTab: (t: TabId) => void;
  fx: Fx;
  dark: boolean;
  onToggleTheme: () => void;
  onToggleSidebar: () => void;
}) {
  return (
    <div className="mob-topbar" style={{ background: "var(--surface)", borderBottom: "1px solid var(--border)", padding: "0 16px", height: 52, display: "flex", alignItems: "center", gap: 12, justifyContent: "space-between", position: "sticky", top: 0, zIndex: 40 }}>
      <div className="mob-show" onClick={onToggleSidebar} style={{ display: "none", width: 36, height: 36, borderRadius: 8, background: "var(--track)", alignItems: "center", justifyContent: "center", cursor: "pointer", flexShrink: 0 }}>
        <svg width="17" height="13" viewBox="0 0 17 13" fill="currentColor" style={{ color: "var(--text-2)" }}><rect width="17" height="2.2" rx="1.1" /><rect y="5.4" width="12" height="2.2" rx="1.1" /><rect y="10.8" width="17" height="2.2" rx="1.1" /></svg>
      </div>

      <nav className="topnav mob-hide" style={{ display: "flex", alignItems: "center", gap: 2, flex: 1, minWidth: 0, overflowX: "auto" }}>
        {NAV.map((n) => {
          const on = tab === n.id;
          return (
            <button key={n.id} onClick={() => onTab(n.id)} className="topnav-item" style={{ display: "flex", alignItems: "center", gap: 7, padding: "7px 12px", borderRadius: 9, border: "none", cursor: "pointer", whiteSpace: "nowrap", fontFamily: "inherit", fontSize: 13.5, fontWeight: on ? 700 : 500, color: on ? "var(--accent)" : "var(--text-3)", ...(on ? { background: "var(--tint-indigo)" } : null) }}>
              <span style={{ display: "flex", color: on ? "var(--accent)" : "var(--text-4)" }}>{n.icon}</span>
              {n.label}
            </button>
          );
        })}
      </nav>

      <h1 className="mob-show" style={{ display: "none", flex: 1, minWidth: 0, fontSize: 16, fontWeight: 700, color: "var(--text)", letterSpacing: "-0.02em", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{title}</h1>

      <div style={{ display: "flex", alignItems: "center", gap: 8, flexShrink: 0 }}>
        <div title={fx.live ? `Курс ЦБ РФ на ${fx.date || "сегодня"}` : "Курс ЦБ РФ · резервные значения"} className="mob-hide" style={{ display: "flex", alignItems: "center", gap: 9, padding: "6px 11px", background: "var(--surface-2)", border: "1px solid var(--border)", borderRadius: 8 }}>
          <span style={{ fontSize: 11, fontWeight: 700, color: "var(--text-4)", textTransform: "uppercase", letterSpacing: ".04em" }}>Курс ЦБ</span>
          <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
            <span style={{ fontSize: 13, fontWeight: 600, color: "var(--text-3)" }}>$</span>
            <span style={{ fontSize: 14, fontWeight: 700, color: "var(--text)" }}>{fx.usd.toFixed(2)} ₽</span>
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
            <span style={{ fontSize: 13, fontWeight: 600, color: "var(--text-3)" }}>€</span>
            <span style={{ fontSize: 14, fontWeight: 700, color: "var(--text)" }}>{fx.eur.toFixed(2)} ₽</span>
          </div>
        </div>

        <div className="mob-show" title={fx.live ? `Курс ЦБ РФ на ${fx.date || "сегодня"}` : "Курс ЦБ РФ · резервные значения"} style={{ display: "none", alignItems: "center", gap: 7, padding: "5px 9px", background: "var(--surface-2)", border: "1px solid var(--border)", borderRadius: 8, flexShrink: 0 }}>
          <span style={{ fontSize: 12.5, fontWeight: 700, color: "var(--text-2)", whiteSpace: "nowrap" }}>$ {fx.usd.toFixed(1)}</span>
          <span style={{ fontSize: 12.5, fontWeight: 700, color: "var(--text-3)", whiteSpace: "nowrap" }}>€ {fx.eur.toFixed(1)}</span>
        </div>

        <div onClick={onToggleTheme} title="Сменить тему" style={{ width: 32, height: 32, background: "var(--track)", borderRadius: 8, display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer", flexShrink: 0 }}>
          {dark ? (
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" style={{ color: "var(--text-3)" }} strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="5" /><line x1="12" y1="1" x2="12" y2="3" /><line x1="12" y1="21" x2="12" y2="23" /><line x1="4.22" y1="4.22" x2="5.64" y2="5.64" /><line x1="18.36" y1="18.36" x2="19.78" y2="19.78" /><line x1="1" y1="12" x2="3" y2="12" /><line x1="21" y1="12" x2="23" y2="12" /><line x1="4.22" y1="19.78" x2="5.64" y2="18.36" /><line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
            </svg>
          ) : (
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" style={{ color: "var(--text-3)" }} strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z" />
            </svg>
          )}
        </div>
      </div>
    </div>
  );
}
