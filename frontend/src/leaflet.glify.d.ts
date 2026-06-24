declare module "leaflet.glify" {
  import type { Map as LeafletMap, LeafletMouseEvent } from "leaflet";

  interface RGB { r: number; g: number; b: number }

  interface GlifyPointsOptions {
    map: LeafletMap;
    data: number[][];
    size?: number | ((index: number) => number);
    color?: RGB | ((index: number) => RGB);
    opacity?: number;
    className?: string;
    click?: (e: LeafletMouseEvent, point: number[], xy?: { x: number; y: number }) => void | boolean;
  }

  interface GlifyLayer { remove(): void }

  interface Glify {
    points(opts: GlifyPointsOptions): GlifyLayer;
  }

  const glify: Glify;
  export default glify;
}
