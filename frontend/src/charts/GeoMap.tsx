import { useEffect, useRef } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import glify from "leaflet.glify";
import type { GeoPoints, PointDetail } from "../api/types";
import { salary } from "../lib/format";

const POINT_RGB = { r: 0x4f / 255, g: 0x46 / 255, b: 0xe5 / 255 };

const TILES = {
  light: "https://{s}.basemaps.cartocdn.com/light_nolabels/{z}/{x}/{y}{r}.png",
  dark: "https://{s}.basemaps.cartocdn.com/dark_nolabels/{z}/{x}/{y}{r}.png",
};

const esc = (s: string) => s.replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c] as string));

const stub = (text: string) => `<div style="min-width:110px;color:#6B7280">${text}</div>`;

function popupHtml(d: PointDetail): string {
  const sal = d.salary > 0 ? esc(salary(d.salary)) : "ЗП не указана";
  const link = d.url
    ? `<a href="${esc(d.url)}" target="_blank" rel="noopener noreferrer" style="display:inline-block;margin-top:7px;color:#4F46E5;font-weight:600;text-decoration:none">Открыть вакансию →</a>`
    : "";
  return (
    `<div style="min-width:150px;max-width:260px">` +
    `<b style="font-size:14px;line-height:1.3">${esc(d.title || "Вакансия")}</b>` +
    `<div style="color:#6B7280;margin-top:3px">${sal}</div>` +
    link +
    `</div>`
  );
}

export function GeoMap({ points, dark, loadDetail }: {
  points: GeoPoints | null;
  dark: boolean;
  loadDetail: (id: number) => Promise<PointDetail>;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const tileRef = useRef<L.TileLayer | null>(null);
  const layerRef = useRef<{ remove(): void } | null>(null);
  const fittedRef = useRef<string>("");
  const detailsRef = useRef(new Map<number, PointDetail>());
  const loadRef = useRef(loadDetail);
  useEffect(() => { loadRef.current = loadDetail; }, [loadDetail]);

  useEffect(() => {
    if (!ref.current || mapRef.current) return;
    const map = L.map(ref.current, { scrollWheelZoom: true, minZoom: 2, worldCopyJump: true }).setView([58, 60], 3);
    map.attributionControl.setPrefix(false);
    mapRef.current = map;
    fittedRef.current = "";
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

  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;
    tileRef.current?.remove();
    tileRef.current = L.tileLayer(dark ? TILES.dark : TILES.light, {
      maxZoom: 18, subdomains: "abcd", attribution: "© OpenStreetMap, © CARTO",
    }).addTo(map);
  }, [dark]);

  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    try { layerRef.current?.remove(); } catch {  }
    layerRef.current = null;

    if (!points) return;
    const { lat, lng, ids } = points;

    const data: number[][] = [];
    for (let i = 0; i < lat.length; i++) {
      if (Number.isFinite(lat[i]) && Number.isFinite(lng[i])) data.push([lat[i], lng[i], ids[i]]);
    }
    if (!data.length) return;

    const lats = Float64Array.from(data, (p) => p[0]).sort();
    const lngs = Float64Array.from(data, (p) => p[1]).sort();
    const at = (arr: Float64Array, q: number) => arr[Math.min(arr.length - 1, Math.max(0, Math.round(q * (arr.length - 1))))];
    const lo = data.length >= 50 ? 0.02 : 0;
    const hi = data.length >= 50 ? 0.98 : 1;
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
        click: (_e, point) => {
          const id = Array.isArray(point) ? point[2] : undefined;
          if (typeof id !== "number") return;
          const popup = L.popup({ closeButton: true }).setLatLng([point[0], point[1]]);

          const cached = detailsRef.current.get(id);
          if (cached) {
            popup.setContent(popupHtml(cached)).openOn(map);
            return;
          }
          popup.setContent(stub("Загрузка…")).openOn(map);
          loadRef.current(id)
            .then((d) => {
              detailsRef.current.set(id, d);
              if (popup.isOpen()) popup.setContent(popupHtml(d));
            })
            .catch(() => {
              if (popup.isOpen()) popup.setContent(stub("Не удалось загрузить"));
            });
        },
      });
    } catch (err) {
      console.error("glify points failed", err);
    }
  }, [points]);

  return (
    <div className="mob-map" style={{ position: "relative", width: "100%", height: 620 }}>
      <div ref={ref} style={{ position: "absolute", inset: 0, borderRadius: 8, overflow: "hidden", border: "1px solid var(--border)", zIndex: 1 }} />
      {!points && (
        <div style={{ position: "absolute", inset: 0, display: "grid", placeItems: "center", zIndex: 2, pointerEvents: "none" }}>
          <div style={{ padding: "7px 14px", borderRadius: 8, background: "var(--surface)", border: "1px solid var(--border)", color: "var(--text-4)", fontSize: 13, animation: "pulse 1.4s ease-in-out infinite" }}>
            Загрузка…
          </div>
        </div>
      )}
    </div>
  );
}
