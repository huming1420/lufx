# MODULE_MAP.md

## 模块映射

### 1. server.py - FastAPI 服务器

#### 职责
- FastAPI 应用入口
- API 路由注册
- 前端 HTML 托管
- 健康检查
- 调用 insight_service

#### 涉及文件
- `backend/server.py`

#### 核心函数
- `health()` - `/api/health` 健康检查
- `dashboard_data()` - `/api/dashboard/data` 获取看板数据
- `insights_cards()` - `/api/insights/cards` 获取卡片洞察
- `insights_dashboard()` - `/api/insights/dashboard` 获取看板洞察
- `create_insight_job()` - `/api/insights/jobs` 创建洞察任务
- `get_insight_job()` - `/api/insights/jobs/{job_id}` 获取任务状态
- `card_insight_legacy()` - `/api/insights/card` 单卡片洞察
- `dashboard_insight_legacy()` - `/api/insights/dashboard` 看板洞察
- `scenario_insight()` - `/api/insights/scenario` 情景洞察
- `upload_excel()` - `/api/upload/excel` Excel 上传

#### 输入
- HTTP 请求（JSON Body）

#### 输出
- HTTP 响应（JSON）

#### 常见修改需求
- 新增 API 端点
- 修改请求/响应格式
- 添加中间件
- 修改 CORS 配置

#### 修改注意事项
- 保持 API 向后兼容
- 不要修改已有端点的请求/响应格式
- 错误处理使用 HTTPException
- 长时间任务使用 BackgroundTasks

---

### 2. insight_service.py - 洞察服务

#### 职责
- 洞察生成主流程
- Fact Pack 构建
- LLM 调用
- JSON Schema 校验
- 输出验证

#### 涉及文件
- `backend/services/insight_service.py`

#### 核心函数
- `get_dashboard_data()` - 获取看板数据
- `build_card_fact_pack()` - 构建卡片 Fact Pack
- `build_dashboard_fact_pack()` - 构建看板 Fact Pack
- `build_scenario_fact_pack()` - 构建情景 Fact Pack
- `generate_card_insight()` - 生成卡片洞察
- `generate_dashboard_insight()` - 生成看板洞察
- `generate_scenario_insight()` - 生成情景洞察
- `validation_status()` - 获取验证状态

#### 输入
- card_id, period, scenario_id, overrides

#### 输出
- 结构化洞察 JSON

#### 常见修改需求
- 修改验证逻辑
- 添加新的洞察类型
- 调整 Fact Pack 结构
- 修改 LLM 调用流程

#### 修改注意事项
- 不要移除 JSON Schema 校验
- Fact Pack 结构变化需要同步修改 Prompt
- 新增洞察类型需要对应 Schema

---

### 3. fact_builder.py - Fact Pack 构建

#### 职责
- 从数据文件加载指标
- 构建三类 Fact Pack
- 调用规则引擎
- 准备 LLM 输入

#### 涉及文件
- `backend/services/fact_builder.py`
- `backend/data/dashboard_metrics_mock.json`
- `backend/data/card_config.json`

#### 核心函数
- `load_dashboard_data()` - 加载看板数据
- `load_card_config()` - 加载卡片配置
- `build_card_fact_pack()` - 构建卡片 Fact Pack
- `build_dashboard_fact_pack()` - 构建看板 Fact Pack
- `build_scenario_fact_pack()` - 构建情景 Fact Pack

#### 输入
- card_id, period, scenario_id, overrides

#### 输出
- 结构化 Fact Pack

#### 常见修改需求
- 修改 Fact Pack 结构
- 添加新的指标
- 修改数据加载逻辑
- 对接真实数据库

#### 修改注意事项
- Fact Pack 结构变化需要同步修改 Prompt
- 保持字段命名一致
- 确保数据类型正确

---

### 4. llm_adapter.py - LLM 适配器

#### 职责
- 统一 LLM 接口
- 支持多种 LLM 提供者
- 处理认证和错误

#### 涉及文件
- `backend/services/llm_adapter.py`
- `backend/services/mock_llm.py`

#### 核心函数
- `get_llm_adapter()` - 获取 LLM 适配器
- `BaseLLMAdapter.generate_json()` - 生成 JSON

#### 输入
- prompt, fact_pack, schema

#### 输出
- 结构化 JSON

#### 常见修改需求
- 添加新的 LLM 适配器
- 修改现有适配器逻辑
- 调整错误处理

#### 修改注意事项
- 保持接口一致
- 不要暴露 API Key
- 添加适当的错误处理

---

### 5. insight_repo.py - 数据访问层

#### 职责
- SQLite 数据库操作
- 洞察结果缓存
- 任务管理
- 数据持久化

#### 涉及文件
- `backend/services/insight_repo.py`
- `backend/sql/init.sql`
- `backend/data/insight.db`

#### 核心函数
- `init_db()` - 初始化数据库
- `get_metric_version()` - 获取指标版本
- `get_current_prompt_version()` - 获取 Prompt 版本
- `find_insight_result()` - 查找洞察结果
- `upsert_insight_result()` - 更新/插入洞察结果
- `find_insight_results_by_type()` - 按类型查找洞察结果
- `get_job()` - 获取任务
- `get_job_items()` - 获取任务项
- `save_scenario_result()` - 保存情景结果

#### 输入
- SQL 查询参数

#### 输出
- 数据库查询结果

#### 常见修改需求
- 修改数据库 schema
- 添加新的查询方法
- 优化查询性能

#### 修改注意事项
- 修改 schema 需要更新 init.sql
- 使用参数化查询防止 SQL 注入
- 添加适当的索引

---

### 6. insight_job_service.py - 任务服务

#### 职责
- 批量洞察生成
- 任务状态管理
- 重试逻辑
- 进度追踪

#### 涉及文件
- `backend/services/insight_job_service.py`

#### 核心函数
- `create_insight_job()` - 创建洞察任务
- `get_job_status()` - 获取任务状态

#### 输入
- period, scope, card_ids, force_refresh

#### 输出
- job_id, status

#### 常见修改需求
- 添加新的任务类型
- 修改重试逻辑
- 调整任务调度

#### 修改注意事项
- 保持任务状态机一致
- 添加适当的错误处理
- 考虑并发场景

---

### 7. rule_engine.py - 规则引擎

#### 职责
- 红黄绿灯判断
- 异常标签生成
- 六维评分
- 根因排序

#### 涉及文件
- `backend/services/rule_engine.py`

#### 输入
- 指标数据

#### 输出
- 规则判断结果

#### 常见修改需求
- 修改评分规则
- 添加新的规则
- 调整阈值

#### 修改注意事项
- 规则变化需要同步修改 Prompt
- 保持逻辑确定性
- 添加适当的注释

---

### 8. lufax_dashboard3.html - 主看板页面

#### 职责
- 展示核心经营指标
- 渲染图表
- 展示卡片洞察
- 用户交互

#### 涉及文件
- `lufax_dashboard3.html`

#### 核心函数
- `loadCardInsights()` - 加载卡片洞察
- 各种图表渲染函数

#### 输入
- API 响应数据

#### 输出
- 渲染后的页面

#### 常见修改需求
- 修改 UI 样式
- 添加新的卡片
- 修改图表配置
- 添加新的交互

#### 修改注意事项
- 保持原有样式和交互
- 不要破坏 ECharts 图表
- 新增功能保持向后兼容

---

### 9. ai_insights.html - AI 洞察页面

#### 职责
- 展示动态解读
- 展示六维诊断
- 展示根因链
- 经营计算器
- 情景解读

#### 涉及文件
- `ai_insights.html`

#### 核心函数
- `loadAiInsights()` - 加载 AI 洞察
- `generateScenarioInsight()` - 生成情景解读
- `updatePredict()` - 更新预测计算

#### 输入
- API 响应数据

#### 输出
- 渲染后的页面

#### 常见修改需求
- 修改 UI 样式
- 修改计算器参数
- 添加新的解读维度

#### 修改注意事项
- 保持原有样式和交互
- 不要破坏前端确定性计算
- 计算器参数变化需要同步后端

---

### 10. prompts/ - Prompt 模板

#### 职责
- 定义 LLM 输入格式
- 指导 LLM 生成洞察
- 确保输出结构

#### 涉及文件
- `backend/prompts/card_insight_v2.txt`
- `backend/prompts/dashboard_insight_v2.txt`
- `backend/prompts/scenario_insight_v2.txt`

#### 输入
- Fact Pack

#### 输出
- Prompt 文本

#### 常见修改需求
- 修改 Prompt 措辞
- 添加新的要求
- 调整输出格式

#### 修改注意事项
- Prompt 变化需要同步修改 Schema
- 测试修改后的输出质量
- 保持输出结构稳定

---

### 11. schemas/ - JSON Schema

#### 职责
- 定义输出结构
- 验证 LLM 输出
- 确保类型正确

#### 涉及文件
- `backend/schemas/card_insight.schema.json`
- `backend/schemas/dashboard_insight.schema.json`
- `backend/schemas/scenario_insight.schema.json`

#### 输入
- LLM 输出 JSON

#### 输出
- 验证结果

#### 常见修改需求
- 添加新的字段
- 修改字段类型
- 调整验证规则

#### 修改注意事项
- Schema 变化需要同步修改 Prompt
- 保持向后兼容
- 添加适当的注释

---

## 模块依赖关系图

```
lufax_dashboard3.html  ai_insights.html
        |                     |
        └──────────┬──────────┘
                   │
              server.py
                   │
         ┌─────────┼─────────┐
         │         │         │
   insight_service  insight_repo
         │              │
   fact_builder    insight_job_service
         │
   rule_engine
         │
   llm_adapter
      │
   mock_llm
```

## 核心数据流向

```
HTML → FastAPI → insight_service → fact_builder
                                      ↓
                                 rule_engine
                                      ↓
                                 fact_builder
                                      ↓
                                 llm_adapter
                                      ↓
                                 LLM
                                      ↓
                                 insight_service
                                      ↓ (Schema validate)
                                 FastAPI
                                      ↓
                                 HTML
```
