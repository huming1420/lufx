from __future__ import annotations

from collections import Counter, defaultdict
from typing import Any, Dict, Iterable, List, Optional


STATUS_WEIGHT = {"red": 3, "yellow": 2, "green": 1}
STATUS_SCORE = {"green": 92, "yellow": 68, "red": 38}
DIMENSION_ORDER = ["profit", "scale", "risk", "cost", "transform", "outlook"]
DIMENSION_LABELS = {
    "profit": "利润健康度",
    "scale": "规模动能",
    "risk": "风险信号",
    "cost": "成本效率",
    "transform": "转型进度",
    "outlook": "前瞻指标",
}


def summarize_lights(metrics: Iterable[Dict[str, Any]]) -> Dict[str, Any]:
    rows = list(metrics)
    counts = Counter((m.get("status") or "unknown").lower() for m in rows)
    worst = "green"
    if counts.get("red"):
        worst = "red"
    elif counts.get("yellow"):
        worst = "yellow"

    red_metrics = [_metric_ref(m) for m in rows if (m.get("status") or "").lower() == "red"]
    yellow_metrics = [_metric_ref(m) for m in rows if (m.get("status") or "").lower() == "yellow"]
    return {
        "status": worst,
        "counts": {
            "red": counts.get("red", 0),
            "yellow": counts.get("yellow", 0),
            "green": counts.get("green", 0),
            "unknown": counts.get("unknown", 0),
        },
        "red_metrics": red_metrics[:8],
        "yellow_metrics": yellow_metrics[:8],
    }


def detect_anomalies(metrics: Iterable[Dict[str, Any]], limit: int = 10) -> List[Dict[str, Any]]:
    anomalies: List[Dict[str, Any]] = []
    for metric in metrics:
        status = (metric.get("status") or "").lower()
        if status not in {"red", "yellow"}:
            continue
        score = _severity_score(metric)
        anomalies.append(
            {
                **_metric_ref(metric),
                "severity": status,
                "severity_score": score,
                "reason": _anomaly_reason(metric),
            }
        )
    anomalies.sort(key=lambda item: item["severity_score"], reverse=True)
    return anomalies[:limit]


def score_dimensions(metrics: Iterable[Dict[str, Any]]) -> List[Dict[str, Any]]:
    grouped: Dict[str, List[Dict[str, Any]]] = defaultdict(list)
    for metric in metrics:
        normalized = _normalize_dimension(metric.get("dimension"))
        grouped[normalized].append(metric)

    result: List[Dict[str, Any]] = []
    for dimension in DIMENSION_ORDER:
        rows = grouped.get(dimension, [])
        if not rows:
            result.append(
                {
                    "dimension": dimension,
                    "label": DIMENSION_LABELS[dimension],
                    "score": 0,
                    "status": "unknown",
                    "driver_metrics": [],
                }
            )
            continue
        score = round(sum(STATUS_SCORE.get((m.get("status") or "").lower(), 55) for m in rows) / len(rows))
        status = _status_from_score(score)
        drivers = sorted(rows, key=_severity_score, reverse=True)[:3]
        result.append(
            {
                "dimension": dimension,
                "label": DIMENSION_LABELS[dimension],
                "score": score,
                "status": status,
                "driver_metrics": [_metric_ref(m) for m in drivers],
            }
        )
    return result


def rank_root_causes(metrics: Iterable[Dict[str, Any]], limit: int = 8) -> List[Dict[str, Any]]:
    candidates = []
    for metric in metrics:
        status = (metric.get("status") or "").lower()
        if status == "green":
            continue
        candidates.append(
            {
                **_metric_ref(metric),
                "dimension": _normalize_dimension(metric.get("dimension")),
                "impact_score": _severity_score(metric),
                "reason": _root_cause_reason(metric),
            }
        )
    candidates.sort(key=lambda item: item["impact_score"], reverse=True)
    for index, item in enumerate(candidates[:limit], start=1):
        item["rank"] = index
    return candidates[:limit]


def project_scenario(params: Dict[str, Any], baseline: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
    baseline = baseline or {}
    anr = _as_number(params.get("anr"), 0.0)
    pricing = _as_number(params.get("pricing_rate"), 0.0)
    credit_loss = _as_number(params.get("credit_loss_rate"), 0.0)
    funding = _as_number(params.get("funding_cost_rate"), 0.0)
    sales = _as_number(params.get("sales_cost_rate"), 0.0)
    opex = _as_number(params.get("opex_rate"), 0.0)
    tax_other = _as_number(params.get("tax_other_rate"), 0.0)
    non_loan_profit = _as_number(params.get("non_loan_profit"), 0.0)

    roa_spread = pricing - credit_loss - funding - sales - opex - tax_other
    loan_profit = anr * roa_spread / 100
    net_profit = loan_profit + non_loan_profit
    budget_gap = net_profit - _as_number(baseline.get("budget_net_profit"), 0.0)
    yoy_gap = net_profit - _as_number(baseline.get("last_year_net_profit"), 0.0)
    status = "green"
    if budget_gap < -2 or net_profit < 0:
        status = "red"
    elif budget_gap < 0:
        status = "yellow"

    return {
        "net_profit": round(net_profit, 2),
        "loan_profit": round(loan_profit, 2),
        "non_loan_profit": round(non_loan_profit, 2),
        "roa_spread": round(roa_spread, 2),
        "budget_gap": round(budget_gap, 2),
        "yoy_gap": round(yoy_gap, 2),
        "status": status,
        "formula": "net_profit = anr * (pricing_rate - credit_loss_rate - funding_cost_rate - sales_cost_rate - opex_rate - tax_other_rate) / 100 + non_loan_profit",
    }


def detect_cross_signals(metrics: Iterable[Dict[str, Any]]) -> List[Dict[str, Any]]:
    rows = list(metrics)
    signals: List[Dict[str, Any]] = []

    scale_metrics = [m for m in rows if _normalize_dimension(m.get("dimension")) == "scale"]
    risk_metrics = [m for m in rows if _normalize_dimension(m.get("dimension")) == "risk"]
    cost_metrics = [m for m in rows if _normalize_dimension(m.get("dimension")) == "cost"]

    has_green_scale = any((m.get("status") or "").lower() == "green" for m in scale_metrics)
    has_red_risk = any((m.get("status") or "").lower() == "red" for m in risk_metrics)
    has_green_cost = any((m.get("status") or "").lower() == "green" for m in cost_metrics)
    has_yoy_positive_disbursement = any(
        _is_disbursement_metric(m) and _yoy_positive(m) for m in rows
    )

    if has_green_scale and has_red_risk:
        facts = [_metric_fact(m) for m in scale_metrics if (m.get("status") or "").lower() == "green"]
        facts += [_metric_fact(m) for m in risk_metrics if (m.get("status") or "").lower() == "red"]
        signals.append({
            "type": "contradiction",
            "signal": "规模边际改善但风险仍处红灯",
            "supporting_facts": facts[:4],
            "analysis_hint": "需要验证规模修复质量",
        })

    if has_green_cost and has_red_risk:
        facts = [_metric_fact(m) for m in cost_metrics if (m.get("status") or "").lower() == "green"]
        facts += [_metric_fact(m) for m in risk_metrics if (m.get("status") or "").lower() == "red"]
        signals.append({
            "type": "offset_failure",
            "signal": "资金成本改善未能抵消风险成本压力",
            "supporting_facts": facts[:4],
            "analysis_hint": "风险成本仍是利润修复主约束",
        })

    if has_yoy_positive_disbursement and has_red_risk:
        disbursement_metrics = [m for m in rows if _is_disbursement_metric(m) and _yoy_positive(m)]
        facts = [_metric_fact(m) for m in disbursement_metrics]
        facts += [_metric_fact(m) for m in risk_metrics if (m.get("status") or "").lower() == "red"]
        signals.append({
            "type": "quality_issue",
            "signal": "新增放款改善但风险指标仍处红灯",
            "supporting_facts": facts[:4],
            "analysis_hint": "新增放款质量需关注",
        })

    return signals


def generate_card_rule_result(
    card_metrics: Iterable[Dict[str, Any]],
    card_config: Optional[Dict[str, Any]] = None,
) -> Dict[str, Any]:
    rows = list(card_metrics)
    suggested_status = _worst_status(rows)
    root_causes = rank_root_causes(rows, limit=4)
    suggested_focus = root_causes[0]["reason"] if root_causes else "暂无明确主因"
    conclusion_hint = f"当前状态{suggested_status}，{suggested_focus}"

    must_mention_facts: List[str] = []
    for m in sorted(rows, key=_severity_score, reverse=True):
        status = (m.get("status") or "").lower()
        if status in {"red", "yellow"}:
            must_mention_facts.append(_metric_fact(m))
        if len(must_mention_facts) >= 4:
            break

    allowed_exploration_types = (
        card_config.get("allowed_exploration_types", ["hidden_risk", "quality_issue", "offset_failure"])
        if card_config
        else ["hidden_risk", "quality_issue", "offset_failure"]
    )

    return {
        "suggested_status": suggested_status,
        "suggested_focus": suggested_focus,
        "conclusion_hint": conclusion_hint,
        "must_mention": must_mention_facts[:4],
        "do_not_mention": ["行业均值", "股票走势", "没有输入依据的精确预测"],
        "allowed_exploration_types": allowed_exploration_types,
    }


def generate_dashboard_rule_result(metrics: Iterable[Dict[str, Any]]) -> Dict[str, Any]:
    rows = list(metrics)
    dim_scores = score_dimensions(rows)
    scored_dims = [d for d in dim_scores if d["status"] != "unknown"]
    health_score = round(sum(d["score"] for d in scored_dims) / len(scored_dims)) if scored_dims else 0

    if health_score >= 80:
        overall_status = "经营健康"
    elif health_score >= 55:
        overall_status = "中性观察"
    else:
        overall_status = "压力偏大"

    root_causes = rank_root_causes(rows, limit=5)
    main_pressure = root_causes[0]["reason"] if root_causes else "暂无明显压力"

    green_metrics = [m for m in rows if (m.get("status") or "").lower() == "green"]
    main_offset = _metric_fact(green_metrics[0]) if green_metrics else "暂无明显抵消因素"

    conclusion_hint = f"{main_pressure}是主要拖累，{main_offset}形成部分对冲"

    must_mention_facts: List[str] = []
    for m in sorted(rows, key=_severity_score, reverse=True):
        must_mention_facts.append(_metric_fact(m))
        if len(must_mention_facts) >= 3:
            break

    return {
        "overall_status": overall_status,
        "main_pressure": main_pressure,
        "main_offset": main_offset,
        "conclusion_hint": conclusion_hint,
        "must_mention": must_mention_facts[:3],
        "do_not_mention": ["股票走势", "外部行业均值", "没有输入依据的精确预测"],
        "allowed_emerging_finding_types": [
            "contradiction",
            "hidden_risk",
            "quality_issue",
            "offset_failure",
            "structural_shift",
            "second_order_effect",
        ],
    }


def generate_scenario_rule_result(
    params: Dict[str, Any],
    projection: Dict[str, Any],
    sensitivity: Optional[Dict[str, Any]] = None,
) -> Dict[str, Any]:
    net_profit = _as_number(projection.get("net_profit"), 0.0)
    credit_loss_rate = _as_number(params.get("credit_loss_rate"), 0.0)

    if net_profit > 0 and credit_loss_rate > 5:
        scenario_quality = "盈利但对风险成本敏感"
    elif net_profit > 0:
        scenario_quality = "盈利且风险成本可控"
    else:
        scenario_quality = "亏损且风险成本承压"

    risk_boundary_hint = "信贷损失率继续上行将压缩ROA和净利润空间"
    upside_hint = "风险成本稳定且ANR不下滑时利润修复更稳"
    fragile_assumption_hint = "信贷损失率维持在当前水平是该情景最脆弱假设"
    hidden_risk_hint = "如果ANR增长来自高风险客群，净利润改善可能被后续风险成本抵消"

    must_mention: List[str] = []
    roa = projection.get("roa")
    if roa is None:
        roa = projection.get("roa_spread")
    if roa is not None:
        must_mention.append(f"预测ROA {roa}%")
    must_mention.append(f"预测净利润{net_profit}亿")
    if sensitivity:
        credit_loss_impact = sensitivity.get("credit_loss_0_5pct_impact")
        if isinstance(credit_loss_impact, dict):
            credit_loss_impact = credit_loss_impact.get("value")
        if credit_loss_impact is not None:
            must_mention.append(f"信贷损失率每变动0.5pct影响净利润{credit_loss_impact}亿")

    return {
        "scenario_quality": scenario_quality,
        "key_variable": "信贷损失率",
        "risk_boundary_hint": risk_boundary_hint,
        "upside_hint": upside_hint,
        "fragile_assumption_hint": fragile_assumption_hint,
        "hidden_risk_hint": hidden_risk_hint,
        "must_mention": must_mention,
        "do_not_mention": ["股票走势", "行业均值", "没有输入依据的精确预测"],
    }


def _normalize_dimension(value: Any) -> str:
    raw = str(value or "").lower()
    if raw in {"profit", "roa"}:
        return "profit"
    if raw in {"scale", "product", "channel", "customer", "vintage"}:
        return "scale"
    if raw in {"risk"}:
        return "risk"
    if raw in {"pricing", "pricing_band", "funding", "funding_source", "opex", "sales"}:
        return "cost"
    if raw in {"transform"}:
        return "transform"
    if raw in {"outlook"}:
        return "outlook"
    return "unknown"


def _metric_ref(metric: Dict[str, Any]) -> Dict[str, Any]:
    return {
        "metric_code": metric.get("metric_code"),
        "metric_name": metric.get("metric_name"),
        "segment": metric.get("segment"),
        "value": metric.get("value"),
        "unit": metric.get("unit"),
        "yoy": metric.get("yoy"),
        "mom": metric.get("mom"),
        "budget": metric.get("budget"),
        "budget_gap": metric.get("budget_gap"),
        "status": metric.get("status"),
    }


def _metric_fact(metric: Dict[str, Any]) -> str:
    name = metric.get("metric_name") or metric.get("metric_code") or "未知指标"
    value = metric.get("value")
    unit = metric.get("unit") or ""
    if value is not None:
        return f"{name}{value}{unit}"
    return name


def _worst_status(metrics: List[Dict[str, Any]]) -> str:
    statuses = {(m.get("status") or "").lower() for m in metrics}
    if "red" in statuses:
        return "red"
    if "yellow" in statuses:
        return "yellow"
    if "green" in statuses:
        return "green"
    return "unknown"


def _is_disbursement_metric(metric: Dict[str, Any]) -> bool:
    code = str(metric.get("metric_code") or "").lower()
    name = str(metric.get("metric_name") or "").lower()
    return "disbursement" in code or "放款" in name or "新增" in name


def _yoy_positive(metric: Dict[str, Any]) -> bool:
    yoy = metric.get("yoy")
    return isinstance(yoy, (int, float)) and yoy > 0


def _severity_score(metric: Dict[str, Any]) -> float:
    status = (metric.get("status") or "").lower()
    score = STATUS_WEIGHT.get(status, 0) * 100.0
    gap = metric.get("budget_gap")
    value = metric.get("value")
    if isinstance(gap, (int, float)):
        denominator = abs(value) if isinstance(value, (int, float)) and value else 1.0
        score += min(abs(gap) / denominator * 100, 80)
    yoy = metric.get("yoy")
    if isinstance(yoy, (int, float)):
        score += min(abs(yoy), 60) * 0.5
    return round(score, 2)


def _anomaly_reason(metric: Dict[str, Any]) -> str:
    parts = []
    if metric.get("budget_gap") is not None:
        parts.append(f"budget_gap={metric.get('budget_gap')}")
    if metric.get("yoy") is not None:
        parts.append(f"yoy={metric.get('yoy')}")
    if metric.get("mom") is not None:
        parts.append(f"mom={metric.get('mom')}")
    return "; ".join(parts) or "status threshold breached"


def _root_cause_reason(metric: Dict[str, Any]) -> str:
    status = metric.get("status") or "unknown"
    name = metric.get("metric_name") or metric.get("metric_code")
    gap = metric.get("budget_gap")
    if gap is None:
        return f"{name} is flagged {status} by configured thresholds."
    return f"{name} is flagged {status} with budget_gap={gap}."


def _status_from_score(score: int) -> str:
    if score < 55:
        return "red"
    if score < 80:
        return "yellow"
    return "green"


def _as_number(value: Any, default: float) -> float:
    return float(value) if isinstance(value, (int, float)) else default
