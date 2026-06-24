export function Sparkline({ vals, color }: { vals: number[]; color: string }) {
  const w = 72, h = 24;
  const mx = Math.max(...vals), mn = Math.min(...vals), rng = mx - mn || 1;
  const pts = vals
    .map((v, i) => `${((i / (vals.length - 1)) * w).toFixed(1)},${((h - 2) - ((v - mn) / rng) * (h - 4)).toFixed(1)}`)
    .join(" ");
  return (
    <svg viewBox={`0 0 ${w} ${h}`} style={{ width: 60, height: 18 }}>
      <polyline points={pts} fill="none" stroke={color} strokeWidth={1.5} strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
