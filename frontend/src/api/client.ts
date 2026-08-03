import { filtersToParams, type Filters } from "../state/filters";
import type { Sort } from "../components/shared";
import type {
  OverviewResponse, MetaResponse, FiltersResponse, FacetsResponse,
  SalaryResponse, SkillsResponse, EmployersResponse, GeoResponse, VacanciesResponse,
  GeoPoints, PointDetail,
} from "./types";

async function getJson<T>(path: string, signal?: AbortSignal): Promise<T> {
  const res = await fetch(path, { headers: { Accept: "application/json" }, signal });
  if (!res.ok) throw new Error(`API ${path} → ${res.status}`);
  return res.json() as Promise<T>;
}

const qs = (f: Filters) => filtersToParams(f).toString();
const sortQs = (s?: Sort) => (s && s.key && s.dir ? `&sort=${s.key}&dir=${s.dir}` : "");

export const api = {
  meta: () => getJson<MetaResponse>("/api/v1/meta"),
  filters: () => getJson<FiltersResponse>("/api/v1/filters"),
  facets: (f: Filters, signal?: AbortSignal) => getJson<FacetsResponse>(`/api/v1/facets?${qs(f)}`, signal),
  suggest: (f: Filters, field: string, q: string, signal?: AbortSignal) =>
    getJson<{ values: string[] }>(`/api/v1/suggest?${qs(f)}&field=${field}&q=${encodeURIComponent(q)}`, signal),
  overview: (f: Filters) => getJson<OverviewResponse>(`/api/v1/overview?${qs(f)}`),
  salary: (f: Filters) => getJson<SalaryResponse>(`/api/v1/salary?${qs(f)}`),
  skills: (f: Filters, limit: number, offset: number, sort?: Sort) =>
    getJson<SkillsResponse>(`/api/v1/skills?${qs(f)}&limit=${limit}&offset=${offset}${sortQs(sort)}`),
  employers: (f: Filters, limit: number, offset: number, sort?: Sort) =>
    getJson<EmployersResponse>(`/api/v1/employers?${qs(f)}&limit=${limit}&offset=${offset}${sortQs(sort)}`),
  geo: (f: Filters) => getJson<GeoResponse>(`/api/v1/geo?${qs(f)}`),
  geoPoints: (f: Filters) => getJson<GeoPoints>(`/api/v1/geo/points?${qs(f)}`),
  geoPoint: (id: number) => getJson<PointDetail>(`/api/v1/geo/point/${id}`),
  vacancies: (f: Filters, limit: number, offset: number, sort?: Sort) =>
    getJson<VacanciesResponse>(`/api/v1/vacancies?${qs(f)}&limit=${limit}&offset=${offset}${sortQs(sort)}`),
};
