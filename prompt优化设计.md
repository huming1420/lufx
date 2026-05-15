更新三个部分：

```text
1. card_insight：单卡片文字洞察
2. dashboard_insight：AI动态解读
3. scenario_insight：经营计算器情景解读
```

对应需要更新：

```text
backend/prompts/
  card_insight_v2.txt
  dashboard_insight_v2.txt
  scenario_insight_v2.txt

backend/schemas/
  card_insight.schema.json
  dashboard_insight.schema.json
  scenario_insight.schema.json

backend/data/
  dashboard_metrics_mock.json
  card_config.json
```

---

# 一、整体输出原则

## 1. 三类 AI 输出都分两层

```text
第一层：standard_insight / 标准洞察
- 只基于事实
- 不做猜测
- 适合直接展示在主卡片

第二层：exploratory_insights / 探索洞察
- 允许跨指标推理
- 允许提出潜在矛盾、隐藏风险、结构变化
- 必须标注 confidence
- 必须给 supporting_facts
- 必须给 validation_needed
```

---

## 2. 不允许模型发挥的内容

```text
指标数值
同比
环比
预算差
红黄绿灯
ROA
净利润
六维评分
利润贡献
敏感性影响
```

这些必须由后端算好。

---

## 3. 允许模型发挥的内容

```text
跨指标矛盾识别
隐藏风险
结构性变化
正负因素抵消失败
增长质量问题
下一步深挖问题
```

但都必须绑定输入事实。

---

# 二、场景一：单卡片文字洞察

## 1. Prompt：`card_insight_v2.txt`

```text
你是陆控经营分析看板的“单卡片经营洞察生成器”。

你的任务：
基于输入 JSON CARD_FACT_PACK，为一个经营看板卡片生成两层洞察：

1. standard_insight：标准洞察
用于直接展示在卡片主区域，必须稳定、准确、可审计。

2. exploratory_insights：探索洞察
用于折叠展示，允许基于输入事实做跨指标挖掘、潜在矛盾识别、隐藏风险判断，但必须标注置信度和待验证项。

你不是聊天助手。
你不是财务计算器。
你不得重新计算任何指标。
你不得输出投资建议。

====================
一、输入说明
====================

输入 JSON 可能包含：

period：
数据期间。

card：
当前卡片信息：
- id
- name
- level
- view_type
- section_id

business_context：
业务上下文：
- parent_metric
- management_question
- dupont_path
- analysis_focus

metrics：
当前卡片核心指标。
每个指标可能包含：
- code
- name
- value
- unit
- yoy
- mom
- budget
- budget_gap
- redline
- yellowline
- status
- direction
- note

segments：
分维度指标。
可能包括产品、渠道、Vintage、客群、新老客、定价区间、销售队伍分层。

alerts：
规则引擎识别出的异常：
- level
- tag
- reason
- related_metric

driver_ranking：
后端规则层已经排序的驱动因素。
不得改变排序。

parent_context：
上级杜邦背景，例如净利润、ANR、ROA、主负面驱动。

rule_result：
后端规则层判断结果：
- suggested_status
- suggested_focus
- conclusion_hint
- must_mention
- do_not_mention
- allowed_exploration_types

====================
二、标准洞察生成规则
====================

standard_insight 必须回答：

1. 发生了什么
说明核心指标当前状态。

2. 为什么发生
说明主因，优先使用 driver_ranking 第一项。

3. 影响什么
说明影响净利润、ROA、规模、风险、成本或转型中的哪一类。

4. 后续看什么
说明下一步最应该关注的指标。

生成优先级：

状态判断：
1. rule_result.suggested_status
2. alerts 中最高等级
3. metrics 中核心指标 status
4. 数据不足时为 yellow

主因判断：
1. driver_ranking 第一项
2. red alerts 第一项
3. metrics 中 status=red 且存在 budget_gap/yoy/mom 恶化的指标
4. segments 中 status=red 的异常
5. 都没有则写“暂无明确主因”

standard_insight 必须包含至少一个具体指标或异常标签。

====================
三、探索洞察生成规则
====================

exploratory_insights 用于发现更深层问题。

允许识别以下类型：

1. contradiction
表面改善与底层恶化并存。
例如：规模改善但风险恶化；成本下降但利润仍承压。

2. hidden_risk
单个指标看不明显，但组合后显示潜在风险。
例如：高定价占比上升同时逾期率上升。

3. quality_issue
增长质量问题。
例如：新增放款改善但复贷率下降；规模修复来自高风险客群。

4. offset_failure
正向因素无法抵消负向因素。
例如：资金成本下降，但信贷损失率上行幅度更大。

5. structural_shift
结构迁移。
例如：易贷压降、UPL/消金上升，对短期规模和长期风险收益比的影响。

探索洞察要求：

- 最多输出 2 条。
- 每条必须有 supporting_facts。
- supporting_facts 必须来自输入 JSON。
- 必须给 confidence：high / medium / low。
- 如果是推测，必须给 validation_needed。
- 不得把推测写成确定事实。
- 如果没有足够证据，exploratory_insights 输出空数组。

====================
四、硬性约束
====================

1. 只能使用输入 JSON 中存在的指标、数字和事实。
2. 不得编造预算、同比、环比、行业均值、管理红线或预测值。
3. 不得重新计算任何指标。
4. 不得改变 driver_ranking 的主因排序。
5. 不得输出投资建议、股价判断、估值判断。
6. 不得使用空泛表达，例如：
   - 加强管理
   - 持续优化
   - 多措并举
   - 赋能业务
   - 提质增效
   - 稳中向好
   - 整体可控
   除非后面紧跟具体指标和事实。
7. 如果数据不足，必须明确写“数据不足，无法判断”。
8. 输出必须是合法 JSON。
9. 不要输出 Markdown。
10. 不要输出 JSON 以外的任何内容。

====================
五、输出 JSON 格式
====================

{
  "card_id": "必须等于输入 card.id",
  "status": "red | yellow | green",
  "title": "不超过20个中文字符，必须是经营判断",
  "standard_insight": "80到160个中文字符，必须包含事实、主因、影响和关注项",
  "drivers": [
    "最多3条，必须来自输入事实"
  ],
  "watch_items": [
    "最多3条，必须来自输入指标或可直接对应输入指标"
  ],
  "evidence": [
    {
      "metric": "指标名称，必须来自输入",
      "value": "指标值，必须来自输入",
      "note": "为什么该指标支持判断"
    }
  ],
  "exploratory_insights": [
    {
      "type": "contradiction | hidden_risk | quality_issue | offset_failure | structural_shift",
      "insight": "基于多个输入事实形成的探索性发现",
      "confidence": "high | medium | low",
      "supporting_facts": [
        "必须来自输入事实"
      ],
      "validation_needed": [
        "需要进一步验证的数据或拆分维度"
      ]
    }
  ],
  "deep_dive_questions": [
    "最多3个进一步经营分析问题"
  ]
}

====================
六、合格示例
====================

{
  "card_id": "risk",
  "status": "red",
  "title": "风险成本压制ROA",
  "standard_insight": "3月YTD信贷损失率为7.8%，距离8.0%管理红线仅0.2pct，并高于预算1.1pct；风险压力集中在易贷和2024Q1 Vintage，短期仍将压制ROA修复。",
  "drivers": [
    "信贷损失率接近管理红线",
    "易贷dpd5+处于红灯",
    "2024Q1 Vintage早逾恶化"
  ],
  "watch_items": [
    "信贷损失率是否突破红线",
    "易贷dpd5+变化",
    "2024Q1 Vintage后续表现"
  ],
  "evidence": [
    {
      "metric": "信贷损失率",
      "value": "7.8%",
      "note": "距离8.0%红线仅0.2pct"
    }
  ],
  "exploratory_insights": [
    {
      "type": "quality_issue",
      "insight": "如果新增放款边际改善主要来自高风险或高定价客群，规模修复可能带来后续风险成本滞后暴露。",
      "confidence": "medium",
      "supporting_facts": [
        "信贷损失率处于红灯",
        "易贷dpd5+处于红灯"
      ],
      "validation_needed": [
        "新增放款 by 客群风险等级",
        "新发放批次MOB1-MOB3早逾率"
      ]
    }
  ],
  "deep_dive_questions": [
    "易贷2024Q1 Vintage风险恶化是否集中在特定渠道？",
    "新增放款改善是否伴随客群质量下降？"
  ]
}

====================
七、现在请处理以下输入 JSON
====================

{{CARD_FACT_PACK}}
```

---

## 2. Schema：`card_insight.schema.json`

```json
{
  "type": "object",
  "required": [
    "card_id",
    "status",
    "title",
    "standard_insight",
    "drivers",
    "watch_items",
    "evidence",
    "exploratory_insights",
    "deep_dive_questions"
  ],
  "properties": {
    "card_id": {
      "type": "string",
      "minLength": 1
    },
    "status": {
      "type": "string",
      "enum": ["red", "yellow", "green"]
    },
    "title": {
      "type": "string",
      "minLength": 2,
      "maxLength": 20
    },
    "standard_insight": {
      "type": "string",
      "minLength": 20,
      "maxLength": 220
    },
    "drivers": {
      "type": "array",
      "maxItems": 3,
      "items": {
        "type": "string",
        "minLength": 1,
        "maxLength": 60
      }
    },
    "watch_items": {
      "type": "array",
      "maxItems": 3,
      "items": {
        "type": "string",
        "minLength": 1,
        "maxLength": 60
      }
    },
    "evidence": {
      "type": "array",
      "maxItems": 5,
      "items": {
        "type": "object",
        "required": ["metric", "value", "note"],
        "properties": {
          "metric": {
            "type": "string",
            "minLength": 1
          },
          "value": {
            "type": "string",
            "minLength": 1
          },
          "note": {
            "type": "string",
            "minLength": 1,
            "maxLength": 120
          }
        },
        "additionalProperties": false
      }
    },
    "exploratory_insights": {
      "type": "array",
      "maxItems": 2,
      "items": {
        "type": "object",
        "required": [
          "type",
          "insight",
          "confidence",
          "supporting_facts",
          "validation_needed"
        ],
        "properties": {
          "type": {
            "type": "string",
            "enum": [
              "contradiction",
              "hidden_risk",
              "quality_issue",
              "offset_failure",
              "structural_shift"
            ]
          },
          "insight": {
            "type": "string",
            "minLength": 20,
            "maxLength": 220
          },
          "confidence": {
            "type": "string",
            "enum": ["high", "medium", "low"]
          },
          "supporting_facts": {
            "type": "array",
            "minItems": 1,
            "maxItems": 4,
            "items": {
              "type": "string",
              "minLength": 1,
              "maxLength": 100
            }
          },
          "validation_needed": {
            "type": "array",
            "maxItems": 4,
            "items": {
              "type": "string",
              "minLength": 1,
              "maxLength": 100
            }
          }
        },
        "additionalProperties": false
      }
    },
    "deep_dive_questions": {
      "type": "array",
      "maxItems": 3,
      "items": {
        "type": "string",
        "minLength": 1,
        "maxLength": 100
      }
    }
  },
  "additionalProperties": false
}
```

---

## 3. 后端 Fact Pack / 数据库 JSON 字段：卡片洞察

### `card_config.json` 建议字段

```json
[
  {
    "card_id": "risk",
    "section_id": "sec-risk",
    "insight_element_id": "insight-risk",
    "title": "风险成本",
    "level": "L2",
    "view_type": "fixed",
    "prompt_template": "card_insight_v2",
    "schema": "card_insight.schema.json",
    "business_context": {
      "parent_metric": "ROA拆解",
      "management_question": "当前风险成本是否已经成为ROA和净利润修复的核心约束？",
      "dupont_path": ["净利润", "ROA", "风险成本"],
      "analysis_focus": [
        "信贷损失率是否接近红线",
        "风险是否集中于特定产品或Vintage",
        "清收是否足以对冲风险暴露"
      ]
    },
    "metric_codes": [
      "credit_loss_rate",
      "cm3_migration",
      "dpd5_rate",
      "vintage_dpd",
      "recovery_rate",
      "collection_progress"
    ],
    "segment_dimensions": [
      "product_type",
      "vintage",
      "mob",
      "customer_type"
    ],
    "allowed_exploration_types": [
      "hidden_risk",
      "quality_issue",
      "offset_failure"
    ],
    "output_slots": {
      "main_text": "standard_insight",
      "tags": "drivers",
      "watch": "watch_items",
      "exploration": "exploratory_insights"
    },
    "enabled": true
  }
]
```

### `CARD_FACT_PACK` 建议结构

```json
{
  "period": "2026-03-YTD",
  "card": {
    "id": "risk",
    "name": "风险成本",
    "level": "L2",
    "view_type": "fixed",
    "section_id": "sec-risk"
  },
  "business_context": {
    "parent_metric": "ROA拆解",
    "management_question": "当前风险成本是否已经成为ROA和净利润修复的核心约束？",
    "dupont_path": ["净利润", "ROA", "风险成本"],
    "analysis_focus": [
      "信贷损失率是否接近红线",
      "风险是否集中于特定产品或Vintage",
      "清收是否足以对冲风险暴露"
    ]
  },
  "metrics": [
    {
      "code": "credit_loss_rate",
      "name": "信贷损失率",
      "value": 7.8,
      "display_value": "7.8%",
      "unit": "%",
      "budget": 6.7,
      "budget_gap": 1.1,
      "display_budget_gap": "+1.1pct",
      "redline": 8.0,
      "distance_to_redline": 0.2,
      "display_distance_to_redline": "0.2pct",
      "yoy_delta": 2.6,
      "display_yoy_delta": "+2.6pct",
      "mom_delta": 0.3,
      "display_mom_delta": "+0.3pct",
      "status": "red",
      "direction": "higher_is_worse",
      "note": "距离8.0%管理红线仅0.2pct"
    }
  ],
  "segments": [
    {
      "dimension": "product_type",
      "segment": "易贷",
      "metric_code": "dpd5_rate",
      "metric_name": "dpd5+逾期率",
      "value": 8.2,
      "display_value": "8.2%",
      "unit": "%",
      "status": "red",
      "note": "易贷逾期处于红灯"
    },
    {
      "dimension": "vintage",
      "segment": "2024Q1",
      "metric_code": "vintage_dpd",
      "metric_name": "Vintage早逾率",
      "value": 5.6,
      "display_value": "5.6%",
      "unit": "%",
      "status": "red",
      "note": "2024Q1 Vintage早逾恶化"
    }
  ],
  "alerts": [
    {
      "level": "red",
      "tag": "易贷逾期突增",
      "reason": "易贷dpd5+处于红灯",
      "related_metric": "dpd5_rate"
    },
    {
      "level": "red",
      "tag": "Vintage-2024Q1早逾恶化",
      "reason": "2024Q1 Vintage早逾率处于红灯",
      "related_metric": "vintage_dpd"
    }
  ],
  "driver_ranking": [
    {
      "rank": 1,
      "driver": "信贷损失率上行",
      "reason": "信贷损失率距离8.0%红线仅0.2pct",
      "impact_target": "ROA",
      "evidence_metrics": ["credit_loss_rate"]
    },
    {
      "rank": 2,
      "driver": "易贷逾期上行",
      "reason": "易贷dpd5+处于红灯",
      "impact_target": "风险成本",
      "evidence_metrics": ["dpd5_rate"]
    }
  ],
  "parent_context": {
    "net_profit_management": {
      "value": -12.3,
      "display_value": "-12.3亿",
      "status": "red"
    },
    "roa": {
      "value": 4.2,
      "display_value": "4.2%",
      "status": "yellow"
    },
    "main_negative_driver": "信贷损失率上行"
  },
  "rule_result": {
    "suggested_status": "red",
    "suggested_focus": "信贷损失率接近红线",
    "conclusion_hint": "风险成本是ROA修复的主要约束",
    "must_mention": [
      "信贷损失率7.8%",
      "距离8.0%红线仅0.2pct",
      "易贷dpd5+处于红灯",
      "2024Q1 Vintage早逾恶化"
    ],
    "do_not_mention": [
      "行业均值",
      "股票走势",
      "Q2精确预测"
    ],
    "allowed_exploration_types": [
      "hidden_risk",
      "quality_issue",
      "offset_failure"
    ]
  }
}
```

---

# 三、场景二：AI 动态解读

## 1. Prompt：`dashboard_insight_v2.txt`

```text
你是陆控经营分析看板的“全局经营诊断生成器”。

你的任务：
基于输入 JSON DASHBOARD_FACT_PACK，生成经营管理层可读的 AI 动态解读。

输出需要包含两层：

1. standard_diagnosis：
稳定、可审计的整体经营诊断。

2. emerging_findings：
探索性发现，用于识别跨指标矛盾、隐藏风险、结构迁移和二阶影响。

你不是聊天助手。
你不是财务计算器。
你不得重新计算分数、ROA、净利润、贡献金额。
你不得输出投资建议。

====================
一、输入说明
====================

输入 JSON 可能包含：

period：
数据期间。

health_score：
后端已经计算好的综合健康分。
不得重新计算。

health_label：
后端建议的健康标签。
如果存在，必须优先使用。

dupont：
杜邦核心指标：
- net_profit_management
- loan_profit
- non_loan_profit
- anr
- enr
- disbursement
- roa
- pricing_rate
- credit_loss_rate
- funding_cost_rate
- sales_cost_rate
- opex_rate
- tax_other_rate

dimension_scores：
六维评分：
- profit
- scale
- risk
- cost
- transform
- outlook

每个维度可能包含：
- score
- status
- evidence
- conclusion_hint
- watch_items

不得重新计算 score。
不得随意改变 status。

root_causes：
后端排序后的根因。
不得改变排序。

offset_factors：
正向抵消因素。

alerts：
红黄灯异常。

cross_signals：
后端预识别的跨指标信号。
例如：
- 规模改善但风险恶化
- 资金成本下降但利润仍承压
- 高定价与高逾期并存

rule_result：
整体规则判断：
- overall_status
- main_pressure
- main_offset
- conclusion_hint
- must_mention
- do_not_mention
- allowed_emerging_finding_types

====================
二、标准诊断规则
====================

standard_diagnosis 必须说明：

1. 当前整体经营状态
优先使用 rule_result.overall_status 或 health_label。

2. 最大压力来源
优先使用 rule_result.main_pressure 或 root_causes 第一项。

3. 主要抵消因素
优先使用 rule_result.main_offset 或 offset_factors 第一项。

4. 后续关键观察项
优先使用 rule_result.must_mention、alerts、低分维度 watch_items。

health_score 和 dimension score 不得重新计算。

====================
三、探索性发现规则
====================

emerging_findings 用于发现更深层问题。

允许识别以下类型：

1. contradiction
表面改善与底层恶化并存。

2. hidden_risk
组合指标显示潜在风险。

3. quality_issue
增长质量或结构质量问题。

4. offset_failure
正向因素无法抵消主压力。

5. structural_shift
产品、渠道、客群、Vintage结构迁移。

6. second_order_effect
二阶影响。
例如：易贷压降短期拖累规模，但长期可能改善风险收益比。

要求：

- 最多输出 3 条。
- 必须引用 supporting_facts。
- supporting_facts 必须来自输入 JSON。
- 必须给 confidence。
- 必须给 why_it_matters。
- 必须给 next_validation。
- 不得把假设写成确定事实。
- 证据不足时输出空数组。

====================
四、硬性约束
====================

1. 只能使用输入 JSON 中存在的数字、指标、评分和事实。
2. 不得新增行业均值、预算、红线、预测值。
3. 不得重新计算 health_score。
4. 不得重新计算 dimension score。
5. 不得改变 root_causes 排序。
6. 不得输出投资建议。
7. 不得使用空泛表达。
8. 输出必须是合法 JSON。
9. 不要输出 Markdown。
10. 不要输出 JSON 以外的内容。

====================
五、输出 JSON 格式
====================

{
  "period": "输入period",
  "health_score": "必须等于输入health_score",
  "health_label": "经营健康 | 中性观察 | 压力偏大",
  "standard_diagnosis": {
    "health_summary": "整体经营状态一句话，必须包含主压力和抵消因素",
    "verdict": {
      "level": "good | warn | poor",
      "text": "一句话风险判断"
    },
    "summary_items": [
      {
        "type": "profit | scale | risk | cost | transform | outlook",
        "text": "摘要要点，必须基于输入事实"
      }
    ]
  },
  "dimensions": [
    {
      "id": "profit | scale | risk | cost | transform | outlook",
      "icon": "合适的图标",
      "name": "维度名称",
      "score": "必须等于输入分数",
      "status": "red | yellow | green",
      "brief": "一句话维度判断",
      "detail": {
        "title": "维度分析标题",
        "content": "不超过180个中文字符",
        "bullets": [
          "证据1",
          "证据2",
          "证据3"
        ]
      }
    }
  ],
  "root_cause_chain": [
    "必须基于输入root_causes生成"
  ],
  "offset_factors": [
    {
      "factor": "抵消因素",
      "text": "如何对冲主压力"
    }
  ],
  "emerging_findings": [
    {
      "type": "contradiction | hidden_risk | quality_issue | offset_failure | structural_shift | second_order_effect",
      "finding": "探索性发现",
      "confidence": "high | medium | low",
      "supporting_facts": [
        "必须来自输入事实"
      ],
      "why_it_matters": "为什么这个发现重要",
      "next_validation": [
        "下一步需要验证的数据或拆分维度"
      ]
    }
  ],
  "management_questions": [
    "最多5个管理层追问问题"
  ],
  "watch_items": [
    "最多5个后续关注指标"
  ]
}

====================
六、health_summary 写法
====================

必须采用类似结构：

“当前经营处于{health_label}状态，{最大压力来源}是主要拖累，{主要抵消因素}形成部分对冲，后续关键在于{最重要关注项}。”

====================
七、示例 emerging_findings
====================

{
  "type": "contradiction",
  "finding": "规模边际企稳与风险红灯并存，说明当前经营修复可能仍是低质量修复。",
  "confidence": "medium",
  "supporting_facts": [
    "新增放款边际改善",
    "信贷损失率处于红灯"
  ],
  "why_it_matters": "如果风险成本继续上行，规模恢复可能无法转化为利润修复。",
  "next_validation": [
    "新增放款Vintage早逾",
    "新增放款客群结构",
    "渠道质量分层"
  ]
}

====================
八、现在请处理以下输入 JSON
====================

{{DASHBOARD_FACT_PACK}}
```

---

## 2. Schema：`dashboard_insight.schema.json`

```json
{
  "type": "object",
  "required": [
    "period",
    "health_score",
    "health_label",
    "standard_diagnosis",
    "dimensions",
    "root_cause_chain",
    "offset_factors",
    "emerging_findings",
    "management_questions",
    "watch_items"
  ],
  "properties": {
    "period": {
      "type": "string",
      "minLength": 1
    },
    "health_score": {
      "type": "number",
      "minimum": 0,
      "maximum": 100
    },
    "health_label": {
      "type": "string",
      "enum": ["经营健康", "中性观察", "压力偏大"]
    },
    "standard_diagnosis": {
      "type": "object",
      "required": ["health_summary", "verdict", "summary_items"],
      "properties": {
        "health_summary": {
          "type": "string",
          "minLength": 20,
          "maxLength": 260
        },
        "verdict": {
          "type": "object",
          "required": ["level", "text"],
          "properties": {
            "level": {
              "type": "string",
              "enum": ["good", "warn", "poor"]
            },
            "text": {
              "type": "string",
              "minLength": 1,
              "maxLength": 100
            }
          },
          "additionalProperties": false
        },
        "summary_items": {
          "type": "array",
          "minItems": 1,
          "maxItems": 6,
          "items": {
            "type": "object",
            "required": ["type", "text"],
            "properties": {
              "type": {
                "type": "string",
                "enum": ["profit", "scale", "risk", "cost", "transform", "outlook"]
              },
              "text": {
                "type": "string",
                "minLength": 1,
                "maxLength": 180
              }
            },
            "additionalProperties": false
          }
        }
      },
      "additionalProperties": false
    },
    "dimensions": {
      "type": "array",
      "minItems": 1,
      "maxItems": 6,
      "items": {
        "type": "object",
        "required": ["id", "icon", "name", "score", "status", "brief", "detail"],
        "properties": {
          "id": {
            "type": "string",
            "enum": ["profit", "scale", "risk", "cost", "transform", "outlook"]
          },
          "icon": {
            "type": "string",
            "minLength": 1,
            "maxLength": 4
          },
          "name": {
            "type": "string",
            "minLength": 1,
            "maxLength": 20
          },
          "score": {
            "type": "number",
            "minimum": 0,
            "maximum": 100
          },
          "status": {
            "type": "string",
            "enum": ["red", "yellow", "green"]
          },
          "brief": {
            "type": "string",
            "minLength": 1,
            "maxLength": 80
          },
          "detail": {
            "type": "object",
            "required": ["title", "content", "bullets"],
            "properties": {
              "title": {
                "type": "string",
                "minLength": 1,
                "maxLength": 40
              },
              "content": {
                "type": "string",
                "minLength": 20,
                "maxLength": 220
              },
              "bullets": {
                "type": "array",
                "maxItems": 3,
                "items": {
                  "type": "string",
                  "minLength": 1,
                  "maxLength": 100
                }
              }
            },
            "additionalProperties": false
          }
        },
        "additionalProperties": false
      }
    },
    "root_cause_chain": {
      "type": "array",
      "maxItems": 8,
      "items": {
        "type": "string",
        "minLength": 1,
        "maxLength": 80
      }
    },
    "offset_factors": {
      "type": "array",
      "maxItems": 5,
      "items": {
        "type": "object",
        "required": ["factor", "text"],
        "properties": {
          "factor": {
            "type": "string",
            "minLength": 1,
            "maxLength": 60
          },
          "text": {
            "type": "string",
            "minLength": 1,
            "maxLength": 160
          }
        },
        "additionalProperties": false
      }
    },
    "emerging_findings": {
      "type": "array",
      "maxItems": 3,
      "items": {
        "type": "object",
        "required": [
          "type",
          "finding",
          "confidence",
          "supporting_facts",
          "why_it_matters",
          "next_validation"
        ],
        "properties": {
          "type": {
            "type": "string",
            "enum": [
              "contradiction",
              "hidden_risk",
              "quality_issue",
              "offset_failure",
              "structural_shift",
              "second_order_effect"
            ]
          },
          "finding": {
            "type": "string",
            "minLength": 20,
            "maxLength": 240
          },
          "confidence": {
            "type": "string",
            "enum": ["high", "medium", "low"]
          },
          "supporting_facts": {
            "type": "array",
            "minItems": 1,
            "maxItems": 5,
            "items": {
              "type": "string",
              "minLength": 1,
              "maxLength": 120
            }
          },
          "why_it_matters": {
            "type": "string",
            "minLength": 1,
            "maxLength": 180
          },
          "next_validation": {
            "type": "array",
            "maxItems": 5,
            "items": {
              "type": "string",
              "minLength": 1,
              "maxLength": 120
            }
          }
        },
        "additionalProperties": false
      }
    },
    "management_questions": {
      "type": "array",
      "maxItems": 5,
      "items": {
        "type": "string",
        "minLength": 1,
        "maxLength": 120
      }
    },
    "watch_items": {
      "type": "array",
      "maxItems": 5,
      "items": {
        "type": "string",
        "minLength": 1,
        "maxLength": 100
      }
    }
  },
  "additionalProperties": false
}
```

---

## 3. 后端 Fact Pack / 数据库 JSON 字段：动态解读

### `DASHBOARD_FACT_PACK` 建议结构

```json
{
  "period": "2026-03-YTD",
  "health_score": 60,
  "health_label": "中性观察",
  "dupont": {
    "net_profit_management": {
      "value": -12.3,
      "display_value": "-12.3亿",
      "unit": "亿",
      "yoy": -168,
      "display_yoy": "-168%",
      "budget_gap": -1.2,
      "display_budget_gap": "-1.2亿",
      "status": "red"
    },
    "anr": {
      "value": 1900,
      "display_value": "1900亿",
      "unit": "亿",
      "yoy": -8,
      "display_yoy": "-8%",
      "status": "yellow"
    },
    "roa": {
      "value": 4.2,
      "display_value": "4.2%",
      "unit": "%",
      "status": "yellow"
    },
    "pricing_rate": {
      "value": 18.2,
      "display_value": "18.2%",
      "status": "green"
    },
    "credit_loss_rate": {
      "value": 7.8,
      "display_value": "7.8%",
      "budget": 6.7,
      "display_budget": "6.7%",
      "redline": 8.0,
      "display_redline": "8.0%",
      "status": "red"
    },
    "funding_cost_rate": {
      "value": 2.05,
      "display_value": "2.05%",
      "yoy_delta": -0.33,
      "display_yoy_delta": "-0.33pct",
      "status": "green"
    },
    "opex_rate": {
      "value": 1.3,
      "display_value": "1.3%",
      "status": "green"
    }
  },
  "dimension_scores": [
    {
      "id": "profit",
      "name": "利润健康度",
      "score": 38,
      "status": "red",
      "evidence": [
        "管理口径净利润-12.3亿",
        "同比-168%",
        "预算差-1.2亿"
      ],
      "conclusion_hint": "利润端仍明显承压",
      "watch_items": [
        "管理口径净利润预算差",
        "贷款业务净利润",
        "ROA变化"
      ]
    },
    {
      "id": "risk",
      "name": "风险信号",
      "score": 22,
      "status": "red",
      "evidence": [
        "信贷损失率7.8%",
        "距离8.0%红线仅0.2pct",
        "易贷24Q1 Vintage早逾恶化"
      ],
      "conclusion_hint": "风险成本是主要拖累",
      "watch_items": [
        "信贷损失率",
        "C-M3迁徙率",
        "Vintage早逾率"
      ]
    }
  ],
  "root_causes": [
    {
      "rank": 1,
      "factor": "信贷损失率上行",
      "impact": -6.7,
      "display_impact": "-6.7亿",
      "unit": "亿",
      "path": [
        "ROA修复受阻",
        "风险成本上行",
        "易贷",
        "2024Q1 Vintage",
        "早逾恶化"
      ]
    },
    {
      "rank": 2,
      "factor": "ANR下降",
      "impact": -3.2,
      "display_impact": "-3.2亿",
      "unit": "亿",
      "path": [
        "贷款规模收缩",
        "易贷压降",
        "新增放款不足"
      ]
    }
  ],
  "offset_factors": [
    {
      "factor": "资金成本下降",
      "impact": 1.4,
      "display_impact": "+1.4亿",
      "evidence": "资金成本同比下降0.33pct"
    },
    {
      "factor": "运营成本改善",
      "impact": 0.9,
      "display_impact": "+0.9亿",
      "evidence": "运营成本处于绿灯状态"
    }
  ],
  "alerts": [
    {
      "level": "red",
      "metric": "信贷损失率",
      "display_value": "7.8%",
      "reason": "距离8.0%红线仅0.2pct"
    },
    {
      "level": "red",
      "metric": "易贷dpd5+",
      "display_value": "8.2%",
      "reason": "易贷逾期处于红灯"
    }
  ],
  "cross_signals": [
    {
      "type": "contradiction",
      "signal": "规模边际改善但风险仍处红灯",
      "supporting_facts": [
        "新增放款边际改善",
        "信贷损失率处于红灯"
      ],
      "analysis_hint": "需要验证规模修复质量"
    },
    {
      "type": "offset_failure",
      "signal": "资金成本改善未能抵消风险成本压力",
      "supporting_facts": [
        "资金成本同比下降0.33pct",
        "信贷损失率7.8%"
      ],
      "analysis_hint": "风险成本仍是利润修复主约束"
    }
  ],
  "rule_result": {
    "overall_status": "中性观察",
    "main_pressure": "信贷损失率上行",
    "main_offset": "资金成本下降和运营成本改善",
    "conclusion_hint": "风险成本压制利润修复，但成本端形成部分对冲",
    "must_mention": [
      "信贷损失率7.8%",
      "资金成本同比下降0.33pct",
      "ANR同比下降8%"
    ],
    "do_not_mention": [
      "股票走势",
      "外部行业均值",
      "没有输入依据的精确预测"
    ],
    "allowed_emerging_finding_types": [
      "contradiction",
      "hidden_risk",
      "quality_issue",
      "offset_failure",
      "structural_shift",
      "second_order_effect"
    ]
  }
}
```

---

# 四、场景三：经营计算器情景解读

## 1. Prompt：`scenario_insight_v2.txt`

```text
你是陆控经营计算器的“情景解释生成器”。

你的任务：
基于输入 JSON SCENARIO_FACT_PACK，解释当前经营情景下的利润结果、关键变量、风险边界和潜在隐藏风险。

输出分为两层：

1. standard_explanation：
标准情景解释，稳定、可审计。

2. scenario_exploration：
探索性解释，用于识别最脆弱假设、隐藏风险和上行惊喜。

你不是聊天助手。
你不是计算器。
你不得重新计算 ROA、净利润、收入、净利率、敏感性影响。
你只能解释输入中已经给出的计算结果。

====================
一、输入说明
====================

输入 JSON 可能包含：

period：
数据期间。

scenario：
bear / base / bull / custom。

scenario_label：
情景名称。

inputs：
用户当前参数：
- anr
- consumer_finance_growth
- pricing_rate
- credit_loss_rate
- npl_rate
- funding_cost_rate
- sales_cost_rate
- opex_rate
- tax_other_rate
- non_loan_profit

computed：
系统已经算好的结果：
- total_cost_rate
- roa
- net_profit
- revenue
- margin
- profit_change_vs_last_year

benchmark：
历史或预算基准。

sensitivity：
系统已经算好的敏感性：
- credit_loss_0_5pct_impact
- pricing_0_5pct_impact
- anr_100bn_impact
- funding_cost_0_5pct_impact

rule_result：
后端规则层判断：
- scenario_quality
- key_variable
- risk_boundary_hint
- upside_hint
- fragile_assumption_hint
- hidden_risk_hint
- must_mention
- do_not_mention

====================
二、标准解释规则
====================

standard_explanation 必须说明：

1. 当前情景质量
优先使用 rule_result.scenario_quality。

2. 利润结果
必须引用 computed.net_profit 或 computed.roa。

3. 利润驱动
说明利润主要来自规模、利差、成本改善还是风险成本假设。

4. 关键变量
优先使用 rule_result.key_variable。
如果没有，则使用 sensitivity 中影响最大的变量。

5. 风险边界
优先使用 rule_result.risk_boundary_hint。

====================
三、探索性解释规则
====================

scenario_exploration 可识别：

1. fragile_assumption
当前情景最脆弱的假设。

2. hidden_risk
参数组合下的隐藏风险。

3. upside_surprise
可能带来上行超预期的条件。

4. stress_point
一旦恶化会显著改变结果的变量。

要求：
- 每个判断必须引用输入中的参数、结果或敏感性。
- 不得新增数字。
- 不得把假设写成确定事实。
- 必须标注 confidence。
- 必须给 validation_needed。

====================
四、硬性约束
====================

1. 不得重新计算任何指标。
2. 不得输出输入中不存在的数字。
3. 不得编造预算、行业均值、预测值。
4. 不得输出投资建议。
5. 不得说“模型预测”，只能说“当前参数情景下”。
6. 不得使用空泛表达。
7. 如果 computed 缺少 roa 或 net_profit，必须输出“计算结果不足，无法判断”。
8. 输出必须是合法 JSON。
9. 不要输出 Markdown。
10. 不要输出 JSON 以外的内容。

====================
五、输出 JSON 格式
====================

{
  "scenario": "输入scenario",
  "scenario_label": "输入scenario_label",
  "standard_explanation": {
    "judgement": "一句话判断当前情景质量，必须引用ROA或净利润",
    "profit_driver": "说明利润结果主要由什么驱动",
    "key_variable": "最关键变量",
    "risk_boundary": "最大风险边界",
    "upside_condition": "上行情形需要满足的条件",
    "sensitivity_comment": "必须引用sensitivity中至少一项",
    "management_commentary": "120到220个中文字符"
  },
  "scenario_exploration": {
    "fragile_assumption": {
      "insight": "当前情景最脆弱的假设",
      "confidence": "high | medium | low",
      "supporting_facts": [
        "必须来自输入"
      ],
      "validation_needed": [
        "需要进一步验证的数据"
      ]
    },
    "hidden_risk": {
      "insight": "隐藏风险",
      "confidence": "high | medium | low",
      "supporting_facts": [
        "必须来自输入"
      ],
      "validation_needed": [
        "需要进一步验证的数据"
      ]
    },
    "upside_surprise": {
      "insight": "上行惊喜条件",
      "confidence": "high | medium | low",
      "supporting_facts": [
        "必须来自输入"
      ],
      "validation_needed": [
        "需要进一步验证的数据"
      ]
    }
  },
  "watch_items": [
    "最多5个后续关注变量"
  ]
}

====================
六、示例
====================

{
  "scenario": "base",
  "scenario_label": "基准情景",
  "standard_explanation": {
    "judgement": "当前参数情景下预测ROA为4.25%，预测净利润为80.8亿，利润结果对信贷损失率假设高度敏感。",
    "profit_driver": "利润主要来自定价率18.2%与总成本率13.95%之间的利差空间，ANR为1900亿提供规模放大。",
    "key_variable": "信贷损失率",
    "risk_boundary": "当前信贷损失率为7.8%，若该指标继续上行，将直接压缩ROA和净利润空间。",
    "upside_condition": "若消金贷款增速维持25%，且信贷损失率不再上行，利润修复稳定性会增强。",
    "sensitivity_comment": "敏感性结果显示，信贷损失率每变动0.5pct影响净利润9.5亿。",
    "management_commentary": "当前情景不是单纯规模驱动，而是由定价、风险成本和ANR共同决定。管理层应优先确认信贷损失率能否稳定在7.8%附近，否则预测净利润存在明显下修风险。"
  },
  "scenario_exploration": {
    "fragile_assumption": {
      "insight": "最脆弱假设是信贷损失率维持在7.8%，因为敏感性显示该指标每变动0.5pct影响净利润9.5亿。",
      "confidence": "high",
      "supporting_facts": [
        "信贷损失率7.8%",
        "信贷损失率每变动0.5pct影响净利润9.5亿"
      ],
      "validation_needed": [
        "C-M3迁徙率",
        "Vintage早逾率",
        "清收预算进度"
      ]
    },
    "hidden_risk": {
      "insight": "如果ANR增长来自高风险客群，净利润改善可能被后续风险成本抵消。",
      "confidence": "medium",
      "supporting_facts": [
        "ANR为1900亿",
        "信贷损失率为7.8%"
      ],
      "validation_needed": [
        "新增放款客群结构",
        "新发放批次MOB1-MOB3早逾率"
      ]
    },
    "upside_surprise": {
      "insight": "如果资金成本继续下降且风险成本不再上行，ROA改善可能比单纯规模扩张更有效。",
      "confidence": "medium",
      "supporting_facts": [
        "综合资金成本2.05%",
        "信贷损失率7.8%"
      ],
      "validation_needed": [
        "资金成本后续走势",
        "信贷损失率变化"
      ]
    }
  },
  "watch_items": [
    "信贷损失率",
    "ANR",
    "消金贷款增速",
    "资金成本",
    "新发放批次早逾率"
  ]
}

====================
七、现在请处理以下输入 JSON
====================

{{SCENARIO_FACT_PACK}}
```

---

## 2. Schema：`scenario_insight.schema.json`

```json
{
  "type": "object",
  "required": [
    "scenario",
    "scenario_label",
    "standard_explanation",
    "scenario_exploration",
    "watch_items"
  ],
  "properties": {
    "scenario": {
      "type": "string",
      "minLength": 1
    },
    "scenario_label": {
      "type": "string",
      "minLength": 1,
      "maxLength": 30
    },
    "standard_explanation": {
      "type": "object",
      "required": [
        "judgement",
        "profit_driver",
        "key_variable",
        "risk_boundary",
        "upside_condition",
        "sensitivity_comment",
        "management_commentary"
      ],
      "properties": {
        "judgement": {
          "type": "string",
          "minLength": 20,
          "maxLength": 180
        },
        "profit_driver": {
          "type": "string",
          "minLength": 20,
          "maxLength": 180
        },
        "key_variable": {
          "type": "string",
          "minLength": 1,
          "maxLength": 40
        },
        "risk_boundary": {
          "type": "string",
          "minLength": 20,
          "maxLength": 180
        },
        "upside_condition": {
          "type": "string",
          "minLength": 20,
          "maxLength": 180
        },
        "sensitivity_comment": {
          "type": "string",
          "minLength": 20,
          "maxLength": 180
        },
        "management_commentary": {
          "type": "string",
          "minLength": 60,
          "maxLength": 260
        }
      },
      "additionalProperties": false
    },
    "scenario_exploration": {
      "type": "object",
      "required": [
        "fragile_assumption",
        "hidden_risk",
        "upside_surprise"
      ],
      "properties": {
        "fragile_assumption": {
          "$ref": "#/$defs/exploration_item"
        },
        "hidden_risk": {
          "$ref": "#/$defs/exploration_item"
        },
        "upside_surprise": {
          "$ref": "#/$defs/exploration_item"
        }
      },
      "additionalProperties": false
    },
    "watch_items": {
      "type": "array",
      "maxItems": 5,
      "items": {
        "type": "string",
        "minLength": 1,
        "maxLength": 80
      }
    }
  },
  "$defs": {
    "exploration_item": {
      "type": "object",
      "required": [
        "insight",
        "confidence",
        "supporting_facts",
        "validation_needed"
      ],
      "properties": {
        "insight": {
          "type": "string",
          "minLength": 20,
          "maxLength": 220
        },
        "confidence": {
          "type": "string",
          "enum": ["high", "medium", "low"]
        },
        "supporting_facts": {
          "type": "array",
          "minItems": 1,
          "maxItems": 5,
          "items": {
            "type": "string",
            "minLength": 1,
            "maxLength": 120
          }
        },
        "validation_needed": {
          "type": "array",
          "maxItems": 5,
          "items": {
            "type": "string",
            "minLength": 1,
            "maxLength": 120
          }
        }
      },
      "additionalProperties": false
    }
  },
  "additionalProperties": false
}
```

---

## 3. 后端 Fact Pack / 数据库 JSON 字段：经营计算器

### `SCENARIO_FACT_PACK` 建议结构

```json
{
  "period": "2026-03-YTD",
  "scenario": "base",
  "scenario_label": "基准情景",
  "inputs": {
    "anr": {
      "name": "平均贷款余额ANR",
      "value": 1900,
      "display_value": "1900亿",
      "unit": "亿"
    },
    "consumer_finance_growth": {
      "name": "消金贷款增速",
      "value": 25,
      "display_value": "25%",
      "unit": "%"
    },
    "pricing_rate": {
      "name": "综合定价率",
      "value": 18.2,
      "display_value": "18.2%",
      "unit": "%"
    },
    "credit_loss_rate": {
      "name": "信贷损失率",
      "value": 7.8,
      "display_value": "7.8%",
      "unit": "%"
    },
    "npl_rate": {
      "name": "不良贷款率",
      "value": 1.2,
      "display_value": "1.2%",
      "unit": "%"
    },
    "funding_cost_rate": {
      "name": "综合资金成本",
      "value": 2.05,
      "display_value": "2.05%",
      "unit": "%"
    },
    "sales_cost_rate": {
      "name": "销售成本率",
      "value": 1.6,
      "display_value": "1.6%",
      "unit": "%"
    },
    "opex_rate": {
      "name": "运营成本率",
      "value": 1.3,
      "display_value": "1.3%",
      "unit": "%"
    },
    "tax_other_rate": {
      "name": "税及其他",
      "value": 1.2,
      "display_value": "1.2%",
      "unit": "%"
    },
    "non_loan_profit": {
      "name": "非贷款业务净利润",
      "value": -5,
      "display_value": "-5亿",
      "unit": "亿"
    }
  },
  "computed": {
    "total_cost_rate": {
      "name": "总成本率",
      "value": 13.95,
      "display_value": "13.95%",
      "unit": "%"
    },
    "roa": {
      "name": "预测ROA",
      "value": 4.25,
      "display_value": "4.25%",
      "unit": "%"
    },
    "net_profit": {
      "name": "预测净利润",
      "value": 80.8,
      "display_value": "80.8亿",
      "unit": "亿"
    },
    "revenue": {
      "name": "预测收入",
      "value": 345.8,
      "display_value": "345.8亿",
      "unit": "亿"
    },
    "margin": {
      "name": "净利率",
      "value": 23.4,
      "display_value": "23.4%",
      "unit": "%"
    },
    "profit_change_vs_last_year": {
      "name": "较上年变化",
      "value": 97.8,
      "display_value": "+97.8亿",
      "unit": "亿"
    }
  },
  "benchmark": {
    "last_year_net_profit": {
      "name": "上年净利润",
      "value": -17,
      "display_value": "-17亿",
      "unit": "亿"
    },
    "current_actual_credit_loss_rate": {
      "name": "当前实际信贷损失率",
      "value": 7.8,
      "display_value": "7.8%",
      "unit": "%"
    },
    "current_actual_pricing_rate": {
      "name": "当前实际定价率",
      "value": 18.2,
      "display_value": "18.2%",
      "unit": "%"
    }
  },
  "sensitivity": {
    "credit_loss_0_5pct_impact": {
      "name": "信贷损失率每变动0.5pct影响",
      "value": 9.5,
      "display_value": "9.5亿",
      "unit": "亿"
    },
    "pricing_0_5pct_impact": {
      "name": "定价率每变动0.5pct影响",
      "value": 9.5,
      "display_value": "9.5亿",
      "unit": "亿"
    },
    "anr_100bn_impact": {
      "name": "ANR每变动100亿影响",
      "value": 4.25,
      "display_value": "4.25亿",
      "unit": "亿"
    },
    "funding_cost_0_5pct_impact": {
      "name": "资金成本每变动0.5pct影响",
      "value": 9.5,
      "display_value": "9.5亿",
      "unit": "亿"
    }
  },
  "rule_result": {
    "scenario_quality": "盈利但对风险成本敏感",
    "key_variable": "信贷损失率",
    "risk_boundary_hint": "信贷损失率继续上行将压缩ROA和净利润空间",
    "upside_hint": "风险成本稳定且ANR不下滑时利润修复更稳",
    "fragile_assumption_hint": "信贷损失率维持在7.8%是该情景最脆弱假设",
    "hidden_risk_hint": "如果ANR增长来自高风险客群，净利润改善可能被后续风险成本抵消",
    "must_mention": [
      "预测ROA 4.25%",
      "预测净利润80.8亿",
      "信贷损失率每变动0.5pct影响净利润9.5亿"
    ],
    "do_not_mention": [
      "股票走势",
      "行业均值",
      "没有输入依据的精确预测"
    ]
  }
}
```

---

# 五、`dashboard_metrics_mock.json` 建议更新字段

这个文件是模拟数据库。建议不要只放扁平指标，而是支持三类数据：

```json
{
  "period": "2026-03-YTD",
  "metric_version": "v1",
  "core_metrics": [],
  "segment_metrics": [],
  "scenario_defaults": [],
  "rules": {}
}
```

## 示例结构

```json
{
  "period": "2026-03-YTD",
  "metric_version": "mock_v2",
  "core_metrics": [
    {
      "metric_code": "net_profit_management",
      "metric_name": "管理口径净利润",
      "value": -12.3,
      "display_value": "-12.3亿",
      "unit": "亿",
      "yoy": -168,
      "display_yoy": "-168%",
      "budget": -11.1,
      "budget_gap": -1.2,
      "display_budget_gap": "-1.2亿",
      "status": "red",
      "direction": "higher_is_better",
      "module": "profit"
    },
    {
      "metric_code": "credit_loss_rate",
      "metric_name": "信贷损失率",
      "value": 7.8,
      "display_value": "7.8%",
      "unit": "%",
      "budget": 6.7,
      "budget_gap": 1.1,
      "display_budget_gap": "+1.1pct",
      "redline": 8.0,
      "distance_to_redline": 0.2,
      "display_distance_to_redline": "0.2pct",
      "status": "red",
      "direction": "higher_is_worse",
      "module": "risk"
    }
  ],
  "segment_metrics": [
    {
      "metric_code": "dpd5_rate",
      "metric_name": "dpd5+逾期率",
      "dimension": "product_type",
      "segment": "易贷",
      "value": 8.2,
      "display_value": "8.2%",
      "unit": "%",
      "status": "red",
      "module": "risk"
    },
    {
      "metric_code": "vintage_dpd",
      "metric_name": "Vintage早逾率",
      "dimension": "vintage",
      "segment": "2024Q1",
      "mob": "MOB3",
      "value": 5.6,
      "display_value": "5.6%",
      "unit": "%",
      "status": "red",
      "module": "risk"
    }
  ],
  "scenario_defaults": [
    {
      "scenario": "base",
      "scenario_label": "基准情景",
      "anr": 1900,
      "consumer_finance_growth": 25,
      "pricing_rate": 18.2,
      "credit_loss_rate": 7.8,
      "npl_rate": 1.2,
      "funding_cost_rate": 2.05,
      "sales_cost_rate": 1.6,
      "opex_rate": 1.3,
      "tax_other_rate": 1.2,
      "non_loan_profit": -5
    }
  ],
  "rules": {
    "health_score": 60,
    "health_label": "中性观察",
    "dimension_scores": [
      {
        "id": "profit",
        "name": "利润健康度",
        "score": 38,
        "status": "red",
        "evidence": [
          "管理口径净利润-12.3亿",
          "同比-168%",
          "预算差-1.2亿"
        ]
      },
      {
        "id": "risk",
        "name": "风险信号",
        "score": 22,
        "status": "red",
        "evidence": [
          "信贷损失率7.8%",
          "距离8.0%红线仅0.2pct"
        ]
      }
    ],
    "root_causes": [
      {
        "rank": 1,
        "factor": "信贷损失率上行",
        "impact": -6.7,
        "display_impact": "-6.7亿",
        "path": [
          "ROA修复受阻",
          "风险成本上行",
          "易贷",
          "2024Q1 Vintage",
          "早逾恶化"
        ]
      }
    ],
    "offset_factors": [
      {
        "factor": "资金成本下降",
        "impact": 1.4,
        "display_impact": "+1.4亿",
        "evidence": "资金成本同比下降0.33pct"
      }
    ],
    "cross_signals": [
      {
        "type": "contradiction",
        "signal": "规模边际改善但风险仍处红灯",
        "supporting_facts": [
          "新增放款边际改善",
          "信贷损失率处于红灯"
        ],
        "analysis_hint": "需要验证规模修复质量"
      }
    ]
  }
}
```

---

# 六、后端数据库表建议同步更新

如果后续从 JSON 迁移数据库，建议至少有这些表。

## 1. `dashboard_metric`

```text
id
period
metric_code
metric_name
value
display_value
unit
yoy
display_yoy
mom
display_mom
budget
budget_gap
display_budget_gap
redline
yellowline
distance_to_redline
display_distance_to_redline
status
direction
module
dimension
segment
product_type
channel_type
vintage
mob
customer_type
source_sheet
batch_id
created_at
```

## 2. `card_config`

```text
card_id
section_id
insight_element_id
title
level
view_type
prompt_template
schema_name
parent_metric
management_question
dupont_path_json
analysis_focus_json
metric_codes_json
segment_dimensions_json
allowed_exploration_types_json
enabled
display_order
```

## 3. `insight_result`

```text
id
period
insight_type
card_id
prompt_version
schema_name
status
title
standard_insight
standard_diagnosis_json
drivers_json
watch_items_json
evidence_json
exploratory_insights_json
emerging_findings_json
scenario_exploration_json
deep_dive_questions_json
management_questions_json
raw_fact_pack_json
raw_llm_output_json
validated
validation_error
created_at
```

## 4. `scenario_default_param`

```text
id
period
scenario
scenario_label
anr
consumer_finance_growth
pricing_rate
credit_loss_rate
npl_rate
funding_cost_rate
sales_cost_rate
opex_rate
tax_other_rate
non_loan_profit
created_at
```

---

# 七、前端字段映射也要同步更新

## 1. 卡片洞察展示

主区域展示：

```text
standard_insight
```

折叠区展示：

```text
exploratory_insights
deep_dive_questions
evidence
```

旧字段：

```text
insight
```

建议兼容：

```javascript
const mainText = data.standard_insight || data.insight || '暂无洞察';
```

---

## 2. AI 动态解读展示

顶部摘要展示：

```text
standard_diagnosis.health_summary
standard_diagnosis.summary_items
```

六维卡片展示：

```text
dimensions
```

新增模块展示：

```text
emerging_findings
management_questions
```

---

## 3. 经营计算器展示

主解释展示：

```text
standard_explanation
```

新增折叠区展示：

```text
scenario_exploration.fragile_assumption
scenario_exploration.hidden_risk
scenario_exploration.upside_surprise
```

---

# 八、最终文件更新清单

```text
backend/prompts/card_insight_v2.txt
backend/prompts/dashboard_insight_v2.txt
backend/prompts/scenario_insight_v2.txt

backend/schemas/card_insight.schema.json
backend/schemas/dashboard_insight.schema.json
backend/schemas/scenario_insight.schema.json

backend/data/card_config.json
backend/data/dashboard_metrics_mock.json
```

关键变化：

```text
card_insight：
insight → standard_insight
新增 exploratory_insights
新增 deep_dive_questions

dashboard_insight：
health_summary → standard_diagnosis.health_summary
新增 emerging_findings
新增 management_questions

scenario_insight：
judgement 等字段放入 standard_explanation
新增 scenario_exploration
```

