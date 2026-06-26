import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const target = env.VITE_API_TARGET || "http://localhost:17000";
  const poll = env.VITE_POLL === "1";
  const siteUrl = env.VITE_SITE_URL || "http://localhost:5173";
  return {
    plugins: [
      react(),
      {
        name: "inject-site-url",
        transformIndexHtml: {
          order: "pre" as const,
          handler: (html: string) => html.replaceAll("%SITE_URL%", siteUrl),
        },
      },
    ],
    server: {
      host: true,
      port: 5173,
      proxy: {
        "/api": { target, changeOrigin: true },
      },
      watch: poll ? { usePolling: true, interval: 300 } : undefined,
    },
  };
});
