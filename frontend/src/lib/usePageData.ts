import { useEffect, useState } from "react";

// Session-scoped, in-memory response cache. Lets tab switches and previously
// seen filter combinations render instantly, while a background refetch keeps
// the data fresh (stale-while-revalidate). Cleared on full page reload.
const cache = new Map<string, unknown>();

export const cacheKey = (...parts: unknown[]): string => JSON.stringify(parts);

export function usePageData<T>(
  fetcher: () => Promise<T>,
  deps: React.DependencyList,
  key?: string,
) {
  const initial = key != null && cache.has(key) ? (cache.get(key) as T) : null;
  const [data, setData] = useState<T | null>(initial);
  const [loading, setLoading] = useState(initial == null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    const cached = key != null && cache.has(key) ? (cache.get(key) as T) : null;
    if (cached != null) {
      setData(cached);
      setLoading(false);
    } else {
      setLoading(true);
    }
    setError(null);

    fetcher()
      .then((d) => {
        if (!alive) return;
        if (key != null) cache.set(key, d);
        setData(d);
        setLoading(false);
      })
      .catch((e) => {
        if (!alive) return;
        setError(String(e));
        setLoading(false);
      });

    return () => {
      alive = false;
    };
  }, deps);

  return { data, loading, error };
}
