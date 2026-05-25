# API_MAP.md

## API 映射表

### 1. GET /api/health

#### 路径
`/api/health`

#### 方法
GET

#### 所在文件
`backend/server.py`

#### 处理函数
`health()`

#### 入参
无

#### 出参
```json
{
  "status": "ok",
  "service": "lufax-dashboard-backend",
  "validation": {
    "jsonschema_available": true,
    "validator": "jsonschema",
    "llm": {
      "adapter": "MockLLMAdapter",
      "protocol": "mock",
      "base_url_configured": false,
      "app_id_configured": false,
      "bot_id_configured": false
    }
  }
}
```

#### 调用方
前端页面、健康检查

#### 注意事项
- 无需认证
- 用于检查服务是否正常

---

### 2. GET /api/dashboard/data

#### 路径
`/api/dashboard/data`

#### 方法
GET

#### 所在文件
`backend/server.py`

#### 处理函数
`dashboard_data()`

#### 入参
无

#### 出参
```json
{
  "period": "2026-03-YTD",
  "metric_version": "...",
  "core_metrics": [...],
  "segment_metrics": [...],
  "scenario_defaults": [...],
  "rules": {...},
  "cards": [...],
  "fact_pack_rules": {...}
}
```

#### 调用方
`lufax_dashboard3.html`, `ai_insights.html`

#### 注意事项
- 返回看板所需的所有数据
- 数据当前来自 Mock JSON

---

### 3. GET /api/insights/page-state

#### 路径
`/api/insights/page-state`

#### 方法
GET

#### 所在文件
`backend/server.py`

#### 处理函数
`insights_page_state()`

#### 入参
- `period` (query, 可选): 数据期间，默认 "2026-03-YTD"

#### 出参
页面状态 JSON

#### 调用方
前端页面

#### 注意事项
- 返回洞察页面状态

---

### 4. GET /api/insights/cards

#### 路径
`/api/insights/cards`

#### 方法
GET

#### 所在文件
`backend/server.py`

#### 处理函数
`insights_cards()`

#### 入参
- `period` (query, 可选): 数据期间，默认 "2026-03-YTD"

#### 出参
```json
{
  "period": "2026-03-YTD",
  "metric_version": "...",
  "prompt_version": "v2",
  "cards": [
    {
      "card_id": "profit",
      "status": "ready",
      "standard_insight": "...",
      "drivers": [...],
      "watch_items": [...],
      "evidence": [...],
      "exploratory_insights": [...],
      "deep_dive_questions": [...]
    }
  ]
}
```

#### 调用方
`lufax_dashboard3.html`

#### 注意事项
- 返回所有卡片的洞察
- 从缓存读取，不会重新生成

---

### 5. GET /api/insights/dashboard

#### 路径
`/api/insights/dashboard`

#### 方法
GET

#### 所在文件
`backend/server.py`

#### 处理函数
`insights_dashboard()`

#### 入参
- `period` (query, 可选): 数据期间，默认 "2026-03-YTD"

#### 出参
看板洞察 JSON（详见 `dashboard_insight.schema.json`）

#### 调用方
`ai_insights.html`

#### 注意事项
- 返回看板动态解读
- 从缓存读取，不会重新生成
- 如果缓存不存在，返回状态 "missing"

---

### 6. POST /api/insights/jobs

#### 路径
`/api/insights/jobs`

#### 方法
POST

#### 所在文件
`backend/server.py`

#### 处理函数
`create_insight_job()`

#### 入参
```json
{
  "period": "2026-03-YTD",
  "scope": "all",
  "card_ids": ["profit", "risk"],
  "force_refresh": false
}
```
- `period`: 数据期间
- `scope`: 任务范围，可选值 "all", "cards", "dashboard", "card"
- `card_ids`: 卡片 ID 列表（scope 为 "cards" 或 "card" 时）
- `force_refresh`: 是否强制刷新缓存

#### 出参
```json
{
  "job_id": "...",
  "period": "2026-03-YTD",
  "scope": "all",
  "status": "pending"
}
```

#### 调用方
前端页面、管理工具

#### 注意事项
- 创建洞察生成任务
- 任务在后台异步执行
- 如果所有洞察已缓存，不创建任务

---

### 7. GET /api/insights/jobs/{job_id}

#### 路径
`/api/insights/jobs/{job_id}`

#### 方法
GET

#### 所在文件
`backend/server.py`

#### 处理函数
`get_insight_job()`

#### 入参
- `job_id` (path): 任务 ID

#### 出参
```json
{
  "job_id": "...",
  "period": "2026-03-YTD",
  "metric_version": "...",
  "prompt_version": "v2",
  "job_type": "all",
  "status": "running",
  "total_count": 10,
  "finished_count": 5,
  "failed_count": 0,
  "force_refresh": false,
  "created_at": "...",
  "started_at": "...",
  "finished_at": null,
  "error_message": "",
  "items": [...]
}
```

#### 调用方
前端页面、管理工具

#### 注意事项
- 返回任务状态和进度
- status 可选值: "pending", "running", "finished", "failed"

---

### 8. POST /api/insights/card

#### 路径
`/api/insights/card`

#### 方法
POST

#### 所在文件
`backend/server.py`

#### 处理函数
`card_insight_legacy()`

#### 入参
```json
{
  "card_id": "profit",
  "period": "2026-03-YTD"
}
```
- `card_id`: 卡片 ID（必需）
- `period`: 数据期间（可选）

#### 出参
卡片洞察 JSON（详见 `card_insight.schema.json`）

#### 调用方
`lufax_dashboard3.html`

#### 注意事项
- 生成单个卡片的洞察
- 如果缓存存在且未过期，返回缓存
- 如果 LLM 输出未通过 Schema 校验，返回 502 错误

---

### 9. POST /api/insights/dashboard

#### 路径
`/api/insights/dashboard`

#### 方法
POST

#### 所在文件
`backend/server.py`

#### 处理函数
`dashboard_insight_legacy()`

#### 入参
```json
{
  "period": "2026-03-YTD"
}
```
- `period`: 数据期间（可选）

#### 出参
看板洞察 JSON（详见 `dashboard_insight.schema.json`）

#### 调用方
`ai_insights.html`

#### 注意事项
- 生成看板动态解读
- 如果缓存存在且未过期，返回缓存
- 如果 LLM 输出未通过 Schema 校验，返回 502 错误

---

### 10. POST /api/insights/scenario

#### 路径
`/api/insights/scenario`

#### 方法
POST

#### 所在文件
`backend/server.py`

#### 处理函数
`scenario_insight()`

#### 入参
```json
{
  "scenario_id": "base",
  "scenario": "base",
  "period": "2026-03-YTD",
  "cache_only": false,
  "inputs": {
    "anr": 1900,
    "consumer_finance_growth": 0.25,
    "pricing_rate": 0.18,
    "credit_loss_rate": 0.08,
    "npl_rate": 0.03,
    "funding_cost_rate": 0.03,
    "sales_cost_rate": 0.02,
    "opex_rate": 0.01,
    "tax_other_rate": 0.01,
    "non_loan_profit": -10
  }
}
```
- `scenario_id` 或 `scenario`: 情景 ID
- `period`: 数据期间（可选）
- `cache_only`: 是否只从缓存读取
- `inputs`: 计算器参数（可选）

#### 出参
情景洞察 JSON（详见 `scenario_insight.schema.json`）

#### 调用方
`ai_insights.html`

#### 注意事项
- 生成经营计算器情景解读
- 基于参数哈希缓存
- LLM 只解释，不计算
- 如果 `cache_only=true` 且无缓存，返回状态 "missing"

---

### 11. POST /api/insights/manual-override

#### 路径
`/api/insights/manual-override`

#### 方法
POST

#### 所在文件
`backend/server.py`

#### 处理函数
`manual_override()`

#### 入参
任意 JSON

#### 出参
```json
{
  "status": "accepted",
  "accepted": true,
  "message": "Manual override accepted by mock backend.",
  "payload": {...}
}
```

#### 调用方
管理工具

#### 注意事项
- 当前是 Mock 实现
- 用于手动覆盖洞察结果

---

### 12. POST /api/upload/excel

#### 路径
`/api/upload/excel`

#### 方法
POST

#### 所在文件
`backend/server.py`

#### 处理函数
`upload_excel()`

#### 入参
```json
{
  "period": "2026-03-YTD"
}
```

#### 出参
```json
{
  "status": "accepted",
  "period": "2026-03-YTD",
  "insight_job_id": "...",
  "message": "Excel data accepted. Insight generation job created."
}
```

#### 调用方
管理工具

#### 注意事项
- 当前是 Mock 实现
- 用于上传 Excel 数据
- 会自动创建洞察生成任务

---

### 13. GET /lufax_dashboard3.html

#### 路径
`/lufax_dashboard3.html`

#### 方法
GET

#### 所在文件
`backend/server.py`

#### 处理函数
`lufax_dashboard()`

#### 入参
无

#### 出参
HTML 页面

#### 调用方
浏览器

#### 注意事项
- 返回主看板页面

---

### 14. GET /ai_insights.html

#### 路径
`/ai_insights.html`

#### 方法
GET

#### 所在文件
`backend/server.py`

#### 处理函数
`ai_insights_page()`

#### 入参
无

#### 出参
HTML 页面

#### 调用方
浏览器

#### 注意事项
- 返回 AI 洞察页面

---

### 15. GET /favicon.ico

#### 路径
`/favicon.ico`

#### 方法
GET

#### 所在文件
`backend/server.py`

#### 处理函数
`favicon()`

#### 入参
无

#### 出参
204 No Content

#### 调用方
浏览器

#### 注意事项
- 空响应

---

## API 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 204 | No Content（favicon） |
| 400 | Bad Request（参数错误） |
| 404 | Not Found（资源不存在） |
| 500 | Internal Server Error（服务器错误） |
| 502 | Bad Gateway（LLM 输出未通过 Schema 校验） |

---

## 通用错误响应

```json
{
  "detail": "错误信息"
}
```
