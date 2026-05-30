from flask import Flask, jsonify, render_template, request

import config
import db_ops

app = Flask(__name__)

_matcher = None


def get_matcher():
    global _matcher
    if _matcher is None:
        from matcher import HybridMatcher
        _matcher = HybridMatcher()
    return _matcher


def _dimensions_payload() -> list[dict]:
    thresholds = config.load_thresholds()
    out = []
    for name, conf in config.DIMENSIONS.items():
        try:
            ref_count, cand_count = db_ops.get_counts(name)
            error = None
        except Exception as e:
            ref_count, cand_count = 0, 0
            error = str(e)
        out.append({
            "name": name,
            "label": conf["label"],
            "relation_type": conf["relation_type"],
            "threshold": thresholds[name],
            "ref_count": ref_count,
            "cand_count": cand_count,
            "error": error,
        })
    return out


@app.route("/")
def index():
    return render_template("index.html")


@app.route("/api/dimensions", methods=["GET"])
def api_dimensions():
    return jsonify({"ok": True, "dimensions": _dimensions_payload()})


@app.route("/api/refresh", methods=["POST"])
def api_refresh():
    return jsonify({"ok": True, "dimensions": _dimensions_payload()})


@app.route("/api/match", methods=["POST"])
def api_match():
    body = request.get_json(force=True)
    dim_name = body.get("dimension")
    threshold = body.get("threshold")

    if dim_name not in config.DIMENSIONS:
        return jsonify({"ok": False, "error": f"Неизвестное измерение: {dim_name}"}), 400

    if threshold is None:
        return jsonify({"ok": False, "error": "Не указан threshold"}), 400

    threshold = float(threshold)
    config.save_threshold(dim_name, threshold)

    try:
        golden_records = db_ops.get_records(dim_name, is_reference=True)
        candidate_records = db_ops.get_records(dim_name, is_reference=False)
    except Exception as e:
        return jsonify({"ok": False, "error": f"Ошибка БД: {e}"}), 500

    if not golden_records or not candidate_records:
        return jsonify({
            "ok": True,
            "dimension": dim_name,
            "threshold": threshold,
            "groups": [],
            "stats": {
                "golden_count": len(golden_records),
                "candidate_count": len(candidate_records),
                "total_matches": 0,
            },
        })

    matcher = get_matcher()
    matcher.prepare_golden(golden_records)

    grouped: dict[str, dict] = {}
    total_matches = 0
    for cand_id, cand_name in candidate_records:
        matches = matcher.find_best_matches(cand_name, threshold=threshold)
        if not matches:
            continue
        best = matches[0]
        total_matches += 1
        key = best["golden_name"]
        if key not in grouped:
            grouped[key] = {
                "golden_id": best["golden_id"],
                "golden_name": best["golden_name"],
                "candidates": [],
            }
        grouped[key]["candidates"].append({
            "id": cand_id,
            "name": cand_name,
            "score": best["final_score"],
            "sem_score": best["sem_score"],
            "fuzz_score": best["fuzz_score"],
        })

    for g in grouped.values():
        g["candidates"].sort(key=lambda c: c["score"], reverse=True)

    groups = sorted(grouped.values(), key=lambda g: len(g["candidates"]), reverse=True)

    return jsonify({
        "ok": True,
        "dimension": dim_name,
        "threshold": threshold,
        "groups": groups,
        "stats": {
            "golden_count": len(golden_records),
            "candidate_count": len(candidate_records),
            "total_matches": total_matches,
        },
    })


@app.route("/api/apply", methods=["POST"])
def api_apply():
    body = request.get_json(force=True)
    dim_name = body.get("dimension")
    merges = body.get("merges", [])

    if dim_name not in config.DIMENSIONS:
        return jsonify({"ok": False, "error": f"Неизвестное измерение: {dim_name}"}), 400

    if not merges:
        return jsonify({"ok": False, "error": "Пустой список объединений."}), 400

    relation_type = config.DIMENSIONS[dim_name]["relation_type"]

    try:
        db_ops.apply_normalization_batch(dim_name, relation_type, merges)
    except Exception as e:
        return jsonify({"ok": False, "error": f"Ошибка при применении: {e}"}), 500

    return jsonify({"ok": True, "applied": len(merges)})


if __name__ == "__main__":
    app.run(host=config.WEB_HOST, port=config.WEB_PORT, debug=True)
