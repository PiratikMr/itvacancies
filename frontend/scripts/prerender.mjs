import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const DIST = join(__dirname, "..", "dist");
const SITE = process.env.VITE_SITE_URL || "http://localhost:5173";

const ROUTES = [
  {
    path: "/salary",
    h1: "Зарплаты и грейды в IT",
    title: "Зарплаты и грейды в IT — медианы по направлениям и опыту | IT Vacancies",
    description: "Зарплаты и грейды в IT: медианные зарплаты по направлениям, опыту и грейдам — от стажёра до тимлида. Прозрачность зарплат и валюты.",
    body: "Медианные зарплаты по направлениям, опыту и грейдам от стажёра до тимлида, прозрачность зарплат и валюты выплат на рынке IT-вакансий.",
  },
  {
    path: "/skills",
    h1: "Востребованные навыки в IT",
    title: "Востребованные навыки в IT — статистика и зарплаты | IT Vacancies",
    description: "Востребованные навыки в IT-вакансиях: частота упоминаний, медианные зарплаты по технологиям и требования к английскому языку.",
    body: "Самые востребованные технологии, частота упоминаний и медианные зарплаты по навыкам, а также требования к английскому языку в IT-вакансиях.",
  },
  {
    path: "/employers",
    h1: "Работодатели на IT-рынке",
    title: "Работодатели в IT — кто активнее всего нанимает | IT Vacancies",
    description: "Работодатели на IT-рынке: кто активнее всего нанимает, число вакансий, медианные зарплаты и динамика найма по компаниям.",
    body: "Кто активнее всего нанимает, число вакансий и медианные зарплаты по компаниям, а также динамика найма на рынке IT.",
  },
  {
    path: "/geo",
    h1: "География IT-вакансий",
    title: "География IT-вакансий — карта по городам и странам | IT Vacancies",
    description: "География IT-вакансий: карта по городам и странам, доля удалённой работы и медианные зарплаты по регионам.",
    body: "Карта IT-вакансий по городам и странам, доля удалённой работы и медианные зарплаты по регионам.",
  },
  {
    path: "/vacancies",
    h1: "Актуальные IT-вакансии",
    title: "IT-вакансии — зарплаты, грейды, формат работы | IT Vacancies",
    description: "Актуальные IT-вакансии: зарплаты, грейды, опыт, формат работы и площадки. Свежие вакансии с гибкими фильтрами.",
    body: "Список актуальных IT-вакансий с зарплатами, грейдами, опытом, форматом работы и площадками.",
  },
];

const escHtml = (s) => s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
const escAttr = (s) => s.replace(/&/g, "&amp;").replace(/"/g, "&quot;");

function replaceOnce(html, re, value, label) {
  if (!re.test(html)) {
    throw new Error(`prerender: anchor not found (${label}) — index.html changed?`);
  }
  return html.replace(re, value);
}

function render(base, r) {
  const url = SITE + r.path;
  let html = base;
  html = replaceOnce(html, /<title>[\s\S]*?<\/title>/, `<title>${escHtml(r.title)}</title>`, "title");
  html = replaceOnce(html, /(<meta name="description" content=")[^"]*(")/, `$1${escAttr(r.description)}$2`, "description");
  html = replaceOnce(html, /(<link rel="canonical" href=")[^"]*(")/, `$1${escAttr(url)}$2`, "canonical");
  html = replaceOnce(html, /(<meta property="og:title" content=")[^"]*(")/, `$1${escAttr(r.title)}$2`, "og:title");
  html = replaceOnce(html, /(<meta property="og:description" content=")[^"]*(")/, `$1${escAttr(r.description)}$2`, "og:description");
  html = replaceOnce(html, /(<meta property="og:url" content=")[^"]*(")/, `$1${escAttr(url)}$2`, "og:url");
  html = replaceOnce(html, /(<h1 id="seo-h1">)[\s\S]*?(<\/h1>)/, `$1${escHtml(r.h1)}$2`, "h1");
  html = replaceOnce(html, /(<p id="seo-intro">)[\s\S]*?(<\/p>)/, `$1${escHtml(r.body)}$2`, "intro");
  return html;
}

const base = readFileSync(join(DIST, "index.html"), "utf8");
for (const r of ROUTES) {
  const dir = join(DIST, r.path);
  mkdirSync(dir, { recursive: true });
  writeFileSync(join(dir, "index.html"), render(base, r));
}

writeFileSync(
  join(DIST, "robots.txt"),
  `User-agent: *\nAllow: /\nDisallow: /api/\n\nSitemap: ${SITE}/sitemap.xml\n`,
);

const SITEMAP = [
  { loc: "/", priority: "1.0" },
  { loc: "/salary", priority: "0.8" },
  { loc: "/skills", priority: "0.8" },
  { loc: "/employers", priority: "0.8" },
  { loc: "/geo", priority: "0.7" },
  { loc: "/vacancies", priority: "0.7" },
];
const urls = SITEMAP.map((u) =>
  `  <url>\n    <loc>${SITE}${u.loc}</loc>\n    <changefreq>daily</changefreq>\n    <priority>${u.priority}</priority>\n  </url>`,
).join("\n");
writeFileSync(
  join(DIST, "sitemap.xml"),
  `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls}\n</urlset>\n`,
);

console.log(`[prerender] site=${SITE}; wrote ${ROUTES.length} section pages + robots.txt + sitemap.xml`);
