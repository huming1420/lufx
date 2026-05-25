# ROUTING.md

## 任务路由表

| 需求类型 | 优先阅读索引 | 必读源码 | 可能关联源码 | 禁止无关读取 |
|----------|--------------|----------|--------------|--------------|
| 修改页面 UI | `.ai/UI_RULES.md`, `.ai/FILE_OWNERSHIP.md` | `lufax_dashboard3.html` 或 `ai_insights.html` | `backend/data/card_config.json` | 不要修改后端服务 |
| 修改前端交互 | `.ai/UI_RULES.md`, `.ai/FILE_OWNERSHIP.md` | `lufax_dashboard3.html` 或 `ai_insights.html` | `backend/server.py` | 不要修改 LLM 逻辑 |
| 修改前端状态管理 | `.ai/UI_RULES.md`, `.ai/FILE_OWNERSHIP.md` | `lufax_dashboard3.html` 或 `ai_insights.html` | - | 不要修改后端 |
| 修改 API 接口 | `.ai/API_MAP.md`, `.ai/FILE_OWNERSHIP.md` | `backend/server.py` | `backend/services/insight_service.py` | 不要修改 Prompt |
| 修改业务逻辑 | `.ai/BUSINESS_RULES.md`, `.ai/MODULE_MAP.md` | `backend/services/fact_builder.py`, `backend/services/rule_engine.py` | `backend/services/insight_service.py` | 不要修改 LLM 逻辑 |
| 修改数据模型 | `.ai/FIELD_SCHEMA.md`, `.ai/ARCHITECTURE.md` | `backend/sql/init.sql`, `backend/services/insight_repo.py` | `backend/services/fact_builder.py` | 不要修改前端 |
| 修改数据库字段 | `.ai/FIELD_SCHEMA.md`, `.ai/ARCHITECTURE.md` | `backend/sql/init.sql`, `backend/services/insight_repo.py` | `backend/services/fact_builder.py` | 不要修改 Prompt |
| 修改配置项 | `.ai/CONFIG_AND_ENV.md`, `.ai/FILE_OWNERSHIP.md` | `.env`, `backend/services/llm_adapter.py` | - | 不要修改业务逻辑 |
| 修改环境变量 | `.ai/CONFIG_AND_ENV.md`, `.ai/FILE_OWNERSHIP.md` | `.env` | `backend/services/llm_adapter.py` | 不要提交真实密钥 |
| 修改认证逻辑 | - | - | - | 当前无认证 |
| 修改权限逻辑 | - | - | - | 当前无权限 |
| 修改缓存逻辑 | `.ai/MODULE_MAP.md`, `.ai/FILE_OWNERSHIP.md` | `backend/services/insight_repo.py` | `backend/services/insight_service.py` | 不要破坏缓存键 |
| 修改日志逻辑 | `.ai/MODULE_MAP.md` | `backend/services/*.py` | - | 不要改变业务逻辑 |
| 修改错误处理 | `.ai/MODULE_MAP.md` | `backend/server.py`, `backend/services/insight_service.py` | - | 不要改变 API 格式 |
| 修改定时任务 | - | - | - | 当前无定时任务 |
| 修改脚本工具 | `.ai/FILE_OWNERSHIP.md` | `check_*.py`, `clear_cache.py` 等 | - | 不要修改核心服务 |
| 修改部署配置 | `.ai/DEPLOYMENT.md`, `.ai/FILE_OWNERSHIP.md` | `start.bat` | - | 不要修改代码逻辑 |
| 修改 Docker 配置 | - | - | - | 当前无 Docker |
| 修改测试用例 | `.ai/TEST_RULES.md` | - | - | 当前无测试 |
| 排查页面不更新 | `.ai/KNOWN_ISSUES.md`, `.ai/UI_RULES.md` | `lufax_dashboard3.html` 或 `ai_insights.html`, `backend/server.py` | `backend/services/insight_repo.py` | 不要随机修改 |
| 排查接口异常 | `.ai/KNOWN_ISSUES.md`, `.ai/API_MAP.md` | `backend/server.py`, `backend/services/insight_service.py` | `backend/services/llm_adapter.py` | 不要修改前端 |
| 排查数据库异常 | `.ai/KNOWN_ISSUES.md`, `.ai/ARCHITECTURE.md` | `backend/services/insight_repo.py`, `backend/sql/init.sql` | `backend/data/insight.db` | 不要修改业务逻辑 |
| 排查构建失败 | `.ai/KNOWN_ISSUES.md`, `.ai/DEPLOYMENT.md` | `start.bat` | - | 不要修改代码 |
| 排查部署失败 | `.ai/KNOWN_ISSUES.md`, `.ai/DEPLOYMENT.md` | `start.bat` | - | 不要修改代码 |
| 排查环境变量不生效 | `.ai/KNOWN_ISSUES.md`, `.ai/CONFIG_AND_ENV.md` | `.env`, `backend/services/llm_adapter.py` | - | 不要提交真实密钥 |
| 修改 Prompt | `.ai/PROMPT_RULES.md`, `.ai/FILE_OWNERSHIP.md` | `backend/prompts/*.txt` | `backend/schemas/*.json` | 不要改变输出结构（除非同步 Schema） |
| 修改 JSON Schema | `.ai/FIELD_SCHEMA.md`, `.ai/FILE_OWNERSHIP.md` | `backend/schemas/*.json` | `backend/prompts/*.txt`, `backend/services/insight_service.py` | 不要移除必需字段 |
| 修改卡片配置 | `.ai/MODULE_MAP.md`, `.ai/FILE_OWNERSHIP.md` | `backend/data/card_config.json` | `backend/services/fact_builder.py`, `lufax_dashboard3.html` | 不要改变结构 |
| 修改 Fact Pack | `.ai/FIELD_SCHEMA.md`, `.ai/MODULE_MAP.md` | `backend/services/fact_builder.py` | `backend/prompts/*.txt`, `backend/services/rule_engine.py` | 不要改变已有字段 |
| 修改 LLM 适配器 | `.ai/MODULE_MAP.md`, `.ai/CONFIG_AND_ENV.md` | `backend/services/llm_adapter.py` | `backend/services/mock_llm.py` | 不要暴露 API Key |
| 新增洞察类型 | `.ai/MODULE_MAP.md`, `.ai/PROMPT_RULES.md` | `backend/services/insight_service.py` | `backend/prompts/*.txt`, `backend/schemas/*.json` | 不要破坏现有类型 |

---

## 常见任务快速参考

### 任务：修改卡片洞察文案
**优先读**：`.ai/PROMPT_RULES.md`
**必读源码**：`backend/prompts/card_insight_v2.txt`
**关联文件**：`backend/schemas/card_insight.schema.json`
**注意**：不要改变输出结构

### 任务：修改六维诊断评分
**优先读**：`.ai/BUSINESS_RULES.md`, `.ai/MODULE_MAP.md`
**必读源码**：`backend/services/rule_engine.py`, `backend/services/fact_builder.py`
**关联文件**：`backend/prompts/dashboard_insight_v2.txt`
**注意**：规则变化需要同步 Prompt

### 任务：修改经营计算器参数
**优先读**：`.ai/UI_RULES.md`, `.ai/FILE_OWNERSHIP.md`
**必读源码**：`ai_insights.html`, `backend/services/fact_builder.py`
**关联文件**：`backend/prompts/scenario_insight_v2.txt`
**注意**：前后端参数需要一致

### 任务：添加新的卡片
**优先读**：`.ai/MODULE_MAP.md`, `.ai/UI_RULES.md`
**必读源码**：`backend/data/card_config.json`, `lufax_dashboard3.html`
**关联文件**：`backend/services/fact_builder.py`
**注意**：配置文件和 HTML 需要同步

### 任务：排查洞察不更新
**优先读**：`.ai/KNOWN_ISSUES.md`
**必读源码**：`backend/services/insight_repo.py`, `backend/services/insight_service.py`
**关联文件**：`backend/server.py`
**注意**：检查缓存、force_refresh 参数

### 任务：切换 LLM 提供者
**优先读**：`.ai/CONFIG_AND_ENV.md`, `.ai/MODULE_MAP.md`
**必读源码**：`backend/services/llm_adapter.py`, `.env`
**关联文件**：-
**注意**：不要暴露 API Key

---

## 默认读取顺序

1. 先读 `.ai/AGENT_START_HERE.md`
2. 再读 `.ai/ROUTING.md`（本文档）
3. 根据需求类型读相应的索引文件
4. 最后读必要的源码文件

**不要一开始就全项目扫描！**
