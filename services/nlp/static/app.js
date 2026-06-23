const Icon = ({ name, size = 16, className = "" }) => {
  const props = {
    width: size, height: size, viewBox: "0 0 24 24", fill: "none",
    stroke: "currentColor", strokeWidth: 1.5,
    strokeLinecap: "round", strokeLinejoin: "round", className,
  };
  const paths = {
    search:    <><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></>,
    play:      <polygon points="6 4 20 12 6 20 6 4"/>,
    check:     <path d="M5 12l5 5L20 7"/>,
    x:         <><path d="M18 6 6 18"/><path d="m6 6 12 12"/></>,
    trash:     <><path d="M3 6h18"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/></>,
    sun:       <><circle cx="12" cy="12" r="4"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="m4.93 4.93 1.41 1.41"/><path d="m17.66 17.66 1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="m6.34 17.66-1.41 1.41"/><path d="m19.07 4.93-1.41 1.41"/></>,
    moon:      <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>,
    chevron:   <path d="m6 9 6 6 6-6"/>,
    sparkles:  <><path d="M12 3v3m0 12v3M3 12h3m12 0h3M5.6 5.6l2.1 2.1m8.6 8.6 2.1 2.1M5.6 18.4l2.1-2.1m8.6-8.6 2.1-2.1"/></>,
    filter:    <path d="M22 3H2l8 9.5V19l4 2v-8.5L22 3z"/>,
    undo:      <><path d="M3 7v6h6"/><path d="M21 17a9 9 0 0 0-15-6.7L3 13"/></>,
    arrowRight:<><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></>,
    refresh:   <><path d="M21 12a9 9 0 0 1-9 9 9 9 0 0 1-7.4-3.9"/><path d="M3 12a9 9 0 0 1 9-9 9 9 0 0 1 7.4 3.9"/><path d="M21 4v5h-5"/><path d="M3 20v-5h5"/></>,
    database:  <><ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M3 5v14a9 3 0 0 0 18 0V5"/><path d="M3 12a9 3 0 0 0 18 0"/></>,
    pencil:    <><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/></>,
    star:      <polygon points="12 2 15 9 22 9.3 16.5 14 18.5 21 12 17 5.5 21 7.5 14 2 9.3 9 9"/>,
    chevronL:  <path d="m15 18-6-6 6-6"/>,
    chevronR:  <path d="m9 18 6-6-6-6"/>,
  };
  return <svg {...props}>{paths[name]}</svg>;
};

const scoreLevel = (s) => (s >= 0.92 ? "high" : s >= 0.85 ? "med" : "low");

function ScorePill({ score }) {
  const lvl = scoreLevel(score);
  const pct = Math.round(score * 100);
  return <span className={`score-pill score-${lvl}`}>{pct}<i>%</i></span>;
}

async function fetchJSON(url, opts) {
  const resp = await fetch(url, opts);
  return resp.json();
}

function App() {
  const [theme, setTheme] = React.useState(() => localStorage.getItem("nlp-theme") || "light");
  const [mode, setMode] = React.useState(() => localStorage.getItem("nlp-mode") || "dedupe");
  const [dimensions, setDimensions] = React.useState([]);
  const [selectedDim, setSelectedDim] = React.useState(null);
  const [thresholds, setThresholds] = React.useState({});
  const [matchData, setMatchData] = React.useState(null);
  const [loading, setLoading] = React.useState(false);
  const [loadingMsg, setLoadingMsg] = React.useState("");
  const [refreshing, setRefreshing] = React.useState(false);
  const [removed, setRemoved] = React.useState(new Set());
  const [search, setSearch] = React.useState("");
  const [sort, setSort] = React.useState("score-desc");
  const [selected, setSelected] = React.useState(new Set());
  const [history, setHistory] = React.useState([]);
  const [toasts, setToasts] = React.useState([]);

  React.useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("nlp-theme", theme);
  }, [theme]);

  React.useEffect(() => { localStorage.setItem("nlp-mode", mode); }, [mode]);

  const pushToast = React.useCallback((kind, msg) => {
    const id = Math.random().toString(36).slice(2);
    setToasts((ts) => [...ts, { id, kind, msg }]);
    setTimeout(() => setToasts((ts) => ts.filter((x) => x.id !== id)), 3500);
  }, []);

  const applyDimensionsPayload = React.useCallback((dims) => {
    setDimensions(dims);
    setThresholds(Object.fromEntries(dims.map((d) => [d.name, d.threshold])));
    setSelectedDim((cur) => cur && dims.some((d) => d.name === cur) ? cur : dims[0]?.name || null);
  }, []);

  const loadDimensions = React.useCallback(async (showToast) => {
    try {
      const data = await fetchJSON("/api/dimensions");
      if (!data.ok) throw new Error(data.error || "unknown");
      applyDimensionsPayload(data.dimensions);
      if (showToast) pushToast("success", "Данные обновлены");
    } catch (e) {
      pushToast("error", "Не удалось загрузить измерения: " + e.message);
    }
  }, [applyDimensionsPayload, pushToast]);

  React.useEffect(() => { loadDimensions(false); }, [loadDimensions]);

  const refresh = async () => {
    setRefreshing(true);
    try {
      const data = await fetchJSON("/api/refresh", { method: "POST" });
      if (!data.ok) throw new Error(data.error || "unknown");
      applyDimensionsPayload(data.dimensions);
      pushToast("success", "Данные обновлены");
    } catch (e) {
      pushToast("error", "Ошибка обновления: " + e.message);
    } finally {
      setTimeout(() => setRefreshing(false), 700);
    }
  };

  const dim = dimensions.find((d) => d.name === selectedDim);
  const currentThreshold = thresholds[selectedDim];

  const setThreshold = (name, val) => {
    const v = Math.max(0.5, Math.min(0.99, Number(val) || 0));
    setThresholds((t) => ({ ...t, [name]: v }));
  };
  const bumpThreshold = (delta) =>
    setThreshold(selectedDim, +(currentThreshold + delta).toFixed(2));

  const matchKey = (g, c) => `${g}:${c}`;

  const resetResults = () => {
    setMatchData(null);
    setRemoved(new Set());
    setSelected(new Set());
    setHistory([]);
    setSearch("");
  };

  const switchMode = (m) => {
    if (m === mode) return;
    if (mode === "dedupe") resetResults();
    setMode(m);
  };

  const runMatch = async () => {
    if (!selectedDim) return;
    setLoadingMsg(`Сопоставление для «${dim?.label || selectedDim}»…`);
    setLoading(true);
    try {
      const data = await fetchJSON("/api/match", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ dimension: selectedDim, threshold: currentThreshold }),
      });
      if (!data.ok) throw new Error(data.error || "unknown");
      setMatchData(data);
      setRemoved(new Set());
      setSelected(new Set());
      setHistory([]);
      setSearch("");
      pushToast(
        "info",
        `Найдено ${data.stats.total_matches} совпадений в ${data.groups.length} группах`,
      );
    } catch (e) {
      pushToast("error", "Ошибка матчинга: " + e.message);
    } finally {
      setLoading(false);
    }
  };

  const allPairs = React.useMemo(() => {
    const out = [];
    if (matchData) {
      for (const g of matchData.groups) for (const c of g.candidates) out.push({ g, c });
    }
    return out;
  }, [matchData]);

  const visiblePairs = React.useMemo(() => {
    const q = search.toLowerCase();
    let pairs = allPairs.filter(({ g, c }) => !removed.has(matchKey(g.golden_id, c.id)));
    if (q) {
      pairs = pairs.filter(({ g, c }) =>
        g.golden_name.toLowerCase().includes(q) || c.name.toLowerCase().includes(q),
      );
    }
    const groupSize = (gid) =>
      allPairs.filter((p) => p.g.golden_id === gid && !removed.has(matchKey(gid, p.c.id))).length;
    const cmp = {
      "score-desc": (a, b) => b.c.score - a.c.score,
      "score-asc":  (a, b) => a.c.score - b.c.score,
      "name-asc":   (a, b) =>
        a.g.golden_name.localeCompare(b.g.golden_name) || a.c.name.localeCompare(b.c.name),
      "group-desc": (a, b) =>
        groupSize(b.g.golden_id) - groupSize(a.g.golden_id) ||
        a.g.golden_name.localeCompare(b.g.golden_name),
    }[sort];
    return [...pairs].sort(cmp);
  }, [allPairs, removed, search, sort]);

  const groupedView = React.useMemo(() => {
    const m = new Map();
    visiblePairs.forEach(({ g, c }) => {
      if (!m.has(g.golden_id)) m.set(g.golden_id, { g, candidates: [] });
      m.get(g.golden_id).candidates.push(c);
    });
    return [...m.values()];
  }, [visiblePairs]);

  const totalActive = allPairs.length - removed.size;

  const removeOne = (gid, cid) => {
    const k = matchKey(gid, cid);
    setHistory((h) => [...h, { keys: [k] }]);
    setRemoved((s) => new Set(s).add(k));
    setSelected((s) => { const n = new Set(s); n.delete(k); return n; });
  };

  const removeGroup = (gid) => {
    const keys = allPairs
      .filter((p) => p.g.golden_id === gid && !removed.has(matchKey(gid, p.c.id)))
      .map((p) => matchKey(gid, p.c.id));
    if (!keys.length) return;
    setHistory((h) => [...h, { keys }]);
    setRemoved((s) => { const n = new Set(s); keys.forEach((k) => n.add(k)); return n; });
    setSelected((s) => { const n = new Set(s); keys.forEach((k) => n.delete(k)); return n; });
  };

  const removeSelected = () => {
    if (!selected.size) return;
    const keys = [...selected];
    setHistory((h) => [...h, { keys }]);
    setRemoved((s) => { const n = new Set(s); keys.forEach((k) => n.add(k)); return n; });
    setSelected(new Set());
  };

  const undo = () => {
    if (!history.length) return;
    const last = history[history.length - 1];
    setHistory((h) => h.slice(0, -1));
    setRemoved((s) => { const n = new Set(s); last.keys.forEach((k) => n.delete(k)); return n; });
    pushToast("info", `Восстановлено ${last.keys.length}`);
  };

  const toggleSelect = (gid, cid) => {
    const k = matchKey(gid, cid);
    setSelected((s) => {
      const n = new Set(s);
      if (n.has(k)) n.delete(k); else n.add(k);
      return n;
    });
  };

  const toggleSelectGroup = (gid) => {
    const keys = groupedView.find((x) => x.g.golden_id === gid)?.candidates
      .map((c) => matchKey(gid, c.id)) || [];
    const allSelected = keys.length > 0 && keys.every((k) => selected.has(k));
    setSelected((s) => {
      const n = new Set(s);
      keys.forEach((k) => allSelected ? n.delete(k) : n.add(k));
      return n;
    });
  };

  const apply = async () => {
    if (!matchData || !totalActive) return;
    if (!confirm(`Применить ${totalActive} объединений к базе данных? Это действие нельзя отменить.`)) return;

    const merges = [];
    for (const g of matchData.groups) {
      for (const c of g.candidates) {
        if (!removed.has(matchKey(g.golden_id, c.id))) {
          merges.push({ candidate_id: c.id, golden_id: g.golden_id });
        }
      }
    }

    setLoadingMsg(`Применение ${merges.length} объединений…`);
    setLoading(true);
    try {
      const data = await fetchJSON("/api/apply", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ dimension: matchData.dimension, merges }),
      });
      if (!data.ok) throw new Error(data.error || "unknown");
      pushToast("success", `Применено ${data.applied} объединений`);
      resetResults();
      loadDimensions(false);
    } catch (e) {
      pushToast("error", "Ошибка: " + e.message);
    } finally {
      setLoading(false);
    }
  };

  const cancel = () => {
    resetResults();
    pushToast("info", "Отменено");
  };

  if (!dimensions.length) {
    return (
      <div className="app">
        <section className="placeholder">
          <div className="placeholder-inner">
            <span className="spinner spinner-lg" />
            <h3>Загрузка измерений…</h3>
          </div>
        </section>
      </div>
    );
  }

  if (!dim) return null;

  return (
    <div className="app">
      <div className="mode-switch" role="tablist">
        <button
          role="tab"
          aria-selected={mode === "dedupe"}
          className={`mode-btn ${mode === "dedupe" ? "active" : ""}`}
          onClick={() => switchMode("dedupe")}
        >
          <Icon name="sparkles" size={14} /> Дедупликация
        </button>
        <button
          role="tab"
          aria-selected={mode === "manage"}
          className={`mode-btn ${mode === "manage" ? "active" : ""}`}
          onClick={() => switchMode("manage")}
        >
          <Icon name="database" size={14} /> Справочник
        </button>
      </div>

      <nav className="dim-tabs" role="tablist">
        {dimensions.map((d) => (
          <button
            key={d.name}
            role="tab"
            aria-selected={selectedDim === d.name}
            className={`dim-tab ${selectedDim === d.name ? "active" : ""}`}
            onClick={() => {
              if (d.name !== selectedDim) {
                setSelectedDim(d.name);
                resetResults();
              }
            }}
          >
            <span className="dim-tab-label">{d.label}</span>
            <span className="dim-tab-stats">
              {d.error ? (
                <span className="err" title={d.error}>ошибка БД</span>
              ) : (
                <>
                  <span><span className="num">{d.ref_count.toLocaleString("ru")}</span> эт.</span>
                  <span className="dim-tab-sep">·</span>
                  <span><span className="num">{d.cand_count.toLocaleString("ru")}</span> канд.</span>
                </>
              )}
            </span>
          </button>
        ))}
      </nav>

      {mode === "manage" ? (
        <ManageView
          dim={dim}
          pushToast={pushToast}
          onChanged={() => loadDimensions(false)}
        />
      ) : (
      <React.Fragment>
      <div className="meta">
        <div className="meta-stats">
          <div className="meta-stat">
            <span className="meta-label">Эталонные</span>
            <span className="meta-value num">{dim.ref_count.toLocaleString("ru")}</span>
          </div>
          <div className="meta-stat">
            <span className="meta-label">Кандидаты</span>
            <span className="meta-value num">{dim.cand_count.toLocaleString("ru")}</span>
          </div>
          <div className="meta-stat meta-threshold">
            <span className="meta-label">Порог сходства</span>
            <div className="threshold-control">
              <button
                className="th-btn"
                onClick={() => bumpThreshold(-0.01)}
                disabled={!!matchData || currentThreshold <= 0.5}
                title="Уменьшить"
              >−</button>
              <input
                type="number"
                className="th-input num"
                min="0.5" max="0.99" step="0.01"
                value={currentThreshold.toFixed(2)}
                disabled={!!matchData}
                onChange={(e) => setThreshold(selectedDim, e.target.value)}
              />
              <button
                className="th-btn"
                onClick={() => bumpThreshold(0.01)}
                disabled={!!matchData || currentThreshold >= 0.99}
                title="Увеличить"
              >+</button>
            </div>
          </div>
        </div>
        <div className="meta-actions">
          {matchData && (
            <button className="btn btn-ghost" onClick={cancel} title="Сбросить результаты">
              <Icon name="x" /> Сбросить
            </button>
          )}
          <button className="btn btn-primary" onClick={runMatch} disabled={loading}>
            {loading ? <span className="spinner" /> : <Icon name="play" />}
            {matchData ? "Перезапустить" : "Запустить матчинг"}
          </button>
        </div>
      </div>

      {matchData ? (
        <section className="results">
          <div className="toolbar">
            <div className="toolbar-left">
              <div className="search">
                <Icon name="search" size={14} />
                <input
                  type="text"
                  placeholder="Фильтр по названию…"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
                {search && (
                  <button className="search-clear" onClick={() => setSearch("")}>
                    <Icon name="x" size={12} />
                  </button>
                )}
              </div>
              <div className="sort">
                <Icon name="filter" size={14} />
                <select value={sort} onChange={(e) => setSort(e.target.value)}>
                  <option value="score-desc">Score ↓</option>
                  <option value="score-asc">Score ↑</option>
                  <option value="name-asc">По имени</option>
                  <option value="group-desc">По размеру группы</option>
                </select>
                <Icon name="chevron" size={14} className="sort-chev" />
              </div>
            </div>
            <div className="toolbar-right">
              {selected.size > 0 && (
                <>
                  <span className="sel-count">{selected.size} выбрано</span>
                  <button className="btn btn-ghost danger" onClick={removeSelected}>
                    <Icon name="x" size={14} /> Отклонить выбранные
                  </button>
                </>
              )}
              {history.length > 0 && (
                <button className="btn btn-ghost" onClick={undo} title="Отменить последнее">
                  <Icon name="undo" size={14} /> Отменить
                </button>
              )}
            </div>
          </div>

          {visiblePairs.length === 0 ? (
            <div className="empty">
              <Icon name="search" size={28} />
              <p>{search ? `Ничего не найдено по «${search}»` : "Все совпадения отклонены"}</p>
            </div>
          ) : (
            <ResultsTable
              groups={groupedView}
              selected={selected}
              toggleSelect={toggleSelect}
              toggleSelectGroup={toggleSelectGroup}
              removeOne={removeOne}
              removeGroup={removeGroup}
              matchKey={matchKey}
            />
          )}
        </section>
      ) : (
        <section className="placeholder">
          <div className="placeholder-inner">
            <Icon name="database" size={28} />
            <h3>Готово к матчингу</h3>
            <p>
              Выбрано измерение <strong>«{dim.label}»</strong>. Запустите матчинг, чтобы найти
              кандидатов для эталонных значений по порогу {currentThreshold.toFixed(2)}.
            </p>
            <button className="btn btn-primary" onClick={runMatch} disabled={loading}>
              {loading ? <span className="spinner" /> : <Icon name="play" />}
              Запустить матчинг
            </button>
          </div>
        </section>
      )}

      {matchData && totalActive > 0 && (
        <div className="apply-bar">
          <div className="apply-info">
            <span className="apply-info-label">К применению</span>
            <span className="apply-info-num num">{totalActive}</span>
            <span className="apply-info-sub">
              {totalActive === 1 ? "объединение" : "объединений"}
            </span>
          </div>
          <div className="apply-actions">
            <button className="btn btn-ghost" onClick={cancel}>Отмена</button>
            <button className="btn btn-primary" onClick={apply}>
              <Icon name="check" /> Применить
            </button>
          </div>
        </div>
      )}
      </React.Fragment>
      )}

      <div className="util-dock">
        <button
          className={`util-btn ${refreshing ? "spinning" : ""}`}
          title="Обновить данные из базы"
          onClick={refresh}
          disabled={refreshing}
        >
          <Icon name="refresh" size={14} />
        </button>
        <button
          className="util-btn"
          title={theme === "light" ? "Тёмная тема" : "Светлая тема"}
          onClick={() => setTheme(theme === "light" ? "dark" : "light")}
        >
          <Icon name={theme === "light" ? "moon" : "sun"} size={14} />
        </button>
      </div>

      <div className="toasts">
        {toasts.map((tt) => (
          <div key={tt.id} className={`toast toast-${tt.kind}`}>
            <Icon name={tt.kind === "success" ? "check" : tt.kind === "error" ? "x" : "sparkles"} size={14} />
            <span>{tt.msg}</span>
          </div>
        ))}
      </div>

      {loading && (
        <div className="loading">
          <div className="loading-card">
            <span className="spinner spinner-lg" />
            <span>{loadingMsg || "Загрузка…"}</span>
          </div>
        </div>
      )}
    </div>
  );
}

function ResultsTable({ groups, selected, toggleSelect, toggleSelectGroup, removeOne, removeGroup, matchKey }) {
  return (
    <div className="table-wrap">
      <table className="results-table">
        <thead>
          <tr>
            <th className="col-check"></th>
            <th className="col-golden">Эталон</th>
            <th className="col-arrow"></th>
            <th className="col-cand">Кандидат</th>
            <th className="col-score">Score</th>
            <th className="col-actions"></th>
          </tr>
        </thead>
        <tbody>
          {groups.map(({ g, candidates }) => {
            const groupKeys = candidates.map((c) => matchKey(g.golden_id, c.id));
            const allSelected = groupKeys.length > 0 && groupKeys.every((k) => selected.has(k));
            const someSelected = !allSelected && groupKeys.some((k) => selected.has(k));
            return (
              <React.Fragment key={g.golden_id}>
                {candidates.map((c, i) => {
                  const k = matchKey(g.golden_id, c.id);
                  const isSel = selected.has(k);
                  return (
                    <tr
                      key={c.id}
                      className={`row ${isSel ? "row-selected" : ""} ${i === 0 ? "row-first" : ""} ${i === candidates.length - 1 ? "row-last" : ""}`}
                    >
                      <td className="col-check">
                        {i === 0 ? (
                          <Checkbox
                            checked={allSelected}
                            indeterminate={someSelected}
                            onChange={() => toggleSelectGroup(g.golden_id)}
                          />
                        ) : (
                          <Checkbox
                            checked={isSel}
                            onChange={() => toggleSelect(g.golden_id, c.id)}
                          />
                        )}
                      </td>
                      <td className="col-golden">
                        {i === 0 ? (
                          <div className="golden-cell">
                            <span className="golden-name">{g.golden_name}</span>
                            <span className="golden-meta">
                              <span className="badge-count">{candidates.length}</span>
                              <button
                                className="cell-btn"
                                onClick={() => removeGroup(g.golden_id)}
                                title="Отклонить группу"
                              >
                                <Icon name="trash" size={12} />
                              </button>
                            </span>
                          </div>
                        ) : (
                          <span className="golden-cont" />
                        )}
                      </td>
                      <td className="col-arrow">
                        <Icon name="arrowRight" size={12} className="arrow-dim" />
                      </td>
                      <td className="col-cand">
                        <span className="cand-name">{c.name}</span>
                      </td>
                      <td className="col-score">
                        <ScorePill score={c.score} />
                      </td>
                      <td className="col-actions">
                        <button
                          className="row-x"
                          onClick={() => removeOne(g.golden_id, c.id)}
                          title="Отклонить"
                        >
                          <Icon name="x" size={14} />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </React.Fragment>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

function Checkbox({ checked, indeterminate, onChange }) {
  const ref = React.useRef(null);
  React.useEffect(() => {
    if (ref.current) ref.current.indeterminate = !!indeterminate;
  }, [indeterminate]);
  return (
    <label className="cb">
      <input ref={ref} type="checkbox" checked={!!checked} onChange={onChange} />
      <span className="cb-box">
        {checked && !indeterminate && <Icon name="check" size={11} />}
        {indeterminate && <span className="cb-dash" />}
      </span>
    </label>
  );
}

function ManageView({ dim, pushToast, onChanged }) {
  const LIMIT = 100;
  const [rows, setRows] = React.useState([]);
  const [total, setTotal] = React.useState(0);
  const [loading, setLoading] = React.useState(false);
  const [busy, setBusy] = React.useState(false);
  const [searchInput, setSearchInput] = React.useState("");
  const [search, setSearch] = React.useState("");
  const [sort, setSort] = React.useState("mentions-desc");
  const [ref, setRef] = React.useState("all");
  const [minC, setMinC] = React.useState("");
  const [maxC, setMaxC] = React.useState("");
  const [page, setPage] = React.useState(0);
  const [selected, setSelected] = React.useState(new Set());
  const [editing, setEditing] = React.useState(null);
  const [editValue, setEditValue] = React.useState("");

  React.useEffect(() => {
    const t = setTimeout(() => setSearch(searchInput.trim()), 300);
    return () => clearTimeout(t);
  }, [searchInput]);

  React.useEffect(() => { setPage(0); }, [dim.name, search, sort, ref, minC, maxC]);
  React.useEffect(() => { setSelected(new Set()); setEditing(null); }, [dim.name]);

  const load = React.useCallback(async () => {
    setLoading(true);
    try {
      const [sortKey, dir] = sort.split("-");
      const params = new URLSearchParams({
        dimension: dim.name, search, ref, sort: sortKey, dir,
        limit: String(LIMIT), offset: String(page * LIMIT),
      });
      if (minC !== "") params.set("min", minC);
      if (maxC !== "") params.set("max", maxC);
      const data = await fetchJSON("/api/records?" + params.toString());
      if (!data.ok) throw new Error(data.error || "unknown");
      setRows(data.rows);
      setTotal(data.total);
    } catch (e) {
      pushToast("error", "Не удалось загрузить записи: " + e.message);
    } finally {
      setLoading(false);
    }
  }, [dim.name, search, ref, sort, minC, maxC, page, pushToast]);

  React.useEffect(() => { load(); }, [load]);

  const post = (url, body) => fetchJSON(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  const toggleSelect = (id) => setSelected((s) => {
    const n = new Set(s);
    if (n.has(id)) n.delete(id); else n.add(id);
    return n;
  });
  const allOnPage = rows.length > 0 && rows.every((r) => selected.has(r.id));
  const someOnPage = !allOnPage && rows.some((r) => selected.has(r.id));
  const toggleSelectAll = () => setSelected((s) => {
    const n = new Set(s);
    rows.forEach((r) => allOnPage ? n.delete(r.id) : n.add(r.id));
    return n;
  });

  const verify = async (ids, value) => {
    if (!ids.length) return;
    setBusy(true);
    try {
      const data = await post("/api/records/verify", { dimension: dim.name, ids, value });
      if (!data.ok) throw new Error(data.error || "unknown");
      pushToast("success", `${value ? "Помечено эталоном" : "Снят эталон"}: ${data.updated}`);
      setSelected(new Set());
      await load();
      onChanged && onChanged();
    } catch (e) {
      pushToast("error", "Ошибка: " + e.message);
    } finally { setBusy(false); }
  };

  const remove = async (ids) => {
    if (!ids.length) return;
    if (!confirm(`Удалить ${ids.length} ${ids.length === 1 ? "запись" : "записей"} вместе со связями? Действие необратимо.`)) return;
    setBusy(true);
    try {
      const data = await post("/api/records/delete", { dimension: dim.name, ids });
      if (!data.ok) throw new Error(data.error || "unknown");
      pushToast("success", `Удалено: ${data.deleted}`);
      setSelected(new Set());
      await load();
      onChanged && onChanged();
    } catch (e) {
      pushToast("error", "Ошибка: " + e.message);
    } finally { setBusy(false); }
  };

  const startEdit = (r) => { setEditing(r.id); setEditValue(r.name); };
  const cancelEdit = () => { setEditing(null); setEditValue(""); };
  const saveEdit = async (r) => {
    const name = editValue.trim();
    if (!name || name === r.name) { cancelEdit(); return; }
    setBusy(true);
    try {
      const data = await post("/api/records/rename", { dimension: dim.name, id: r.id, name });
      if (!data.ok) throw new Error(data.error || "unknown");
      pushToast("success", "Переименовано");
      cancelEdit();
      await load();
      onChanged && onChanged();
    } catch (e) {
      pushToast("error", "Ошибка: " + e.message);
    } finally { setBusy(false); }
  };

  const setPreset = (lo, hi) => { setMinC(lo); setMaxC(hi); };

  const from = total === 0 ? 0 : page * LIMIT + 1;
  const to = page * LIMIT + rows.length;
  const hasPrev = page > 0;
  const hasNext = (page + 1) * LIMIT < total;

  return (
    <section className="manage">
      <div className="toolbar">
        <div className="toolbar-left">
          <div className="search">
            <Icon name="search" size={14} />
            <input
              type="text"
              placeholder="Поиск…"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
            />
            {searchInput && (
              <button className="search-clear" onClick={() => setSearchInput("")}>
                <Icon name="x" size={12} />
              </button>
            )}
          </div>
          <div className="sort">
            <Icon name="filter" size={14} />
            <select value={sort} onChange={(e) => setSort(e.target.value)}>
              <option value="mentions-desc">Упоминания ↓</option>
              <option value="mentions-asc">Упоминания ↑</option>
              <option value="name-asc">Имя A→Я</option>
              <option value="name-desc">Имя Я→A</option>
              <option value="id-asc">ID</option>
            </select>
            <Icon name="chevron" size={14} className="sort-chev" />
          </div>
          <div className="sort">
            <select value={ref} onChange={(e) => setRef(e.target.value)}>
              <option value="all">Все</option>
              <option value="ref">Эталоны</option>
              <option value="cand">Кандидаты</option>
            </select>
            <Icon name="chevron" size={14} className="sort-chev" />
          </div>
          <div className="range">
            <span className="range-label">упом.</span>
            <input type="number" min="0" className="num range-num" placeholder="0"
              value={minC} onChange={(e) => setMinC(e.target.value)} />
            <span className="range-dash">–</span>
            <input type="number" min="0" className="num range-num" placeholder="∞"
              value={maxC} onChange={(e) => setMaxC(e.target.value)} />
            <button className="chip" onClick={() => setPreset("0", "0")}>0</button>
            <button className="chip" onClick={() => setPreset("0", "2")}>≤2</button>
            {(minC !== "" || maxC !== "") && (
              <button className="chip" onClick={() => setPreset("", "")}>сброс</button>
            )}
          </div>
        </div>
      </div>

      <div className="table-wrap">
        <table className="records-table">
          <thead>
            <tr>
              <th className="col-check">
                <Checkbox checked={allOnPage} indeterminate={someOnPage} onChange={toggleSelectAll} />
              </th>
              <th className="col-name">Название</th>
              <th className="col-ment">Упом.</th>
              <th className="col-ref">Эталон</th>
              <th className="col-actions"></th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id} className={`row ${selected.has(r.id) ? "row-selected" : ""}`}>
                <td className="col-check">
                  <Checkbox checked={selected.has(r.id)} onChange={() => toggleSelect(r.id)} />
                </td>
                <td className="col-name">
                  {editing === r.id ? (
                    <div className="edit-cell">
                      <input
                        autoFocus
                        className="edit-input"
                        value={editValue}
                        onChange={(e) => setEditValue(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === "Enter") saveEdit(r);
                          if (e.key === "Escape") cancelEdit();
                        }}
                      />
                      <button className="cell-btn" onClick={() => saveEdit(r)} title="Сохранить">
                        <Icon name="check" size={13} />
                      </button>
                      <button className="cell-btn" onClick={cancelEdit} title="Отмена">
                        <Icon name="x" size={13} />
                      </button>
                    </div>
                  ) : (
                    <div className="name-cell">
                      <span className="rec-name">{r.name}</span>
                      <button className="cell-btn rec-edit" onClick={() => startEdit(r)} title="Переименовать">
                        <Icon name="pencil" size={12} />
                      </button>
                    </div>
                  )}
                </td>
                <td className="col-ment num">{r.mentions.toLocaleString("ru")}</td>
                <td className="col-ref">
                  <button
                    className={`ref-badge ${r.is_reference ? "on" : "off"}`}
                    onClick={() => verify([r.id], !r.is_reference)}
                    disabled={busy}
                    title={r.is_reference ? "Снять эталон" : "Сделать эталоном"}
                  >
                    {r.is_reference && <Icon name="check" size={12} />}
                    {r.is_reference ? "эталон" : "—"}
                  </button>
                </td>
                <td className="col-actions">
                  <button className="row-x" onClick={() => remove([r.id])} title="Удалить">
                    <Icon name="trash" size={14} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!loading && rows.length === 0 && (
          <div className="empty">
            <Icon name="database" size={28} />
            <p>Нет записей по текущим фильтрам</p>
          </div>
        )}
      </div>

      <div className="manage-foot">
        <div className="pager">
          <button className="th-btn" onClick={() => setPage((p) => p - 1)} disabled={!hasPrev}>
            <Icon name="chevronL" size={14} />
          </button>
          <span className="pager-info num">{from}–{to} / {total.toLocaleString("ru")}</span>
          <button className="th-btn" onClick={() => setPage((p) => p + 1)} disabled={!hasNext}>
            <Icon name="chevronR" size={14} />
          </button>
        </div>
        {loading && <span className="spinner" />}
      </div>

      {selected.size > 0 && (
        <div className="apply-bar">
          <div className="apply-info">
            <span className="apply-info-label">Выбрано</span>
            <span className="apply-info-num num">{selected.size}</span>
          </div>
          <div className="apply-actions">
            <button className="btn btn-ghost" onClick={() => verify([...selected], false)} disabled={busy}>
              Снять эталон
            </button>
            <button className="btn btn-ghost danger" onClick={() => remove([...selected])} disabled={busy}>
              <Icon name="trash" size={14} /> Удалить
            </button>
            <button className="btn btn-primary" onClick={() => verify([...selected], true)} disabled={busy}>
              <Icon name="star" size={14} /> Подтвердить эталон
            </button>
          </div>
        </div>
      )}
    </section>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<App />);
