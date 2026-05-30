import re
import torch
from sentence_transformers import SentenceTransformer, util
from rapidfuzz import fuzz
import config


class TextNormalizer:
    """Препроцессинг текста перед матчингом."""

    _version_re = re.compile(r"\s+\d[\d.]*(?:\s*(?:lts|rc|beta|alpha|sp)\d*)?$", re.IGNORECASE)
    _brackets_re = re.compile(r"\s*[\(\[\{].*?[\)\]\}]\s*")
    _multi_space_re = re.compile(r"\s{2,}")
    _punct_re = re.compile(r"[,;:!?\"'`~@#$%^&*+=<>{}|\\]")
    _slash_sep_re = re.compile(r"\s*/\s*")

    _char_map = str.maketrans("соаерхум", "coaepxym")

    def __init__(self):
        sorted_prefixes = sorted(config.NOISE_PREFIXES, key=len, reverse=True)
        pattern = "|".join(re.escape(p) for p in sorted_prefixes)
        self._noise_re = re.compile(rf"^(?:{pattern})\s+", re.IGNORECASE)

    def normalize(self, text: str) -> str:
        t = text.strip().lower()
        t = self._brackets_re.sub(" ", t)
        t = self._noise_re.sub("", t)
        t = self._punct_re.sub(" ", t)
        t = self._multi_space_re.sub(" ", t).strip()
        return t

    def strip_version(self, text: str) -> str:
        return self._version_re.sub("", text).strip()

    def visual_normalize(self, text: str) -> str:
        return text.translate(self._char_map)

    def tokenize(self, text: str) -> set[str]:
        return set(text.split())

    def split_slash(self, text: str) -> list[str]:
        parts = self._slash_sep_re.split(text)
        return [p.strip() for p in parts if p.strip()]


class HybridMatcher:
    def __init__(self):
        print(f"[matcher] Загрузка модели '{config.SEMANTIC_MODEL_NAME}'...")
        self.model = SentenceTransformer(config.SEMANTIC_MODEL_NAME)
        self.normalizer = TextNormalizer()
        self.golden_embeddings = None
        self.golden_names: list[str] = []
        self.golden_ids: list[int] = []
        self._golden_normalized: list[str] = []
        self._golden_no_ver: list[str] = []
        self._golden_tokens: list[set[str]] = []

    def prepare_golden(self, golden_records: list[tuple[int, str]]):
        if not golden_records:
            return

        self.golden_ids = [r[0] for r in golden_records]
        self.golden_names = [r[1] for r in golden_records]

        norm = self.normalizer
        self._golden_normalized = [norm.normalize(n) for n in self.golden_names]
        self._golden_no_ver = [norm.strip_version(n) for n in self._golden_normalized]
        self._golden_tokens = [norm.tokenize(n) for n in self._golden_normalized]

        encode_texts = self._golden_normalized
        print(f"[matcher] Кодирование {len(encode_texts)} эталонных записей...")
        self.golden_embeddings = self.model.encode(encode_texts, convert_to_tensor=True, show_progress_bar=False)
        print(f"[matcher] Готово.")

    def find_best_matches(self, cand_name: str, threshold: float) -> list[dict]:
        if self.golden_embeddings is None or not self.golden_names:
            return []

        norm = self.normalizer

        c_norm = norm.normalize(cand_name)
        c_no_ver = norm.strip_version(c_norm)
        c_vis = norm.visual_normalize(c_norm)
        c_tokens = norm.tokenize(c_norm)

        cand_embed = self.model.encode([c_norm], convert_to_tensor=True, show_progress_bar=False)
        cosine_scores = util.cos_sim(cand_embed, self.golden_embeddings)[0]

        matches = []
        for i, sem_tensor in enumerate(cosine_scores):
            sem_score = sem_tensor.item()

            g_norm = self._golden_normalized[i]
            g_no_ver = self._golden_no_ver[i]
            g_vis = norm.visual_normalize(g_norm)
            g_tokens = self._golden_tokens[i]
            golden_name = self.golden_names[i]
            golden_id = self.golden_ids[i]

            len_c = len(c_norm)
            len_g = len(g_norm)

            token_sort = fuzz.token_sort_ratio(c_vis, g_vis) / 100.0
            wratio = fuzz.WRatio(c_vis, g_vis) / 100.0

            union = c_tokens | g_tokens
            intersection = c_tokens & g_tokens
            jaccard = len(intersection) / len(union) if union else 0.0

            final_score = (
                sem_score * config.SEMANTIC_WEIGHT
                + token_sort * config.TOKEN_SORT_WEIGHT
                + wratio * config.WRATIO_WEIGHT
                + jaccard * config.JACCARD_WEIGHT
            )

            if c_no_ver == g_no_ver:
                final_score = max(final_score, 0.98)

            if c_tokens and g_tokens:
                if c_tokens <= g_tokens or g_tokens <= c_tokens:
                    final_score = max(final_score, threshold + 0.05)

            exact = fuzz.ratio(c_vis, g_vis) / 100.0

            if len_g <= 3 and len_c <= 3:
                if exact < 0.90:
                    final_score = 0.0
            elif len_g <= 2 and len_c > len_g + 2:
                final_score = 0.0
            elif len_c <= 2 and len_g > len_c + 2:
                final_score = 0.0
            elif max(len_g, len_c) <= 6 and exact < 0.80:
                if sem_score < 0.90:
                    final_score = 0.0

            if c_norm == g_norm:
                continue

            if final_score >= threshold:
                matches.append({
                    "golden_id": golden_id,
                    "golden_name": golden_name,
                    "candidate_name": cand_name,
                    "final_score": round(final_score, 4),
                    "sem_score": round(sem_score, 4),
                    "fuzz_score": round(wratio, 4),
                    "token_sort": round(token_sort, 4),
                    "jaccard": round(jaccard, 4),
                })

        matches.sort(key=lambda x: x["final_score"], reverse=True)
        return matches
