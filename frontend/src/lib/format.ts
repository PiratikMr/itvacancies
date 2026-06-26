export const nfmt = (n: number): string => Math.round(n).toLocaleString("ru-RU");

export const compact = (n: number): string => {
  const abs = Math.abs(n);
  if (abs < 1000) return String(Math.round(n));
  if (abs < 1_000_000) {
    const k = n / 1000;
    return (abs < 10_000 && k % 1 !== 0 ? k.toFixed(1) : String(Math.round(k))) + "k";
  }
  const m = n / 1_000_000;
  return (abs < 10_000_000 && m % 1 !== 0 ? m.toFixed(1) : String(Math.round(m))) + "M";
};

const moneyCompact = (n: number): string => {
  const a = Math.abs(n);
  if (a < 1000) return String(Math.round(n));
  if (a < 1_000_000) return `${Math.round(n / 1000)}k`;
  const m = n / 1_000_000;
  return `${a < 10_000_000 ? m.toFixed(2) : a < 100_000_000 ? m.toFixed(1) : Math.round(m)}M`;
};

export const salaryNum = (n: number | null | undefined): string =>
  n == null || n <= 0 ? "—" : moneyCompact(n);

export const salary = (n: number | null | undefined): string =>
  n == null || n <= 0 ? "—" : `${moneyCompact(n)} ₽`;

export const salaryFull = (n: number | null | undefined): string =>
  n == null || n <= 0 ? "—" : `${nfmt(n)} ₽`;

const MONTHS = ["янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"];
export const weekLabel = (iso: string): string => {
  const d = new Date(iso + "T00:00:00");
  if (isNaN(d.getTime())) return iso;
  return `${d.getDate()} ${MONTHS[d.getMonth()]}`;
};

export const monthShort = (iso: string): string => {
  const d = new Date(iso + "T00:00:00");
  return isNaN(d.getTime()) ? iso : MONTHS[d.getMonth()];
};

export const yearOf = (iso: string): string => {
  const d = new Date(iso + "T00:00:00");
  return isNaN(d.getTime()) ? "" : String(d.getFullYear());
};

// Detects whether a timeseries is bucketed by month (vs week) from the actual
// step between consecutive points — robust even when the data spans less than
// the selected period (e.g. a year filter over only a few months of data).
export const isMonthlyBuckets = (periods: string[]): boolean => {
  if (periods.length < 2) return false;
  const a = new Date(periods[0] + "T00:00:00").getTime();
  const b = new Date(periods[1] + "T00:00:00").getTime();
  return b - a > 20 * 864e5;
};
