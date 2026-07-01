import { useMemo } from "react";
import { tip } from "../lib/tooltip";
import { compact } from "../lib/format";

export interface ComboPoint { x: string; bar: number; line: number; year?: string }

export function ComboChart({ data, barName, lineName, mobile = false }: { data: ComboPoint[]; barName: string; lineName: string; mobile?: boolean }) {
  const W = mobile ? 340 : 760, H = mobile ? 260 : 300;
  const pL = mobile ? 28 : 32, pR = mobile ? 30 : 34, pT = 26, pB = 26;
  const cW = W - pL - pR, cH = H - pT - pB;
  const n = Math.max(1, data.length);
  const barMax = Math.max(1, ...data.map((d) => d.bar));
  const lineMax = Math.max(0.1, ...data.map((d) => d.line));
  const slot = cW / n;
  const bw = Math.min(30, slot * 0.5);
  const xc = (i: number) => pL + (i + 0.5) * slot;
  const yBar = (v: number) => pT + cH - (v / barMax) * cH;
  const yLine = (v: number) => pT + cH - (v / lineMax) * cH;
  const axisFs = mobile ? 11.5 : 10.5, lblFs = mobile ? 12 : 11;
  const step = Math.max(1, Math.ceil(data.length / (mobile ? 5 : 7)));
  const lastIdx = data.length - 1;
  const labeled = (i: number) => i === lastIdx || (i % step === 0 && lastIdx - i >= Math.ceil(step * 0.6));
  const labeledIdx = data.map((_, i) => i).filter(labeled);
  const firstLbl = labeledIdx[0], lastLbl = labeledIdx[labeledIdx.length - 1];
  const anchorFor = (i: number): "start" | "end" | "middle" => (i === firstLbl ? "start" : i === lastLbl ? "end" : "middle");

  const linePath = useMemo(
    () => data.map((d, i) => `${i ? "L" : "M"}${xc(i).toFixed(1)},${yLine(d.line).toFixed(1)}`).join(" "),
    [data, mobile]
  );

  return (
    <svg viewBox={`0 0 ${W} ${H}`} style={{ width: "100%", height: "auto", display: "block" }}>
      {[0, 0.5, 1].map((f) => {
        const gy = pT + cH - f * cH;
        return (
          <g key={f}>
            <line x1={pL} x2={W - pR} y1={gy} y2={gy} stroke="var(--track)" strokeWidth={1} />
            <text x={pL - 7} y={gy + 4} textAnchor="end" fontSize={axisFs} fill="var(--text-4)">{compact(barMax * f)}</text>
            <text x={W - pR + 7} y={gy + 4} textAnchor="start" fontSize={axisFs} fill="#4F46E5">{(lineMax * f).toFixed(1)}</text>
          </g>
        );
      })}

      {data.map((d, i) => (
        <rect key={"b" + i} x={(xc(i) - bw / 2).toFixed(1)} y={yBar(d.bar).toFixed(1)} width={bw.toFixed(1)} height={Math.max(0, pT + cH - yBar(d.bar)).toFixed(1)} rx={3}
          fill="#C7D2FE" {...tip(`${d.x}${d.year ? " " + d.year : ""}: ${d.bar} ${barName} · ${d.line} ${lineName}`)} style={{ cursor: "pointer", opacity: 0, animation: `fadeIn .5s ease ${i * 0.03}s both` }} />
      ))}

      <path d={linePath} fill="none" stroke="#4F46E5" strokeWidth={2.5} strokeLinecap="round" strokeLinejoin="round"
        pathLength={1} style={{ strokeDasharray: 1, strokeDashoffset: 1, animation: "lineDraw 1s cubic-bezier(.4,0,.2,1) .2s forwards" }} />
      {data.map((d, i) => (
        <circle key={"p" + i} cx={xc(i).toFixed(1)} cy={yLine(d.line).toFixed(1)} r={2.8} fill="var(--surface)" stroke="#4F46E5" strokeWidth={2}
          style={{ opacity: 0, animation: `fadeIn .3s ease ${0.5 + i * 0.03}s both` }} />
      ))}
      {data.map((d, i) => labeled(i) ? (
        <text key={"ll" + i} x={xc(i).toFixed(1)} y={(yLine(d.line) - 9).toFixed(1)} textAnchor={anchorFor(i)} fontSize={lblFs} fontWeight={700} fill="#4F46E5"
          style={{ paintOrder: "stroke", stroke: "var(--surface)", strokeWidth: 3, strokeLinejoin: "round", opacity: 0, animation: `fadeIn .4s ease ${0.7 + i * 0.02}s both` }}>{d.line}</text>
      ) : null)}

      {(() => { let ly = ""; return data.map((d, i) => {
        if (!labeled(i)) return null;
        let lab = d.x; if (d.year && d.year !== ly) { lab = `${d.x} ${d.year}`; ly = d.year; }
        return <text key={"x" + i} x={xc(i).toFixed(1)} y={H - 6} textAnchor={anchorFor(i)} fontSize={lblFs} fill="var(--text-4)">{lab}</text>;
      }); })()}
    </svg>
  );
}
