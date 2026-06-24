import { useEffect, useState } from "react";

export function usePageData<T>(fetcher: () => Promise<T>, deps: React.DependencyList) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    setError(null);
    fetcher()
      .then((d) => alive && (setData(d), setLoading(false)))
      .catch((e) => alive && (setError(String(e)), setLoading(false)));
    return () => {
      alive = false;
    };
  }, deps);

  return { data, loading, error };
}
