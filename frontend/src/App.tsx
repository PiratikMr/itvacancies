import { useEffect, useState } from "react";
import "./theme.css";
import { Sidebar } from "./components/Sidebar";
import { TopBar, type Fx } from "./components/TopBar";
import { OverviewPage } from "./pages/OverviewPage";
import { SalaryPage } from "./pages/SalaryPage";
import { SkillsPage } from "./pages/SkillsPage";
import { EmployersPage } from "./pages/EmployersPage";
import { GeoPage } from "./pages/GeoPage";
import { VacanciesPage } from "./pages/VacanciesPage";
import { api } from "./api/client";
import {
  DEFAULT_FILTERS, readUrl, writeUrl, type Filters, type TabId,
} from "./state/filters";

const TITLES: Record<TabId, string> = {
  overview: "Обзор рынка", salary: "Зарплаты и грейды", skills: "Навыки",
  employers: "Работодатели", geo: "География", vacancies: "Вакансии",
};
const SUBS: Record<TabId, string> = {
  overview: "IT-вакансии", salary: "Оплата труда", skills: "Востребованные технологии",
  employers: "Топ работодателей", geo: "Страны и города", vacancies: "Актуальные вакансии",
};

const FX_FALLBACK: Fx = { usd: 79.2, eur: 92.45, live: false, date: "" };

export default function App() {
  const initial = readUrl(window.location.search);
  const [tab, setTab] = useState<TabId>(initial.tab);
  const [filters, setFilters] = useState<Filters>({ ...DEFAULT_FILTERS, ...initial.filters });
  const [dark, setDark] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [fx, setFx] = useState<Fx>(FX_FALLBACK);
  const [dataUpdatedAt, setDataUpdatedAt] = useState<string | null>(null);
  const [facets, setFacets] = useState<Record<string, string[]>>({});
  const [salaryMax, setSalaryMax] = useState<number | null>(null);

  useEffect(() => {
    try {
      const d = localStorage.getItem("itscope-theme") === "dark";
      setDark(d);
      document.body.classList.toggle("dark", d);
    } catch {  }

    api.meta()
      .then((m) => {
        setDataUpdatedAt(m.data_updated_at);
        if (m.fx && m.fx.usd && m.fx.eur) {
          setFx({ usd: m.fx.usd, eur: m.fx.eur, live: true, date: (m.data_updated_at || "").slice(0, 10) });
        }
      })
      .catch(() => {});
  }, []);

  useEffect(() => { writeUrl(tab, filters); }, [tab, filters]);

  useEffect(() => {
    const ctrl = new AbortController();
    const t = setTimeout(() => {
      api.facets(filters, ctrl.signal)
        .then((r) => { setFacets(r.facets); if (r.salary?.max != null) setSalaryMax(r.salary.max); })
        .catch(() => {});
    }, 250);
    return () => { clearTimeout(t); ctrl.abort(); };
  }, [filters]);

  const searchFacet = (field: string, q: string) => api.suggest(filters, field, q).then((r) => r.values);

  const toggleTheme = () => {
    const d = !dark;
    setDark(d);
    try { localStorage.setItem("itscope-theme", d ? "dark" : "light"); } catch {  }
    document.body.classList.toggle("dark", d);
  };

  return (
    <div style={{ display: "flex", minHeight: "100vh", fontSize: 14, lineHeight: 1.5 }}>
      <Sidebar
        tab={tab} onTab={setTab}
        filters={filters} onFilters={setFilters}
        facets={facets}
        salaryMax={salaryMax}
        onSearch={searchFacet}
        dataUpdatedAt={dataUpdatedAt}
        open={sidebarOpen} onClose={() => setSidebarOpen(false)}
      />

      {sidebarOpen && (
        <div onClick={() => setSidebarOpen(false)} style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,.45)", zIndex: 49, cursor: "pointer" }} />
      )}

      <main className="mob-main" style={{ marginLeft: 244, flex: 1, minHeight: "100vh", display: "flex", flexDirection: "column" }}>
        <TopBar
          title={TITLES[tab]} subtitle={SUBS[tab]}
          fx={fx} dark={dark}
          onToggleTheme={toggleTheme}
          onToggleSidebar={() => setSidebarOpen((v) => !v)}
        />

        <div className="mob-content" style={{ padding: "20px 28px", flex: 1 }}>
          {tab === "overview" && <OverviewPage filters={filters} />}
          {tab === "salary" && <SalaryPage filters={filters} />}
          {tab === "skills" && <SkillsPage filters={filters} />}
          {tab === "employers" && <EmployersPage filters={filters} />}
          {tab === "geo" && <GeoPage filters={filters} />}
          {tab === "vacancies" && <VacanciesPage filters={filters} />}
        </div>
      </main>

      <BottomNav tab={tab} onTab={(t) => { setTab(t); setSidebarOpen(false); }} />
    </div>
  );
}

const BNAV: { id: TabId; label: string }[] = [
  { id: "overview", label: "Обзор" }, { id: "salary", label: "Зарплаты" },
  { id: "skills", label: "Навыки" }, { id: "employers", label: "Компании" },
  { id: "geo", label: "Гео" }, { id: "vacancies", label: "Вакансии" },
];

function BottomNav({ tab, onTab }: { tab: TabId; onTab: (t: TabId) => void }) {
  return (
    <nav className="mob-bnav" style={{ display: "none", position: "fixed", bottom: 0, left: 0, right: 0, background: "var(--surface)", borderTop: "1px solid var(--border)", zIndex: 60, alignItems: "stretch" }}>
      {BNAV.map((n) => (
        <div key={n.id} onClick={() => onTab(n.id)} style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 2, padding: "7px 2px", cursor: "pointer", color: tab === n.id ? "var(--accent)" : "var(--text-3)" }}>
          <span style={{ fontSize: 11, fontWeight: 600 }}>{n.label}</span>
        </div>
      ))}
    </nav>
  );
}
