from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
from typing import Any, Dict, List, Optional

from dotenv import load_dotenv
load_dotenv(Path(__file__).resolve().parents[1] / ".env", override=True)

from fastapi import BackgroundTasks, Body, FastAPI, HTTPException
from fastapi.responses import FileResponse, Response
from fastapi.staticfiles import StaticFiles

try:
    from .services import insight_service
    from .services import insight_repo
    from .services import insight_job_service
except ImportError:
    from services import insight_service  # type: ignore
    from services import insight_repo  # type: ignore
    from services import insight_job_service  # type: ignore


ROOT_DIR = Path(__file__).resolve().parents[1]

app = FastAPI(title="Lufax Dashboard Backend", version="0.2.0")


@app.on_event("startup")
def _startup() -> None:
    insight_repo.init_db()


@app.get("/api/health")
def health() -> Dict[str, Any]:
    return {
        "status": "ok",
        "service": "lufax-dashboard-backend",
        "validation": insight_service.validation_status(),
    }


@app.get("/api/dashboard/data")
def dashboard_data() -> Dict[str, Any]:
    try:
        return insight_service.get_dashboard_data()
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Failed to load dashboard data: {exc}") from exc


@app.get("/api/insights/page-state")
def insights_page_state(period: str = "") -> Dict[str, Any]:
    if not period:
        period = "2026-03-YTD"
    return insight_repo.get_page_state(period)


@app.get("/api/insights/cards")
def insights_cards(period: str = "") -> Dict[str, Any]:
    if not period:
        period = "2026-03-YTD"
    metric_version = insight_repo.get_metric_version(period)
    prompt_version = insight_repo.get_current_prompt_version()
    results = insight_repo.find_insight_results_by_type(period, metric_version, prompt_version, "card")
    cards = []
    for r in results:
        if r.get("status") == "ready":
            cards.append(insight_repo._hydrate_card_result(r))
        elif r.get("status") == "failed":
            cards.append({
                "card_id": r.get("card_id", ""),
                "status": "failed",
                "standard_insight": r.get("error_message") or "洞察生成失败",
                "drivers": [],
                "watch_items": [],
                "evidence": [],
                "exploratory_insights": [],
                "deep_dive_questions": [],
            })
        else:
            cards.append({"card_id": r.get("card_id", ""), "status": r.get("status", "missing"), "standard_insight": ""})
    return {"period": period, "metric_version": metric_version, "prompt_version": prompt_version, "cards": cards}


@app.get("/api/insights/dashboard")
def insights_dashboard(period: str = "") -> Dict[str, Any]:
    if not period:
        period = "2026-03-YTD"
    metric_version = insight_repo.get_metric_version(period)
    prompt_version = insight_repo.get_current_prompt_version()
    r = insight_repo.find_insight_result(period, metric_version, prompt_version, "dashboard", "")
    if r and r.get("status") == "ready":
        return {"period": period, "status": "ready", **insight_repo._hydrate_dashboard_result(r)}
    latest = insight_repo.find_latest_ready_result(period, prompt_version, "dashboard", "")
    if latest:
        hydrated = insight_repo._hydrate_dashboard_result(latest)
        hydrated["stale"] = True
        return {"period": period, "status": "ready", **hydrated}
    if r and r.get("status") == "failed":
        return {"period": period, "status": "failed", "error_message": r.get("error_message") or "动态解读生成失败"}
    if r and r.get("status") in ("pending", "running"):
        return {"period": period, "status": "running"}
    return {"period": period, "status": "missing"}


@app.post("/api/insights/jobs")
def create_insight_job(background_tasks: BackgroundTasks, payload: Any = Body(default_factory=dict)) -> Dict[str, Any]:
    payload = _require_payload_dict(payload)
    period = payload.get("period", "2026-03-YTD")
    scope = payload.get("scope", "all")
    card_ids = payload.get("card_ids") or []
    force_refresh = bool(payload.get("force_refresh", False))

    if scope not in ("all", "cards", "dashboard", "card"):
        raise HTTPException(status_code=400, detail="scope must be one of: all, cards, dashboard, card")

    job_id = insight_job_service.create_insight_job(
        period=period,
        scope=scope,
        card_ids=card_ids if card_ids else None,
        force_refresh=force_refresh,
    )
    if not job_id:
        return {"job_id": None, "message": "All items already cached, no job created"}

    if force_refresh and scope in ("all", "dashboard"):
        background_tasks.add_task(_warm_default_scenario_cache, period, True)

    return {"job_id": job_id, "period": period, "scope": scope, "status": "pending"}


@app.get("/api/insights/jobs/{job_id}")
def get_insight_job(job_id: str) -> Dict[str, Any]:
    job = insight_repo.get_job(job_id)
    if job is None:
        raise HTTPException(status_code=404, detail="Job not found")
    items = insight_repo.get_job_items(job_id)
    return {
        "job_id": job["job_id"],
        "period": job["period"],
        "metric_version": job["metric_version"],
        "prompt_version": job["prompt_version"],
        "job_type": job["job_type"],
        "status": job["status"],
        "total_count": job["total_count"],
        "finished_count": job["finished_count"],
        "failed_count": job["failed_count"],
        "force_refresh": bool(job.get("force_refresh")),
        "created_at": job.get("created_at"),
        "started_at": job.get("started_at"),
        "finished_at": job.get("finished_at"),
        "error_message": job.get("error_message", ""),
        "items": [
            {
                "id": it["id"],
                "insight_type": it["insight_type"],
                "card_id": it.get("card_id", ""),
                "status": it["status"],
                "retry_count": it.get("retry_count", 0),
                "error_message": it.get("error_message", ""),
            }
            for it in items
        ],
    }


@app.post("/api/insights/scenario")
def scenario_insight(payload: Any = Body(default_factory=dict)) -> Dict[str, Any]:
    payload = _require_payload_dict(payload)
    scenario_id = str(payload.get("scenario_id") or payload.get("scenario") or "base")
    period = _optional_str(payload.get("period")) or "2026-03-YTD"
    metric_version = insight_repo.get_metric_version(period)
    prompt_version = insight_repo.get_current_prompt_version()
    params_hash = _compute_scenario_hash(payload, scenario_id, period)

    cached = insight_repo.find_scenario_cache(params_hash, period, metric_version, prompt_version)
    if not cached:
        cached = insight_repo.find_matching_scenario_cache(payload, scenario_id, period, metric_version, prompt_version)
    if cached and cached.get("status") == "ready":
        raw = cached.get("raw_llm_output_json", "{}")
        try:
            result = json.loads(raw) if isinstance(raw, str) else raw
            return {"status": "ready", **result} if isinstance(result, dict) else {"status": "ready", "data": result}
        except (json.JSONDecodeError, TypeError):
            pass

    if bool(payload.get("cache_only", False)):
        return {
            "period": period,
            "scenario": scenario_id,
            "status": "missing",
            "message": "当前参数组合暂无AI预测解读缓存，请点击生成AI情景解读。",
        }

    try:
        fact_pack = insight_service.build_scenario_fact_pack(
            scenario_id=scenario_id,
            overrides=payload,
            period=period,
        )
        result = insight_service.generate_scenario_insight(
            scenario_id=scenario_id,
            overrides=payload,
            period=period,
        )
        insight_repo.save_scenario_result(params_hash, result, period, fact_pack)
        return {"status": "ready", **result}
    except insight_service.InsightValidationError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail="Failed to generate scenario insight") from exc


@app.post("/api/insights/card")
def card_insight_legacy(payload: Any = Body(default_factory=dict)) -> Dict[str, Any]:
    payload = _require_payload_dict(payload)
    card_id = payload.get("card_id") or payload.get("id")
    if not card_id:
        raise HTTPException(status_code=400, detail="card_id is required")
    period = _optional_str(payload.get("period")) or "2026-03-YTD"
    metric_version = insight_repo.get_metric_version(period)
    prompt_version = insight_repo.get_current_prompt_version()
    cached = insight_repo.find_insight_result(period, metric_version, prompt_version, "card", str(card_id))
    if cached and cached.get("status") == "ready":
        raw = cached.get("raw_llm_output_json", "{}")
        try:
            return json.loads(raw) if isinstance(raw, str) else raw
        except (json.JSONDecodeError, TypeError):
            pass
    try:
        return insight_service.generate_card_insight(str(card_id), period)
    except ValueError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except insight_service.InsightValidationError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail="Failed to generate card insight") from exc


@app.post("/api/insights/dashboard")
def dashboard_insight_legacy(payload: Any = Body(default_factory=dict)) -> Dict[str, Any]:
    payload = _require_payload_dict(payload)
    period = _optional_str(payload.get("period")) or "2026-03-YTD"
    metric_version = insight_repo.get_metric_version(period)
    prompt_version = insight_repo.get_current_prompt_version()
    cached = insight_repo.find_insight_result(period, metric_version, prompt_version, "dashboard", "")
    if cached and cached.get("status") == "ready":
        raw = cached.get("raw_llm_output_json", "{}")
        try:
            return json.loads(raw) if isinstance(raw, str) else raw
        except (json.JSONDecodeError, TypeError):
            pass
    try:
        fact_pack = insight_service.build_dashboard_fact_pack(period)
        result = insight_service.generate_dashboard_insight(period)
        insight_repo.upsert_insight_result({
            "period": period,
            "metric_version": metric_version,
            "prompt_version": prompt_version,
            "schema_name": "dashboard_insight.schema.json",
            "insight_type": "dashboard",
            "card_id": "",
            "status": "ready",
            "title": result.get("health_label", ""),
            "standard_insight": result.get("standard_diagnosis", {}).get("health_summary", ""),
            "standard_diagnosis_json": json.dumps(result.get("standard_diagnosis", {}), ensure_ascii=False),
            "drivers_json": json.dumps(result.get("root_cause_chain", []), ensure_ascii=False),
            "watch_items_json": json.dumps(result.get("watch_items", []), ensure_ascii=False),
            "evidence_json": json.dumps(result.get("offset_factors", []), ensure_ascii=False),
            "emerging_findings_json": json.dumps(result.get("emerging_findings", []), ensure_ascii=False),
            "management_questions_json": json.dumps(result.get("management_questions", []), ensure_ascii=False),
            "raw_fact_pack_json": json.dumps(fact_pack, ensure_ascii=False),
            "raw_llm_output_json": json.dumps(result, ensure_ascii=False),
            "validated": True,
            "finished_at": insight_repo._now_iso(),
            "updated_at": insight_repo._now_iso(),
        })
        return result
    except insight_service.InsightValidationError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail="Failed to generate dashboard insight") from exc


@app.post("/api/insights/manual-override")
def manual_override(payload: Any = Body(default_factory=dict)) -> Dict[str, Any]:
    payload = _require_payload_dict(payload)
    return {
        "status": "accepted",
        "accepted": True,
        "message": "Manual override accepted by mock backend.",
        "payload": payload,
    }


@app.post("/api/upload/excel")
def upload_excel(background_tasks: BackgroundTasks, payload: Any = Body(default_factory=dict)) -> Dict[str, Any]:
    payload = _require_payload_dict(payload)
    period = payload.get("period", "2026-03-YTD")
    job_id = insight_job_service.create_insight_job(
        period=period,
        scope="all",
        force_refresh=True,
    )
    background_tasks.add_task(_warm_default_scenario_cache, period, True)
    return {
        "status": "accepted",
        "period": period,
        "insight_job_id": job_id,
        "message": "Excel data accepted. Insight generation job created." if job_id else "Excel data accepted. All insights already cached.",
    }


@app.get("/lufax_dashboard3.html")
def lufax_dashboard() -> FileResponse:
    return FileResponse(ROOT_DIR / "lufax_dashboard3.html")


@app.get("/ai_insights.html")
def ai_insights_page() -> FileResponse:
    return FileResponse(ROOT_DIR / "ai_insights.html")


@app.get("/favicon.ico")
def favicon() -> Response:
    return Response(status_code=204)


app.mount("/", StaticFiles(directory=ROOT_DIR, html=True), name="static")


def _optional_str(value: Any) -> Optional[str]:
    return str(value) if value is not None else None


def _require_payload_dict(payload: Any) -> Dict[str, Any]:
    if not isinstance(payload, dict):
        raise HTTPException(status_code=400, detail="payload must be an object")
    return payload


def _entry_value(entry: Any) -> Any:
    if isinstance(entry, dict):
        return entry.get("value")
    return entry


def _canonical_scenario_payload(fact_pack: Dict[str, Any]) -> Dict[str, Any]:
    inputs = fact_pack.get("inputs") if isinstance(fact_pack.get("inputs"), dict) else {}
    computed = fact_pack.get("computed") if isinstance(fact_pack.get("computed"), dict) else {}
    sensitivity = fact_pack.get("sensitivity") if isinstance(fact_pack.get("sensitivity"), dict) else {}
    return {
        "period": fact_pack.get("period") or "2026-03-YTD",
        "language": "zh-CN",
        "scenario": fact_pack.get("scenario") or "base",
        "scenario_label": fact_pack.get("scenario_label") or fact_pack.get("scenario") or "base",
        "inputs": {
            key: _entry_value(inputs.get(key))
            for key in (
                "anr",
                "consumer_finance_growth",
                "pricing_rate",
                "credit_loss_rate",
                "npl_rate",
                "funding_cost_rate",
                "sales_cost_rate",
                "opex_rate",
                "tax_other_rate",
                "non_loan_profit",
            )
        },
        "computed": {
            key: _entry_value(computed.get(key))
            for key in ("total_cost_rate", "roa", "net_profit", "revenue", "margin")
        },
        "sensitivity": {
            key: _entry_value(sensitivity.get(key))
            for key in ("credit_loss_0_5pct_impact", "pricing_0_5pct_impact")
        },
    }


def _warm_default_scenario_cache(period: str, force_refresh: bool = False) -> None:
    metric_version = insight_repo.get_metric_version(period)
    prompt_version = insight_repo.get_current_prompt_version()
    data = insight_service.get_dashboard_data()
    scenario_defaults = data.get("scenario_defaults") if isinstance(data.get("scenario_defaults"), list) else []
    scenario_ids = [str(item.get("scenario")) for item in scenario_defaults if item.get("scenario")] or ["bear", "base", "bull"]
    for scenario_id in scenario_ids:
        try:
            fact_pack = insight_service.build_scenario_fact_pack(scenario_id=scenario_id, overrides={}, period=period)
            canonical_payload = _canonical_scenario_payload(fact_pack)
            params_hash = _compute_scenario_hash(canonical_payload, scenario_id, period)
            cached = insight_repo.find_scenario_cache(params_hash, period, metric_version, prompt_version)
            if cached and cached.get("status") == "ready" and not force_refresh:
                continue
            result = insight_service.generate_scenario_insight(
                scenario_id=scenario_id,
                overrides=canonical_payload,
                period=period,
            )
            insight_repo.save_scenario_result(params_hash, result, period, fact_pack)
        except Exception:
            # Scenario cache warming must not block data import or dashboard refresh.
            continue


def _compute_scenario_hash(payload: Dict[str, Any], scenario_id: str, period: str) -> str:
    inputs = payload.get("inputs") if isinstance(payload.get("inputs"), dict) else {}
    computed = payload.get("computed") if isinstance(payload.get("computed"), dict) else {}
    sensitivity = payload.get("sensitivity") if isinstance(payload.get("sensitivity"), dict) else {}
    flat_inputs = {
        k: payload[k]
        for k in sorted(payload.keys())
        if k not in (
            "period",
            "scenario",
            "scenario_id",
            "scenario_label",
            "inputs",
            "computed",
            "sensitivity",
            "language",
            "cache_only",
            "force_generate",
        )
    }
    hash_parts = {
        "period": period,
        "scenario_id": scenario_id,
        "flat_inputs": flat_inputs,
        "inputs": {k: inputs[k] for k in sorted(inputs.keys()) if k not in ("period",)},
        "computed": {k: computed[k] for k in sorted(computed.keys()) if k not in ("period", "margin")},
        "sensitivity": {k: sensitivity[k] for k in sorted(sensitivity.keys())},
    }
    raw = json.dumps(hash_parts, sort_keys=True, ensure_ascii=False)
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()[:16]
