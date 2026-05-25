# FILE_OWNERSHIP.md

## 文件所有权表

| 文件路径 | 文件类型 | 主要职责 | 可修改内容 | 禁止修改内容 | 关联文件 |
|----------|----------|----------|------------|--------------|----------|
| `backend/server.py` | Python | FastAPI 入口、路由注册 | 新增 API 端点、中间件 | 现有 API 请求/响应格式 | `.ai/API_MAP.md`, `backend/services/insight_service.py` |
| `backend/services/insight_service.py` | Python | 洞察服务主逻辑 | 验证逻辑、新增洞察类型 | 移除 Schema 校验、改变 Fact Pack 结构 | `.ai/PROMPT_RULES.md`, `backend/prompts/*.txt`, `backend/schemas/*.json`, `backend/services/fact_builder.py`, `backend/services/llm_adapter.py` |
| `backend/services/fact_builder.py` | Python | Fact Pack 构建 | Fact Pack 结构、数据加载 | 改变已有字段结构、类型 | `.ai/FIELD_SCHEMA.md`, `backend/data/dashboard_metrics_mock.json`, `backend/data/card_config.json`, `backend/services/rule_engine.py` |
| `backend/services/llm_adapter.py` | Python | LLM 适配器 | 新增适配器、错误处理 | 暴露 API Key、改变接口 | `.ai/CONFIG_AND_ENV.md`, `backend/services/mock_llm.py`, `.env` |
| `backend/services/mock_llm.py` | Python | Mock LLM | Mock 输出内容、格式 | 改变接口 | `backend/services/llm_adapter.py` |
| `backend/services/insight_repo.py` | Python | 数据访问层 | 新增查询方法、优化查询 | 改变已有表结构（除非同步更新） | `backend/sql/init.sql`, `backend/data/insight.db` |
| `backend/services/insight_job_service.py` | Python | 任务服务 | 任务逻辑、重试策略 | 改变任务状态机 | `backend/services/insight_repo.py` |
| `backend/services/rule_engine.py` | Python | 规则引擎 | 评分规则、阈值 | 改变规则输出结构 | `backend/services/fact_builder.py` |
| `backend/prompts/card_insight_v2.txt` | Text | 卡片洞察 Prompt | 措辞、要求 | 改变输出结构（除非同步 Schema） | `.ai/PROMPT_RULES.md`, `backend/schemas/card_insight.schema.json` |
| `backend/prompts/dashboard_insight_v2.txt` | Text | 看板洞察 Prompt | 措辞、要求 | 改变输出结构（除非同步 Schema） | `.ai/PROMPT_RULES.md`, `backend/schemas/dashboard_insight.schema.json` |
| `backend/prompts/scenario_insight_v2.txt` | Text | 情景洞察 Prompt | 措辞、要求 | 改变输出结构（除非同步 Schema） | `.ai/PROMPT_RULES.md`, `backend/schemas/scenario_insight.schema.json` |
| `backend/schemas/card_insight.schema.json` | JSON | 卡片洞察 Schema | 新增字段、调整规则 | 移除必需字段、改变类型 | `backend/prompts/card_insight_v2.txt`, `backend/services/insight_service.py` |
| `backend/schemas/dashboard_insight.schema.json` | JSON | 看板洞察 Schema | 新增字段、调整规则 | 移除必需字段、改变类型 | `backend/prompts/dashboard_insight_v2.txt`, `backend/services/insight_service.py` |
| `backend/schemas/scenario_insight.schema.json` | JSON | 情景洞察 Schema | 新增字段、调整规则 | 移除必需字段、改变类型 | `backend/prompts/scenario_insight_v2.txt`, `backend/services/insight_service.py` |
| `backend/data/card_config.json` | JSON | 卡片配置 | 新增卡片、修改配置 | 改变现有卡片结构 | `backend/services/fact_builder.py`, `lufax_dashboard3.html` |
| `backend/data/dashboard_metrics_mock.json` | JSON | Mock 指标数据 | 更新数据、新增指标 | 改变现有指标结构 | `backend/services/fact_builder.py` |
| `backend/sql/init.sql` | SQL | 数据库初始化 | 新增表、索引 | 改变现有表结构（除非同步迁移） | `backend/services/insight_repo.py` |
| `lufax_dashboard3.html` | HTML | 主看板页面 | 新增卡片、修改样式 | 破坏原有 UI、移除 ECharts | `.ai/UI_RULES.md`, `backend/data/card_config.json` |
| `ai_insights.html` | HTML | AI 洞察页面 | 修改样式、新增维度 | 破坏前端确定性计算、移除计算器 | `.ai/UI_RULES.md`, `backend/services/fact_builder.py` |
| `.env` | Env | 环境变量 | 新增配置、更新值 | 提交真实 API Key | `.ai/CONFIG_AND_ENV.md` |
| `start.bat` | Batch | 启动脚本 | 修改启动参数、添加检查 | 改变启动方式 | `.ai/DEPLOYMENT.md` |
| `项目背景及工程介绍.md` | Markdown | 项目文档 | 更新说明、添加文档 | 移除核心原则说明 | `.ai/PROJECT_CONTEXT.md`, `.ai/ARCHITECTURE.md` |

---

## 关键文件详细说明

### backend/server.py
- **可修改**：新增 API 端点、添加中间件、修改错误处理
- **禁止修改**：现有 API 的请求/响应格式（保持向后兼容）
- **注意**：长时间任务使用 BackgroundTasks

### backend/services/insight_service.py
- **可修改**：验证逻辑、新增洞察类型、调整 LLM 调用流程
- **禁止修改**：移除 JSON Schema 校验、改变 Fact Pack 结构
- **注意**：Fact Pack 结构变化需要同步修改 Prompt

### backend/services/fact_builder.py
- **可修改**：Fact Pack 结构、数据加载逻辑、对接真实数据库
- **禁止修改**：改变已有字段结构和类型
- **注意**：保持字段命名一致

### backend/services/llm_adapter.py
- **可修改**：新增 LLM 适配器、调整错误处理
- **禁止修改**：暴露 API Key、改变 BaseLLMAdapter 接口
- **注意**：API Key 只从环境变量读取

### backend/prompts/*.txt
- **可修改**：Prompt 措辞、添加新的要求
- **禁止修改**：改变输出结构（除非同步修改 Schema）
- **注意**：修改后需要测试输出质量

### backend/schemas/*.json
- **可修改**：新增字段、调整验证规则
- **禁止修改**：移除必需字段、改变字段类型
- **注意**：Schema 变化需要同步修改 Prompt

### lufax_dashboard3.html
- **可修改**：新增卡片、修改 UI 样式、调整图表配置
- **禁止修改**：破坏原有 UI 风格、移除 ECharts 图表
- **注意**：保持原有交互逻辑

### ai_insights.html
- **可修改**：修改 UI 样式、新增解读维度、调整计算器参数
- **禁止修改**：破坏前端确定性计算、移除计算器
- **注意**：计算器参数变化需要同步后端

### .env
- **可修改**：新增配置项、更新配置值
- **禁止修改**：提交真实 API Key 到仓库
- **注意**：敏感信息只在本地配置

---

## 文件修改 checklist

修改任何文件前，请确认：
- [ ] 阅读了 `.ai/FILE_OWNERSHIP.md`（本文档）
- [ ] 阅读了 `.ai/ROUTING.md`
- [ ] 明确了可修改和禁止修改的内容
- [ ] 确认了关联文件是否需要同步修改
- [ ] 考虑了向后兼容性
- [ ] 不会破坏"计算和解释分离"原则

修改后，请确认：
- [ ] 更新了 `.ai/TASK_LOG.md`
- [ ] 如果涉及字段变化，更新了 `.ai/FIELD_SCHEMA.md`
- [ ] 如果涉及 API 变化，更新了 `.ai/API_MAP.md`
- [ ] 如果涉及 UI 变化，更新了 `.ai/UI_RULES.md`
- [ ] 如果涉及 Prompt 变化，更新了 `.ai/PROMPT_RULES.md`
- [ ] 如果涉及配置变化，更新了 `.ai/CONFIG_AND_ENV.md`
- [ ] 如果涉及部署变化，更新了 `.ai/DEPLOYMENT.md`
