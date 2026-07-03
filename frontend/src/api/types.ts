export interface NamedCount {
  name: string;
  count: number;
  median_salary?: number;
  trend?: number | null;
}

export interface KpiDelta {
  total: number | null;
  active: number | null;
  median_salary: number | null;
  remote_pct: number | null;
}

export interface KpiSpark {
  total: number[];
  active: number[];
  median_salary: number[];
  remote_pct: number[];
}

export interface OverviewKpis {
  total: number;
  active: number;
  median_salary: number;
  remote_pct: number;
  spark: KpiSpark;
  delta?: KpiDelta | null;
}

export interface TimePoint {
  period: string;
  count: number;
}

export interface PlatformStat {
  name: string;
  count: number;
  avg_close_days: number | null;
}

export interface GradeShare {
  name: string;
  pct: number;
}

export interface OverviewResponse {
  kpis: OverviewKpis;
  timeseries: TimePoint[];
  grades: GradeShare[];
  directions: NamedCount[];
  top_skills: NamedCount[];
  formats: NamedCount[];
  platforms: PlatformStat[];
}

export interface MetaResponse {
  data_updated_at: string | null;
  fx: { usd: number; eur: number } | null;
}

export interface FiltersResponse {
  options: Record<string, string[]>;
  salary: { min?: number; max?: number };
  published_at: { min?: string; max?: string };
}

export interface FacetsResponse {
  facets: Record<string, string[]>;
  salary: { min?: number; max?: number };
}

export interface GradeBand { name: string; min: number; median: number; max: number; count: number; }
export interface SalaryResponse {
  kpis: { senior_median: number | null; middle_median: number | null; junior_median: number | null; sr_jr_ratio: number | null };
  grade_band: GradeBand[];
  by_direction: { name: string; median: number; count: number }[];
  by_experience: { name: string; count: number; median: number }[];
  transparency: { with_salary_pct: number; total: number };
  by_currency: { name: string; count: number; median: number }[];
}

export interface SkillRow { name: string; count: number; median: number; }
export interface FieldSkillRow { field: string; skill: string; count: number; field_total: number; }
export interface SkillPairRow { skill_a: string; skill_b: string; count: number; median: number; }
export interface SkillsResponse {
  kpis: {
    avg_skills: number; english_pct: number;
    top_demand: { name: string; count: number };
    top_paid: { name: string; median: number };
  };
  english: { levels: { level: string; count: number; median: number }[]; not_required: number; total: number };
  market_median: number;
  by_field: FieldSkillRow[];
  pairs: SkillPairRow[];
  table: { rows: SkillRow[]; total: number; limit: number; offset: number };
}

export interface EmployerRow { name: string; count: number; active: number; median: number; }
export interface HiringPoint { period: string; employers: number; per_employer: number; }
export interface EmployersResponse {
  kpis: { unique_employers: number; active_employers: number; avg_per_company: number; avg_close_days: number };
  dynamics: HiringPoint[];
  top_active: { name: string; active: number }[];
  table: { rows: EmployerRow[]; total: number; limit: number; offset: number };
}

export interface MapPoint { lat: number; lng: number; title: string; salary: number; url: string; }
export interface CountryRow { name: string; count: number; cities: number; median: number; remote_pct: number; }
export interface GeoResponse {
  kpis: { countries: number; cities: number; outside_russia_pct: number; median_outside: number };
  map: MapPoint[];
  countries: CountryRow[];
}

export interface VacancyRow {
  vacancy_id: number; title: string; employer: string; url: string;
  salary: number; grades: string[]; experience: string; schedules: string[];
  platform: string; published_at: string;
}
export interface VacanciesResponse { rows: VacancyRow[]; total: number; limit: number; offset: number; }
