# FIELD_SCHEMA.md

## 字段 Schema

### 一、API 请求字段

#### 1. POST /api/insights/card

| 字段名 | 类型 | 含义 | 来源 | 使用位置 | 是否可空 | 默认值 | 兼容规则 |
|--------|------|------|------|----------|----------|--------|----------|
| card_id | string | 卡片 ID | 前端请求 | `backend/server.py` | 否 | - | 必须在 `card_config.json` 中存在 |
| period | string | 数据期间 | 前端请求 | `backend/server.py` | 是 | "2026-03-YTD" | 格式一致 |
| id | string | 卡片 ID（别名） | 前端请求 | `backend/server.py` | 否 | - | 同 card_id |

---

#### 2. POST /api/insights/dashboard

| 字段名 | 类型 | 含义 | 来源 | 使用位置 | 是否可空 | 默认值 | 兼容规则 |
|--------|------|------|------|----------|----------|--------|----------|
| period | string | 数据期间 | 前端请求 | `backend/server.py` | 是 | "2026-03-YTD" | 格式一致 |

---

#### 3. POST /api/insights/scenario

| 字段名 | 类型 | 含义 | 来源 | 使用位置 | 是否可空 | 默认值 | 兼容规则 |
|--------|------|------|------|----------|----------|--------|----------|
| scenario_id | string | 情景 ID | 前端请求 | `backend/server.py` | 是 | - | 可选 "bear", "base", "bull" |
| scenario | string | 情景 ID（别名） | 前端请求 | `backend/server.py` | 是 | - | 同 scenario_id |
| period | string | 数据期间 | 前端请求 | `backend/server.py` | 是 | "2026-03-YTD" | 格式一致 |
| cache_only | boolean | 只从缓存读取 | 前端请求 | `backend/server.py` | 是 | false | - |
| inputs.anr | number | ANR | 前端请求 | `backend/server.py`, `backend/services/fact_builder.py` | 是 | - | - |
| inputs.consumer_finance_growth | number | 消金增速 | 前端请求 | `backend/server.py`, `backend/services/fact_builder.py` | 是 | - | - |
| inputs.pricing_rate | number | 定价率 | 前端请求 | `backend/server.py`, `backend/services/fact_builder.py` | 是 | - | - |
| inputs.credit_loss_rate | number | 信贷损失率 | 前端请求 | `backend/server.py`, `backend/services/fact_builder.py` | 是 | - | - |
| inputs.npl_rate | number | 不良率 | 前端请求 | `backend/server.py`, `backend/services/fact_builder.py` | 是 | - | - |
| inputs.funding_cost_rate | number | 资金成本率 | 前端请求 | `backend/server.py`, `backend/services/fact_builder.py` | 是 | - | - |
| inputs.sales_cost_rate | number | 销售成本率 | 前端请求 | `backend/server.py`, `backend/services/fact_builder.py` | 是 | - | - |
| inputs.opex_rate | number | 运营成本率 | 前端请求 | `backend/server.py`, `backend/services/fact_builder.py` | 是 | - | - |
| inputs.tax_other_rate | number | 税及其他率 | 前端请求 | `backend/server.py`, `backend/services/fact_builder.py` | 是 | - | - |
| inputs.non_loan_profit | number | 非贷款利润 | 前端请求 | `backend/server.py`, `backend/services/fact_builder.py` | 是 | - | - |

---

#### 4. POST /api/insights/jobs

| 字段名 | 类型 | 含义 | 来源 | 使用位置 | 是否可空 | 默认值 | 兼容规则 |
|--------|------|------|------|----------|----------|--------|----------|
| period | string | 数据期间 | 前端请求 | `backend/server.py` | 是 | "2026-03-YTD" | 格式一致 |
| scope | string | 任务范围 | 前端请求 | `backend/server.py` | 是 | "all" | 必须是 "all", "cards", "dashboard", "card" 之一 |
| card_ids | array | 卡片 ID 列表 | 前端请求 | `backend/server.py` | 是 | - | scope 为 "cards" 或 "card" 时使用 |
| force_refresh | boolean | 强制刷新 | 前端请求 | `backend/server.py` | 是 | false | - |

---

### 二、API 响应字段

#### 1. 卡片洞察（card_insight）

完整定义见 `backend/schemas/card_insight.schema.json`

| 字段名 | 类型 | 含义 | 来源 | 使用位置 | 是否可空 | 默认值 | 兼容规则 |
|--------|------|------|------|----------|----------|--------|----------|
| card_id | string | 卡片 ID | LLM 输出 | `lufax_dashboard3.html` | 否 | - | - |
| status | string | 状态 | LLM 输出 | `lufax_dashboard3.html` | 否 | - | 必须是 "red", "yellow", "green" 之一 |
| title | string | 标题 | LLM 输出 | `lufax_dashboard3.html` | 否 | - | 2-20 字符 |
| standard_insight | string | 标准洞察 | LLM 输出 | `lufax_dashboard3.html` | 否 | - | 20-220 字符 |
| drivers | array | 驱动因素 | LLM 输出 | `lufax_dashboard3.html` | 否 | - | 最多 3 项，每项 1-60 字符 |
| watch_items | array | 关注项 | LLM 输出 | `lufax_dashboard3.html` | 否 | - | 最多 3 项，每项 1-60 字符 |
| evidence | array | 证据 | LLM 输出 | `lufax_dashboard3.html` | 否 | - | 最多 5 项 |
| evidence[].metric | string | 指标 | LLM 输出 | `lufax_dashboard3.html` | 否 | - | - |
| evidence[].value | string | 值 | LLM 输出 | `lufax_dashboard3.html` | 否 | - | - |
| evidence[].note | string | 说明 | LLM 输出 | `lufax_dashboard3.html` | 否 | - | 1-120 字符 |
| exploratory_insights | array | 探索性洞察 | LLM 输出 | `lufax_dashboard3.html` | 否 | - | 最多 2 项 |
| exploratory_insights[].type | string | 类型 | LLM 输出 | `lufax_dashboard3.html` | 否 | - | 枚举值见 Schema |
| exploratory_insights[].insight | string | 洞察 | LLM 输出 | `lufax_dashboard3.html` | 否 | - | 20-220 字符 |
| exploratory_insights[].confidence | string | 置信度 | LLM 输出 | `lufax_dashboard3.html` | 否 | - | "high", "medium", "low" |
| exploratory_insights[].supporting_facts | array | 支撑事实 | LLM 输出 | `lufax_dashboard3.html` | 否 | - | 1-4 项 |
| exploratory_insights[].validation_needed | array | 需验证项 | LLM 输出 | `lufax_dashboard3.html` | 否 | - | 最多 4 项 |
| deep_dive_questions | array | 深入问题 | LLM 输出 | `lufax_dashboard3.html` | 否 | - | 最多 3 项，每项 1-100 字符 |

---

#### 2. 看板洞察（dashboard_insight）

完整定义见 `backend/schemas/dashboard_insight.schema.json`

| 字段名 | 类型 | 含义 | 来源 | 使用位置 | 是否可空 | 默认值 | 兼容规则 |
|--------|------|------|------|----------|----------|--------|----------|
| period | string | 数据期间 | LLM 输出 | `ai_insights.html` | 否 | - | - |
| health_score | number | 健康分 | Fact Pack | `ai_insights.html` | 否 | - | 0-100，**LLM 不得重新计算** |
| health_label | string | 健康标签 | Fact Pack | `ai_insights.html` | 否 | - | 枚举值见 Schema |
| standard_diagnosis | object | 标准诊断 | LLM 输出 | `ai_insights.html` | 否 | - | - |
| standard_diagnosis.health_summary | string | 健康摘要 | LLM 输出 | `ai_insights.html` | 否 | - | 20-260 字符 |
| standard_diagnosis.verdict | object | 结论 | LLM 输出 | `ai_insights.html` | 否 | - | - |
| standard_diagnosis.verdict.level | string | 级别 | LLM 输出 | `ai_insights.html` | 否 | - | "good", "warn", "poor" |
| standard_diagnosis.verdict.text | string | 文本 | LLM 输出 | `ai_insights.html` | 否 | - | 1-100 字符 |
| standard_diagnosis.summary_items | array | 摘要项 | LLM 输出 | `ai_insights.html` | 否 | - | 1-6 项 |
| dimensions | array | 六维诊断 | LLM 输出 | `ai_insights.html` | 否 | - | 1-6 项 |
| dimensions[].id | string | 维度 ID | LLM 输出 | `ai_insights.html` | 否 | - | 枚举值见 Schema |
| dimensions[].score | number | 评分 | Fact Pack | `ai_insights.html` | 否 | - | 0-100，**LLM 不得重新计算** |
| dimensions[].status | string | 状态 | Fact Pack | `ai_insights.html` | 否 | - | "red", "yellow", "green"，**LLM 不得随意改变** |
| dimensions[].role | string | 角色 | LLM 输出 | `ai_insights.html` | 否 | - | 枚举值见 Schema |
| root_cause_chain | array | 根因链 | LLM 输出 | `ai_insights.html` | 否 | - | 最多 8 项，**LLM 不得改变后端排序** |
| offset_factors | array | 抵消因素 | LLM 输出 | `ai_insights.html` | 否 | - | 最多 5 项 |
| emerging_findings | array | 探索性发现 | LLM 输出 | `ai_insights.html` | 否 | - | 最多 3 项 |
| management_questions | array | 管理层问题 | LLM 输出 | `ai_insights.html` | 否 | - | 最多 5 项 |
| watch_items | array | 关注项 | LLM 输出 | `ai_insights.html` | 否 | - | 最多 5 项 |

---

#### 3. 情景洞察（scenario_insight）

完整定义见 `backend/schemas/scenario_insight.schema.json`

| 字段名 | 类型 | 含义 | 来源 | 使用位置 | 是否可空 | 默认值 | 兼容规则 |
|--------|------|------|------|----------|----------|--------|----------|
| scenario | string | 情景 ID | LLM 输出 | `ai_insights.html` | 否 | - | - |
| scenario_label | string | 情景标签 | LLM 输出 | `ai_insights.html` | 否 | - | 1-30 字符 |
| standard_explanation | object | 标准解释 | LLM 输出 | `ai_insights.html` | 否 | - | - |
| standard_explanation.judgement | string | 判断 | LLM 输出 | `ai_insights.html` | 否 | - | 20-180 字符 |
| standard_explanation.profit_driver | string | 利润驱动 | LLM 输出 | `ai_insights.html` | 否 | - | 20-180 字符 |
| standard_explanation.key_variable | string | 关键变量 | LLM 输出 | `ai_insights.html` | 否 | - | 1-40 字符 |
| standard_explanation.risk_boundary | string | 风险边界 | LLM 输出 | `ai_insights.html` | 否 | - | 20-180 字符 |
| standard_explanation.upside_condition | string | 上行条件 | LLM 输出 | `ai_insights.html` | 否 | - | 20-180 字符 |
| standard_explanation.sensitivity_comment | string | 敏感性说明 | LLM 输出 | `ai_insights.html` | 否 | - | 20-180 字符 |
| standard_explanation.management_commentary | string | 管理层评论 | LLM 输出 | `ai_insights.html` | 否 | - | 60-260 字符 |
| scenario_exploration | object | 情景探索 | LLM 输出 | `ai_insights.html` | 否 | - | - |
| scenario_exploration.fragile_assumption | object | 脆弱假设 | LLM 输出 | `ai_insights.html` | 否 | - | - |
| scenario_exploration.hidden_risk | object | 隐藏风险 | LLM 输出 | `ai_insights.html` | 否 | - | - |
| scenario_exploration.upside_surprise | object | 上行惊喜 | LLM 输出 | `ai_insights.html` | 否 | - | - |
| watch_items | array | 关注项 | LLM 输出 | `ai_insights.html` | 否 | - | 最多 5 项 |

---

### 三、数据库字段

#### 1. insight_result 表

| 字段名 | 类型 | 含义 | 来源 | 使用位置 | 是否可空 | 默认值 | 兼容规则 |
|--------|------|------|------|----------|----------|--------|----------|
| id | integer | ID | 数据库 | `backend/services/insight_repo.py` | 否 | AUTOINCREMENT | - |
| period | string | 数据期间 | 后端 | `backend/services/insight_repo.py` | 否 | - | - |
| metric_version | string | 指标版本 | 后端 | `backend/services/insight_repo.py` | 否 | "" | - |
| prompt_version | string | Prompt 版本 | 后端 | `backend/services/insight_repo.py` | 否 | "v2" | - |
| schema_name | string | Schema 名称 | 后端 | `backend/services/insight_repo.py` | 否 | "" | - |
| insight_type | string | 洞察类型 | 后端 | `backend/services/insight_repo.py` | 否 | - | "card", "dashboard" |
| card_id | string | 卡片 ID | 后端 | `backend/services/insight_repo.py` | 否 | "" | - |
| status | string | 状态 | 后端 | `backend/services/insight_repo.py` | 否 | "pending" | "pending", "running", "ready", "failed" |
| title | string | 标题 | LLM 输出 | `backend/services/insight_repo.py` | 否 | "" | - |
| standard_insight | string | 标准洞察 | LLM 输出 | `backend/services/insight_repo.py` | 否 | "" | - |
| standard_diagnosis_json | string | 标准诊断 JSON | LLM 输出 | `backend/services/insight_repo.py` | 否 | "" | - |
| standard_explanation_json | string | 标准解释 JSON | LLM 输出 | `backend/services/insight_repo.py` | 否 | "" | - |
| drivers_json | string | 驱动因素 JSON | LLM 输出 | `backend/services/insight_repo.py` | 否 | "[]" | - |
| watch_items_json | string | 关注项 JSON | LLM 输出 | `backend/services/insight_repo.py` | 否 | "[]" | - |
| evidence_json | string | 证据 JSON | LLM 输出 | `backend/services/insight_repo.py` | 否 | "[]" | - |
| exploratory_insights_json | string | 探索性洞察 JSON | LLM 输出 | `backend/services/insight_repo.py` | 否 | "[]" | - |
| emerging_findings_json | string | 探索性发现 JSON | LLM 输出 | `backend/services/insight_repo.py` | 否 | "[]" | - |
| scenario_exploration_json | string | 情景探索 JSON | LLM 输出 | `backend/services/insight_repo.py` | 否 | "{}" | - |
| deep_dive_questions_json | string | 深入问题 JSON | LLM 输出 | `backend/services/insight_repo.py` | 否 | "[]" | - |
| management_questions_json | string | 管理层问题 JSON | LLM 输出 | `backend/services/insight_repo.py` | 否 | "[]" | - |
| raw_fact_pack_json | string | 原始 Fact Pack JSON | 后端 | `backend/services/insight_repo.py` | 否 | "{}" | - |
| raw_llm_output_json | string | 原始 LLM 输出 JSON | LLM 输出 | `backend/services/insight_repo.py` | 否 | "{}" | - |
| validated | integer | 是否已验证 | 后端 | `backend/services/insight_repo.py` | 否 | 0 | 0 或 1 |
| validation_error | string | 验证错误 | 后端 | `backend/services/insight_repo.py` | 否 | "" | - |
| error_message | string | 错误信息 | 后端 | `backend/services/insight_repo.py` | 否 | "" | - |
| started_at | string | 开始时间 | 后端 | `backend/services/insight_repo.py` | 是 | null | ISO 8601 |
| finished_at | string | 完成时间 | 后端 | `backend/services/insight_repo.py` | 是 | null | ISO 8601 |
| created_at | string | 创建时间 | 数据库 | `backend/services/insight_repo.py` | 否 | datetime('now') | ISO 8601 |
| updated_at | string | 更新时间 | 数据库 | `backend/services/insight_repo.py` | 否 | datetime('now') | ISO 8601 |

**唯一约束**：(period, metric_version, prompt_version, insight_type, card_id)

---

#### 2. insight_job 表

| 字段名 | 类型 | 含义 | 来源 | 使用位置 | 是否可空 | 默认值 | 兼容规则 |
|--------|------|------|------|----------|----------|--------|----------|
| job_id | string | 任务 ID | 后端 | `backend/services/insight_repo.py` | 否 | - | PRIMARY KEY |
| period | string | 数据期间 | 后端 | `backend/services/insight_repo.py` | 否 | - | - |
| metric_version | string | 指标版本 | 后端 | `backend/services/insight_repo.py` | 否 | "" | - |
| prompt_version | string | Prompt 版本 | 后端 | `backend/services/insight_repo.py` | 否 | "v2" | - |
| job_type | string | 任务类型 | 后端 | `backend/services/insight_repo.py` | 否 | "all" | - |
| status | string | 状态 | 后端 | `backend/services/insight_repo.py` | 否 | "pending" | "pending", "running", "finished", "failed" |
| total_count | integer | 总数 | 后端 | `backend/services/insight_repo.py` | 否 | 0 | - |
| finished_count | integer | 完成数 | 后端 | `backend/services/insight_repo.py` | 否 | 0 | - |
| failed_count | integer | 失败数 | 后端 | `backend/services/insight_repo.py` | 否 | 0 | - |
| force_refresh | integer | 强制刷新 | 后端 | `backend/services/insight_repo.py` | 否 | 0 | 0 或 1 |
| created_at | string | 创建时间 | 数据库 | `backend/services/insight_repo.py` | 否 | datetime('now') | ISO 8601 |
| started_at | string | 开始时间 | 后端 | `backend/services/insight_repo.py` | 是 | null | ISO 8601 |
| finished_at | string | 完成时间 | 后端 | `backend/services/insight_repo.py` | 是 | null | ISO 8601 |
| updated_at | string | 更新时间 | 数据库 | `backend/services/insight_repo.py` | 否 | datetime('now') | ISO 8601 |
| error_message | string | 错误信息 | 后端 | `backend/services/insight_repo.py` | 否 | "" | - |

---

#### 3. insight_job_item 表

| 字段名 | 类型 | 含义 | 来源 | 使用位置 | 是否可空 | 默认值 | 兼容规则 |
|--------|------|------|------|----------|----------|--------|----------|
| id | integer | ID | 数据库 | `backend/services/insight_repo.py` | 否 | AUTOINCREMENT | - |
| job_id | string | 任务 ID | 后端 | `backend/services/insight_repo.py` | 否 | - | FOREIGN KEY |
| insight_type | string | 洞察类型 | 后端 | `backend/services/insight_repo.py` | 否 | - | - |
| card_id | string | 卡片 ID | 后端 | `backend/services/insight_repo.py` | 否 | "" | - |
| status | string | 状态 | 后端 | `backend/services/insight_repo.py` | 否 | "pending" | - |
| retry_count | integer | 重试次数 | 后端 | `backend/services/insight_repo.py` | 否 | 0 | - |
| error_message | string | 错误信息 | 后端 | `backend/services/insight_repo.py` | 否 | "" | - |
| started_at | string | 开始时间 | 后端 | `backend/services/insight_repo.py` | 是 | null | ISO 8601 |
| finished_at | string | 完成时间 | 后端 | `backend/services/insight_repo.py` | 是 | null | ISO 8601 |
| updated_at | string | 更新时间 | 数据库 | `backend/services/insight_repo.py` | 否 | datetime('now') | ISO 8601 |

---

### 四、配置字段

#### 1. 卡片配置（card_config.json）

| 字段名 | 类型 | 含义 | 来源 | 使用位置 | 是否可空 | 默认值 | 兼容规则 |
|--------|------|------|------|----------|----------|--------|----------|
| card_id | string | 卡片 ID | 配置 | `backend/services/fact_builder.py`, `lufax_dashboard3.html` | 否 | - | - |
| dom_id | string | DOM ID | 配置 | `lufax_dashboard3.html` | 否 | - | - |
| insight_element_id | string | 洞察元素 ID | 配置 | `lufax_dashboard3.html` | 否 | - | - |
| section_id | string | Section ID | 配置 | `lufax_dashboard3.html` | 否 | - | - |
| name | string | 名称 | 配置 | `lufax_dashboard3.html` | 否 | - | - |
| level | string | 级别 | 配置 | `backend/services/fact_builder.py` | 否 | - | "L0", "L1", "L2", "L3" |
| view_type | string | 视图类型 | 配置 | `backend/services/fact_builder.py` | 否 | - | - |
| metric_codes | array | 指标代码 | 配置 | `backend/services/fact_builder.py` | 否 | - | - |
| prompt_template | string | Prompt 模板 | 配置 | `backend/services/fact_builder.py` | 否 | - | - |
| schema | string | Schema | 配置 | `backend/services/fact_builder.py` | 否 | - | - |
| parent_card | string | 父卡片 | 配置 | `backend/services/fact_builder.py` | 是 | null | - |
| business_context | object | 业务上下文 | 配置 | `backend/services/fact_builder.py` | 否 | - | - |
| segment_dimensions | array | 细分维度 | 配置 | `backend/services/fact_builder.py` | 否 | - | - |
| allowed_exploration_types | array | 允许的探索类型 | 配置 | `backend/services/fact_builder.py` | 否 | - | - |
| output_slots | object | 输出槽 | 配置 | `backend/services/fact_builder.py` | 否 | - | - |
| enabled | boolean | 是否启用 | 配置 | `backend/services/fact_builder.py` | 否 | - | - |

---

### 五、重要约束

1. **LLM 不得重新计算的字段**：
   - `health_score`
   - `dimensions[].score`
   - `dimensions[].status`
   - 所有指标值（ROA、净利润、同比、环比等）

2. **LLM 输出的数字必须来自 Fact Pack**：
   - 不得编造行业均值
   - 不得编造预测值
   - 不得编造预算差
   - 不得编造红线

3. **字段命名不一致记录**：
   - 暂无
