# AI 经营洞察模块优化 Spec

## Why
当前 AI 洞察页面内容偏静态，后端 prompt 对输出格式约束不够精确，JSON Schema 与优化方案目标结构存在字段缺失（summary_items 缺 label/color、dimensions 缺 role/next_validation、bullets 是字符串数组而非对象数组、offset_factors 缺 evidence），前端缺少 emerging_findings、management_questions 模块，summary_items 渲染不使用 label/color，dimension 卡片不显示 role，detail 不展示 next_validation。需要按《ai洞察优化方案.md》完整落地。

## What Changes
- 替换 `dashboard_insight_v2.txt` 为优化方案第七节的完整提示词（15 节，含详细字段约束和自检规则）
- 更新 `dashboard_insight.schema.json`：summary_items 增加 label/color、dimensions 增加 role 和 detail.next_validation、bullets 从 string[] 改为 {icon,text}[] 对象数组、offset_factors 增加 evidence
- 更新 `mock_llm.py` 的 `generate_dashboard_insight()` 及相关函数输出新字段
- 更新 `ai_insights.html`：新增 emerging_findings / management_questions / watch_items 独立区块；summary_items 渲染使用 label + color；dimension 卡片显示 role；detail 展示 next_validation；offset_factors 展示 evidence；整体 HTML 结构对齐优化方案第六节布局
- 更新 `_renderDashboardData()` 适配新数据结构

## Impact
- Affected specs: dashboard_insight 场景（prompt + schema + mock + 前端渲染）
- Affected code: `backend/prompts/dashboard_insight_v2.txt`、`backend/schemas/dashboard_insight.schema.json`、`backend/services/mock_llm.py`、`ai_insights.html`
- **BREAKING**: dashboard insight API 响应字段结构变更（summary_items 增加 label/color、dimensions 增加 role 和 detail.next_validation、bullets 从 string[] 改为 {icon,text}[]、offset_factors 增加 evidence）

## ADDED Requirements

### Requirement: 完整提示词替换
系统 SHALL 将 `dashboard_insight_v2.txt` 替换为《ai洞察优化方案.md》第七节定义的完整提示词，包含：角色定义、输入说明、标准诊断规则、探索性发现规则、六维卡片规则、bullets 规则、root_cause_chain 规则、offset_factors 规则、emerging_findings 规则、management_questions 规则、watch_items 规则、硬性约束、输出前自检、`{{DASHBOARD_FACT_PACK}}` 占位符。

#### Scenario: Prompt 加载成功
- **WHEN** insight_service 调用 `_generate` 加载 dashboard_insight_v2.txt
- **THEN** prompt 文本包含十五节完整内容，且最后包含 `{{DASHBOARD_FACT_PACK}}` 占位符

### Requirement: Schema 字段增强
系统 SHALL 更新 `dashboard_insight.schema.json`，具体变更：
1. `standard_diagnosis.summary_items` 每个 item 增加 `label`（string, 1-20 字符）和 `color`（enum: red/yellow/green/accent/purple），且 `label` 和 `color` 为 required
2. `dimensions` 每个 item 增加 `role`（enum: 结果承压/核心拖累/边际改善/正向抵消/结构观察/待验证），required
3. `dimensions[].detail.bullets` 从 `string[]` 改为对象数组 `{icon: string, text: string}`，icon 枚举 🔴🟡🟢🔵，text 允许 `<strong>` 标签，每个维度 3-4 条
4. `dimensions[].detail` 增加 `next_validation`（string[], maxItems:5），required
5. `offset_factors` 每个 item 增加 `evidence`（string[], minItems:1, maxItems:5），required

#### Scenario: Schema 校验 summary_items 含 label 和 color
- **WHEN** LLM 输出的 summary_items 包含 label 和 color
- **THEN** schema 校验通过

#### Scenario: Schema 校验 dimensions 含 role 和 next_validation
- **WHEN** LLM 输出的 dimensions 包含 role 和 detail.next_validation
- **THEN** schema 校验通过

#### Scenario: Schema 校验 bullets 为对象数组
- **WHEN** LLM 输出的 bullets 为 `[{icon: "🔴", text: "..."}]` 格式
- **THEN** schema 校验通过

#### Scenario: Schema 拒绝 bullets 为字符串数组
- **WHEN** LLM 输出的 bullets 为 `["证据1", "证据2"]` 格式
- **THEN** schema 校验返回错误

### Requirement: Mock LLM 输出新字段
系统 SHALL 更新 `mock_llm.py` 的 `generate_dashboard_insight()` 及相关函数：
1. `_dashboard_summary_items()` 输出增加 `label` 和 `color` 字段
2. `_dimension()` 输出增加 `role` 字段
3. `_dimension()` 的 `detail.bullets` 输出为 `{icon, text}` 对象数组而非字符串数组
4. `_dimension()` 的 `detail` 增加 `next_validation` 字段
5. `_offset_factors()` 输出增加 `evidence` 字段

#### Scenario: Mock Dashboard Insight 包含新字段
- **WHEN** mock_llm 生成 dashboard insight
- **THEN** 输出包含 summary_items[].label、summary_items[].color、dimensions[].role、dimensions[].detail.bullets 为对象数组、dimensions[].detail.next_validation、offset_factors[].evidence

### Requirement: 前端 Summary Items 动态渲染
系统 SHALL 使 `ai_insights.html` 的 AI 实时解读摘要区域从 `summary_items` 数组动态渲染，每条显示 color 色点 + label 标签 + text 要点，而非当前硬编码的 HTML。

#### Scenario: Summary Items 动态渲染
- **WHEN** 后端返回 summary_items 含 label/color/text
- **THEN** 摘要区域每条显示对应颜色色点、加粗 label、text 内容

### Requirement: Dimension 卡片显示 Role
系统 SHALL 在六维诊断卡片上显示 `role` 属性（如"结果承压""正向抵消"等），以标签形式展示在卡片内。

#### Scenario: Role 标签展示
- **WHEN** dimension 数据包含 role 字段
- **THEN** 维度卡片内显示 role 标签

### Requirement: Detail 展示 Next Validation
系统 SHALL 在深度解读详情框中展示 `next_validation` 验证指标列表，位于 bullets 下方。

#### Scenario: Next Validation 展示
- **WHEN** 用户点击维度卡片查看深度解读
- **THEN** 详情框内 bullets 下方展示"后续验证指标"列表

### Requirement: 新增 Emerging Findings 模块
系统 SHALL 在动态解读 Tab 中新增"AI 探索性发现"模块，展示 `emerging_findings` 数组。当数组为空时隐藏该模块。

#### Scenario: Emerging Findings 非空展示
- **WHEN** emerging_findings 数组有内容
- **THEN** 展示探索性发现区域，每条显示 type 标签、finding 内容、confidence 标记、supporting_facts、why_it_matters、next_validation

#### Scenario: Emerging Findings 为空隐藏
- **WHEN** emerging_findings 数组为空
- **THEN** 不展示探索性发现区域

### Requirement: 新增 Management Questions 模块
系统 SHALL 在动态解读 Tab 中新增"管理层追问"模块，展示 `management_questions` 数组，最多 5 条。

#### Scenario: Management Questions 展示
- **WHEN** management_questions 数组有内容
- **THEN** 以列表形式展示管理层追问问题

### Requirement: Watch Items 增强展示
系统 SHALL 将当前"观察清单"增强为独立的"后续验证指标"模块，展示 `watch_items` 数组，与抵消因子和探索性发现并列。

#### Scenario: Watch Items 独立展示
- **WHEN** watch_items 数组有内容
- **THEN** 以独立区块展示后续验证指标列表

### Requirement: Offset Factors 展示 Evidence
系统 SHALL 在抵消因子展示中增加 evidence 证据列表。

#### Scenario: Evidence 展示
- **WHEN** offset_factors 项包含 evidence 数组
- **THEN** 每个抵消因子下方展示其证据列表

### Requirement: 页面 HTML 结构对齐优化方案
系统 SHALL 将 `pane-interpret` 的 HTML 结构调整为优化方案第六节定义的布局：
1. 顶部经营结论区（综合评分 + AI 实时摘要）
2. 六维诊断卡片（dims-grid + detail-box）
3. 根因链 + 抵消因子
4. 探索性发现
5. 管理层追问 + 后续验证指标

#### Scenario: HTML 结构完整
- **WHEN** 页面加载
- **THEN** pane-interpret 包含上述五个 section 且顺序正确

## MODIFIED Requirements

### Requirement: _renderDashboardData 适配新数据结构
`ai_insights.html` 中的 `_renderDashboardData()` SHALL 新增对 `emerging_findings`、`management_questions` 的渲染调用，并更新 `summary_items`、`offset_factors` 的渲染逻辑以支持新增字段。

### Requirement: Bullets 渲染支持 HTML strong 标签
`ai_insights.html` 中的 `showAiDetail()` 在渲染 bullets.text 时 SHALL 保留 `<strong>` 标签（当前使用 escapeHtml 会转义），同时仍过滤不允许的 HTML 标签。

## REMOVED Requirements
无删除需求。
