import { useEffect, useRef } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import glify from "leaflet.glify";
import type { MapPoint } from "../api/types";
import { salary } from "../lib/format";

const POINT_RGB = { r: 0x4f / 255, g: 0x46 / 255, b: 0xe5 / 255 };

const TILES = {
  light: "https://{s}.basemaps.cartocdn.com/light_nolabels/{z}/{x}/{y}{r}.png",
  dark: "https://{s}.basemaps.cartocdn.com/dark_nolabels/{z}/{x}/{y}{r}.png",
};

const esc = (s: string) => s.replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c] as string));

function popupHtml(p: MapPoint): string {
  const sal = p.salary > 0 ? esc(salary(p.salary)) : "ЗП не указана";
  const link = p.url
    ? `<a href="${esc(p.url)}" target="_blank" rel="noopener noreferrer" style="display:inline-block;margin-top:7px;color:#4F46E5;font-weight:600;text-decoration:none">Открыть вакансию →</a>`
    : "";
  return (
    `<div style="min-width:150px;max-width:260px">` +
    `<b style="font-size:14px;line-height:1.3">${esc(p.title || "Вакансия")}</b>` +
    `<div style="color:#6B7280;margin-top:3px">${sal}</div>` +
    link +
    `</div>`
  );
}

export function GeoMap({ points, dark }: { points: MapPoint[]; dark: boolean }) {
  const ref = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const tileRef = useRef<L.TileLayer | null>(null);
  const layerRef = useRef<{ remove(): void } | null>(null);
  const fittedRef = useRef<string>("");

  // Create the map once; keep it alive across filter / theme changes.
  useEffect(() => {
    if (!ref.current || mapRef.current) return;
    const map = L.map(ref.current, { scrollWheelZoom: true, minZoom: 2, worldCopyJump: true }).setView([58, 60], 3);
    map.attributionControl.setPrefix(false);
    mapRef.current = map;
    fittedRef.current = "";  // fresh map → let the points layer re-fit the view (survives StrictMode remount)
    const t = setTimeout(() => map.invalidateSize(), 80);
    return () => {
      clearTimeout(t);
      try { layerRef.current?.remove(); } catch {  }
      layerRef.current = null;
      tileRef.current = null;
      map.remove();
      mapRef.current = null;
    };
  }, []);

  // Tiles follow the theme without rebuilding the map.
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;
    tileRef.current?.remove();
    tileRef.current = L.tileLayer(dark ? TILES.dark : TILES.light, {
      maxZoom: 18, subdomains: "abcd", attribution: "© OpenStreetMap, © CARTO",
    }).addTo(map);
  }, [dark]);

  // Replace only the points layer when data changes — no map teardown.
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    const valid = points.filter((p) => Number.isFinite(p.lat) && Number.isFinite(p.lng));
    const data = valid.map((p) => [p.lat, p.lng]);

    try { layerRef.current?.remove(); } catch {  }
    layerRef.current = null;

    if (!data.length) return;

    // Fit the view to where the bulk of the points are: the 2nd–98th percentile
    // of lat/lng, so a few far-flung outliers don't force a whole-world view
    // (a Russia-only set still zooms to Russia, Poland-only to Poland). Small
    // sets aren't trimmed — there every point matters. Re-fit only when the
    // extent changes, so a background refetch doesn't reset a manual zoom.
    const lats = valid.map((p) => p.lat).sort((a, b) => a - b);
    const lngs = valid.map((p) => p.lng).sort((a, b) => a - b);
    const at = (arr: number[], q: number) => arr[Math.min(arr.length - 1, Math.max(0, Math.round(q * (arr.length - 1))))];
    const lo = valid.length >= 50 ? 0.02 : 0;
    const hi = valid.length >= 50 ? 0.98 : 1;
    const minLat = at(lats, lo), maxLat = at(lats, hi);
    const minLng = at(lngs, lo), maxLng = at(lngs, hi);
    const sig = `${minLat.toFixed(3)},${minLng.toFixed(3)},${maxLat.toFixed(3)},${maxLng.toFixed(3)}`;
    if (sig !== fittedRef.current) {
      fittedRef.current = sig;
      map.invalidateSize();
      map.fitBounds([[minLat, minLng], [maxLat, maxLng]], { padding: [24, 24], maxZoom: 8, animate: false });
    }

    try {
      layerRef.current = glify.points({
        map,
        data,
        size: 8,
        color: POINT_RGB,
        opacity: 0.72,
        click: (e, point) => {
          let p = valid[data.indexOf(point)];
          if (!p && e.latlng) {
            let best = Infinity;
            for (const c of valid) {
              const d = (c.lat - e.latlng.lat) ** 2 + (c.lng - e.latlng.lng) ** 2;
              if (d < best) { best = d; p = c; }
            }
          }
          if (p) L.popup({ closeButton: true }).setLatLng([p.lat, p.lng]).setContent(popupHtml(p)).openOn(map);
        },
      });
    } catch (err) {
      console.error("glify points failed", err);
    }
  }, [points]);

  return <div ref={ref} className="mob-map" style={{ width: "100%", height: 620, borderRadius: 8, overflow: "hidden", border: "1px solid var(--border)", zIndex: 1 }} />;
}
