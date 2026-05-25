# ARCHITECTURE.md

## 技术栈

### 前端
- 框架：原生 HTML5 + JavaScript（无框架）
- 图表库：ECharts 5.4.3
- 样式：CSS 变量（深色主题）
- 通信：fetch API

### 后端
- Web 框架：FastAPI
- Python 版本：3.12+
- ASGI 服务器：uvicorn
- 数据库：SQLite
- 依赖：python-dotenv, jsonschema（可选）

### AI 层
- LLM 提供者：火山引擎 MiniMax（默认）
- 可切换：内部 LLM、PAIC 智能体平台
- 输出格式：JSON
- 校验：JSON Schema Draft7

## 前端框架

无框架，原生 JavaScript，使用 ECharts 做图表。

## 后端框架

FastAPI，异步处理，使用 BackgroundTasks 处理批量洞察生成。

## 数据库或存储

- SQLite 数据库：`backend/data/insight.db`
- 主要表：
  - `insight_result` - 洞察结果缓存
  - `insight_job` - 洞察任务
  - `insight_job_item` - 洞察任务项
- 数据文件：
  - `backend/data/dashboard_metrics_mock.json` - Mock 指标数据
  - `backend/data/card_config.json` - 卡片配置

## 认证方式

当前没有实现认证和授权。

## API 通信方式

- 协议：HTTP/HTTPS
- 格式：JSON
- Content-Type：application/json
- CORS：未配置（当前前后端同域）

## 构建方式

无需构建，Python 直接运行，HTML 直接访问。

## 部署方式

Windows：运行 `start.bat`
手动：`python -m uvicorn backend.server:app --host 0.0.0.0 --port 8000`

## 目录分层

```
项目根目录/
├── lufax_dashboard3.html          # 主看板页面
├── ai_insights.html               # AI 洞察页面
├── start.bat                      # 启动脚本
├── .env                           # 环境变量
├── backend/
│   ├── server.py                  # FastAPI 入口
│   ├── sql/
│   │   └── init.sql               # 数据库初始化
│   ├── services/
│   │   ├── insight_service.py     # 洞察服务
│   │   ├── fact_builder.py        # Fact Pack 构建
│   │   ├── rule_engine.py         # 规则引擎
│   │   ├── llm_adapter.py         # LLM 适配器
│   │   ├── mock_llm.py            # Mock LLM
│   │   ├── insight_repo.py        # 数据访问
│   │   └── insight_job_service.py # 任务服务
│   ├── prompts/
│   │   ├── card_insight_v2.txt
│   │   ├── dashboard_insight_v2.txt
│   │   └── scenario_insight_v2.txt
│   ├── schemas/
│   │   ├── card_insight.schema.json
│   │   ├── dashboard_insight.schema.json
│   │   └── scenario_insight.schema.json
│   └── data/
│       ├── insight.db
│       ├── dashboard_metrics_mock.json
│       └── card_config.json
├── .ai/                           # AI 索引目录（本目录）
└── 项目背景及工程介绍.md
```

## 模块依赖关系

```
HTML 页面
  ↓
FastAPI (server.py)
  ↓
insight_service.py
  ├─→ fact_builder.py (构建 Fact Pack)
  ├─→ llm_adapter.py (调用 LLM)
  │   └─→ mock_llm.py (或其他 Adapter)
  ├─→ rule_engine.py (规则判断)
  └─→ insight_repo.py (数据访问)
      └─→ SQLite
```

## 核心架构图

```
┌─────────────────────────────────────────────────────────┐
│                     前端展示层                            │
│  ┌──────────────────┐     ┌───────────────────────┐     │
│  │lufax_dashboard3  │     │  ai_insights.html     │     │
│  │ (主经营看板)     │     │  (动态解读+计算器)    │     │
│  └────────┬─────────┘     └───────────┬───────────┘     │
└───────────┼────────────────────────────┼─────────────────┘
            │                            │
            └────────────┬───────────────┘
                         │
┌────────────────────────┼────────────────────────────────┐
│                     后端 API 层                          │
│              ┌───────────────────┐                       │
│              │   FastAPI Server  │                       │
│              │   (server.py)     │                       │
│              └─────────┬─────────┘                       │
└────────────────────────┼────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
┌───────▼───────┐ ┌────▼──────┐ ┌───────▼───────┐
│ insight_job_  │ │insight_   │ │ scenario_      │
│ service.py    │ │service.py │ │ insight API    │
└───────┬───────┘ └────┬──────┘ └───────┬───────┘
        │               │               │
        └───────────────┼───────────────┘
                        │
┌───────────────────────┼───────────────────────────────┐
│                   业务逻辑层                           │
│  ┌─────────────────────┐  ┌───────────────────────┐  │
│  │ fact_builder.py     │  │ rule_engine.py        │  │
│  │ (构建 Fact Pack)    │  │ (规则判断)            │  │
│  └──────────┬──────────┘  └───────────┬───────────┘  │
│             │                         │               │
│  ┌──────────▼───────────┐             │               │
│  │ insight_repo.py      │             │               │
│  │ (数据访问)           │             │               │
│  └──────────┬───────────┘             │               │
└─────────────┼─────────────────────────┼───────────────┘
              │                         │
┌─────────────▼──────────┐  ┌───────────▼──────────────┐
│      数据存储层        │  │         AI 层            │
│  ┌─────────────────┐  │  │  ┌─────────────────────┐ │
│  │  SQLite         │  │  │  │ llm_adapter.py      │ │
│  │  (insight.db)   │  │  │  └──────────┬──────────┘ │
│  └─────────────────┘  │  │             │            │
│  ┌─────────────────┐  │  │  ┌──────────▼──────────┐ │
│  │ Mock 数据文件   │  │  │  │  mock_llm.py        │ │
│  │ (JSON)          │  │  │  │  (或真实 LLM)       │ │
│  └─────────────────┘  │  │  └─────────────────────┘ │
└────────────────────────┘  └──────────────────────────┘
```

## 核心设计原则

### 1. 计算和解释分离
- **后端/前端**负责确定性计算
- **LLM**只负责解释

### 2. Fact Pack 是唯一输入
LLM 不直接读原始数据，只读结构化 Fact Pack

### 3. 标准洞察 + 探索洞察
- standard_insight：稳定、可审计
- exploratory_insights：深度挖掘、带证据

### 4. JSON Schema 校验
LLM 输出必须通过 Schema 校验

### 5. 数字白名单
LLM 输出的数字必须来自 Fact Pack

## 关键设计决策

| 决策 | 原因 | 影响 |
|------|------|------|
| 不使用前端框架 | 保持简单，便于维护 | 前端功能受限 |
| 使用 SQLite | 轻量级，无需额外服务 | 不适合高并发 |
| 计算与解释分离 | LLM 不可控，计算必须确定 | 架构稍复杂 |
| Mock LLM | 本地开发方便 | 需要维护 Mock |
| 缓存洞察结果 | 减少 LLM 调用，提高响应速度 | 缓存一致性问题 |

## 环境变量

见 `.ai/CONFIG_AND_ENV.md`
