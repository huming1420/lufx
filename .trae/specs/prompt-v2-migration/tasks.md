# Tasks

- [x] Task 1: 创建三个 V2 Prompt 文件
  - [x] SubTask 1.1: 创建 `backend/prompts/card_insight_v2.txt`，严格按照优化设计文档的完整 prompt
  - [x] SubTask 1.2: 创建 `backend/prompts/dashboard_insight_v2.txt`，严格按照优化设计文档的完整 prompt
  - [x] SubTask 1.3: 创建 `backend/prompts/scenario_insight_v2.txt`，严格按照优化设计文档的完整 prompt

- [x] Task 2: 更新三个 JSON Schema 为 V2 版本
  - [x] SubTask 2.1: 更新 `backend/schemas/card_insight.schema.json` 为 v2 结构（新增 standard_insight/exploratory_insights/deep_dive_questions，evidence 改为对象数组）
  - [x] SubTask 2.2: 更新 `backend/schemas/dashboard_insight.schema.json` 为 v2 结构（新增 standard_diagnosis/emerging_findings/management_questions，dimensions 嵌套 detail）
  - [x] SubTask 2.3: 更新 `backend/schemas/scenario_insight.schema.json` 为 v2 结构（新增 standard_explanation/scenario_exploration，使用 $defs）

- [x] Task 3: 更新 `backend/data/dashboard_metrics_mock.json` 为 V2 结构
  - [x] SubTask 3.1: 将扁平 metrics 数组拆分为 core_metrics 和 segment_metrics，每个指标添加 display_value 等显示字段
  - [x] SubTask 3.2: 添加 rules 结构（health_score / health_label / dimension_scores / root_causes / offset_factors / cross_signals）

- [x] Task 4: 更新 `backend/data/card_config.json` 为 V2 结构
  - [x] SubTask 4.1: 为每个卡片配置添加 business_context / segment_dimensions / allowed_exploration_types / output_slots / schema / enabled 字段
  - [x] SubTask 4.2: 更新 prompt_template 从 v1 引用改为 v2 引用，insight_element_id 替代 dom_id

- [x] Task 5: 重构 `backend/services/rule_engine.py` 增加 V2 能力
  - [x] SubTask 5.1: 更新维度映射为 profit/scale/risk/cost/transform/outlook
  - [x] SubTask 5.2: 新增 `detect_cross_signals()` 函数，识别 contradiction / offset_failure / quality_issue 跨指标信号
  - [x] SubTask 5.3: 新增 `generate_card_rule_result()` 函数，生成单卡片规则判断结果
  - [x] SubTask 5.4: 新增 `generate_dashboard_rule_result()` 函数，生成全局规则判断结果
  - [x] SubTask 5.5: 新增 `generate_scenario_rule_result()` 函数，生成情景规则判断结果

- [x] Task 6: 重构 `backend/services/fact_builder.py` 生成 V2 Fact Pack
  - [x] SubTask 6.1: 重构 `build_card_fact_pack()` 输出 v2 结构（含 business_context / segments / alerts / driver_ranking / parent_context / rule_result）
  - [x] SubTask 6.2: 重构 `build_dashboard_fact_pack()` 输出 v2 结构（含 dupont / dimension_scores / root_causes / offset_factors / alerts / cross_signals / rule_result）
  - [x] SubTask 6.3: 重构 `build_scenario_fact_pack()` 输出 v2 结构（含 inputs / computed / benchmark / sensitivity / rule_result，均含 name/display_value）
  - [x] SubTask 6.4: 适配 dashboard_metrics_mock.json 的新结构（core_metrics / segment_metrics / rules）

- [x] Task 7: 重构 `backend/services/mock_llm.py` 输出 V2 结构
  - [x] SubTask 7.1: 重构 `generate_card_insight()` 输出 standard_insight / evidence 对象数组 / exploratory_insights / deep_dive_questions
  - [x] SubTask 7.2: 重构 `generate_dashboard_insight()` 输出 standard_diagnosis / dimensions 嵌套 detail / emerging_findings / management_questions
  - [x] SubTask 7.3: 重构 `generate_scenario_insight()` 输出 standard_explanation / scenario_exploration

- [x] Task 8: 更新 `backend/services/insight_service.py` 引用 V2 文件
  - [x] SubTask 8.1: 将 `generate_card_insight` 的 prompt 和 schema 引用从 v1 切换到 v2
  - [x] SubTask 8.2: 将 `generate_dashboard_insight` 的 prompt 和 schema 引用从 v1 切换到 v2
  - [x] SubTask 8.3: 将 `generate_scenario_insight` 的 prompt 和 schema 引用从 v1 切换到 v2
  - [x] SubTask 8.4: 更新 `get_dashboard_data()` 适配 v2 数据结构

- [x] Task 9: 端到端验证
  - [x] SubTask 9.1: 启动 FastAPI 服务器，验证 /api/health 返回正常
  - [x] SubTask 9.2: 验证 POST /api/insights/card 返回符合 v2 schema 的 JSON
  - [x] SubTask 9.3: 验证 POST /api/insights/dashboard 返回符合 v2 schema 的 JSON
  - [x] SubTask 9.4: 验证 POST /api/insights/scenario 返回符合 v2 schema 的 JSON

# Task Dependencies
- Task 1 (Prompt 文件) 和 Task 2 (Schema) 可并行，无依赖
- Task 3 (mock data) 和 Task 4 (card config) 可并行，无依赖
- Task 5 (rule_engine) 依赖 Task 3（需要新数据结构中的 rules）
- Task 6 (fact_builder) 依赖 Task 3、Task 4、Task 5
- Task 7 (mock_llm) 依赖 Task 2（需要 v2 schema 定义）
- Task 8 (insight_service) 依赖 Task 1、Task 2、Task 6、Task 7
- Task 9 (验证) 依赖 Task 8
