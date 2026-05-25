from __future__ import annotations

import asyncio
import json
import logging
import traceback
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

try:
    from . import insight_repo
    from .insight_service import generate_card_insight, generate_dashboard_insight, InsightValidationError
except ImportError:
    import insight_repo  # type: ignore
    from insight_service import generate_card_insight, generate_dashboard_insight, InsightValidationError  # type: ignore

logger = logging.getLogger("insight_job_service")

LLM_CONCURRENCY = 2
LLM_SEMAPHORE: Optional[asyncio.Semaphore] = None


def _get_semaphore() -> asyncio.Semaphore:
    global LLM_SEMAPHORE
    if LLM_SEMAPHORE is None:
        LLM_SEMAPHORE = asyncio.Semaphore(LLM_CONCURRENCY)
    return LLM_SEMAPHORE


def create_insight_job(
    period: str,
    scope: str = "all",
    card_ids: Optional[List[str]] = None,
    force_refresh: bool = False,
) -> str:
    """创建洞察生成任务。

    根据指定的统计周期和范围（全部、卡片、仪表盘）收集需要生成的洞察项，
    检查缓存命中情况以跳过已存在的有效结果，并过滤掉已在运行中的重复目标。
    创建任务记录后立即启动异步执行流程。

    Args:
        period: 统计周期标识。
        scope: 生成范围，可选值为 "all"（全部）、"cards"（所有卡片）、"dashboard"（仪表盘）、"card"（指定卡片）。
        card_ids: 当 scope 为 "card" 时指定的卡片 ID 列表。
        force_refresh: 是否强制刷新，忽略缓存直接重新生成。

    Returns:
        创建的任务 ID；若所有项均已命中缓存或存在完全重叠的活跃任务，则返回对应任务 ID 或空字符串。
    """
    metric_version = insight_repo.get_metric_version(period)
    prompt_version = insight_repo.get_current_prompt_version()

    items: List[Dict[str, Any]] = []
    job_type = scope

    if scope in ("all", "cards"):
        try:
            from . import fact_builder as fb  # type: ignore
        except ImportError:
            import fact_builder as fb  # type: ignore
        cards = fb.load_card_config()
        all_card_ids = [c.get("card_id") for c in cards if c.get("enabled", True)]
        target_ids = card_ids if card_ids else all_card_ids
        for cid in target_ids:
            if cid in all_card_ids:
                if force_refresh or not insight_repo.is_cache_hit(period, metric_version, prompt_version, "card", cid):
                    items.append({"insight_type": "card", "card_id": cid})
        job_type = "all" if scope == "all" else "cards"

    if scope in ("all", "dashboard"):
        if force_refresh or not insight_repo.is_cache_hit(period, metric_version, prompt_version, "dashboard", ""):
            items.append({"insight_type": "dashboard", "card_id": ""})
        if scope == "all" and job_type == "all":
            pass
        elif scope == "dashboard":
            job_type = "dashboard"

    if scope == "card" and card_ids:
        for cid in card_ids:
            if force_refresh or not insight_repo.is_cache_hit(period, metric_version, prompt_version, "card", cid):
                items.append({"insight_type": "card", "card_id": cid})
        job_type = "card"

    items = _dedupe_items(items)
    active_targets = _active_item_targets(period, metric_version, prompt_version)
    overlapping_job_ids: List[str] = []
    filtered_items: List[Dict[str, Any]] = []
    for it in items:
        target = (str(it.get("insight_type", "")), str(it.get("card_id", "")))
        active_job_id = active_targets.get(target)
        if active_job_id:
            overlapping_job_ids.append(active_job_id)
            continue
        filtered_items.append(it)

    if overlapping_job_ids and not filtered_items:
        return overlapping_job_ids[0]

    items = filtered_items
    if not items:
        return ""

    job_id = insight_repo.create_job(
        period=period,
        metric_version=metric_version,
        prompt_version=prompt_version,
        job_type=job_type,
        total_count=len(items),
        force_refresh=force_refresh,
    )

    insight_repo.create_job_items(job_id, items)

    for it in items:
        insight_repo.upsert_insight_result({
            "period": period,
            "metric_version": metric_version,
            "prompt_version": prompt_version,
            "insight_type": it["insight_type"],
            "card_id": it.get("card_id", ""),
            "status": "pending",
        })

    try:
        loop = asyncio.get_running_loop()
        loop.create_task(run_insight_job(job_id, period, metric_version, prompt_version))
    except RuntimeError:
        try:
            loop = asyncio.get_event_loop()
            if loop.is_running():
                loop.create_task(run_insight_job(job_id, period, metric_version, prompt_version))
            else:
                loop.run_until_complete(run_insight_job(job_id, period, metric_version, prompt_version))
        except Exception:
            import threading
            t = threading.Thread(
                target=_run_job_in_new_loop,
                args=(job_id, period, metric_version, prompt_version),
                daemon=True,
            )
            t.start()

    return job_id


def _run_job_in_new_loop(job_id: str, period: str, metric_version: str, prompt_version: str) -> None:
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    try:
        loop.run_until_complete(run_insight_job(job_id, period, metric_version, prompt_version))
    finally:
        loop.close()


async def run_insight_job(
    job_id: str,
    period: str,
    metric_version: str,
    prompt_version: str,
) -> None:
    """执行洞察生成任务的主流程。

    将任务状态更新为运行中，获取所有待处理的子项，
    使用信号量控制 LLM 并发度，并发执行各子项的洞察生成，
    所有子项完成后统一计算并更新任务最终状态。

    Args:
        job_id: 任务唯一标识。
        period: 统计周期标识。
        metric_version: 指标版本号。
        prompt_version: 提示词版本号。
    """
    insight_repo.update_job_status(job_id, "running", started_at=_now_iso())

    pending_items = insight_repo.get_pending_job_items(job_id)
    if not pending_items:
        insight_repo.update_job_status(job_id, "finished", finished_at=_now_iso())
        return

    sem = _get_semaphore()
    tasks = [_process_item_with_semaphore(sem, item, job_id, period, metric_version, prompt_version) for item in pending_items]
    await asyncio.gather(*tasks)

    finalize_job_status(job_id)


async def _process_item_with_semaphore(
    sem: asyncio.Semaphore,
    item: Dict[str, Any],
    job_id: str,
    period: str,
    metric_version: str,
    prompt_version: str,
) -> None:
    async with sem:
        await _process_one_item(item, job_id, period, metric_version, prompt_version)


async def _process_one_item(
    item: Dict[str, Any],
    job_id: str,
    period: str,
    metric_version: str,
    prompt_version: str,
) -> None:
    """处理单个洞察子项，包含重试与降级机制。

    将子项状态更新为运行中，调用 LLM 生成洞察结果。
    首次失败后自动重试一次；若仍然失败，则尝试使用 mock_llm 生成降级结果。
    最终根据生成结果更新子项状态并同步任务计数。

    Args:
        item: 子项信息字典，包含 id、insight_type、card_id 等字段。
        job_id: 所属任务 ID。
        period: 统计周期标识。
        metric_version: 指标版本号。
        prompt_version: 提示词版本号。
    """
    item_id = item["id"]
    insight_type = item["insight_type"]
    card_id = item.get("card_id", "")

    insight_repo.update_job_item_status(item_id, "running", started_at=_now_iso())
    insight_repo.update_insight_result_status(
        period, metric_version, prompt_version, insight_type, card_id,
        "running", started_at=_now_iso(),
    )

    success = await _generate_one_item(period, metric_version, prompt_version, insight_type, card_id, item_id)

    if not success and item.get("retry_count", 0) == 0:
        insight_repo.update_job_item_status(item_id, "pending", retry_count=1, error_message="")
        success = await _generate_one_item(period, metric_version, prompt_version, insight_type, card_id, item_id)

    if not success:
        fallback_written = _write_fallback(period, metric_version, prompt_version, insight_type, card_id)
        if fallback_written:
            insight_repo.update_job_item_status(item_id, "ready", retry_count=2, error_message="", finished_at=_now_iso())
        else:
            insight_repo.update_job_item_status(item_id, "failed", retry_count=2, error_message="LLM generation failed after retry", finished_at=_now_iso())
            insight_repo.update_insight_result_status(
                period, metric_version, prompt_version, insight_type, card_id,
                "failed", error_message="LLM generation failed after retry", finished_at=_now_iso(),
            )
    _update_job_counts(job_id)


async def _generate_one_item(
    period: str,
    metric_version: str,
    prompt_version: str,
    insight_type: str,
    card_id: str,
    item_id: int,
) -> bool:
    """调用 LLM 服务生成单个洞察结果。

    根据洞察类型（card/dashboard）构建事实包并调用对应的生成函数，
    将返回结果持久化到数据库。捕获验证错误与一般异常并记录日志。

    Args:
        period: 统计周期标识。
        metric_version: 指标版本号。
        prompt_version: 提示词版本号。
        insight_type: 洞察类型，"card" 或 "dashboard"。
        card_id: 卡片 ID，dashboard 类型时为空字符串。
        item_id: 子项数据库 ID。

    Returns:
        生成成功返回 True，失败返回 False。
    """
    try:
        loop = asyncio.get_running_loop()

        fact_pack: Optional[Dict[str, Any]] = None
        if insight_type == "card":
            fact_pack = _build_fact_pack(insight_type, card_id, period)
            result = await loop.run_in_executor(None, generate_card_insight, card_id, period)
        elif insight_type == "dashboard":
            fact_pack = _build_fact_pack(insight_type, card_id, period)
            result = await loop.run_in_executor(None, generate_dashboard_insight, period)
        else:
            logger.warning("Unknown insight_type: %s", insight_type)
            return False

        _save_generated_result(period, metric_version, prompt_version, insight_type, card_id, result, fact_pack)

        insight_repo.update_job_item_status(item_id, "ready", finished_at=_now_iso())
        insight_repo.update_insight_result_status(
            period, metric_version, prompt_version, insight_type, card_id,
            "ready", finished_at=_now_iso(),
        )
        return True

    except InsightValidationError as exc:
        logger.warning("Validation error for %s/%s: %s", insight_type, card_id, exc)
        insight_repo.update_job_item_status(item_id, "failed", error_message=str(exc), finished_at=_now_iso())
        return False
    except Exception as exc:
        logger.error("LLM generation error for %s/%s: %s", insight_type, card_id, exc)
        traceback.print_exc()
        insight_repo.update_job_item_status(item_id, "failed", error_message="LLM generation failed", finished_at=_now_iso())
        return False


def _save_generated_result(
    period: str,
    metric_version: str,
    prompt_version: str,
    insight_type: str,
    card_id: str,
    result: Dict[str, Any],
    fact_pack: Optional[Dict[str, Any]] = None,
) -> None:
    r: Dict[str, Any] = {
        "period": period,
        "metric_version": metric_version,
        "prompt_version": prompt_version,
        "insight_type": insight_type,
        "card_id": card_id,
        "status": "ready",
        "validated": 1,
        "finished_at": _now_iso(),
        "raw_fact_pack_json": json.dumps(fact_pack or {}, ensure_ascii=False),
        "raw_llm_output_json": json.dumps(result, ensure_ascii=False),
    }

    if insight_type == "card":
        r["title"] = result.get("title", "")
        r["standard_insight"] = result.get("standard_insight", "")
        r["drivers_json"] = json.dumps(result.get("drivers", []), ensure_ascii=False)
        r["watch_items_json"] = json.dumps(result.get("watch_items", []), ensure_ascii=False)
        r["evidence_json"] = json.dumps(result.get("evidence", []), ensure_ascii=False)
        r["exploratory_insights_json"] = json.dumps(result.get("exploratory_insights", []), ensure_ascii=False)
        r["deep_dive_questions_json"] = json.dumps(result.get("deep_dive_questions", []), ensure_ascii=False)
        r["schema_name"] = "card_insight.schema.json"
    elif insight_type == "dashboard":
        r["title"] = result.get("health_label", "")
        r["standard_insight"] = result.get("standard_diagnosis", {}).get("health_summary", "") if isinstance(result.get("standard_diagnosis"), dict) else ""
        r["standard_diagnosis_json"] = json.dumps(result.get("standard_diagnosis", {}), ensure_ascii=False)
        r["drivers_json"] = json.dumps(result.get("root_cause_chain", []), ensure_ascii=False)
        r["watch_items_json"] = json.dumps(result.get("watch_items", []), ensure_ascii=False)
        r["evidence_json"] = json.dumps(result.get("offset_factors", []), ensure_ascii=False)
        r["emerging_findings_json"] = json.dumps(result.get("emerging_findings", []), ensure_ascii=False)
        r["management_questions_json"] = json.dumps(result.get("management_questions", []), ensure_ascii=False)
        r["schema_name"] = "dashboard_insight.schema.json"

    insight_repo.upsert_insight_result(r)


def _build_fact_pack(insight_type: str, card_id: str, period: str) -> Dict[str, Any]:
    try:
        from . import fact_builder as fb  # type: ignore
    except ImportError:
        import fact_builder as fb  # type: ignore
    if insight_type == "card":
        return fb.build_card_fact_pack(card_id, period)
    if insight_type == "dashboard":
        return fb.build_dashboard_fact_pack(period)
    return {}


def _dedupe_items(items: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    seen = set()
    result: List[Dict[str, Any]] = []
    for it in items:
        target = (str(it.get("insight_type", "")), str(it.get("card_id", "")))
        if target in seen:
            continue
        seen.add(target)
        result.append(it)
    return result


def _active_item_targets(period: str, metric_version: str, prompt_version: str) -> Dict[tuple[str, str], str]:
    targets: Dict[tuple[str, str], str] = {}
    for job in insight_repo.get_active_jobs(period):
        if job.get("metric_version") != metric_version or job.get("prompt_version") != prompt_version:
            continue
        for item in insight_repo.get_job_items(job["job_id"]):
            if item.get("status") not in ("pending", "running"):
                continue
            target = (str(item.get("insight_type", "")), str(item.get("card_id", "")))
            targets.setdefault(target, job["job_id"])
    return targets


def _write_fallback(
    period: str,
    metric_version: str,
    prompt_version: str,
    insight_type: str,
    card_id: str,
) -> bool:
    try:
        try:
            from . import mock_llm  # type: ignore
        except ImportError:
            import mock_llm  # type: ignore
        fact_pack = _build_fact_pack(insight_type, card_id, period)
        fallback = mock_llm.generate(fact_pack)
        _save_generated_result(period, metric_version, prompt_version, insight_type, card_id, fallback, fact_pack)
        return True
    except Exception as exc:
        logger.error("Fallback generation failed for %s/%s: %s", insight_type, card_id, exc)
        return False


def finalize_job_status(job_id: str) -> None:
    """根据子项完成情况最终确定任务状态。

    统计任务下所有子项的就绪、失败、跳过数量，
    按规则判定任务整体状态：全部成功为 finished，全部失败为 failed，
    部分成功或未完成则为 partial_failed，并将结果回写数据库。

    Args:
        job_id: 要结算状态的任务 ID。
    """
    job = insight_repo.get_job(job_id)
    if job is None:
        return

    counts = insight_repo.count_job_item_statuses(job_id)
    finished = counts.get("ready", 0) + counts.get("failed", 0) + counts.get("skipped", 0)
    failed = counts.get("failed", 0)
    total = job.get("total_count", 0)

    if finished < total:
        status = "partial_failed"
    elif failed > 0 and failed == total:
        status = "failed"
    elif failed > 0:
        status = "partial_failed"
    else:
        status = "finished"

    insight_repo.update_job_status(
        job_id,
        status=status,
        finished_count=finished,
        failed_count=failed,
        finished_at=_now_iso(),
    )


def _update_job_counts(job_id: str) -> None:
    counts = insight_repo.count_job_item_statuses(job_id)
    finished = counts.get("ready", 0) + counts.get("failed", 0) + counts.get("skipped", 0)
    failed = counts.get("failed", 0)
    insight_repo.update_job_status(job_id, "running", finished_count=finished, failed_count=failed)


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()
