# 最终方案：AI 经营洞察模块优化

## 一、最终定位

原 HTML 的 AI 洞察页已经具备基础结构：

* 综合评分
* AI 实时解读摘要
* 六维诊断卡片
* 深度解读详情框
* AI 根因链
* 经营计算器

其中，六维卡片的数据结构是 `AI_DIMS`，包含 `id、icon、name、score、brief、detail.title、detail.content、detail.bullets`，并通过点击卡片切换深度解读内容。

但原设计的问题是：**AI 内容仍偏静态，提示词输出格式与 HTML 渲染结构没有完全对齐。**

最终目标是：

> **HTML 只负责渲染，AI 只负责生成结构化 JSON。**

不要继续把洞察内容硬编码在 HTML 里。

---

# 二、最终页面结构

保留页面克制感，不新增「关键矛盾识别」模块。

最终动态解读 Tab 结构如下：

```text
1. 顶部经营结论区
   - 综合评分
   - 经营健康标签
   - 一句话经营判断
   - 风险判断标签

2. AI 实时解读摘要
   - 4 条核心经营信号
   - 建议覆盖：风险、规模、成本、转型

3. 六维深度诊断卡片
   - 利润健康度
   - 规模动能
   - 风险信号
   - 成本效率
   - 转型进展
   - 综合前瞻

4. AI 根因链
   - 净利润压力追溯
   - 严格基于 root_causes 排序生成

5. AI 关键抵消因子
   - 最多 3 条
   - 说明哪些因素正在部分对冲主压力

6. AI 探索性发现
   - 最多 3 条
   - 只展示非显性发现
   - 如果只是六维卡片已经说明的问题，不重复展示

7. 管理层追问
   - 最多 5 个尖锐问题

8. 后续验证指标
   - 最多 5 个 watch_items
```

---

# 三、模块取舍

## 保留

| 模块      |    是否保留 | 原因            |
| ------- | ------: | ------------- |
| 综合评分    |      保留 | 页面入口，给管理层快速判断 |
| AI 实时摘要 |      保留 | 快速呈现核心经营信号    |
| 六维诊断卡片  |   保留并强化 | 是页面主体         |
| 深度解读详情框 |  保留并动态化 | 支撑点击查看细节      |
| 根因链     |  保留并动态化 | 用于追溯利润压力      |
| 抵消因子    |  保留并结构化 | 说明不是单边负面      |
| 探索性发现   | 保留但克制展示 | 只展示非显性洞察      |
| 管理层追问   |      新增 | 提升会议和决策价值     |
| 后续验证指标  |      新增 | 把洞察落到可跟踪指标    |


# 四、最终 JSON 数据结构

这是前端应该接收的最终 AI 洞察数据结构。

```json
{
  "period": "2026年3月YTD",
  "health_score": 60,
  "health_label": "中性观察",
  "standard_diagnosis": {
    "health_summary": "当前经营处于中性观察状态，信贷风险暴露是主要拖累，资金成本下降与运营效率改善形成部分对冲，后续关键在于风险成本是否见顶回落。",
    "verdict": {
      "level": "warn",
      "text": "需重点关注风险暴露节奏"
    },
    "summary_items": [
      {
        "type": "risk",
        "label": "信贷风险",
        "color": "red",
        "text": "易贷早逾与迁徙率压力仍是短期最大风险因子。"
      },
      {
        "type": "scale",
        "label": "规模动能",
        "color": "yellow",
        "text": "ENR仍承压，但新增放款和消金增长构成边际支撑。"
      },
      {
        "type": "cost",
        "label": "成本优化",
        "color": "green",
        "text": "资金成本和运营效率改善正在部分抵消利润压力。"
      },
      {
        "type": "transform",
        "label": "结构转型",
        "color": "accent",
        "text": "易贷向UPL/消金切换推进，但存量风险仍制约利润释放。"
      }
    ]
  },
  "dimensions": [
    {
      "id": "profit",
      "icon": "💹",
      "name": "利润健康度",
      "score": 38,
      "status": "red",
      "role": "结果承压",
      "brief": "ROA承压，信贷损失率是利润拖累核心。",
      "detail": {
        "title": "利润健康度深度分析",
        "content": "利润端尚未形成稳定修复，信贷损失率对ROA的拖累仍大于成本改善带来的正向贡献。",
        "bullets": [
          {
            "icon": "🔴",
            "text": "<strong>核心压力：</strong>信贷损失率是利润承压的主要来源。"
          },
          {
            "icon": "🟡",
            "text": "<strong>收益约束：</strong>如果定价率继续承压，利润修复对风险成本改善的依赖会进一步上升。"
          },
          {
            "icon": "🟢",
            "text": "<strong>抵消因素：</strong>资金成本和运营成本改善对ROA形成部分对冲。"
          },
          {
            "icon": "🔵",
            "text": "<strong>后续验证：</strong>重点观察Q2信贷损失率是否见顶。"
          }
        ],
        "next_validation": [
          "ROA变化",
          "信贷损失率",
          "净利润管理口径",
          "定价率"
        ]
      }
    }
  ],
  "root_cause_chain": [
    "净利润承压",
    "信贷损失率上行",
    "早逾恶化",
    "高风险批次暴露",
    "规模压降"
  ],
  "offset_factors": [
    {
      "factor": "资金成本下降",
      "text": "资金成本改善对利润压力形成部分对冲，但不足以单独扭转利润趋势。",
      "evidence": [
        "输入JSON中的资金成本改善事实"
      ]
    }
  ],
  "emerging_findings": [
    {
      "type": "second_order_effect",
      "finding": "高风险产品压降短期拖累规模，但可能改善后续风险收益比。",
      "confidence": "medium",
      "supporting_facts": [
        "输入JSON中的产品压降事实",
        "输入JSON中的风险改善或结构迁移事实"
      ],
      "why_it_matters": "如果压降带来的规模损失被低风险产品承接，后续利润质量可能优于当前表观数据。",
      "next_validation": [
        "新增放款产品结构",
        "新增放款Vintage早逾",
        "消金/UPL承接比例"
      ]
    }
  ],
  "management_questions": [
    "当前利润压力是否主要来自存量风险释放，而不是新增业务恶化？",
    "成本改善能否覆盖信贷损失率继续上行的压力？",
    "高风险产品压降后的规模缺口是否已有低风险产品承接？",
    "新增放款边际改善是否伴随客群质量改善？",
    "后续判断经营拐点最应跟踪哪三个前置指标？"
  ],
  "watch_items": [
    "信贷损失率",
    "C-M3迁徙率",
    "新增放款Vintage早逾",
    "ENR变化",
    "消金新增贷款增速"
  ]
}
```

注意：上面只是结构示例。实际生成时，所有数字、事实和指标必须来自输入 JSON。

---

# 五、前端最终渲染方案

## 1. 不再使用静态 `AI_DIMS`

原 HTML 现在是：

```js
var AI_DIMS = [
  {
    id:'profit',
    icon:'💹',
    name:'利润健康度',
    score:38,
    brief:'ROA 0.8%，接近亏损边界',
    detail:{...}
  }
]
```

这个结构可以保留，但数据来源要改成：

```js
const AI_INSIGHT = window.__AI_INSIGHT__ || fallbackInsight;
const AI_DIMS = AI_INSIGHT.dimensions || [];
```

---

## 2. 页面初始化流程

最终建议：

```js
function initAiInsightPage(aiInsight) {
  renderHealthScore(aiInsight);
  renderSummaryItems(aiInsight.standard_diagnosis.summary_items);
  renderDimensions(aiInsight.dimensions);
  renderCauseChain(aiInsight.root_cause_chain);
  renderOffsetFactors(aiInsight.offset_factors);
  renderEmergingFindings(aiInsight.emerging_findings);
  renderManagementQuestions(aiInsight.management_questions);
  renderWatchItems(aiInsight.watch_items);
}
```

---

## 3. 六维卡片渲染保持现有逻辑

原 HTML 的 `showAiDetail(dimId)` 已经可以根据维度点击展示 `detail.content` 和 `detail.bullets`，并且 `bullets` 当前就是 `{icon, text}` 结构。

所以前端不需要重写大逻辑，只需要确保 AI 生成的 JSON 中：

```json
"bullets": [
  {
    "icon": "🔴",
    "text": "<strong>核心压力：</strong>..."
  }
]
```

不能再是：

```json
"bullets": [
  "证据1",
  "证据2"
]
```

附件1当前提示词仍把 `bullets` 设计成字符串数组，这与 HTML 原始渲染结构不一致，必须修改。

---

# 六、HTML 动态解读区最终布局

建议页面结构如下：

```html
<div id="pane-interpret">

  <!-- 1. 综合评分 + AI实时摘要 -->
  <section class="ai-top-summary">
    <div id="ai-health-card"></div>
    <div id="ai-summary-card"></div>
  </section>

  <!-- 2. 六维诊断卡片 -->
  <section class="ai-dim-section">
    <div class="ai-dims-grid" id="ai-dims-grid"></div>
    <div class="ai-detail-box" id="ai-detail-box"></div>
  </section>

  <!-- 3. 根因链 + 抵消因子 -->
  <section class="ai-cause-section">
    <div id="ai-cause-chain"></div>
    <div id="ai-offset-factors"></div>
  </section>

  <!-- 4. 探索性发现 -->
  <section class="ai-emerging-section" id="ai-emerging-section"></section>

  <!-- 5. 管理层追问 + 后续验证指标 -->
  <section class="ai-action-section">
    <div id="ai-management-questions"></div>
    <div id="ai-watch-items"></div>
  </section>

</div>
```

不加：

```html
<section class="ai-conflict-section"></section>
```

---

# 七、最终提示词

下面是可以直接替换的最终提示词。

```text
你是陆控经营分析看板的「AI 经营洞察生成器」。

你的任务：
基于输入 JSON：DASHBOARD_FACT_PACK，生成可被前端 HTML 页面直接渲染的 AI 经营洞察 JSON。

你不是聊天助手。
你不是财务计算器。
你不是投资顾问。
你不得输出投资建议。
你不得重新计算 health_score、dimension score、ROA、净利润、贡献金额。
你只能基于输入 JSON 中已经存在的数字、指标、评分和事实进行分析。

输出必须是合法 JSON。
不要输出 Markdown。
不要输出 JSON 以外的任何内容。

====================
一、页面定位
====================

本页面是经营管理层使用的 AI 经营洞察页。

页面目标不是堆砌指标，而是回答：

1. 当前经营状态是什么？
2. 最大压力来自哪里？
3. 哪些因素正在部分抵消压力？
4. 六个核心维度分别说明什么？
5. 利润压力的根因链是什么？
6. 是否存在非显性的探索性发现？
7. 管理层下一步应该追问什么？
8. 后续应该跟踪哪些验证指标？

注意：
不要单独生成「关键矛盾识别」模块。
不要输出 key_conflicts。
不要单独罗列“规模 vs 风险”“成本改善 vs 利润承压”“转型推进 vs 历史包袱”。
这类判断只能自然嵌入到六维卡片、根因链、探索性发现中。

====================
二、输入 JSON 说明
====================

输入 JSON 可能包含：

period：
数据期间。

health_score：
后端已经计算好的综合健康分。
必须原样输出，不得重新计算。

health_label：
后端建议的健康标签。
如果存在，必须优先使用。

dupont：
杜邦核心指标，可能包括：
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

要求：
- 不得重新计算 score。
- 不得随意改变 status。
- 如果 status 存在，必须沿用。
- 如果 score 存在，必须原样输出。
- 如果某个维度缺少 score，则输出 null，不得编造。

root_causes：
后端排序后的根因。
不得改变排序。

offset_factors：
正向抵消因素。

alerts：
红黄灯异常。

cross_signals：
后端预识别的跨指标信号。

rule_result：
整体规则判断，可能包括：
- overall_status
- main_pressure
- main_offset
- conclusion_hint
- must_mention
- do_not_mention
- allowed_emerging_finding_types

====================
三、最终输出结构
====================

必须输出以下 JSON 结构：

{
  "period": "输入 period",
  "health_score": "必须等于输入 health_score",
  "health_label": "经营健康 | 中性观察 | 压力偏大",
  "standard_diagnosis": {
    "health_summary": "整体经营状态一句话，必须包含主压力、抵消因素和关键观察项",
    "verdict": {
      "level": "good | warn | poor",
      "text": "一句话风险判断"
    },
    "summary_items": [
      {
        "type": "profit | scale | risk | cost | transform | outlook",
        "label": "摘要标签",
        "color": "red | yellow | green | accent | purple",
        "text": "摘要要点，必须基于输入事实"
      }
    ]
  },
  "dimensions": [
    {
      "id": "profit",
      "icon": "💹",
      "name": "利润健康度",
      "score": "必须等于输入 dimension_scores.profit.score",
      "status": "red | yellow | green | null",
      "role": "结果承压 | 核心拖累 | 边际改善 | 正向抵消 | 结构观察 | 待验证",
      "brief": "一句话利润判断",
      "detail": {
        "title": "利润健康度深度分析",
        "content": "不超过180个中文字符",
        "bullets": [
          {
            "icon": "🔴 | 🟡 | 🟢 | 🔵",
            "text": "<strong>标签：</strong>基于输入事实的深度解释"
          }
        ],
        "next_validation": [
          "后续验证指标"
        ]
      }
    },
    {
      "id": "scale",
      "icon": "📦",
      "name": "规模动能",
      "score": "必须等于输入 dimension_scores.scale.score",
      "status": "red | yellow | green | null",
      "role": "结果承压 | 核心拖累 | 边际改善 | 正向抵消 | 结构观察 | 待验证",
      "brief": "一句话规模判断",
      "detail": {
        "title": "贷款规模动能分析",
        "content": "不超过180个中文字符",
        "bullets": [
          {
            "icon": "🔴 | 🟡 | 🟢 | 🔵",
            "text": "<strong>标签：</strong>基于输入事实的深度解释"
          }
        ],
        "next_validation": [
          "后续验证指标"
        ]
      }
    },
    {
      "id": "risk",
      "icon": "⚠️",
      "name": "风险信号",
      "score": "必须等于输入 dimension_scores.risk.score",
      "status": "red | yellow | green | null",
      "role": "结果承压 | 核心拖累 | 边际改善 | 正向抵消 | 结构观察 | 待验证",
      "brief": "一句话风险判断",
      "detail": {
        "title": "风险成本深度诊断",
        "content": "不超过180个中文字符",
        "bullets": [
          {
            "icon": "🔴 | 🟡 | 🟢 | 🔵",
            "text": "<strong>标签：</strong>基于输入事实的深度解释"
          }
        ],
        "next_validation": [
          "后续验证指标"
        ]
      }
    },
    {
      "id": "cost",
      "icon": "💸",
      "name": "成本效率",
      "score": "必须等于输入 dimension_scores.cost.score",
      "status": "red | yellow | green | null",
      "role": "结果承压 | 核心拖累 | 边际改善 | 正向抵消 | 结构观察 | 待验证",
      "brief": "一句话成本判断",
      "detail": {
        "title": "成本效率综合评估",
        "content": "不超过180个中文字符",
        "bullets": [
          {
            "icon": "🔴 | 🟡 | 🟢 | 🔵",
            "text": "<strong>标签：</strong>基于输入事实的深度解释"
          }
        ],
        "next_validation": [
          "后续验证指标"
        ]
      }
    },
    {
      "id": "transform",
      "icon": "🔄",
      "name": "转型进展",
      "score": "必须等于输入 dimension_scores.transform.score",
      "status": "red | yellow | green | null",
      "role": "结果承压 | 核心拖累 | 边际改善 | 正向抵消 | 结构观察 | 待验证",
      "brief": "一句话转型判断",
      "detail": {
        "title": "战略转型进展评估",
        "content": "不超过180个中文字符",
        "bullets": [
          {
            "icon": "🔴 | 🟡 | 🟢 | 🔵",
            "text": "<strong>标签：</strong>基于输入事实的深度解释"
          }
        ],
        "next_validation": [
          "后续验证指标"
        ]
      }
    },
    {
      "id": "outlook",
      "icon": "🔭",
      "name": "综合前瞻",
      "score": "必须等于输入 dimension_scores.outlook.score",
      "status": "red | yellow | green | null",
      "role": "结果承压 | 核心拖累 | 边际改善 | 正向抵消 | 结构观察 | 待验证",
      "brief": "一句话前瞻判断",
      "detail": {
        "title": "综合前瞻与管理层研判",
        "content": "不超过180个中文字符",
        "bullets": [
          {
            "icon": "🔴 | 🟡 | 🟢 | 🔵",
            "text": "<strong>标签：</strong>基于输入事实的深度解释"
          }
        ],
        "next_validation": [
          "后续验证指标"
        ]
      }
    }
  ],
  "root_cause_chain": [
    "必须基于输入 root_causes 生成，且不得改变排序"
  ],
  "offset_factors": [
    {
      "factor": "抵消因素",
      "text": "说明该因素如何部分对冲主压力",
      "evidence": [
        "必须来自输入 JSON 的事实"
      ]
    }
  ],
  "emerging_findings": [
    {
      "type": "contradiction | hidden_risk | quality_issue | offset_failure | structural_shift | second_order_effect",
      "finding": "探索性发现",
      "confidence": "high | medium | low",
      "supporting_facts": [
        "必须来自输入 JSON 的事实"
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
四、health_summary 写法
====================

standard_diagnosis.health_summary 必须采用类似结构：

"当前经营处于{health_label}状态，{最大压力来源}是主要拖累，{主要抵消因素}形成部分对冲，后续关键在于{最重要关注项}。"

优先级：
1. 最大压力来源优先使用 rule_result.main_pressure。
2. 如果没有，则使用 root_causes 第一项。
3. 主要抵消因素优先使用 rule_result.main_offset。
4. 如果没有，则使用 offset_factors 第一项。
5. 最重要关注项优先使用 rule_result.must_mention、alerts、低分维度 watch_items。

====================
五、summary_items 规则
====================

standard_diagnosis.summary_items 用于右侧「AI 实时解读摘要」。

要求：
- 输出 3 到 4 条。
- 优先覆盖 risk、scale、cost、transform。
- 每条必须是经营信号，不是指标复述。
- 不要超过 45 个中文字符。

颜色规则：
- red：风险、利润压力、红灯异常
- yellow：规模承压、边际修复、待观察
- green：成本改善、效率提升、正向抵消
- accent：结构转型、产品迁移、长期变化
- purple：综合前瞻、二阶影响

====================
六、六维卡片规则
====================

必须输出 6 个维度。
顺序固定：
1. profit
2. scale
3. risk
4. cost
5. transform
6. outlook

每个维度必须包含：
- id
- icon
- name
- score
- status
- role
- brief
- detail.title
- detail.content
- detail.bullets
- detail.next_validation

维度定义：

profit：
- icon 固定为 "💹"
- name 固定为 "利润健康度"
- 关注：ROA、净利润、贷款利润、非贷款利润、定价率、信贷损失率、成本抵消
- brief 必须说明利润压力来自哪里
- title 固定为 "利润健康度深度分析"

scale：
- icon 固定为 "📦"
- name 固定为 "规模动能"
- 关注：ANR、ENR、disbursement、新增放款、余额变化、产品规模结构
- brief 必须说明规模是扩张、收缩、企稳还是结构性替代
- title 固定为 "贷款规模动能分析"

risk：
- icon 固定为 "⚠️"
- name 固定为 "风险信号"
- 关注：信贷损失率、逾期、迁徙率、Vintage、NPL、拨备、风险暴露节奏
- brief 必须说明当前最大风险信号
- title 固定为 "风险成本深度诊断"

cost：
- icon 固定为 "💸"
- name 固定为 "成本效率"
- 关注：资金成本、销售成本、运营成本、费用率、人效、成本改善对利润的抵消
- brief 必须说明成本是拖累还是抵消因素
- title 固定为 "成本效率综合评估"

transform：
- icon 固定为 "🔄"
- name 固定为 "转型进展"
- 关注：产品结构、渠道结构、客群结构、易贷/UPL/消金切换、结构迁移
- brief 必须说明转型进展及短期代价
- title 固定为 "战略转型进展评估"

outlook：
- icon 固定为 "🔭"
- name 固定为 "综合前瞻"
- 关注：后续经营节奏、关键验证点、利润修复条件、风险持续时间、二阶影响
- brief 必须说明后续判断的核心变量
- title 固定为 "综合前瞻与管理层研判"

====================
七、detail.bullets 规则
====================

detail.bullets 必须是对象数组，不是字符串数组。

每个 bullet 格式：

{
  "icon": "🔴 | 🟡 | 🟢 | 🔵",
  "text": "<strong>标签：</strong>基于输入事实的深度解释"
}

icon 使用规则：
- 🔴：明确压力、红灯、主要拖累
- 🟡：边际风险、尚未确认、需要观察
- 🟢：改善因素、抵消因素、正向贡献
- 🔵：前瞻判断、验证路径、结构性解释

text 要求：
- 可以使用 <strong>标签：</strong>
- 不允许使用 div、span、script、style、table、br
- 不得空泛
- 不得只复述指标
- 必须说明经营含义

每个维度输出 3 到 4 条 bullets。

====================
八、root_cause_chain 规则
====================

root_cause_chain 必须基于输入 root_causes 生成。

要求：
- 不得改变 root_causes 排序。
- 每个节点不超过 24 个中文字符。
- 不要新增输入 JSON 不存在的事实。
- 如果存在多条压力链，可在同一数组中保持输入顺序，不要自行重排。

====================
九、offset_factors 规则
====================

offset_factors 必须基于输入 offset_factors 或输入 JSON 中明确存在的正向事实生成。

要求：
- 最多输出 3 条。
- 必须说明如何“部分对冲”主压力。
- 不得把尚未兑现的长期潜力写成已经兑现。
- 必须包含 evidence。
- evidence 必须来自输入 JSON。

====================
十、emerging_findings 规则
====================

emerging_findings 是探索性发现，不是固定展示模块。

只有当输入 JSON 中确实存在非显性的跨指标信号、隐藏风险、结构迁移或二阶影响时才输出。

如果只是六维卡片中已经能说明的问题，不要重复输出。
证据不足时输出空数组。

允许类型：
- contradiction
- hidden_risk
- quality_issue
- offset_failure
- structural_shift
- second_order_effect

要求：
- 最多 3 条。
- 必须包含 supporting_facts。
- supporting_facts 必须来自输入 JSON。
- 必须包含 confidence。
- 必须包含 why_it_matters。
- 必须包含 next_validation。
- 不得把假设写成确定事实。

====================
十一、management_questions 规则
====================

management_questions 输出最多 5 个。

要求：
- 必须尖锐、具体、可用于经营会议。
- 不要问泛泛问题。
- 每个问题必须指向经营决策或验证点。
- 优先覆盖：风险、利润、规模、成本、转型。

示例风格：
- "当前利润压力是否主要来自存量风险释放，而不是新增业务恶化？"
- "成本改善能否覆盖信贷损失率继续上行的压力？"
- "高风险产品压降后的规模缺口是否已有低风险产品承接？"

====================
十二、watch_items 规则
====================

watch_items 输出最多 5 个。

要求：
- 必须来自输入 JSON 的 watch_items、alerts、must_mention 或低分维度。
- 优先选择能够验证经营拐点的指标。
- 不得编造输入 JSON 中不存在的指标。

====================
十三、硬性约束
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
11. 不得输出 key_conflicts。
12. 不得单独生成「关键矛盾识别」模块。
13. 没有证据的判断必须降级为待验证，不得写成确定事实。
14. 所有字段必须存在；没有内容时使用空数组、空字符串或 null。

====================
十四、输出前自检
====================

输出前检查：

1. 是否只输出 JSON？
2. JSON 是否合法？
3. health_score 是否等于输入？
4. 是否完整输出 6 个 dimensions？
5. dimensions 顺序是否为 profit、scale、risk、cost、transform、outlook？
6. 每个 dimension 是否都有 role、brief、detail、bullets、next_validation？
7. bullets 是否为对象数组？
8. bullets.text 是否只使用允许的 <strong> 标签？
9. root_cause_chain 是否保持输入 root_causes 排序？
10. emerging_findings 是否没有重复六维卡片中的普通结论？
11. 是否没有输出 key_conflicts？
12. 是否没有输出投资建议？

====================
十五、现在请处理以下输入 JSON
====================

{{DASHBOARD_FACT_PACK}}
```

---

# 八、前端需要同步改的字段

## 必须支持的新字段

| 字段                                         | 用途          |
| ------------------------------------------ | ----------- |
| `standard_diagnosis.summary_items[].label` | AI 实时摘要标签   |
| `standard_diagnosis.summary_items[].color` | 摘要颜色        |
| `dimensions[].role`                        | 显示该维度的经营角色  |
| `dimensions[].detail.next_validation`      | 深度解读下方的验证指标 |
| `offset_factors[].evidence`                | 抵消因子证据      |
| `management_questions`                     | 管理层追问       |
| `watch_items`                              | 后续验证指标      |

---

# 九、前端渲染

1. `AI_DIMS` 从静态数据改为 AI JSON。
2. `detail.bullets` 按 `{icon, text}` 渲染。
3. `summary_items` 动态渲染右侧 AI 实时摘要。
4. `root_cause_chain` 动态渲染。
5. `offset_factors` 动态渲染。
6. 增加 `role` 展示。
7. 增加 emerging_findings 模块，但当数组为空时隐藏。
