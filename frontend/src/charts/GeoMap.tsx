import { useEffect, useRef } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import glify from "leaflet.glify";
import type { CityPoint } from "../api/types";
import { nfmt, salary } from "../lib/format";

const POINT_RGB = { r: 0x4f / 255, g: 0x46 / 255, b: 0xe5 / 255 };

export function GeoMap({ points, dark }: { points: CityPoint[]; dark: boolean }) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!ref.current) return;
    const map = L.map(ref.current, { scrollWheelZoom: true, minZoom: 2, worldCopyJump: true }).setView([58, 60], 3);
    map.attributionControl.setPrefix(false);

    const tiles = dark
      ? "https://{s}.basemaps.cartocdn.com/dark_nolabels/{z}/{x}/{y}{r}.png"
      : "https://{s}.basemaps.cartocdn.com/light_nolabels/{z}/{x}/{y}{r}.png";
    L.tileLayer(tiles, { maxZoom: 18, subdomains: "abcd", attribution: "© OpenStreetMap, © CARTO" }).addTo(map);

    const valid = points.filter((p) => Number.isFinite(p.lat) && Number.isFinite(p.lng));
    const data = valid.map((p) => [p.lat, p.lng]);

    const esc = (s: string) => s.replace(/[&<>]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;" }[c] as string));
    const popupFor = (p: CityPoint) => {
      const html = p.count > 1
        ? `<b style="font-size:14px">${esc(p.city || "—")}</b><br><span style="color:#6B7280">${nfmt(p.count)} вакансий · медиана ${salary(p.median)}</span>`
        : `<b style="font-size:14px">${esc(p.title || "Вакансия")}</b><br><span style="color:#6B7280">${salary(p.median)}${p.city ? ` · ${esc(p.city)}` : ""}</span>`;
      L.popup({ closeButton: false }).setLatLng([p.lat, p.lng]).setContent(html).openOn(map);
    };

    let layer: { remove(): void } | null = null;
    try {
      layer = glify.points({
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
          if (p) popupFor(p);
        },
      });
    } catch (err) {
      console.error("glify points failed", err);
    }

    const t = setTimeout(() => map.invalidateSize(), 80);
    return () => { clearTimeout(t); try { layer?.remove(); } catch {  } map.remove(); };
  }, [points, dark]);

  return <div ref={ref} className="mob-map" style={{ width: "100%", height: 620, borderRadius: 8, overflow: "hidden", border: "1px solid var(--border)", zIndex: 1 }} />;
}
