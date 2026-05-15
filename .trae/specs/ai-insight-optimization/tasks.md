# Tasks

- [ ] Task 1: 更新 prompt 文件 - 将 `dashboard_insight_v2.txt` 替换为优化方案第七节的完整提示词（含十五节内容）
- [ ] Task 2: 更新 schema 文件 - 按 spec.md 更新 `dashboard_insight.schema.json` 的字段约束
  - [ ] Subtask 2.1: summary_items 增加 label/color
  - [ ] Subtask 2.2: dimensions 增加 role
  - [ ] Subtask 2.3: bullets 从 string[] 改为 {icon, text}[] 对象数组
  - [ ] Subtask 2.4: dimensions.detail 增加 next_validation
  - [ ] Subtask 2.5: offset_factors 增加 evidence
- [ ] Task 3: 更新 mock_llm.py - 输出新字段
  - [ ] Subtask 3.1: _dashboard_summary_items() 增加 label/color
  - [ ] Subtask 3.2: _dimension() 增加 role
  - [ ] Subtask 3.3: _dimension() 的 bullets 改为 {icon, text}[]
  - [ ] Subtask 3.4: _dimension() 的 detail 增加 next_validation
  - [ ] Subtask 3.5: _offset_factors() 增加 evidence
- [ ] Task 4: 更新 ai_insights.html - 前端渲染改造
  - [ ] Subtask 4.1: 调整 HTML 结构，新增 emerging_findings / management_questions / watch_items 独立区块
  - [ ] Subtask 4.2: summary_items 动态渲染（color 色点 + label + text）
  - [ ] Subtask 4.3: 维度卡片显示 role 标签
  - [ ] Subtask 4.4: 深度解读框展示 next_validation
  - [ ] Subtask 4.5: 渲染 emerging_findings 模块（空时隐藏）
  - [ ] Subtask 4.6: 渲染 management_questions 模块
  - [ ] Subtask 4.7: 增强 offset_factors 展示 evidence
  - [ ] Subtask 4.8: 增强 watch_items 为独立模块
  - [ ] Subtask 4.9: bullets.text 支持 &lt;strong&gt; 标签渲染
  - [ ] Subtask 4.10: 更新 _renderDashboardData() 适配新结构
- [ ] Task 5: 端到端验证 - 启动后端，访问页面验证各模块正常显示

# Task Dependencies
- Task 3 依赖 Task 2（需要新 schema 作为输出参考）
- Task 4 依赖 Task 1、Task 2、Task 3（需要完整的新数据结构）
- Task 5 依赖 Task 1、Task 2、Task 3、Task 4
