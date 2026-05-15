from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict, List, Optional

try:
    from . import rule_engine
except ImportError:
    import rule_engine

BACKEND_DIR = Path(__file__).resolve().parents[1]
DATA_DIR = BACKEND_DIR / "data"

DUPONT_METRIC_CODES = [
    "net_profit_management", "anr", "roa", "pricing_rate",
    "credit_loss_rate", "funding_cost_rate", "opex_rate",
]

SCENARIO_INPUT_NAMES = {
    "anr": "平均贷款余额ANR",
    "consumer_finance_growth": "消金贷款增速",
    "pricing_rate": "综合定价率",
    "credit_loss_rate": "信贷损失率",
    "npl_rate": "不良贷款率",
    "funding_cost_rate": "综合资金成本",
    "sales_cost_rate": "销售成本率",
    "opex_rate": "运营成本率",
    "tax_other_rate": "税及其他",
    "non_loan_profit": "非贷款业务净利润",
}

SCENARIO_INPUT_UNITS = {
    "anr": "亿",
    "consumer_finance_growth": "%",
    "pricing_rate": "%",
    "credit_loss_rate": "%",
    "npl_rate": "%",
    "funding_cost_rate": "%",
    "sales_cost_rate": "%",
    "opex_rate": "%",
    "tax_other_rate": "%",
    "non_loan_profit": "亿",
}

COMPUTED_NAMES = {
    "total_cost_rate": "总成本率",
    "roa": "预测ROA",
    "net_profit": "预测净利润",
    "revenue": "预测收入",
    "margin": "净利率",
    "profit_change_vs_last_year": "较上年变化",
}

_IMPACT_TARGET_MAP = {
    "profit": "净利润",
    "scale": "规模",
    "risk": "风险成本",
    "cost": "成本",
    "transform": "转型",
    "outlook": "前瞻",
}


def load_dashboard_data() -> Dict[str, Any]:
    return _load_json(DATA_DIR / "dashboard_metrics_mock.json")


def load_card_config() -> List[Dict[str, Any]]:
    return _load_json(DATA_DIR / "card_config.json")


def build_card_fact_pack(card_id: str, period: Optional[str] = None) -> Dict[str, Any]:
    dashboard_data = load_dashboard_data()
    cards = load_card_config()
    card = next((c for c in cards if c.get("card_id") == card_id), None)
    if card is None:
        raise ValueError(f"Unknown card_id: {card_id}")

    effective_period = period or dashboard_data.get("period")
    core_metrics = dashboard_data.get("core_metrics", [])
    segment_metrics = dashboard_data.get("segment_metrics", [])
    rules = dashboard_data.get("rules", {})
    metric_codes = set(card.get("metric_codes") or [])

    card_core = [m for m in core_metrics if m.get("metric_code") in metric_codes]
    card_segs = [m for m in segment_metrics if m.get("metric_code") in metric_codes]
    all_card = _ensure_dimension(card_core + card_segs)

    return {
        "pack_type": "CARD_FACT_PACK",
        "period": effective_period,
        "card": {
            "id": card.get("card_id"),
            "name": card.get("name"),
            "level": card.get("level"),
            "view_type": card.get("view_type"),
            "section_id": card.get("section_id"),
        },
        "business_context": card.get("business_context", {}),
        "metrics": [_format_card_metric(m) for m in card_core],
        "segments": [_format_card_segment(m) for m in card_segs],
        "alerts": _build_card_alerts(all_card),
        "driver_ranking": _build_card_driver_ranking(all_card),
        "parent_context": _build_parent_context(core_metrics, rules),
        "rule_result": rule_engine.generate_card_rule_result(all_card, card),
    }


def build_dashboard_fact_pack(period: Optional[str] = None) -> Dict[str, Any]:
    dashboard_data = load_dashboard_data()
    core_metrics = dashboard_data.get("core_metrics", [])
    rules = dashboard_data.get("rules", {})
    effective_period = period or dashboard_data.get("period")

    adapted = _ensure_dimension(core_metrics)

    dimension_scores = rules.get("dimension_scores") or rule_engine.score_dimensions(adapted)
    root_causes = rules.get("root_causes") or rule_engine.rank_root_causes(adapted, limit=10)
    offset_factors = rules.get("offset_factors") or []
    cross_signals = rules.get("cross_signals") or rule_engine.detect_cross_signals(adapted)

    for rc in root_causes:
        rc.setdefault("unit", "亿")

    return {
        "pack_type": "DASHBOARD_FACT_PACK",
        "period": effective_period,
        "health_score": rules.get("health_score", 0),
        "health_label": rules.get("health_label", ""),
        "dupont": _build_dupont(core_metrics),
        "dimension_scores": dimension_scores,
        "root_causes": root_causes,
        "offset_factors": offset_factors,
        "alerts": _build_dashboard_alerts(core_metrics),
        "cross_signals": cross_signals,
        "rule_result": rule_engine.generate_dashboard_rule_result(adapted),
    }


def build_scenario_fact_pack(
    scenario_id: str = "base",
    overrides: Optional[Dict[str, Any]] = None,
    period: Optional[str] = None,
) -> Dict[str, Any]:
    dashboard_data = load_dashboard_data()
    core_metrics = dashboard_data.get("core_metrics", [])
    scenario_defaults_list = dashboard_data.get("scenario_defaults", [])
    effective_period = period or dashboard_data.get("period")

    scenario_default = next(
        (s for s in scenario_defaults_list if s.get("scenario") == scenario_id),
        next((s for s in scenario_defaults_list if s.get("scenario") == "base"), {}),
    )
    scenario_label = scenario_default.get("scenario_label", scenario_id)

    payload = overrides or {}
    raw_inputs = payload.get("inputs") if isinstance(payload.get("inputs"), dict) else payload
    params = {k: v for k, v in scenario_default.items() if k not in ("scenario", "scenario_label")}
    params.update(_normalize_scenario_inputs({k: v for k, v in raw_inputs.items() if v is not None}))

    baseline = _extract_baseline(core_metrics)
    projection = rule_engine.project_scenario(params, baseline)

    inputs = _build_scenario_inputs(params)
    computed = _build_scenario_computed(params, projection)
    benchmark = _build_scenario_benchmark(core_metrics, baseline)
    sensitivity = _build_scenario_sensitivity(params, projection)
    rule_result = rule_engine.generate_scenario_rule_result(params, projection, sensitivity)

    return {
        "pack_type": "SCENARIO_FACT_PACK",
        "period": effective_period,
        "scenario": scenario_id,
        "scenario_label": scenario_label,
        "inputs": inputs,
        "computed": computed,
        "benchmark": benchmark,
        "sensitivity": sensitivity,
        "rule_result": rule_result,
    }


def _ensure_dimension(metrics: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    result = []
    for m in metrics:
        d = m.get("module") or m.get("dimension")
        result.append({**m, "dimension": d})
    return result


def _format_card_metric(metric: Dict[str, Any]) -> Dict[str, Any]:
    return {
        "code": metric.get("metric_code"),
        "name": metric.get("metric_name"),
        "value": metric.get("value"),
        "display_value": metric.get("display_value"),
        "unit": metric.get("unit"),
        "yoy": metric.get("yoy"),
        "display_yoy": metric.get("display_yoy"),
        "mom": metric.get("mom"),
        "budget": metric.get("budget"),
        "budget_gap": metric.get("budget_gap"),
        "display_budget_gap": metric.get("display_budget_gap"),
        "redline": metric.get("redline"),
        "yellowline": metric.get("yellowline"),
        "distance_to_redline": metric.get("distance_to_redline"),
        "display_distance_to_redline": metric.get("display_distance_to_redline"),
        "status": metric.get("status"),
        "direction": metric.get("direction"),
        "note": _generate_metric_note(metric),
    }


def _format_card_segment(segment: Dict[str, Any]) -> Dict[str, Any]:
    return {
        "dimension": segment.get("dimension"),
        "segment": segment.get("segment"),
        "metric_code": segment.get("metric_code"),
        "metric_name": segment.get("metric_name"),
        "value": segment.get("value"),
        "display_value": segment.get("display_value"),
        "unit": segment.get("unit"),
        "status": segment.get("status"),
        "note": _generate_segment_note(segment),
    }


def _generate_metric_note(metric: Dict[str, Any]) -> str:
    status = (metric.get("status") or "").lower()
    redline = metric.get("redline")
    distance = metric.get("distance_to_redline")
    unit = metric.get("unit", "")
    budget_gap = metric.get("budget_gap")
    display_budget_gap = metric.get("display_budget_gap")
    if status == "red" and redline is not None and distance is not None:
        return f"距离{redline}{unit}红线仅{abs(distance)}{unit}"
    if budget_gap is not None and display_budget_gap:
        direction = "超预算" if budget_gap > 0 else "低于预算"
        return f"{direction}{display_budget_gap}"
    return ""


def _generate_segment_note(segment: Dict[str, Any]) -> str:
    status = (segment.get("status") or "").lower()
    name = segment.get("metric_name") or segment.get("metric_code") or ""
    seg = segment.get("segment") or ""
    if status == "red":
        return f"{seg}{name}处于红灯"
    if status == "yellow":
        return f"{seg}{name}处于黄灯"
    return ""


def _build_alert_reason(metric: Dict[str, Any]) -> str:
    status = (metric.get("status") or "").lower()
    name = metric.get("metric_name") or metric.get("metric_code") or ""
    segment = metric.get("segment")
    redline = metric.get("redline")
    distance = metric.get("distance_to_redline")
    unit = metric.get("unit", "")
    budget_gap = metric.get("budget_gap")
    display_budget_gap = metric.get("display_budget_gap")
    prefix = f"{segment}" if segment else name
    if status == "red" and redline is not None and distance is not None:
        return f"{prefix}距离{redline}{unit}红线仅{abs(distance)}{unit}"
    if budget_gap is not None and display_budget_gap:
        direction = "超预算" if budget_gap > 0 else "低于预算"
        return f"{prefix}{direction}{display_budget_gap}"
    return f"{prefix}处于{status}灯"


def _build_driver_name(metric: Dict[str, Any]) -> str:
    name = metric.get("metric_name") or metric.get("metric_code") or ""
    direction = metric.get("direction", "")
    yoy = metric.get("yoy")
    if isinstance(yoy, (int, float)) and yoy != 0:
        if direction == "higher_is_worse":
            return f"{name}上行" if yoy > 0 else f"{name}下行"
        if direction == "higher_is_better":
            return f"{name}下行" if yoy < 0 else f"{name}上行"
    return name


def _build_card_alerts(metrics: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    anomalies = rule_engine.detect_anomalies(metrics, limit=6)
    return [
        {
            "level": a.get("severity"),
            "tag": f"{a.get('segment')}{a.get('metric_name', '')}" if a.get("segment") else (a.get("metric_name") or a.get("metric_code")),
            "reason": _build_alert_reason(a),
            "related_metric": a.get("metric_code"),
        }
        for a in anomalies
    ]


def _build_card_driver_ranking(metrics: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    causes = rule_engine.rank_root_causes(metrics, limit=5)
    return [
        {
            "rank": c.get("rank"),
            "driver": _build_driver_name(c),
            "reason": _build_alert_reason(c),
            "impact_target": _IMPACT_TARGET_MAP.get(c.get("dimension"), c.get("dimension", "")),
            "evidence_metrics": [c["metric_code"]] if c.get("metric_code") else [],
        }
        for c in causes
    ]


def _build_parent_context(core_metrics: List[Dict[str, Any]], rules: Dict[str, Any]) -> Dict[str, Any]:
    npm = next((m for m in core_metrics if m.get("metric_code") == "net_profit_management"), {})
    roa = next((m for m in core_metrics if m.get("metric_code") == "roa"), {})
    root_causes = rules.get("root_causes", [])
    main_negative_driver = root_causes[0].get("factor", "") if root_causes else ""
    return {
        "net_profit_management": {
            "value": npm.get("value"),
            "display_value": npm.get("display_value"),
            "status": npm.get("status"),
        },
        "roa": {
            "value": roa.get("value"),
            "display_value": roa.get("display_value"),
            "status": roa.get("status"),
        },
        "main_negative_driver": main_negative_driver,
    }


def _build_dupont(core_metrics: List[Dict[str, Any]]) -> Dict[str, Any]:
    metric_map = {m.get("metric_code"): m for m in core_metrics}
    dupont: Dict[str, Any] = {}
    for code in DUPONT_METRIC_CODES:
        m = metric_map.get(code)
        if m is None:
            continue
        if code == "credit_loss_rate":
            dupont[code] = {
                "value": m.get("value"),
                "display_value": m.get("display_value"),
                "budget": m.get("budget"),
                "display_budget": format_display_value(m.get("budget"), m.get("unit")),
                "redline": m.get("redline"),
                "display_redline": format_display_value(m.get("redline"), m.get("unit")),
                "status": m.get("status"),
            }
        elif code == "funding_cost_rate":
            dupont[code] = {
                "value": m.get("value"),
                "display_value": m.get("display_value"),
                "yoy_delta": m.get("yoy"),
                "display_yoy_delta": m.get("display_yoy"),
                "status": m.get("status"),
            }
        else:
            dupont[code] = {
                "value": m.get("value"),
                "display_value": m.get("display_value"),
                "unit": m.get("unit"),
                "yoy": m.get("yoy"),
                "display_yoy": m.get("display_yoy"),
                "budget_gap": m.get("budget_gap"),
                "display_budget_gap": m.get("display_budget_gap"),
                "status": m.get("status"),
            }
    return dupont


def _build_dashboard_alerts(core_metrics: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    anomalies = rule_engine.detect_anomalies(core_metrics, limit=12)
    return [
        {
            "level": a.get("severity"),
            "metric": a.get("metric_name") or a.get("metric_code"),
            "display_value": format_display_value(a.get("value"), a.get("unit")),
            "reason": _build_alert_reason(a),
        }
        for a in anomalies
    ]


def _build_scenario_inputs(params: Dict[str, Any]) -> Dict[str, Any]:
    inputs: Dict[str, Any] = {}
    for key, name in SCENARIO_INPUT_NAMES.items():
        value = params.get(key)
        unit = SCENARIO_INPUT_UNITS.get(key, "")
        inputs[key] = {
            "name": name,
            "value": value,
            "display_value": format_display_value(value, unit),
            "unit": unit,
        }
    return inputs


def _build_scenario_computed(params: Dict[str, Any], projection: Dict[str, Any]) -> Dict[str, Any]:
    anr = _num(params.get("anr"))
    pricing = _num(params.get("pricing_rate"))
    credit_loss = _num(params.get("credit_loss_rate"))
    funding = _num(params.get("funding_cost_rate"))
    sales = _num(params.get("sales_cost_rate"))
    opex = _num(params.get("opex_rate"))
    tax_other = _num(params.get("tax_other_rate"))

    cost_parts = [credit_loss, funding, sales, opex, tax_other]
    total_cost_rate = sum(cost_parts) if all(v is not None for v in cost_parts) else None
    roa_val = projection.get("roa_spread")
    net_profit = projection.get("net_profit")
    revenue = anr * pricing / 100 if anr is not None and pricing is not None else None
    margin = (roa_val / pricing * 100) if roa_val is not None and pricing else None
    profit_change = projection.get("yoy_gap")

    return {
        "total_cost_rate": _make_computed_entry("total_cost_rate", total_cost_rate, "%"),
        "roa": _make_computed_entry("roa", roa_val, "%"),
        "net_profit": _make_computed_entry("net_profit", net_profit, "亿"),
        "revenue": _make_computed_entry("revenue", revenue, "亿"),
        "margin": _make_computed_entry("margin", margin, "%"),
        "profit_change_vs_last_year": _make_computed_entry("profit_change_vs_last_year", profit_change, "亿"),
    }


def _make_computed_entry(key: str, value: Any, unit: str) -> Dict[str, Any]:
    num_val = _num(value)
    return {
        "name": COMPUTED_NAMES.get(key, key),
        "value": num_val,
        "display_value": format_display_value(num_val, unit),
        "unit": unit,
    }


def _build_scenario_benchmark(core_metrics: List[Dict[str, Any]], baseline: Dict[str, Any]) -> Dict[str, Any]:
    clr = next((m for m in core_metrics if m.get("metric_code") == "credit_loss_rate"), {})
    pr = next((m for m in core_metrics if m.get("metric_code") == "pricing_rate"), {})
    last_year_npm = baseline.get("last_year_net_profit")
    return {
        "last_year_net_profit": {
            "name": "上年净利润",
            "value": last_year_npm,
            "display_value": format_display_value(last_year_npm, "亿"),
            "unit": "亿",
        },
        "current_actual_credit_loss_rate": {
            "name": "当前实际信贷损失率",
            "value": clr.get("value"),
            "display_value": clr.get("display_value") or format_display_value(clr.get("value"), clr.get("unit")),
            "unit": clr.get("unit", "%"),
        },
        "current_actual_pricing_rate": {
            "name": "当前实际定价率",
            "value": pr.get("value"),
            "display_value": pr.get("display_value") or format_display_value(pr.get("value"), pr.get("unit")),
            "unit": pr.get("unit", "%"),
        },
    }


def _build_scenario_sensitivity(params: Dict[str, Any], projection: Dict[str, Any]) -> Dict[str, Any]:
    anr = _num(params.get("anr"))
    roa_spread = _num(projection.get("roa_spread"))

    credit_loss_impact = round(anr * 0.5 / 100, 2) if anr else None
    pricing_impact = round(anr * 0.5 / 100, 2) if anr else None
    anr_impact = round(roa_spread, 2) if roa_spread else None
    funding_impact = round(anr * 0.5 / 100, 2) if anr else None

    return {
        "credit_loss_0_5pct_impact": {
            "name": "信贷损失率每变动0.5pct影响",
            "value": credit_loss_impact,
            "display_value": format_display_value(credit_loss_impact, "亿"),
            "unit": "亿",
        },
        "pricing_0_5pct_impact": {
            "name": "定价率每变动0.5pct影响",
            "value": pricing_impact,
            "display_value": format_display_value(pricing_impact, "亿"),
            "unit": "亿",
        },
        "anr_100bn_impact": {
            "name": "ANR每变动100亿影响",
            "value": anr_impact,
            "display_value": format_display_value(anr_impact, "亿"),
            "unit": "亿",
        },
        "funding_cost_0_5pct_impact": {
            "name": "资金成本每变动0.5pct影响",
            "value": funding_impact,
            "display_value": format_display_value(funding_impact, "亿"),
            "unit": "亿",
        },
    }


def _extract_baseline(core_metrics: List[Dict[str, Any]]) -> Dict[str, Any]:
    npm = next((m for m in core_metrics if m.get("metric_code") == "net_profit_management"), {})
    budget_npm = npm.get("budget")
    last_year_npm = None
    yoy = npm.get("yoy")
    value = npm.get("value")
    if isinstance(yoy, (int, float)) and isinstance(value, (int, float)) and yoy != 0:
        denominator = 1 + yoy / 100
        if abs(denominator) > 1e-9:
            last_year_npm = round(value / denominator, 2)
    return {
        "budget_net_profit": budget_npm,
        "last_year_net_profit": last_year_npm,
    }


def _normalize_scenario_inputs(inputs: Dict[str, Any]) -> Dict[str, Any]:
    mapping = {
        "cg": "consumer_finance_growth",
        "price": "pricing_rate",
        "closs": "credit_loss_rate",
        "npl": "npl_rate",
        "fund": "funding_cost_rate",
        "scost": "sales_cost_rate",
        "ocost": "opex_rate",
        "tax": "tax_other_rate",
    }
    normalized: Dict[str, Any] = {}
    for key, value in inputs.items():
        normalized[mapping.get(key, key)] = value
    return normalized


def format_display_value(value: Any, unit: str) -> str:
    if value is None:
        return ""
    if isinstance(value, (int, float)):
        if unit == "亿":
            return f"{value}亿"
        if unit == "%":
            return f"{value}%"
        if unit == "pct":
            return f"{value}pct"
        if unit:
            return f"{value}{unit}"
        return str(value)
    return str(value)


def format_display_yoy(value: Any, unit: str = "%") -> str:
    if value is None:
        return ""
    if isinstance(value, (int, float)):
        prefix = "+" if value > 0 else ""
        if unit == "pct":
            return f"{prefix}{value}pct"
        return f"{prefix}{value}{unit}"
    return str(value)


def format_display_budget_gap(value: Any, unit: str = "") -> str:
    if value is None:
        return ""
    if isinstance(value, (int, float)):
        prefix = "+" if value > 0 else ""
        if unit == "pct":
            return f"{prefix}{value}pct"
        if unit:
            return f"{prefix}{value}{unit}"
        return f"{prefix}{value}"
    return str(value)


def _num(value: Any) -> Optional[float]:
    return float(value) if isinstance(value, (int, float)) else None


def _load_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)
