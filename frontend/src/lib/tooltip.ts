let el: HTMLDivElement | null = null;

function ensure(): HTMLDivElement {
  if (!el) {
    el = document.createElement("div");
    el.className = "tt";
    document.body.appendChild(el);
  }
  return el;
}

function move(e: { clientX: number; clientY: number }) {
  const t = ensure();
  t.style.left = e.clientX + 14 + "px";
  t.style.top = e.clientY - 34 + "px";
}

export function tip(text: string) {
  return {
    onMouseEnter: (e: React.MouseEvent) => {
      const t = ensure();
      t.textContent = text;
      t.className = "tt show";
      move(e);
    },
    onMouseMove: (e: React.MouseEvent) => move(e),
    onMouseLeave: () => {
      if (el) el.className = "tt";
    },
  };
}
