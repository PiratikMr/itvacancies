import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

// Локальная разработка: Vite отдаёт фронт на :5173 и проксирует /api на FastAPI.
// Тот же origin → CORS не нужен, как и в проде (nginx так же проксирует /api).
//
// Цель прокси настраивается через VITE_API_TARGET:
// - локальный Node:        http://localhost:17000  (api опубликован на 127.0.0.1:17000)
// - dev-контейнер в Docker: http://api:8000        (внутри сети data_platform_net)
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const target = env.VITE_API_TARGET || "http://localhost:17000";
  const poll = env.VITE_POLL === "1"; // bind-mount в Docker на Windows → нужен polling
  return {
    plugins: [react()],
    server: {
      host: true, // слушать 0.0.0.0 — нужно при запуске в контейнере
      port: 5173,
      proxy: {
        "/api": { target, changeOrigin: true },
      },
      watch: poll ? { usePolling: true, interval: 300 } : undefined,
    },
  };
});
