from __future__ import annotations

import json
import hashlib
import sqlite3
import threading
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional

BACKEND_DIR = Path(__file__).resolve().parents[1]
DB_PATH = BACKEND_DIR / "data" / "insight.db"

_local = threading.local()


def _get_conn() -> sqlite3.Connection:
    if not hasattr(_local, "conn") or _local.conn is None:
        _local.conn = sqlite3.connect(str(DB_PATH), check_same_thread=False)
        _local.conn.row_factory = sqlite3.Row
        _local.conn.execute("PRAGMA journal_mode=WAL")
        _local.conn.execute("PRAGMA foreign_keys=ON")
    return _local.conn


def init_db() -> None:
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    conn = _get_conn()
    sql_path = BACKEND_DIR / "sql" / "init.sql"
    if sql_path.exists():
        conn.executescript(sql_path.read_text(encoding="utf-8"))
    else:
        conn.executescript(_INLINE_DDL)
    conn.commit()


_INLINE_DDL = """
CREATE TABLE IF NOT EXISTS insight_result (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    period TEXT NOT NULL,
    metric_version TEXT NOT NULL DEFAULT '',
    prompt_version TEXT NOT NULL DEFAULT 'v2',
    schema_name TEXT NOT NULL DEFAULT '',
    insight_type TEXT NOT NULL,
    card_id TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'pending',
    title TEXT NOT NULL DEFAULT '',
    standard_insight TEXT NOT NULL DEFAULT '',
    standard_diagnosis_json TEXT NOT NULL DEFAULT '',
    standard_explanation_json TEXT NOT NULL DEFAULT '',
    drivers_json TEXT NOT NULL DEFAULT '[]',
    watch_items_json TEXT NOT NULL DEFAULT '[]',
    evidence_json TEXT NOT NULL DEFAULT '[]',
    exploratory_insights_json TEXT NOT NULL DEFAULT '[]',
    emerging_findings_json TEXT NOT NULL DEFAULT '[]',
    scenario_exploration_json TEXT NOT NULL DEFAULT '{}',
    deep_dive_questions_json TEXT NOT NULL DEFAULT '[]',
    management_questions_json TEXT NOT NULL DEFAULT '[]',
    raw_fact_pack_json TEXT NOT NULL DEFAULT '{}',
    raw_llm_output_json TEXT NOT NULL DEFAULT '{}',
    validated INTEGER NOT NULL DEFAULT 0,
    validation_error TEXT NOT NULL DEFAULT '',
    error_message TEXT NOT NULL DEFAULT '',
    started_at TEXT,
    finished_at TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    UNIQUE(period, metric_version, prompt_version, insight_type, card_id)
);

CREATE TABLE IF NOT EXISTS insight_job (
    job_id TEXT PRIMARY KEY,
    period TEXT NOT NULL,
    metric_version TEXT NOT NULL DEFAULT '',
    prompt_version TEXT NOT NULL DEFAULT 'v2',
    job_type TEXT NOT NULL DEFAULT 'all',
    status TEXT NOT NULL DEFAULT 'pending',
    total_count INTEGER NOT NULL DEFAULT 0,
    finished_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    force_refresh INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    started_at TEXT,
    finished_at TEXT,
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    error_message TEXT NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS insight_job_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id TEXT NOT NULL,
    insight_type TEXT NOT NULL,
    card_id TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'pending',
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT NOT NULL DEFAULT '',
    started_at TEXT,
    finished_at TEXT,
    updated_at TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (job_id) REFERENCES insight_job(job_id)
);

CREATE INDEX IF NOT EXISTS idx_insight_result_lookup
    ON insight_result(period, metric_version, prompt_version, insight_type, card_id);

CREATE INDEX IF NOT EXISTS idx_insight_result_status
    ON insight_result(status);

CREATE INDEX IF NOT EXISTS idx_insight_job_period_status
    ON insight_job(period, status);

CREATE INDEX IF NOT EXISTS idx_insight_job_item_job
    ON insight_job_item(job_id);
"""


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def _row_to_dict(row: Optional[sqlite3.Row]) -> Optional[Dict[str, Any]]:
    if row is None:
        return None
    return dict(row)


def _rows_to_dicts(rows: List[sqlite3.Row]) -> List[Dict[str, Any]]:
    return [dict(r) for r in rows]


def get_metric_version(period: str) -> str:
    try:
        import fact_builder as fb  # type: ignore
    except ImportError:
        from . import fact_builder as fb  # type: ignore
    data = fb.load_dashboard_data()
    cards = fb.load_card_config()
    base_version = data.get("metric_version", "mock_v2")
    version_payload = {
        "period": period or data.get("period", ""),
        "core_metrics": data.get("core_metrics", []),
        "segment_metrics": data.get("segment_metrics", []),
        "scenario_defaults": data.get("scenario_defaults", []),
        "rules": data.get("rules", {}),
        "cards": cards,
    }
    digest = hashlib.sha1(
        json.dumps(version_payload, ensure_ascii=False, sort_keys=True).encode("utf-8")
    ).hexdigest()[:10]
    return f"{base_version}:{digest}"


def get_current_prompt_version() -> str:
    return "v2"


def find_insight_result(
    period: str,
    metric_version: str,
    prompt_version: str,
    insight_type: str,
    card_id: str,
) -> Optional[Dict[str, Any]]:
    conn = _get_conn()
    row = conn.execute(
        "SELECT * FROM insight_result WHERE period=? AND metric_version=? AND prompt_version=? AND insight_type=? AND card_id=?",
        (period, metric_version, prompt_version, insight_type, card_id),
    ).fetchone()
    return _row_to_dict(row)


def find_insight_results_by_type(
    period: str,
    metric_version: str,
    prompt_version: str,
    insight_type: str,
) -> List[Dict[str, Any]]:
    conn = _get_conn()
    rows = conn.execute(
        "SELECT * FROM insight_result WHERE period=? AND metric_version=? AND prompt_version=? AND insight_type=?",
        (period, metric_version, prompt_version, insight_type),
    ).fetchall()
    return _rows_to_dicts(rows)


def find_latest_ready_result(
    period: str,
    prompt_version: str,
    insight_type: str,
    card_id: str,
) -> Optional[Dict[str, Any]]:
    conn = _get_conn()
    row = conn.execute(
        """SELECT * FROM insight_result
           WHERE period=? AND prompt_version=? AND insight_type=? AND card_id=? AND status='ready'
           ORDER BY updated_at DESC LIMIT 1""",
        (period, prompt_version, insight_type, card_id),
    ).fetchone()
    return _row_to_dict(row)


def upsert_insight_result(r: Dict[str, Any]) -> None:
    conn = _get_conn()
    conn.execute(
        """INSERT INTO insight_result
            (period, metric_version, prompt_version, schema_name, insight_type, card_id,
             status, title, standard_insight, standard_diagnosis_json, standard_explanation_json,
             drivers_json, watch_items_json, evidence_json, exploratory_insights_json,
             emerging_findings_json, scenario_exploration_json, deep_dive_questions_json,
             management_questions_json, raw_fact_pack_json, raw_llm_output_json,
             validated, validation_error, error_message, started_at, finished_at, updated_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(period, metric_version, prompt_version, insight_type, card_id)
            DO UPDATE SET
                status=excluded.status, title=excluded.title,
                standard_insight=excluded.standard_insight,
                standard_diagnosis_json=excluded.standard_diagnosis_json,
                standard_explanation_json=excluded.standard_explanation_json,
                drivers_json=excluded.drivers_json, watch_items_json=excluded.watch_items_json,
                evidence_json=excluded.evidence_json,
                exploratory_insights_json=excluded.exploratory_insights_json,
                emerging_findings_json=excluded.emerging_findings_json,
                scenario_exploration_json=excluded.scenario_exploration_json,
                deep_dive_questions_json=excluded.deep_dive_questions_json,
                management_questions_json=excluded.management_questions_json,
                raw_fact_pack_json=excluded.raw_fact_pack_json,
                raw_llm_output_json=excluded.raw_llm_output_json,
                validated=excluded.validated, validation_error=excluded.validation_error,
                error_message=excluded.error_message,
                started_at=excluded.started_at, finished_at=excluded.finished_at,
                updated_at=excluded.updated_at
        """,
        (
            r.get("period", ""), r.get("metric_version", ""), r.get("prompt_version", "v2"),
            r.get("schema_name", ""), r.get("insight_type", ""), r.get("card_id", ""),
            r.get("status", "pending"), r.get("title", ""), r.get("standard_insight", ""),
            r.get("standard_diagnosis_json", ""), r.get("standard_explanation_json", ""),
            r.get("drivers_json", "[]"), r.get("watch_items_json", "[]"),
            r.get("evidence_json", "[]"), r.get("exploratory_insights_json", "[]"),
            r.get("emerging_findings_json", "[]"), r.get("scenario_exploration_json", "{}"),
            r.get("deep_dive_questions_json", "[]"), r.get("management_questions_json", "[]"),
            r.get("raw_fact_pack_json", "{}"), r.get("raw_llm_output_json", "{}"),
            r.get("validated", 0), r.get("validation_error", ""),
            r.get("error_message", ""), r.get("started_at"), r.get("finished_at"),
            _now_iso(),
        ),
    )
    conn.commit()


def update_insight_result_status(
    period: str,
    metric_version: str,
    prompt_version: str,
    insight_type: str,
    card_id: str,
    status: str,
    error_message: str = "",
    started_at: Optional[str] = None,
    finished_at: Optional[str] = None,
) -> None:
    conn = _get_conn()
    parts = ["status=?", "error_message=?", "updated_at=?"]
    vals: List[Any] = [status, error_message, _now_iso()]
    if started_at is not None:
        parts.append("started_at=?")
        vals.append(started_at)
    if finished_at is not None:
        parts.append("finished_at=?")
        vals.append(finished_at)
    vals.extend([period, metric_version, prompt_version, insight_type, card_id])
    conn.execute(
        f"UPDATE insight_result SET {', '.join(parts)} WHERE period=? AND metric_version=? AND prompt_version=? AND insight_type=? AND card_id=?",
        vals,
    )
    conn.commit()


def create_job(
    period: str,
    metric_version: str,
    prompt_version: str,
    job_type: str,
    total_count: int,
    force_refresh: bool = False,
) -> str:
    job_id = uuid.uuid4().hex[:12]
    conn = _get_conn()
    conn.execute(
        """INSERT INTO insight_job
            (job_id, period, metric_version, prompt_version, job_type, status, total_count, finished_count, failed_count, force_refresh, created_at, updated_at)
            VALUES (?,?,?,?,?,?,?,0,0,?,?,?)""",
        (job_id, period, metric_version, prompt_version, job_type, "pending", total_count, 1 if force_refresh else 0, _now_iso(), _now_iso()),
    )
    conn.commit()
    return job_id


def get_job(job_id: str) -> Optional[Dict[str, Any]]:
    conn = _get_conn()
    row = conn.execute("SELECT * FROM insight_job WHERE job_id=?", (job_id,)).fetchone()
    return _row_to_dict(row)


def get_active_jobs(period: str) -> List[Dict[str, Any]]:
    conn = _get_conn()
    rows = conn.execute(
        "SELECT * FROM insight_job WHERE period=? AND status IN ('pending','running') ORDER BY created_at DESC",
        (period,),
    ).fetchall()
    return _rows_to_dicts(rows)


def has_running_job(period: str, metric_version: str, job_type: str) -> bool:
    conn = _get_conn()
    row = conn.execute(
        "SELECT 1 FROM insight_job WHERE period=? AND metric_version=? AND job_type=? AND status IN ('pending','running') LIMIT 1",
        (period, metric_version, job_type),
    ).fetchone()
    return row is not None


def update_job_status(
    job_id: str,
    status: str,
    finished_count: Optional[int] = None,
    failed_count: Optional[int] = None,
    error_message: str = "",
    started_at: Optional[str] = None,
    finished_at: Optional[str] = None,
) -> None:
    conn = _get_conn()
    parts = ["status=?", "error_message=?", "updated_at=?"]
    vals: List[Any] = [status, error_message, _now_iso()]
    if finished_count is not None:
        parts.append("finished_count=?")
        vals.append(finished_count)
    if failed_count is not None:
        parts.append("failed_count=?")
        vals.append(failed_count)
    if started_at is not None:
        parts.append("started_at=?")
        vals.append(started_at)
    if finished_at is not None:
        parts.append("finished_at=?")
        vals.append(finished_at)
    vals.append(job_id)
    conn.execute(
        f"UPDATE insight_job SET {', '.join(parts)} WHERE job_id=?", vals
    )
    conn.commit()


def create_job_items(job_id: str, items: List[Dict[str, Any]]) -> None:
    conn = _get_conn()
    conn.executemany(
        "INSERT INTO insight_job_item (job_id, insight_type, card_id, status, updated_at) VALUES (?,?,?,?,?)",
        [(job_id, it.get("insight_type", ""), it.get("card_id", ""), "pending", _now_iso()) for it in items],
    )
    conn.commit()


def get_job_items(job_id: str) -> List[Dict[str, Any]]:
    conn = _get_conn()
    rows = conn.execute(
        "SELECT * FROM insight_job_item WHERE job_id=? ORDER BY id", (job_id,)
    ).fetchall()
    return _rows_to_dicts(rows)


def get_pending_job_items(job_id: str) -> List[Dict[str, Any]]:
    conn = _get_conn()
    rows = conn.execute(
        "SELECT * FROM insight_job_item WHERE job_id=? AND status IN ('pending','running') ORDER BY id",
        (job_id,),
    ).fetchall()
    return _rows_to_dicts(rows)


def update_job_item_status(
    item_id: int,
    status: str,
    retry_count: Optional[int] = None,
    error_message: str = "",
    started_at: Optional[str] = None,
    finished_at: Optional[str] = None,
) -> None:
    conn = _get_conn()
    parts = ["status=?", "error_message=?", "updated_at=?"]
    vals: List[Any] = [status, error_message, _now_iso()]
    if retry_count is not None:
        parts.append("retry_count=?")
        vals.append(retry_count)
    if started_at is not None:
        parts.append("started_at=?")
        vals.append(started_at)
    if finished_at is not None:
        parts.append("finished_at=?")
        vals.append(finished_at)
    vals.append(item_id)
    conn.execute(
        f"UPDATE insight_job_item SET {', '.join(parts)} WHERE id=?", vals
    )
    conn.commit()


def count_job_item_statuses(job_id: str) -> Dict[str, int]:
    conn = _get_conn()
    rows = conn.execute(
        "SELECT status, COUNT(*) as cnt FROM insight_job_item WHERE job_id=? GROUP BY status",
        (job_id,),
    ).fetchall()
    return {r["status"]: r["cnt"] for r in rows}


def is_cache_hit(
    period: str,
    metric_version: str,
    prompt_version: str,
    insight_type: str,
    card_id: str,
) -> bool:
    result = find_insight_result(period, metric_version, prompt_version, insight_type, card_id)
    if result is None:
        return False
    return result.get("status") == "ready"


def is_cache_stale(
    period: str,
    metric_version: str,
    prompt_version: str,
    insight_type: str,
    card_id: str,
    current_metric_version: str,
    current_prompt_version: str,
) -> bool:
    if metric_version != current_metric_version:
        return True
    if prompt_version != current_prompt_version:
        return True
    return False


def get_page_state(period: str) -> Dict[str, Any]:
    metric_version = get_metric_version(period)
    prompt_version = get_current_prompt_version()

    try:
        import fact_builder as fb  # type: ignore
    except ImportError:
        from . import fact_builder as fb  # type: ignore
    cards = fb.load_card_config()

    card_ids = [c.get("card_id") for c in cards if c.get("enabled", True)]

    card_states: List[Dict[str, Any]] = []
    for cid in card_ids:
        result = find_insight_result(period, metric_version, prompt_version, "card", cid)
        if result and result.get("status") == "ready":
            card_states.append(_hydrate_card_result(result))
        elif result and result.get("status") == "failed":
            card_states.append({
                "card_id": cid,
                "status": "failed",
                "standard_insight": result.get("error_message") or "洞察生成失败，请稍后重试",
                "drivers": [],
                "watch_items": [],
                "evidence": [],
                "exploratory_insights": [],
                "deep_dive_questions": [],
            })
        elif result and result.get("status") in ("pending", "running"):
            stale = find_latest_ready_result(period, prompt_version, "card", cid)
            if stale:
                hydrated = _hydrate_card_result(stale)
                hydrated["stale"] = True
                card_states.append(hydrated)
            else:
                card_states.append({"card_id": cid, "status": "running", "standard_insight": ""})
        else:
            stale = find_latest_ready_result(period, prompt_version, "card", cid)
            if stale:
                hydrated = _hydrate_card_result(stale)
                hydrated["stale"] = True
                card_states.append(hydrated)
            else:
                card_states.append({"card_id": cid, "status": "missing", "standard_insight": ""})

    dashboard_result = find_insight_result(period, metric_version, prompt_version, "dashboard", "")
    dashboard_state: Dict[str, Any]
    if dashboard_result and dashboard_result.get("status") == "ready":
        dashboard_state = _hydrate_dashboard_result(dashboard_result)
    elif dashboard_result and dashboard_result.get("status") == "failed":
        dashboard_state = {
            "status": "failed",
            "error_message": dashboard_result.get("error_message") or "动态解读生成失败",
        }
    elif dashboard_result and dashboard_result.get("status") in ("pending", "running"):
        dashboard_state = {"status": "running"}
    else:
        dashboard_state = {"status": "missing"}

    active_jobs = get_active_jobs(period)
    calibrated_jobs = []
    for j in active_jobs:
        item_counts = count_job_item_statuses(j["job_id"])
        real_finished = item_counts.get("ready", 0) + item_counts.get("failed", 0) + item_counts.get("skipped", 0)
        real_failed = item_counts.get("failed", 0)
        all_done = real_finished >= j.get("total_count", 0) and j.get("total_count", 0) > 0
        
        created_at = datetime.fromisoformat(j["created_at"])
        job_age_seconds = (datetime.now(timezone.utc) - created_at).total_seconds()
        
        if all_done:
            final_status = "partial_failed" if real_failed > 0 and real_failed < j["total_count"] else ("failed" if real_failed == j["total_count"] else "finished")
            update_job_status(j["job_id"], final_status, finished_count=real_finished, failed_count=real_failed, finished_at=_now_iso())
            continue
        
        if job_age_seconds > 600:
            remaining_items = j.get("total_count", 0) - real_finished
            final_status = "partial_failed" if real_failed > 0 else "failed"
            update_job_status(j["job_id"], final_status, finished_count=real_finished, failed_count=real_failed + remaining_items, finished_at=_now_iso())
            conn = _get_conn()
            conn.execute(
                "UPDATE insight_job_item SET status='failed', error_message='任务超时', finished_at=? WHERE job_id=? AND status IN ('pending','running')",
                (_now_iso(), j["job_id"])
            )
            # Also update insight_result for timed-out items
            conn.execute(
                """UPDATE insight_result SET status='failed', error_message='任务超时', finished_at=?
                    WHERE period=? AND metric_version=? AND prompt_version=?
                    AND status IN ('pending','running')
                    AND EXISTS (
                        SELECT 1 FROM insight_job_item 
                        WHERE job_id=? AND insight_result.insight_type=insight_job_item.insight_type 
                        AND insight_result.card_id=insight_job_item.card_id
                    )""",
                (_now_iso(), j["period"], j["metric_version"], j["prompt_version"], j["job_id"])
            )
            conn.commit()
            continue
        
        calibrated_jobs.append({
            "job_id": j.get("job_id"),
            "job_type": j.get("job_type"),
            "status": j.get("status"),
            "total_count": j.get("total_count", 0),
            "finished_count": max(real_finished, j.get("finished_count", 0)),
            "failed_count": max(real_failed, j.get("failed_count", 0)),
        })

    return {
        "period": period,
        "metric_version": metric_version,
        "prompt_version": prompt_version,
        "cards": card_states,
        "dashboard": dashboard_state,
        "active_jobs": calibrated_jobs,
    }


def _hydrate_card_result(r: Dict[str, Any]) -> Dict[str, Any]:
    return {
        "card_id": r.get("card_id", ""),
        "status": "ready",
        "title": r.get("title", ""),
        "standard_insight": r.get("standard_insight", ""),
        "drivers": _safe_json(r.get("drivers_json", "[]")),
        "watch_items": _safe_json(r.get("watch_items_json", "[]")),
        "evidence": _safe_json(r.get("evidence_json", "[]")),
        "exploratory_insights": _safe_json(r.get("exploratory_insights_json", "[]")),
        "deep_dive_questions": _safe_json(r.get("deep_dive_questions_json", "[]")),
    }


def _hydrate_dashboard_result(r: Dict[str, Any]) -> Dict[str, Any]:
    raw = r.get("raw_llm_output_json", "")
    if raw:
        try:
            parsed = _safe_json(raw)
            if isinstance(parsed, dict) and parsed.get("period"):
                parsed["status"] = "ready"
                return _repair_dashboard_dimensions(parsed)
        except Exception:
            pass
    result: Dict[str, Any] = {"status": "ready"}
    diag = r.get("standard_diagnosis_json", "")
    if diag:
        result["standard_diagnosis"] = _safe_json(diag)
    drivers = r.get("drivers_json", "[]")
    if drivers:
        result["root_cause_chain"] = _safe_json(drivers)
    watch = r.get("watch_items_json", "[]")
    if watch:
        result["watch_items"] = _safe_json(watch)
    emerging = r.get("emerging_findings_json", "[]")
    if emerging:
        result["emerging_findings"] = _safe_json(emerging)
    mgmt = r.get("management_questions_json", "[]")
    if mgmt:
        result["management_questions"] = _safe_json(mgmt)
    if r.get("title"):
        result["health_label"] = r.get("title")
    if r.get("standard_insight"):
        result["health_summary"] = r.get("standard_insight")
    evidence = r.get("evidence_json", "[]")
    if evidence:
        ev = _safe_json(evidence)
        if isinstance(ev, list) and ev:
            result["offset_factors"] = ev
    return _repair_dashboard_dimensions(result)


def _repair_dashboard_dimensions(result: Dict[str, Any]) -> Dict[str, Any]:
    try:
        from . import fact_builder as fb  # type: ignore
        from . import mock_llm  # type: ignore
    except ImportError:
        import fact_builder as fb  # type: ignore
        import mock_llm  # type: ignore

    data = fb.load_dashboard_data()
    rules = data.get("rules", {}) if isinstance(data, dict) else {}
    source = rules.get("dimension_scores") or []
    repaired = mock_llm._dashboard_dimensions(source)
    if repaired:
        result["dimensions"] = repaired
        diag = result.get("standard_diagnosis")
        if isinstance(diag, dict):
            diag["summary_items"] = mock_llm._summary_items(rules.get("root_causes") or [], repaired)
    return result


def _safe_json(text: str) -> Any:
    if not text:
        return []
    try:
        return json.loads(text)
    except (json.JSONDecodeError, TypeError):
        return []


def find_scenario_cache(
    params_hash: str,
    period: str = "",
    metric_version: str = "",
    prompt_version: str = "",
) -> Optional[Dict[str, Any]]:
    conn = _get_conn()
    row = conn.execute(
        """SELECT * FROM insight_result
           WHERE insight_type='scenario' AND card_id=? AND period=? AND metric_version=? AND prompt_version=? AND status='ready'
           ORDER BY updated_at DESC LIMIT 1""",
        (params_hash, period, metric_version, prompt_version),
    ).fetchone()
    return _row_to_dict(row)


def find_matching_scenario_cache(
    payload: Dict[str, Any],
    scenario_id: str,
    period: str = "",
    metric_version: str = "",
    prompt_version: str = "",
) -> Optional[Dict[str, Any]]:
    conn = _get_conn()
    rows = conn.execute(
        """SELECT * FROM insight_result
           WHERE insight_type='scenario' AND period=? AND metric_version=? AND prompt_version=? AND status='ready'
           ORDER BY updated_at DESC LIMIT 50""",
        (period, metric_version, prompt_version),
    ).fetchall()
    for row in rows:
        r = _row_to_dict(row)
        raw = _safe_json(r.get("raw_llm_output_json", "{}"))
        fact_pack = _safe_json(r.get("raw_fact_pack_json", "{}"))
        if not isinstance(raw, dict) or not isinstance(fact_pack, dict):
            continue
        if str(raw.get("scenario") or fact_pack.get("scenario") or "") != str(scenario_id):
            continue
        if _scenario_payload_matches_fact_pack(payload, fact_pack):
            return r
    return None


def _scenario_payload_matches_fact_pack(payload: Dict[str, Any], fact_pack: Dict[str, Any]) -> bool:
    payload_inputs = payload.get("inputs") if isinstance(payload.get("inputs"), dict) else {}
    payload_computed = payload.get("computed") if isinstance(payload.get("computed"), dict) else {}
    payload_sensitivity = payload.get("sensitivity") if isinstance(payload.get("sensitivity"), dict) else {}
    fact_inputs = fact_pack.get("inputs") if isinstance(fact_pack.get("inputs"), dict) else {}
    fact_computed = fact_pack.get("computed") if isinstance(fact_pack.get("computed"), dict) else {}
    fact_sensitivity = fact_pack.get("sensitivity") if isinstance(fact_pack.get("sensitivity"), dict) else {}

    checks = [
        (payload_inputs, fact_inputs, ["anr", "consumer_finance_growth", "pricing_rate", "credit_loss_rate", "npl_rate", "funding_cost_rate", "sales_cost_rate", "opex_rate", "tax_other_rate", "non_loan_profit"]),
        (payload_computed, fact_computed, ["total_cost_rate", "roa", "net_profit", "revenue"]),
        (payload_sensitivity, fact_sensitivity, ["credit_loss_0_5pct_impact", "pricing_0_5pct_impact"]),
    ]
    for left, right, keys in checks:
        for key in keys:
            if not _num_close(_scenario_value(left.get(key)), _scenario_value(right.get(key))):
                return False
    return True


def _scenario_value(value: Any) -> Any:
    if isinstance(value, dict):
        return value.get("value")
    return value


def _num_close(left: Any, right: Any, tolerance: float = 0.02) -> bool:
    try:
        lval = float(left)
        rval = float(right)
        return abs(lval - rval) <= tolerance
    except (TypeError, ValueError):
        return str(left) == str(right)


def save_scenario_result(
    params_hash: str,
    data: Dict[str, Any],
    period: str = "",
    raw_fact_pack: Optional[Dict[str, Any]] = None,
) -> None:
    metric_version = get_metric_version(period) if period else ""
    prompt_version = get_current_prompt_version()
    r = {
        "period": period,
        "metric_version": metric_version,
        "prompt_version": prompt_version,
        "insight_type": "scenario",
        "card_id": params_hash,
        "status": "ready",
        "title": data.get("scenario_label", ""),
        "standard_insight": "",
        "standard_explanation_json": json.dumps(data.get("standard_explanation", {}), ensure_ascii=False),
        "scenario_exploration_json": json.dumps(data.get("scenario_exploration", {}), ensure_ascii=False),
        "watch_items_json": json.dumps(data.get("watch_items", []), ensure_ascii=False),
        "raw_fact_pack_json": json.dumps(raw_fact_pack or {}, ensure_ascii=False),
        "raw_llm_output_json": json.dumps(data, ensure_ascii=False),
        "validated": 1,
        "finished_at": _now_iso(),
    }
    upsert_insight_result(r)
