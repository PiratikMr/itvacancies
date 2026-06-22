export interface Fx { usd: number; eur: number; live: boolean; date: string }

export function TopBar({
  title, subtitle, fx, dark, onToggleTheme, onToggleSidebar,
}: {
  title: string;
  subtitle: string;
  fx: Fx;
  dark: boolean;
  onToggleTheme: () => void;
  onToggleSidebar: () => void;
}) {
  return (
    <div className="mob-topbar" style={{ background: "var(--surface)", borderBottom: "1px solid var(--border)", padding: "0 28px", height: 52, display: "flex", alignItems: "center", justifyContent: "space-between", position: "sticky", top: 0, zIndex: 40 }}>
      <div className="mob-show" onClick={onToggleSidebar} style={{ display: "none", width: 36, height: 36, borderRadius: 8, background: "var(--track)", alignItems: "center", justifyContent: "center", cursor: "pointer", flexShrink: 0 }}>
        <svg width="17" height="13" viewBox="0 0 17 13" fill="currentColor" style={{ color: "var(--text-2)" }}><rect width="17" height="2.2" rx="1.1" /><rect y="5.4" width="12" height="2.2" rx="1.1" /><rect y="10.8" width="17" height="2.2" rx="1.1" /></svg>
      </div>

      <div style={{ display: "flex", alignItems: "baseline", gap: 10, flex: 1, minWidth: 0 }}>
        <h1 style={{ fontSize: 17, fontWeight: 700, color: "var(--text)", letterSpacing: "-0.02em", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{title}</h1>
        <span className="mob-page-sub" style={{ fontSize: 13, color: "var(--text-4)" }}>{subtitle}</span>
      </div>

      <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
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

        <div onClick={onToggleTheme} title="Сменить тему" style={{ width: 32, height: 32, background: "var(--track)", borderRadius: 8, display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer" }}>
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
