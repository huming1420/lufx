from __future__ import annotations

from typing import Any, Dict, List, Optional


DIM_META = {
    "profit": ("💹", "利润健康度"),
    "scale": ("📦", "规模动能"),
    "risk": ("⚠️", "风险信号"),
    "cost": ("💸", "成本效率"),
    "transform": ("🔄", "转型进展"),
    "outlook": ("🔭", "综合前瞻"),
}

DIM_ORDER = ["profit", "scale", "risk", "cost", "transform", "outlook"]

DIMENSION_MODEL = {
    "profit": {
        "icon": "💹",
        "name": "利润健康度",
        "score": 38,
        "status": "red",
        "role": "结果承压",
        "brief": "ROA 0.8%，接近亏损边界",
        "detail_title": "利润健康度深度分析",
        "content": "利润端处于弱修复阶段，管理口径净利润仍低于预算，ROA接近盈亏边界。短期能否转正，核心取决于信贷损失率能否继续下行，以及消金新增规模是否带来有效利润贡献。",
        "bullets": [
            ("🔴", "<strong>利润边界：</strong>管理口径净利润-12.3亿、同比-168%，利润修复仍未完成。"),
            ("🟡", "<strong>ROA压力：</strong>ROA仍接近亏损边界，风险成本轻微波动都会放大利润弹性。"),
            ("🔵", "<strong>验证窗口：</strong>后续重点看贷款业务净利润、ROA环比和预算差是否同步改善。"),
        ],
        "next_validation": ["管理口径净利润预算差", "贷款业务净利润", "ROA环比变化"],
    },
    "scale": {
        "icon": "📦",
        "name": "规模动能",
        "score": 52,
        "status": "yellow",
        "role": "结构观察",
        "brief": "ENR -8%，消金高增对冲中",
        "detail_title": "贷款规模动能分析",
        "content": "规模端不是单纯扩张，而是传统易贷压降与消金增长之间的结构切换。ENR同比下降仍拖累收入基数，消金高增提供对冲，但必须验证新增规模质量能否稳定穿越风险暴露周期。",
        "bullets": [
            ("🟡", "<strong>总量承压：</strong>ANR下降对利润形成负向影响，规模修复还未形成确定趋势。"),
            ("🟢", "<strong>结构对冲：</strong>消金增长对冲易贷压降，是规模动能的主要正向来源。"),
            ("🔵", "<strong>质量验证：</strong>新增放款客群、渠道和早逾表现决定规模增长是否能转化为利润。"),
        ],
        "next_validation": ["ANR环比变化", "消金新增放款", "新增客群早逾率"],
    },
    "risk": {
        "icon": "⚠️",
        "name": "风险信号",
        "score": 22,
        "status": "red",
        "role": "核心拖累",
        "brief": "Vintage恶化+C-M3逼红线",
        "detail_title": "风险成本深度诊断",
        "content": "风险成本仍是最核心拖累，信贷损失率距离红线较近，且易贷Vintage早逾恶化显示风险暴露尚未完全结束。若C-M3未在Q2转向稳定，利润修复节奏会继续后移。",
        "bullets": [
            ("🔴", "<strong>红线压力：</strong>信贷损失率7.8%，距离8.0%红线仅0.2pct。"),
            ("🔴", "<strong>暴露节奏：</strong>易贷24Q1 Vintage早逾恶化，说明存量风险仍在释放。"),
            ("🔵", "<strong>关键窗口：</strong>Q2需验证C-M3迁徙率和Vintage早逾是否转稳。"),
        ],
        "next_validation": ["信贷损失率", "C-M3迁徙率", "Vintage早逾率"],
    },
    "cost": {
        "icon": "💸",
        "name": "成本效率",
        "score": 74,
        "status": "green",
        "role": "正向抵消",
        "brief": "资金/运营双降，超预算",
        "detail_title": "成本效率综合评估",
        "content": "成本端是当前最清晰的正向抵消项，资金成本下降和运营效率改善共同缓冲风险成本压力。问题在于成本优化的空间有限，难以单独覆盖信贷损失率上行带来的利润冲击。",
        "bullets": [
            ("🟢", "<strong>资金成本：</strong>资金成本同比下降0.33pct，对利润形成约+1.4亿贡献。"),
            ("🟢", "<strong>运营效率：</strong>运营成本改善贡献约+0.9亿，是短期利润的抵消项。"),
            ("🟡", "<strong>边际约束：</strong>成本改善可对冲部分压力，但无法替代风险成本修复。"),
        ],
        "next_validation": ["资金成本走势", "运营费用率", "销售成本率"],
    },
    "transform": {
        "icon": "🔄",
        "name": "转型进展",
        "score": 65,
        "status": "yellow",
        "role": "结构观察",
        "brief": "消金替代稳步推进",
        "detail_title": "战略转型进展评估",
        "content": "转型方向成立，但仍处在利润兑现前的验证期。消金增长正在替代传统易贷规模，若客群质量、渠道效率和风险成本不能同步改善，结构迁移会先体现为收入和风险节奏错配。",
        "bullets": [
            ("🟢", "<strong>方向确认：</strong>消金业务增长正在承接传统业务压降后的规模空缺。"),
            ("🟡", "<strong>短期代价：</strong>结构迁移期可能出现规模、收入和风险暴露不同步。"),
            ("🔵", "<strong>验证重点：</strong>需跟踪消金渗透率、直销产能和新增客群质量。"),
        ],
        "next_validation": ["消金占比", "直销产能", "新增客群质量"],
    },
    "outlook": {
        "icon": "🔭",
        "name": "综合前瞻",
        "score": 55,
        "status": "yellow",
        "role": "待验证",
        "brief": "H2转机，全年盈亏待定",
        "detail_title": "综合前瞻与分析师研判",
        "content": "综合多维度信号，AI判断陆控2026年经营节奏为H1筑底、H2反转。核心催化剂在于风险成本能否在Q2见顶，以及消金规模能否持续超预期增长；治理修复和评级改善为估值重评奠定基础。",
        "bullets": [
            ("🟡", "<strong>Q2关键时间窗：</strong>若C-M3在Q2转向平稳、易贷清收明显改善，则H2净利润有望转正；反之压力将延续至2027年。"),
            ("🟢", "<strong>长期逻辑清晰：</strong>消金赛道渗透率仍低，平安消金依托集团资源有独特竞争优势，NPL 1.2%领先行业，天花板显著。"),
            ("🔵", "<strong>分析师预测区间：</strong>中性情景2026年净利润约-5亿至+5亿；乐观情景+10至15亿，取决于信贷损失率能否降至7%以内。"),
            ("🟡", "<strong>主要风险：</strong>若宏观环境持续承压、消费者还款能力进一步恶化，或复牌后抛压冲击，将扰乱利润修复节奏。"),
        ],
        "next_validation": ["Q2 C-M3迁徙率", "信贷损失率是否降至7%以内", "消金新增规模"],
    },
}


def generate(fact_pack: Dict[str, Any]) -> Dict[str, Any]:
    pack_type = fact_pack.get("pack_type")
    if pack_type == "CARD_FACT_PACK":
        return generate_card_insight(fact_pack)
    if pack_type == "DASHBOARD_FACT_PACK":
        return generate_dashboard_insight(fact_pack)
    if pack_type == "SCENARIO_FACT_PACK":
        return generate_scenario_insight(fact_pack)
    raise ValueError(f"Unsupported fact pack type: {pack_type}")


def generate_card_insight(fact_pack: Dict[str, Any]) -> Dict[str, Any]:
    card = fact_pack.get("card", {})
    metrics = fact_pack.get("metrics", [])
    segments = fact_pack.get("segments", [])
    alerts = fact_pack.get("alerts", [])
    drivers = fact_pack.get("driver_ranking", [])
    rule = fact_pack.get("rule_result", {})
    evidence_source = metrics or segments or alerts
    status = _normalize_status(rule.get("suggested_status") or _worst_status(evidence_source))
    lead = _driver_text(drivers[0]) if drivers else _metric_sentence(evidence_source[0] if evidence_source else {})
    second = _driver_text(drivers[1]) if len(drivers) > 1 else _metric_sentence(evidence_source[1] if len(evidence_source) > 1 else {})
    watch = _watch_items(alerts, metrics, segments)
    name = card.get("name") or card.get("id") or "当前卡片"
    insight = f"{name}当前为{_status_cn(status)}。{lead or '规则引擎未识别明确主因'}"
    if second:
        insight += f"；同时{second}"
    insight += f"。后续重点关注{watch[0] if watch else '核心指标趋势'}。"

    return {
        "card_id": str(card.get("id") or "unknown"),
        "status": status,
        "title": _title_by_status(status),
        "standard_insight": insight[:220],
        "drivers": [(_driver_text(d) or str(d))[:60] for d in drivers[:3]] or [_metric_sentence(m)[:60] for m in evidence_source[:3]],
        "watch_items": [w[:60] for w in watch[:3]] or ["核心指标趋势"],
        "evidence": [_evidence_object(item) for item in evidence_source[:5]],
        "exploratory_insights": _card_exploratory(status, drivers, alerts),
        "deep_dive_questions": _card_questions(name, drivers, alerts),
    }


def generate_dashboard_insight(fact_pack: Dict[str, Any]) -> Dict[str, Any]:
    score = _num(fact_pack.get("health_score"), 0)
    label = str(fact_pack.get("health_label") or _health_label(score))
    dims = _dashboard_dimensions(fact_pack.get("dimension_scores", []))
    roots = fact_pack.get("root_causes", [])
    offsets = fact_pack.get("offset_factors", [])
    alerts = fact_pack.get("alerts", [])
    cross = fact_pack.get("cross_signals", [])
    pressure = _root_text(roots[0]) if roots else "规则引擎未识别重大根因"
    offset_text = _offset_text(offsets[0]) if offsets else "暂无明显抵消因子"
    verdict_level = "poor" if score < 55 else "warn" if score < 80 else "good"
    verdict_text = "经营压力偏大，需优先处理核心拖累" if score < 55 else "经营处于观察区间，需跟踪关键变量" if score < 80 else "经营状态较稳，关注改善持续性"

    return {
        "period": str(fact_pack.get("period") or ""),
        "health_score": score,
        "health_label": label,
        "standard_diagnosis": {
            "health_summary": f"综合评分{score}分，整体状态为{label}；主要压力来自{pressure}，{offset_text}形成部分对冲，后续需验证风险成本和规模质量变化。"[:260],
            "verdict": {"level": verdict_level, "text": verdict_text},
            "summary_items": _summary_items(roots, dims),
        },
        "dimensions": dims,
        "root_cause_chain": [_root_text(r)[:80] for r in roots[:8]] or ["规则引擎未识别重大根因"],
        "offset_factors": [_offset_factor(o) for o in offsets[:5]] or [_default_offset()],
        "emerging_findings": _dashboard_findings(cross, roots, alerts),
        "management_questions": _management_questions(roots, alerts),
        "watch_items": _dashboard_watch_items(roots, alerts),
    }


def generate_scenario_insight(fact_pack: Dict[str, Any]) -> Dict[str, Any]:
    scenario = str(fact_pack.get("scenario") or "custom")
    label = str(fact_pack.get("scenario_label") or scenario)
    inputs = fact_pack.get("inputs", {})
    computed = fact_pack.get("computed", {})
    sensitivity = fact_pack.get("sensitivity", {})
    rule = fact_pack.get("rule_result", {})
    anr = _entry_value(inputs.get("anr"))
    pricing = _entry_value(inputs.get("pricing_rate"))
    credit_loss = _entry_value(inputs.get("credit_loss_rate"))
    roa = _entry_value(computed.get("roa"))
    net_profit = _entry_value(computed.get("net_profit"))
    total_cost = _entry_value(computed.get("total_cost_rate"))
    credit_impact = _entry_value(sensitivity.get("credit_loss_0_5pct_impact"))
    pricing_impact = _entry_value(sensitivity.get("pricing_0_5pct_impact"))

    return {
        "scenario": scenario,
        "scenario_label": label,
        "standard_explanation": {
            "judgement": f"当前{label}下预测ROA为{_fmt(roa)}%，预测净利润为{_fmt(net_profit)}亿，情景质量为{rule.get('scenario_quality', '待验证')}。"[:180],
            "profit_driver": f"利润主要由ANR {_fmt(anr)}亿、定价率{_fmt(pricing)}%与总成本率{_fmt(total_cost)}%共同驱动，其中信贷损失率{_fmt(credit_loss)}%是关键约束。"[:180],
            "key_variable": str(rule.get("key_variable") or "信贷损失率")[:40],
            "risk_boundary": f"{rule.get('risk_boundary_hint') or '信贷损失率继续上行将压缩ROA和净利润空间'}；当前每0.5pct变动约影响净利润{_fmt(credit_impact)}亿。"[:180],
            "upside_condition": f"{rule.get('upside_hint') or '风险成本稳定且ANR不下滑时利润修复更稳'}，若定价保持且风险改善，利润弹性会进一步释放。"[:180],
            "sensitivity_comment": f"敏感性显示，信贷损失率每0.5pct影响约{_fmt(credit_impact)}亿，定价率每0.5pct影响约{_fmt(pricing_impact)}亿，需重点跟踪风险与定价。"[:180],
            "management_commentary": f"管理层应将该情景作为预算压力测试输入，优先验证信贷损失率、ANR增长质量和定价稳定性。若规模增长来自高风险客群，当前净利润改善可能被后续风险成本抵消；若风险成本稳定，则利润修复更具持续性。"[:260],
        },
        "scenario_exploration": {
            "fragile_assumption": _exploration(
                f"最脆弱假设是信贷损失率维持在{_fmt(credit_loss)}%，因为该指标每0.5pct变动会带来约{_fmt(credit_impact)}亿净利润影响。",
                "high",
                [f"信贷损失率{_fmt(credit_loss)}%", f"敏感性影响{_fmt(credit_impact)}亿"],
                ["C-M3迁徙率", "Vintage早逾率", "分产品损失率"],
            ),
            "hidden_risk": _exploration(
                "如果ANR增长来自高风险客群，净利润改善可能被后续风险成本抵消，需要验证规模增长质量。",
                "medium",
                [f"ANR {_fmt(anr)}亿", f"信贷损失率{_fmt(credit_loss)}%"],
                ["新增客群风险分布", "放款渠道逾期表现"],
            ),
            "upside_surprise": _exploration(
                "若资金成本和运营成本继续改善，且风险成本不再上行，ROA修复可能快于单纯规模扩张带来的改善。",
                "medium",
                [f"预测ROA {_fmt(roa)}%", f"预测净利润{_fmt(net_profit)}亿"],
                ["资金成本趋势", "运营成本率", "定价率稳定性"],
            ),
        },
        "watch_items": ["信贷损失率", "ANR增长质量", "综合定价率", "NPL不良率", "运营成本率"],
    }


def _dashboard_dimensions(source: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    by_id = {str(d.get("id")): d for d in source if d.get("id")}
    result = []
    for dim_id in DIM_ORDER:
        item = by_id.get(dim_id, {})
        model = DIMENSION_MODEL[dim_id]
        score = _num(item.get("score"), model["score"])
        status = _normalize_status(item.get("status") or model["status"] or _status_from_score(score))
        evidence = item.get("evidence") or []
        brief = str(model["brief"])
        content = str(model["content"])
        if evidence and dim_id in ("profit", "risk"):
            content = f"{brief}。主要依据为{'; '.join(str(x) for x in evidence[:3])}。{model['content']}"
        result.append({
            "id": dim_id,
            "icon": model["icon"],
            "name": str(item.get("name") or model["name"])[:20],
            "score": score,
            "status": status,
            "role": model["role"],
            "brief": brief[:80],
            "detail": {
                "title": str(model["detail_title"])[:40],
                "content": content[:260],
                "bullets": [
                    {"icon": icon, "text": text}
                    for icon, text in model["bullets"]
                ],
                "next_validation": [str(x)[:120] for x in (item.get("watch_items") or model["next_validation"])[:5]],
            },
        })
    return result


def _summary_items(roots: List[Dict[str, Any]], dims: List[Dict[str, Any]]) -> List[Dict[str, str]]:
    dim_map = {str(d.get("id")): d for d in dims if d.get("id")}
    root_texts = [_root_text(r) for r in roots if _root_text(r)]
    result: List[Dict[str, str]] = []

    def add(dim_id: str, label: str, color: str, text: str) -> None:
        if len(result) >= 4 or not text:
            return
        result.append({"type": dim_id, "label": label[:20], "color": color, "text": text[:220]})

    risk_dim = dim_map.get("risk", {})
    scale_dim = dim_map.get("scale", {})
    cost_dim = dim_map.get("cost", {})
    transform_dim = dim_map.get("transform", {})

    pressure = root_texts[0] if root_texts else str(risk_dim.get("brief") or "风险成本仍是利润修复的首要验证点")
    add(
        "risk",
        "风险成本",
        "red",
        f"{pressure}，对ROA和净利润形成直接约束；后续需验证Vintage、迁徙率和分产品损失率是否同步改善。",
    )

    add(
        "scale",
        "规模质量",
        "yellow",
        f"{scale_dim.get('brief') or '规模动能处于观察区间'}；规模扩张只有在客群质量稳定时才会放大利润，需跟踪ANR环比和新增放款风险结构。",
    )

    add(
        "cost",
        "成本抵消",
        "green",
        f"{cost_dim.get('brief') or '资金及运营成本改善提供部分对冲'}；该改善能否持续决定风险压力被吸收的程度，需关注资金成本和费用率走势。",
    )

    add(
        "transform",
        "转型进展",
        "accent",
        f"{transform_dim.get('brief') or '产品和渠道结构仍在迁移'}；短期会带来规模与风险节奏错配，需验证消金增长、直销产能和客群上移质量。",
    )

    return result


def _card_exploratory(status: str, drivers: List[Dict[str, Any]], alerts: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    items = []
    if drivers:
        items.append({
            "type": "hidden_risk",
            "insight": f"{_driver_text(drivers[0])}可能继续传导到利润、ROA或风险成本，需判断是否为结构性变化而非短期波动。"[:220],
            "confidence": "high" if status == "red" else "medium",
            "supporting_facts": [_driver_text(drivers[0])[:100]],
            "validation_needed": ["连续三期趋势", "相关指标传导关系"],
        })
    if alerts:
        items.append({
            "type": "quality_issue",
            "insight": f"规则引擎检测到{_alert_text(alerts[0])}，需要验证该异常是否持续，以及是否集中在特定产品、渠道或客群。"[:220],
            "confidence": "medium",
            "supporting_facts": [_alert_text(alerts[0])[:100]],
            "validation_needed": ["分产品数据", "分渠道数据"],
        })
    return items[:2]


def _dashboard_findings(cross: List[Dict[str, Any]], roots: List[Dict[str, Any]], alerts: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    sources = cross or roots or alerts
    findings = []
    for source in sources[:3]:
        text = str(source.get("signal") or source.get("factor") or source.get("metric") or source.get("tag") or "潜在经营信号")
        facts = source.get("supporting_facts") or source.get("path") or [text]
        findings.append({
            "type": source.get("type") if source.get("type") in {"contradiction", "hidden_risk", "quality_issue", "offset_failure", "structural_shift", "second_order_effect"} else "hidden_risk",
            "finding": f"{text}，该信号可能影响利润修复节奏，需要结合后续指标验证。"[:240],
            "confidence": "medium",
            "supporting_facts": [str(f)[:120] for f in facts[:5]],
            "why_it_matters": "该信号会影响管理层对风险、规模和成本抵消效果的判断。"[:180],
            "next_validation": ["后续月度趋势", "分产品/分渠道拆解"],
        })
    return findings


def _management_questions(roots: List[Dict[str, Any]], alerts: List[Dict[str, Any]]) -> List[str]:
    questions = [f"{_root_text(r)[:70]}是否会继续恶化？" for r in roots[:3]]
    questions += [f"{_alert_text(a)[:70]}是否需要专项跟踪？" for a in alerts[:2]]
    return (questions or ["当前经营状态是否需要调整策略方向？"])[:5]


def _dashboard_watch_items(roots: List[Dict[str, Any]], alerts: List[Dict[str, Any]]) -> List[str]:
    items = []
    for source in roots[:3] + alerts[:2]:
        item = source.get("factor") or source.get("metric") or source.get("tag") or source.get("related_metric")
        if item:
            items.append(str(item)[:100])
    return (items or ["信贷损失率", "净利润", "ANR规模"])[:5]


def _watch_items(alerts: List[Dict[str, Any]], metrics: List[Dict[str, Any]], segments: List[Dict[str, Any]]) -> List[str]:
    items = [str(a.get("tag") or a.get("metric") or a.get("related_metric")) for a in alerts if a]
    items += [str(m.get("name") or m.get("metric_name") or m.get("code")) for m in metrics + segments if _normalize_status(m.get("status")) != "green"]
    return [i for i in items if i and i != "None"]


def _evidence_object(item: Dict[str, Any]) -> Dict[str, str]:
    metric = item.get("metric") or item.get("metric_name") or item.get("name") or item.get("tag") or item.get("code") or item.get("metric_code") or "指标"
    value = item.get("display_value") or _fmt(item.get("value"))
    note = item.get("note") or item.get("reason") or f"状态{_status_cn(item.get('status'))}"
    return {"metric": str(metric), "value": str(value or "--"), "note": str(note)[:120]}


def _driver_text(item: Dict[str, Any]) -> str:
    if not item:
        return ""
    if item.get("driver") and item.get("reason"):
        return f"{item.get('driver')}：{item.get('reason')}"
    if item.get("factor") and item.get("display_impact"):
        return f"{item.get('factor')}影响{item.get('display_impact')}"
    return str(item.get("driver") or item.get("reason") or item.get("factor") or item.get("conclusion_hint") or _metric_sentence(item))


def _root_text(item: Dict[str, Any]) -> str:
    factor = item.get("factor") or item.get("reason") or item.get("metric_name") or item.get("metric_code") or "根因"
    impact = item.get("display_impact")
    return f"{factor}{'（影响' + str(impact) + '）' if impact else ''}"


def _offset_text(item: Dict[str, Any]) -> str:
    factor = item.get("factor") or "正向因素"
    impact = item.get("display_impact")
    return f"{factor}{'贡献' + str(impact) if impact else ''}"


def _offset_factor(item: Dict[str, Any]) -> Dict[str, Any]:
    return {
        "factor": str(item.get("factor") or "正向因素")[:60],
        "text": f"{_offset_text(item)}，对经营压力形成部分对冲。"[:160],
        "evidence": [str(item.get("evidence") or _offset_text(item))[:120]],
    }


def _default_offset() -> Dict[str, Any]:
    return {"factor": "暂无明显抵消因子", "text": "当前未检测到显著正向抵消效应，需持续监控潜在利好指标。", "evidence": ["绿灯指标较少"]}


def _metric_sentence(item: Dict[str, Any]) -> str:
    name = item.get("name") or item.get("metric_name") or item.get("metric") or item.get("code") or item.get("metric_code") or "指标"
    value = item.get("display_value") or _fmt(item.get("value"))
    status = _status_cn(item.get("status"))
    note = item.get("note") or item.get("reason") or ""
    return f"{name}{'为' + str(value) if value not in ('', None) else ''}，状态{status}{'，' + str(note) if note else ''}"


def _alert_text(item: Dict[str, Any]) -> str:
    return str(item.get("tag") or item.get("metric") or item.get("reason") or item.get("related_metric") or "异常信号")


def _card_questions(name: str, drivers: List[Dict[str, Any]], alerts: List[Dict[str, Any]]) -> List[str]:
    questions = [f"{name}的核心拖累是否会延续？"]
    if drivers:
        questions.append(f"{_driver_text(drivers[0])[:60]}是否为结构性变化？")
    if alerts:
        questions.append(f"{_alert_text(alerts[0])[:60]}是否需要专项拆解？")
    return questions[:3]


def _exploration(insight: str, confidence: str, facts: List[str], validation: List[str]) -> Dict[str, Any]:
    return {
        "insight": insight[:220],
        "confidence": confidence,
        "supporting_facts": [str(f)[:120] for f in facts[:5]],
        "validation_needed": [str(v)[:120] for v in validation[:5]],
    }


def _entry_value(entry: Any) -> Optional[float]:
    if isinstance(entry, dict):
        return _num(entry.get("value"), None)
    return _num(entry, None)


def _normalize_status(status: Any) -> str:
    return str(status) if status in ("red", "yellow", "green") else "yellow"


def _worst_status(items: List[Dict[str, Any]]) -> str:
    statuses = {_normalize_status(item.get("status") or item.get("level")) for item in items}
    if "red" in statuses:
        return "red"
    if "yellow" in statuses:
        return "yellow"
    if "green" in statuses:
        return "green"
    return "yellow"


def _status_from_score(score: float) -> str:
    if score < 55:
        return "red"
    if score < 80:
        return "yellow"
    return "green"


def _status_cn(status: Any) -> str:
    return {"red": "红灯", "yellow": "黄灯", "green": "绿灯"}.get(str(status), "待确认")


def _health_label(score: float) -> str:
    if score >= 80:
        return "经营健康"
    if score >= 55:
        return "中性观察"
    return "压力偏大"


def _role(status: str, dim_id: str) -> str:
    if status == "red":
        return "结果承压" if dim_id == "profit" else "核心拖累"
    if status == "green":
        return "正向抵消"
    if dim_id in ("scale", "transform"):
        return "结构观察"
    return "待验证" if dim_id == "outlook" else "边际改善"


def _color(item: Dict[str, Any]) -> str:
    status = _normalize_status(item.get("status") or item.get("level"))
    return {"red": "red", "yellow": "yellow", "green": "green"}.get(status, "yellow")


def _title_by_status(status: str) -> str:
    return {"red": "经营承压", "yellow": "经营观察", "green": "经营稳健"}[status]


def _num(value: Any, default: Optional[float] = 0) -> Optional[float]:
    return float(value) if isinstance(value, (int, float)) else default


def _fmt(value: Any) -> str:
    if value is None:
        return "--"
    if isinstance(value, float):
        return f"{value:.2f}".rstrip("0").rstrip(".")
    return str(value)
