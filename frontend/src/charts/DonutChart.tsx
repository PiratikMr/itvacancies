import { useMemo, useRef } from "react";
import { tip } from "../lib/tooltip";

export interface DonutSlice {
  name: string;
  pct: number;
  color: string;
}

export function DonutChart({ data, centerLabel }: { data: DonutSlice[]; centerLabel?: string }) {
  const cx = 190, cy = 116, r = 64, sw = 24, gap = 2.5;
  const circ = 2 * Math.PI * r;
  const outerR = r + sw / 2 + 6, lineR = outerR + 22;
  const nameRef = useRef<SVGTextElement>(null);
  const subRef = useRef<SVGTextElement>(null);
  const top = data[0];

  const { arcs, callouts } = useMemo(() => {
    let cumDash = 0, cumAngle = 0;
    const arcs: JSX.Element[] = [];
    const callouts: JSX.Element[] = [];
    data.forEach((d, i) => {
      const dash = Math.max((d.pct / 100) * circ - gap, 0);
      const offset = circ / 4 - cumDash;
      arcs.push(
        <circle key={"a" + i} data-seg={i} cx={cx} cy={cy} r={r} fill="none" stroke={d.color}
          strokeWidth={sw} strokeLinecap="butt" strokeDasharray={`${dash} ${circ - dash}`} strokeDashoffset={offset}
          style={{ cursor: "pointer", transition: "stroke-width .18s ease,opacity .18s ease", animation: `arcGrow .8s cubic-bezier(.4,0,.2,1) ${i * 0.1}s both`, "--dash": dash, "--gap": circ - dash, "--circ": circ } as any} />
      );
      cumDash += (d.pct / 100) * circ;
      const segA = (d.pct / 100) * 2 * Math.PI;
      const midA = -Math.PI / 2 + cumAngle + segA / 2;
      cumAngle += segA;
      const p1x = cx + outerR * Math.cos(midA), p1y = cy + outerR * Math.sin(midA);
      const p2x = cx + lineR * Math.cos(midA), p2y = cy + lineR * Math.sin(midA);
      const isRight = Math.cos(midA) >= 0;
      const tx = p2x + (isRight ? 7 : -7);
      const anchor = isRight ? "start" : "end";
      const cdel = 0.5 + i * 0.08;
      callouts.push(
        <g key={"c" + i}>
          <line x1={p1x.toFixed(1)} y1={p1y.toFixed(1)} x2={p2x.toFixed(1)} y2={p2y.toFixed(1)} stroke={d.color} strokeWidth={1.5} strokeLinecap="round" style={{ animation: `fadeIn .5s ease ${cdel}s both` }} />
          <circle cx={p2x.toFixed(1)} cy={p2y.toFixed(1)} r={2.5} fill={d.color} style={{ animation: `fadeIn .5s ease ${cdel}s both` }} />
          <text x={tx.toFixed(1)} y={(p2y - 3).toFixed(1)} textAnchor={anchor} fill="var(--text-2)" fontSize={14} fontWeight={600} style={{ animation: `fadeIn .5s ease ${cdel}s both` }}>{d.name}</text>
          <text x={tx.toFixed(1)} y={(p2y + 12).toFixed(1)} textAnchor={anchor} fill="var(--text-4)" fontSize={13} fontWeight={600} style={{ animation: `fadeIn .5s ease ${cdel}s both` }}>{d.pct}%</text>
        </g>
      );
    });
    return { arcs, callouts };
  }, [data]);

  const setAll = (root: SVGSVGElement, active: number | null) => {
    data.forEach((_, j) => {
      const a = root.querySelector<SVGCircleElement>(`[data-seg="${j}"]`);
      if (!a) return;
      if (active == null) { a.style.opacity = "1"; a.style.strokeWidth = sw + "px"; }
      else { a.style.opacity = j === active ? "1" : "0.28"; a.style.strokeWidth = (j === active ? sw + 5 : sw) + "px"; }
    });
  };

  return (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "center", flex: 1 }}>
      <svg viewBox="0 0 380 236" style={{ width: "100%", height: "auto", display: "block" }}
        onMouseLeave={(e) => {
          setAll(e.currentTarget, null);
          if (nameRef.current && top) { nameRef.current.textContent = top.name; nameRef.current.setAttribute("fill", top.color); }
          if (subRef.current) subRef.current.textContent = centerLabel ?? "";
        }}>
        {data.map((d, i) => {
          const t = tip(`${d.name}: ${d.pct}% вакансий`);
          return (
            <g key={"h" + i}
              onMouseEnter={(e) => {
                t.onMouseEnter(e);
                const root = e.currentTarget.ownerSVGElement!;
                setAll(root, i);
                if (nameRef.current) { nameRef.current.textContent = d.name; nameRef.current.setAttribute("fill", d.color); }
                if (subRef.current) subRef.current.textContent = `${d.pct}% вакансий`;
              }}
              onMouseMove={t.onMouseMove}
              onMouseLeave={t.onMouseLeave}>
              {arcs[i]}
            </g>
          );
        })}
        {callouts}
        <text ref={nameRef} x={cx} y={cy} textAnchor="middle" fill={top?.color ?? "#4F46E5"} fontSize={22} fontWeight={800} style={{ transition: "fill .18s ease" }}>{top?.name ?? ""}</text>
        <text ref={subRef} x={cx} y={cy + 18} textAnchor="middle" fill="var(--text-4)" fontSize={13} fontWeight={500}>{centerLabel ?? ""}</text>
      </svg>
    </div>
  );
}
