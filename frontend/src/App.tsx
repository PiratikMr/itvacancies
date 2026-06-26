import { lazy, Suspense, useEffect, useState } from "react";
import "./theme.css";
import { Sidebar } from "./components/Sidebar";
import { TopBar, type Fx } from "./components/TopBar";
import { Notice } from "./components/shared";
import { OverviewPage } from "./pages/OverviewPage";
import { SalaryPage } from "./pages/SalaryPage";
import { SkillsPage } from "./pages/SkillsPage";
import { EmployersPage } from "./pages/EmployersPage";
import { VacanciesPage } from "./pages/VacanciesPage";

// Geo pulls in Leaflet + glify — load it only when the tab is opened.
const GeoPage = lazy(() => import("./pages/GeoPage").then((m) => ({ default: m.GeoPage })));
import { api } from "./api/client";
import {
  DEFAULT_FILTERS, readUrl, writeUrl, TAB_PATHS, type Filters, type TabId,
} from "./state/filters";

const TITLES: Record<TabId, string> = {
  overview: "Обзор рынка", salary: "Зарплаты и грейды", skills: "Навыки",
  employers: "Работодатели", geo: "География", vacancies: "Вакансии",
};

// Per-section SEO metadata. `h1` is a visually-hidden heading for crawlers;
// title/description/canonical are updated as the user navigates.
const META: Record<TabId, { h1: string; title: string; desc: string }> = {
  overview: {
    h1: "Аналитика рынка IT-вакансий",
    title: "Аналитика рынка IT-вакансий — зарплаты, навыки, работодатели | IT Vacancies",
    desc: "Аналитика рынка IT-вакансий России: динамика публикаций, медианные зарплаты, грейды и доля удалённой работы. Данные обновляются ежедневно.",
  },
  salary: {
    h1: "Зарплаты и грейды в IT",
    title: "Зарплаты и грейды в IT — медианы по направлениям и опыту | IT Vacancies",
    desc: "Зарплаты и грейды в IT: медианные зарплаты по направлениям, опыту и грейдам — от стажёра до тимлида. Прозрачность зарплат и валюты.",
  },
  skills: {
    h1: "Востребованные навыки в IT",
    title: "Востребованные навыки в IT — статистика и зарплаты | IT Vacancies",
    desc: "Востребованные навыки в IT-вакансиях: частота упоминаний, медианные зарплаты по технологиям и требования к английскому языку.",
  },
  employers: {
    h1: "Работодатели на IT-рынке",
    title: "Работодатели в IT — кто активнее всего нанимает | IT Vacancies",
    desc: "Работодатели на IT-рынке: кто активнее всего нанимает, число вакансий, медианные зарплаты и динамика найма по компаниям.",
  },
  geo: {
    h1: "География IT-вакансий",
    title: "География IT-вакансий — карта по городам и странам | IT Vacancies",
    desc: "География IT-вакансий: карта по городам и странам, доля удалённой работы и медианные зарплаты по регионам.",
  },
  vacancies: {
    h1: "Актуальные IT-вакансии",
    title: "IT-вакансии — зарплаты, грейды, формат работы | IT Vacancies",
    desc: "Актуальные IT-вакансии: зарплаты, грейды, опыт, формат работы и площадки. Свежие вакансии с гибкими фильтрами.",
  },
};

const FX_FALLBACK: Fx = { usd: 79.2, eur: 92.45, live: false, date: "" };

export default function App() {
  const initial = readUrl(window.location.pathname, window.location.search);
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
    const m = META[tab];
    document.title = m.title;
    document.querySelector('meta[name="description"]')?.setAttribute("content", m.desc);
    document.querySelector('link[rel="canonical"]')?.setAttribute("href", `https://itvacancies.tech${TAB_PATHS[tab]}`);
  }, [tab]);

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
        filters={filters} onFilters={setFilters}
        facets={facets}
        salaryMax={salaryMax}
        onSearch={searchFacet}
        dataUpdatedAt={dataUpdatedAt}
        open={sidebarOpen}
      />

      {sidebarOpen && (
        <div onClick={() => setSidebarOpen(false)} style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,.45)", zIndex: 49, cursor: "pointer" }} />
      )}

      <main className="mob-main" style={{ marginLeft: 244, flex: 1, minHeight: "100vh", display: "flex", flexDirection: "column" }}>
        <TopBar
          title={TITLES[tab]} tab={tab} onTab={setTab}
          fx={fx} dark={dark}
          onToggleTheme={toggleTheme}
          onToggleSidebar={() => setSidebarOpen((v) => !v)}
        />

        <div className="mob-content" style={{ padding: "20px 28px", flex: 1 }}>
          <h1 className="sr-only">{META[tab].h1}</h1>
          {tab === "overview" && <OverviewPage filters={filters} />}
          {tab === "salary" && <SalaryPage filters={filters} />}
          {tab === "skills" && <SkillsPage filters={filters} />}
          {tab === "employers" && <EmployersPage filters={filters} />}
          {tab === "geo" && (
            <Suspense fallback={<Notice text="Загрузка…" />}>
              <GeoPage filters={filters} />
            </Suspense>
          )}
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
