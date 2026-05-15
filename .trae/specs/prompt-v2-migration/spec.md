# Prompt V2 改造规格

## Why
当前后端 prompt、schema、fact pack 和 mock 输出均为 v1 简化版本：prompt 只有 10-13 行粗略约束、schema 字段扁平且缺少探索洞察层、fact pack 缺少业务上下文/分维度/规则判断等关键输入、mock_llm 输出不符合 v2 结构。按照《prompt优化设计.md》的完整设计，需要将三个 AI 场景整体升级到 v2，实现标准洞察 + 探索洞察双层输出，并为 LLM 提供更丰富的结构化事实输入。

## What Changes
- 新增 `card_insight_v2.txt`、`dashboard_insight_v2.txt`、`scenario_insight_v2.txt` 三个完整 prompt 文件
- 替换三个 JSON Schema 为 v2 版本（字段嵌套化、新增探索洞察层、增加长度/数量约束）
- 重构 `fact_builder.py`，生成符合 v2 设计的 CARD_FACT_PACK / DASHBOARD_FACT_PACK / SCENARIO_FACT_PACK 结构
- 重构 `rule_engine.py`，新增 cross_signals 识别、rule_result 生成、维度映射更新（profit/scale/risk/cost/transform/outlook）
- 重构 `mock_llm.py`，输出符合 v2 schema 的 mock 数据
- 更新 `insight_service.py`，引用 v2 prompt 和 schema
- 更新 `card_config.json`，新增 business_context / segment_dimensions / allowed_exploration_types / output_slots 等字段
- 更新 `dashboard_metrics_mock.json`，从扁平 metrics 数组升级为 core_metrics / segment_metrics / scenario_defaults / rules 结构
- 保留 v1 prompt/schema 文件不删除，仅新增 v2 文件并切换引用

## Impact
- Affected specs: card_insight、dashboard_insight、scenario_insight 三个 AI 场景
- Affected code: `fact_builder.py`、`rule_engine.py`、`mock_llm.py`、`insight_service.py`、`server.py`（可能需微调）
- Affected data: `card_config.json`、`dashboard_metrics_mock.json`
- Affected schemas: 三个 `.schema.json` 文件
- Affected prompts: 三个 v2 `.txt` 文件（新增）
- **BREAKING**: API 响应字段结构变更（`insight` → `standard_insight`，`health_summary` 嵌套进 `standard_diagnosis`，scenario 字段嵌套进 `standard_explanation`/`scenario_exploration`）

## ADDED Requirements

### Requirement: V2 Prompt 文件
系统 SHALL 提供三个 v2 prompt 文件，分别对应 card_insight、dashboard_insight、scenario_insight，内容严格按照《prompt优化设计.md》中的完整 prompt 定义，包含输入说明、生成规则、硬性约束、输出 JSON 格式、合格示例和 `{{FACT_PACK}}` 占位符。

#### Scenario: Prompt 加载成功
- **WHEN** insight_service 调用 `_generate` 并指定 v2 prompt 文件
- **THEN** 系统能正确加载 prompt 文本并将 Fact Pack 注入占位符

### Requirement: V2 JSON Schema
系统 SHALL 提供三个 v2 schema 文件，严格按照《prompt优化设计.md》中的 schema 定义，包含嵌套对象结构、exploratory_insights / emerging_findings / scenario_exploration 等新字段、以及长度和数量约束。

#### Scenario: Schema 校验通过
- **WHEN** LLM/Mock 输出符合 v2 schema 的 JSON
- **THEN** schema 校验无错误

#### Scenario: Schema 校验拒绝
- **WHEN** LLM/Mock 输出缺少 required 字段或字段类型不匹配
- **THEN** schema 校验返回错误列表

### Requirement: V2 Fact Pack 生成
系统 SHALL 通过 fact_builder 生成符合 v2 设计的三类 Fact Pack：
1. CARD_FACT_PACK：包含 period、card（含 id/name/level/view_type/section_id）、business_context、metrics（含 display_value/display_yoy_delta/display_budget_gap/distance_to_redline/note 等显示字段）、segments、alerts、driver_ranking、parent_context、rule_result
2. DASHBOARD_FACT_PACK：包含 period、health_score、health_label、dupont、dimension_scores（含 evidence/conclusion_hint/watch_items）、root_causes、offset_factors、alerts、cross_signals、rule_result
3. SCENARIO_FACT_PACK：包含 period、scenario、scenario_label、inputs（含 name/display_value）、computed（含 name/display_value）、benchmark、sensitivity（含 name/display_value）、rule_result

#### Scenario: Card Fact Pack 包含业务上下文
- **WHEN** build_card_fact_pack 被调用
- **THEN** 输出包含 business_context（parent_metric / management_question / dupont_path / analysis_focus）

#### Scenario: Dashboard Fact Pack 包含 cross_signals
- **WHEN** build_dashboard_fact_pack 被调用
- **THEN** 输出包含 cross_signals 数组，每个元素有 type / signal / supporting_facts / analysis_hint

#### Scenario: Scenario Fact Pack 包含 rule_result
- **WHEN** build_scenario_fact_pack 被调用
- **THEN** 输出包含 rule_result（scenario_quality / key_variable / risk_boundary_hint / upside_hint / fragile_assumption_hint / hidden_risk_hint / must_mention / do_not_mention）

### Requirement: Rule Engine V2 增强
系统 SHALL 在 rule_engine 中新增以下能力：
1. cross_signals 识别：基于指标组合自动检测 contradiction / offset_failure / quality_issue 等跨指标信号
2. rule_result 生成：为三个场景生成结构化规则判断结果（suggested_status / must_mention / do_not_mention / allowed_exploration_types 等）
3. 维度映射更新：从 profit/scale/pricing/risk/funding/opex 更新为 profit/scale/risk/cost/transform/outlook

#### Scenario: cross_signals 检测到矛盾信号
- **WHEN** 指标中同时存在规模改善（status=green）和风险恶化（status=red）
- **THEN** cross_signals 包含 type=contradiction 的信号

### Requirement: V2 Mock LLM 输出
系统 SHALL 使 mock_llm.py 的输出符合 v2 schema，包括：
1. card_insight：standard_insight（替代 insight）、evidence（对象数组替代字符串数组）、exploratory_insights、deep_dive_questions
2. dashboard_insight：standard_diagnosis（嵌套对象）、emerging_findings、management_questions
3. scenario_insight：standard_explanation（嵌套对象）、scenario_exploration（含 fragile_assumption / hidden_risk / upside_surprise）

#### Scenario: Mock Card Insight 输出符合 V2 Schema
- **WHEN** mock_llm 生成 card insight
- **THEN** 输出包含 standard_insight、exploratory_insights、deep_dive_questions 字段且通过 schema 校验

### Requirement: card_config.json V2 升级
系统 SHALL 将 card_config.json 中每个卡片配置新增以下字段：business_context（parent_metric / management_question / dupont_path / analysis_focus）、segment_dimensions、allowed_exploration_types、output_slots、schema、enabled。

#### Scenario: 卡片配置包含业务上下文
- **WHEN** 加载 card_config.json
- **THEN** 每个启用的卡片配置包含 business_context 对象

### Requirement: dashboard_metrics_mock.json V2 升级
系统 SHALL 将 dashboard_metrics_mock.json 从扁平 metrics 数组升级为包含 core_metrics / segment_metrics / scenario_defaults / rules 四部分的 v2 结构，且每个指标包含 display_value / display_yoy / display_budget_gap 等显示字段。

#### Scenario: 指标包含显示字段
- **WHEN** 加载 dashboard_metrics_mock.json
- **THEN** core_metrics 中的每个指标包含 display_value 字段

## MODIFIED Requirements

### Requirement: insight_service 引用 V2 文件
insight_service.py 中 `generate_card_insight` SHALL 引用 `card_insight_v2.txt` 和 `card_insight.schema.json`；`generate_dashboard_insight` SHALL 引用 `dashboard_insight_v2.txt` 和 `dashboard_insight.schema.json`；`generate_scenario_insight` SHALL 引用 `scenario_insight_v2.txt` 和 `scenario_insight.schema.json`。

### Requirement: API 响应字段兼容
server.py 的三个 AI 接口 SHALL 返回 v2 结构的 JSON。前端暂不做改动，但后端 API 响应字段变更需记录为 BREAKING。

## REMOVED Requirements
无删除需求。v1 prompt/schema 文件保留但不引用。
