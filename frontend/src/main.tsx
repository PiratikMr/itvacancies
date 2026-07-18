import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";

const stopGesture = (e: Event) => e.preventDefault();
for (const type of ["gesturestart", "gesturechange", "gestureend"]) {
  document.addEventListener(type, stopGesture, { passive: false });
}

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>
);
