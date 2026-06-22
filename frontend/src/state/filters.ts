export type TabId = "overview" | "salary" | "skills" | "employers" | "geo" | "vacancies";

export const LIST_FACETS = [
  "platform", "grades", "fields", "skills", "experience",
  "format", "employments", "employer", "currency",
] as const;
export type ListFacet = (typeof LIST_FACETS)[number];

export interface Filters {
  period: string;
  status: string;
  salaryMin: number | null;
  salaryMax: number | null;
  platform: string[];
  grades: string[];
  fields: string[];
  skills: string[];
  experience: string[];
  format: string[];
  employments: string[];
  employer: string[];
  currency: string[];
}

const emptyLists = (): Pick<Filters, ListFacet> => ({
  platform: [], grades: [], fields: [], skills: [], experience: [],
  format: [], employments: [], employer: [], currency: [],
});

export const DEFAULT_FILTERS: Filters = {
  period: "3m", status: "all", salaryMin: null, salaryMax: null, ...emptyLists(),
};

export const CLEARED_FILTERS: Filters = {
  period: "all", status: "all", salaryMin: null, salaryMax: null, ...emptyLists(),
};

export function hasActiveFilters(f: Filters): boolean {
  return (
    f.period !== "all" || f.status !== "all" ||
    f.salaryMin != null || f.salaryMax != null ||
    LIST_FACETS.some((k) => f[k].length > 0)
  );
}

export function filtersToParams(f: Filters): URLSearchParams {
  const p = new URLSearchParams();
  p.set("period", f.period || "3m");
  if (f.status !== "all") p.set("status", f.status);
  if (f.salaryMin != null || f.salaryMax != null) {
    p.set("salary", `${f.salaryMin ?? ""},${f.salaryMax ?? ""}`);
  }
  for (const k of LIST_FACETS) {
    if (f[k].length) p.set(k, f[k].join(","));
  }
  return p;
}

const csv = (p: URLSearchParams, k: string): string[] => {
  const v = p.get(k);
  return v ? v.split(",").map((s) => s.trim()).filter(Boolean) : [];
};

export function readUrl(search: string): { tab: TabId; filters: Filters } {
  const p = new URLSearchParams(search);
  const tab = (p.get("tab") as TabId) || "overview";

  let salaryMin: number | null = null;
  let salaryMax: number | null = null;
  const salary = p.get("salary");
  if (salary) {
    const [lo, hi] = salary.split(",");
    salaryMin = lo ? Number(lo) : null;
    salaryMax = hi ? Number(hi) : null;
  }

  const lists = {} as Pick<Filters, ListFacet>;
  for (const k of LIST_FACETS) lists[k] = csv(p, k);

  return {
    tab,
    filters: {
      period: p.get("period") || "3m",
      status: p.get("status") || "all",
      salaryMin, salaryMax,
      ...lists,
    },
  };
}

export function writeUrl(tab: TabId, filters: Filters): void {
  const p = filtersToParams(filters);
  if (tab !== "overview") p.set("tab", tab);
  const qs = p.toString();
  window.history.replaceState(null, "", qs ? `/?${qs}` : "/");
}
