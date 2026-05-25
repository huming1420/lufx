# PROJECT_CONTEXT.md

## 项目名称

陆金所控股经营分析看板 LLM 改造项目

## 项目类型

金融科技 - 经营分析可视化 + AI 洞察生成系统

- 前端：HTML + JavaScript + ECharts
- 后端：Python + FastAPI + SQLite
- AI：LLM（火山引擎 MiniMax）

## 核心目标

将静态 HTML 看板改造为由 AI 动态生成洞察的系统，同时保证：
1. 指标计算准确
2. 经营逻辑可控
3. LLM 不胡编数字
4. 输出结构稳定
5. 后续可接入内部 LLM 或智能体平台
6. 前端页面样式和原有交互尽量不破坏

## 主要用户

- 陆金所控股管理层
- 经营分析团队
- 业务部门负责人

## 使用场景

1. **经营分析看板** (`lufax_dashboard3.html`) - 查看核心经营指标，每个卡片有 AI 文字洞察
2. **AI 洞察页** (`ai_insights.html`) - 查看动态解读（六维诊断、根因链等）和经营计算器

## 核心业务流程

```
Excel 数据上传
  → 数据库存储
  → 指标计算与规则判断
  → 生成结构化 Fact Pack
  → 调用 LLM 生成洞察
  → JSON Schema 校验
  → 前端动态渲染
```

## 主要功能模块

### 1. 单卡片洞察
- 为看板每个经营卡片生成 AI 文字洞察
- API：`POST /api/insights/card`
- 输出包含：standard_insight, drivers, watch_items, exploratory_insights

### 2. 动态解读
- 为 AI 洞察页生成经营诊断
- API：`POST /api/insights/dashboard`
- 输出包含：health_score, dimensions, root_cause_chain, emerging_findings

### 3. 经营计算器
- 确定性计算 ROA 和净利润
- LLM 只负责解释计算结果
- API：`POST /api/insights/scenario`

### 4. 洞察任务管理
- 批量生成洞察
- 任务状态追踪
- 缓存管理
- API：`POST /api/insights/jobs`, `GET /api/insights/jobs/{job_id}`


⚠️ **重要边界：**
1. LLM 不负责计算任何指标（ROA、净利润、同比、环比等）
2. LLM 输出的数字必须全部来自 Fact Pack
3. LLM 不直接读取 Excel 或数据库
4. 系统不负责数据采集和 ETL（假设数据已经准备好）
5. 系统不负责用户认证和权限管理


## 技术栈快速一览

- 前端：HTML5, JavaScript, ECharts 5.4.3
- 后端：Python 3.12+, FastAPI
- 数据库：替换
- LLM：替换
- 部署：uvicorn, start.bat（Windows）

## 关键文件位置

| 文件 | 说明 |
|------|------|
| `backend/server.py` | FastAPI 入口 |
| `backend/services/insight_service.py` | 洞察服务核心 |
| `backend/services/fact_builder.py` | Fact Pack 构建 |
| `backend/services/llm_adapter.py` | LLM 适配器 |
| `backend/prompts/*.txt` | Prompt 模板 |
| `backend/schemas/*.json` | JSON Schema |
| `backend/data/card_config.json` | 卡片配置 |
| `lufax_dashboard3.html` | 主看板页面 |
| `ai_insights.html` | AI 洞察页面 |
