import { useEffect, useState } from "react";

export function useIsMobile(breakpoint = 768): boolean {
  const query = `(max-width: ${breakpoint}px)`;
  const [mobile, setMobile] = useState<boolean>(
    () => typeof window !== "undefined" && window.matchMedia(query).matches
  );
  useEffect(() => {
    const mql = window.matchMedia(query);
    const on = () => setMobile(mql.matches);
    on();
    mql.addEventListener("change", on);
    return () => mql.removeEventListener("change", on);
  }, [query]);
  return mobile;
}
