# KNOWN_ISSUES.md

## 已知问题

### 1. 数据来源主要是 Mock 文件
**状态**：已知，待改进
**描述**：当前指标数据主要来自 `backend/data/dashboard_metrics_mock.json`，不是真实数据库
**影响**：数据更新需要手动编辑 JSON 文件
**排查路径**：检查 `backend/services/fact_builder.py` 的 `load_dashboard_data()` 函数
**相关文件**：`backend/data/dashboard_metrics_mock.json`, `backend/services/fact_builder.py`
**建议**：后续对接真实数据库

---

### 2. 缺少单元测试
**状态**：已知，待改进
**描述**：项目当前没有单元测试、集成测试
**影响**：重构风险较高
**排查路径**：-
**相关文件**：-
**建议**：后续添加测试覆盖核心逻辑

---

### 3. 没有用户认证和权限管理
**状态**：已知，当前不涉及
**描述**：系统没有实现用户登录、权限控制
**影响**：不适合直接暴露到公网
**排查路径**：-
**相关文件**：-
**建议**：如果需要公网访问，添加认证层

---

## 已解决问题

（暂无）

---

## 已排查但未解决问题

（暂无）

---

## 不要重复排查的问题

（暂无）

---

## 常见误判

### 误判 1：让 LLM 做计算
**问题**：试图让 LLM 计算 ROA、净利润、同比、环比等指标
**正确做法**：后端/前端做确定性计算，LLM 只负责解释
**相关文件**：`项目背景及工程介绍.md`, `.ai/PROJECT_CONTEXT.md`

### 误判 2：修改前端确定性计算
**问题**：修改或移除 `ai_insights.html` 中的计算器逻辑
**正确做法**：保留前端计算，只添加 AI 解释
**相关文件**：`ai_insights.html`, `.ai/FILE_OWNERSHIP.md`

### 误判 3：暴露 API Key
**问题**：把 `.env` 中的真实 API Key 提交到仓库
**正确做法**：`.env` 只在本地配置，不要提交真实密钥
**相关文件**：`.env`, `.ai/CONFIG_AND_ENV.md`

### 误判 4：跳过 Schema 校验
**问题**：修改代码时移除或绕过 JSON Schema 校验
**正确做法**：保持校验，必要时调整 Schema
**相关文件**：`backend/services/insight_service.py`, `backend/schemas/*.json`

### 误判 5：不使用 Fact Pack
**问题**：让 LLM 直接读取原始数据而不是 Fact Pack
**正确做法**：所有 LLM 输入必须是结构化 Fact Pack
**相关文件**：`backend/services/fact_builder.py`, `backend/prompts/*.txt`

---

## 页面不更新问题排查

### 问题：卡片洞察不更新
排查步骤：
1. 检查是否使用了 `force_refresh=true`
2. 检查 `insight_result` 表中的缓存状态
3. 检查 LLM 调用是否成功
4. 检查浏览器缓存（硬刷新 Ctrl+F5）

相关文件：
- `backend/server.py` - `card_insight_legacy()` 函数
- `backend/services/insight_repo.py` - `find_insight_result()` 函数
- `lufax_dashboard3.html` - `loadCardInsights()` 函数

### 问题：动态解读不更新
排查步骤：
1. 检查是否使用了 `force_refresh=true`
2. 检查 `insight_result` 表中的缓存状态
3. 检查 LLM 调用是否成功
4. 检查浏览器缓存

相关文件：
- `backend/server.py` - `dashboard_insight_legacy()` 函数
- `backend/services/insight_repo.py` - `find_insight_result()` 函数
- `ai_insights.html` - `loadAiInsights()` 函数

---

## 接口异常问题排查

### 问题：500 错误
排查步骤：
1. 查看服务器日志
2. 检查数据库文件是否存在
3. 检查依赖是否安装
4. 检查 `.env` 配置

相关文件：
- `backend/server.py`
- `backend/services/insight_service.py`

### 问题：502 错误（InsightValidationError）
排查步骤：
1. LLM 输出未通过 Schema 校验
2. 检查 Schema 是否正确
3. 检查 Prompt 是否正确引导 LLM 输出

相关文件：
- `backend/services/insight_service.py` - `_validate_output()` 函数
- `backend/schemas/*.json`

### 问题：404 错误
排查步骤：
1. 检查 API 路径是否正确
2. 检查 card_id 是否存在

相关文件：
- `backend/server.py`
- `backend/data/card_config.json`

---

## 数据库异常问题排查

### 问题：数据库锁定
排查步骤：
1. 检查是否有多个进程同时访问
2. 检查是否有长时间运行的事务

相关文件：
- `backend/services/insight_repo.py`

### 问题：表不存在
排查步骤：
1. 检查 `insight.db` 是否存在
2. 检查是否调用了 `init_db()`

相关文件：
- `backend/sql/init.sql`
- `backend/services/insight_repo.py` - `init_db()` 函数

---

## 环境变量不生效问题排查

### 问题：配置不生效
排查步骤：
1. 检查 `.env` 文件位置是否正确（项目根目录）
2. 检查环境变量名称是否正确
3. 重启服务器

相关文件：
- `.env`
- `backend/server.py`
- `backend/services/llm_adapter.py`

---

## 缓存问题排查

### 问题：旧数据一直显示
排查步骤：
1. 使用 `force_refresh=true` 参数
2. 清除 `insight_result` 表
3. 清除浏览器缓存

相关文件：
- `backend/services/insight_repo.py`
- `clear_cache.py`（如果有）

---

## 构建问题排查

### 问题：依赖安装失败
排查步骤：
1. 检查 Python 版本（需要 3.12+）
2. 检查 pip 源
3. 手动安装依赖

相关文件：
- `start.bat`

### 问题：服务器启动失败
排查步骤：
1. 检查端口 8000 是否被占用
2. 检查依赖是否安装
3. 检查 `.env` 配置

相关文件：
- `start.bat`
- `backend/server.py`
