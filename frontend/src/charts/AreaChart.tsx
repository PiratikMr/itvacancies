import { useMemo } from "react";
import { tip } from "../lib/tooltip";
import { nfmt, compact } from "../lib/format";

export interface AreaPoint {
  x: string;
  y: number;
  year?: string;
}

export function AreaChart({ data }: { data: AreaPoint[] }) {
  const W = 780, H = 230, pL = 14, pR = 18, pT = 36, pB = 30;
  const cW = W - pL - pR, cH = H - pT - pB;

  const { pts, linePath, areaPath, mx, step } = useMemo(() => {
    const vals = data.map((d) => d.y);
    const mx = Math.max(1, ...vals);
    const n = Math.max(1, data.length - 1);
    const pts = data.map((d, i) => ({
      x: pL + (i / n) * cW,
      y: pT + cH - (d.y / mx) * cH,
      label: d.x,
      year: d.year,
      v: d.y,
    }));
    let linePath = pts.length ? `M${pts[0].x.toFixed(1)},${pts[0].y.toFixed(1)}` : "";
    for (let i = 1; i < pts.length; i++) {
      const cp = (pts[i - 1].x + pts[i].x) / 2;
      linePath += ` C${cp.toFixed(1)},${pts[i - 1].y.toFixed(1)} ${cp.toFixed(1)},${pts[i].y.toFixed(1)} ${pts[i].x.toFixed(1)},${pts[i].y.toFixed(1)}`;
    }
    const areaPath = pts.length
      ? `${linePath} L${(pL + cW).toFixed(1)},${pT + cH} L${pL},${pT + cH} Z`
      : "";
    const step = Math.max(1, Math.ceil(pts.length / 8));
    return { pts, linePath, areaPath, mx, step };
  }, [data]);

  const labeled = (i: number) => i % step === 0 || i === pts.length - 1;

  return (
    <svg viewBox={`0 0 ${W} ${H}`} style={{ width: "100%", height: "auto", display: "block" }}>
      <defs>
        <linearGradient id="ag1" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#4F46E5" stopOpacity={0.14} />
          <stop offset="100%" stopColor="#4F46E5" stopOpacity={0.01} />
        </linearGradient>
        <clipPath id="cc1">
          <rect x={pL} y={pT} width={cW} height={cH + 1} />
        </clipPath>
      </defs>

      {[0.33, 0.66, 1].map((f) => {
        const gy = pT + cH - f * cH;
        return <line key={f} x1={pL} x2={W - pR} y1={gy} y2={gy} stroke="var(--track)" strokeWidth={1} />;
      })}

      <g clipPath="url(#cc1)">
        <path d={areaPath} fill="url(#ag1)" style={{ opacity: 0, animation: "fadeIn .8s ease .3s both" }} />
        <path
          d={linePath}
          fill="none"
          stroke="#4F46E5"
          strokeWidth={2.5}
          strokeLinecap="round"
          strokeLinejoin="round"
          pathLength={1}
          style={{ strokeDasharray: 1, strokeDashoffset: 1, animation: "lineDraw 1.1s cubic-bezier(.4,0,.2,1) .1s forwards" }}
        />
      </g>

      {pts.map((p, i) =>
        labeled(i) ? (
          <text key={"vl" + i} x={p.x} y={(p.y - 11).toFixed(1)} textAnchor="middle" fontSize={11.5} fontWeight={700} fill="#4F46E5"
            style={{ paintOrder: "stroke", stroke: "var(--surface)", strokeWidth: 3, strokeLinejoin: "round", opacity: 0, animation: `fadeIn .4s ease ${0.6 + i * 0.02}s both` }}>
            {compact(p.v)}
          </text>
        ) : null
      )}

      {pts.map((p, i) => (
        <circle key={"dc" + i} cx={p.x} cy={p.y} r={2.6} fill="var(--surface)" stroke="#4F46E5" strokeWidth={2}
          {...tip(`${p.label}${p.year ? " " + p.year : ""}: ${nfmt(p.v)} вак.`)}
          style={{ cursor: "pointer", opacity: 0, animation: `fadeIn .3s ease ${0.5 + i * 0.02}s both` }} />
      ))}

      {(() => {
        let lastYear = "";
        return pts.map((p, i) => {
          if (!labeled(i)) return null;
          let label = p.label;
          if (p.year && p.year !== lastYear) {
            label = `${p.label} ${p.year}`;
            lastYear = p.year;
          }
          return (
            <text key={"xl" + i} x={p.x} y={H - 6} textAnchor="middle" fill="var(--text-4)" fontSize={11.5}>
              {label}
            </text>
          );
        });
      })()}
    </svg>
  );
}
