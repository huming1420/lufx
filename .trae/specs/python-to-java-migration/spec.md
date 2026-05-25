# Python FastAPI → Java Spring Boot 迁移 Spec

## Why
当前项目后端由 Python FastAPI 实现，需要迁移为 Java Spring Boot 后端，保持原有前端页面可继续调用，不改变现有 API 路径、请求参数、响应字段和业务语义。

## What Changes
- 将 Python FastAPI 后端完整迁移为 Java Spring Boot 1.5.9 后端
- **BREAKING**: 数据库从 SQLite 迁移为 PostgreSQL 10+（表结构语义不变，SQL 语法需适配）
- 将 asyncio 异步模型迁移为 ThreadPoolTaskExecutor + Semaphore 并发模型
- 将 Python dict 动态类型迁移为 Java 强类型 Entity/DTO/Request/Response
- 将 Python 文件系统资源读取迁移为 Java classpath 资源读取
- 将 Python jsonschema 校验迁移为 Java json-schema-validator 校验
- LLM 适配器从 Python urllib 迁移为 Java RestTemplate
- 前端 HTML 无需修改，Java 后端必须完全兼容原 API 调用

## Impact
- Affected specs: ai-insight-optimization, prompt-v2-migration（业务逻辑保持一致，仅语言和框架变更）
- Affected code: backend/ 目录全部 Python 代码 → src/main/java/ 全新 Java 工程
- Affected data: backend/data/insight.db (SQLite) → PostgreSQL 数据库
- Affected config: .env → application.properties

## ADDED Requirements

### Requirement: Java Maven 工程骨架
系统 SHALL 提供标准 Maven 工程，使用 Spring Boot 1.5.9.RELEASE + JDK 1.8 + MyBatis 1.3.x。

#### Scenario: 工程构建
- **WHEN** 执行 `mvn clean package`
- **THEN** 成功生成可运行 jar 包

#### Scenario: 工程启动
- **WHEN** 执行 `java -jar target/*.jar` 或 `mvn spring-boot:run`
- **THEN** Spring Boot 应用在 8000 端口启动成功

---

### Requirement: API 路径和响应兼容
系统 SHALL 保持以下 API 路径、HTTP 方法、请求参数、响应 JSON 结构与原 Python 后端完全一致：

| HTTP 方法 | 路径 | 说明 |
|-----------|------|------|
| GET | /api/health | 健康检查 |
| GET | /api/dashboard/data | 仪表盘数据 |
| GET | /api/insights/page-state | 洞察页面状态 |
| GET | /api/insights/cards | 卡片洞察列表 |
| GET | /api/insights/dashboard | 仪表盘洞察 |
| POST | /api/insights/jobs | 创建洞察任务 |
| GET | /api/insights/jobs/{job_id} | 查询任务状态 |
| POST | /api/insights/card | 单卡片洞察（旧接口） |
| POST | /api/insights/dashboard | 仪表盘洞察（旧接口） |
| POST | /api/insights/scenario | 情景洞察 |
| POST | /api/insights/manual-override | 人工覆写 |
| POST | /api/upload/excel | Excel 上传触发 |
| GET | /lufax_dashboard3.html | 仪表盘页面 |
| GET | /ai_insights.html | AI 洞察页面 |
| GET | /favicon.ico | 网站图标 |

#### Scenario: 前端无感迁移
- **WHEN** 前端 HTML 文件不修改任何 API 调用逻辑
- **AND** Java 后端启动后替代 Python 后端
- **THEN** 前端所有功能正常工作，API 响应字段和值与 Python 后端一致

---

### Requirement: 数据库 PostgreSQL 迁移
系统 SHALL 将 SQLite 表迁移为 PostgreSQL 10+，保持表结构语义不变。

#### Scenario: SQL 语法适配
- **WHEN** 执行 init_postgres.sql
- **THEN** 成功创建 insight_result、insight_job、insight_job_item 三张表
- **AND** 主键、自增、唯一约束、索引正确创建
- **AND** datetime('now') 替换为 now()
- **AND** INSERT OR REPLACE 替换为 ON CONFLICT ... DO UPDATE
- **AND** INTEGER 布尔字段替换为 BOOLEAN 或 SMALLINT

---

### Requirement: MyBatis 数据访问层
系统 SHALL 使用 MyBatis Mapper XML 实现 insight_result、insight_job、insight_job_item 的 CRUD 操作，优先使用 XML 便于维护 SQL。

#### Scenario: 缓存查询
- **WHEN** 调用 insight_result 的缓存查询（按 period + metric_version + prompt_version + insight_type + card_id）
- **THEN** 返回与 Python insight_repo.py 相同的缓存结果

#### Scenario: Scenario 缓存匹配
- **WHEN** 调用 find_matching_scenario_cache
- **THEN** 按 tolerance 0.02 容差匹配 inputs/computed/sensitivity 中的数值
- **AND** 匹配逻辑与 Python 完全一致

---

### Requirement: 计算和解释分离
系统 SHALL 保持计算与解释分离的业务原则。

#### Scenario: Fact Pack 是 LLM 唯一业务输入
- **WHEN** 调用 LLM 生成洞察
- **THEN** LLM 只接收 Fact Pack 作为业务输入
- **AND** LLM 不直接读取数据库
- **AND** LLM 不自行推导指标（预算差、同比、环比、ROA、不良率等）

---

### Requirement: JSON Schema 校验
系统 SHALL 保留 JSON Schema 校验，使用 card_insight.schema.json、dashboard_insight.schema.json、scenario_insight.schema.json 校验 LLM 输出。

#### Scenario: 校验通过
- **WHEN** LLM 输出通过 Schema 校验
- **THEN** 正常返回洞察结果

#### Scenario: 校验失败
- **WHEN** LLM 输出不通过 Schema 校验
- **THEN** 返回与原 Python 逻辑兼容的错误响应

---

### Requirement: 缓存机制
系统 SHALL 保留洞察结果缓存机制。

#### Scenario: 缓存命中
- **WHEN** 按 period + metric_version + prompt_version + insight_type + card_id 查询缓存
- **AND** 缓存存在且 force_refresh 不为 true
- **THEN** 直接返回缓存结果

#### Scenario: 强制刷新
- **WHEN** force_refresh=true
- **THEN** 绕过缓存重新生成洞察

---

### Requirement: 任务状态机
系统 SHALL 保留任务状态机：pending → running → finished/failed，以及 ready 状态。

#### Scenario: 任务生命周期
- **WHEN** 创建洞察任务
- **THEN** 任务状态为 pending
- **WHEN** 任务开始执行
- **THEN** 任务状态为 running
- **WHEN** 所有子项完成
- **THEN** 任务状态为 finished
- **WHEN** 任务执行出错
- **THEN** 任务状态为 failed

#### Scenario: 任务超时
- **WHEN** 任务运行超过 600 秒
- **THEN** get_page_state 中标记该任务为超时状态

---

### Requirement: LLM 适配器
系统 SHALL 提供 LlmAdapter 接口，至少包含 MockLlmAdapter 和 ExternalLlmAdapter 实现。

#### Scenario: Mock 模式
- **WHEN** 配置 llm.adapter=mock
- **THEN** 使用 MockLlmAdapter 生成确定性 mock 输出

#### Scenario: 外部 LLM 模式
- **WHEN** 配置 llm.adapter=external
- **THEN** 使用 ExternalLlmAdapter 调用外部 LLM API
- **AND** API Key 从 application.properties 或环境变量读取
- **AND** API Key 不写入代码
- **AND** API Key 不返回给前端

#### Scenario: PAIC Agents 协议
- **WHEN** 配置 llm.protocol=paic_agents
- **THEN** ExternalLlmAdapter 先登录获取 token，再创建 conversation，再发送消息

---

### Requirement: 并发控制
系统 SHALL 使用 ThreadPoolTaskExecutor + Semaphore 替代 Python asyncio，LLM 并发度默认为 2。

#### Scenario: 并发度控制
- **WHEN** 同时执行多个洞察生成任务
- **THEN** LLM 调用并发度不超过 llm.concurrency 配置值（默认 2）

---

### Requirement: 资源文件迁移
系统 SHALL 将 Prompt、Schema、Mock 数据文件迁移到 src/main/resources/ 下，Java 代码从 classpath 读取。

#### Scenario: jar 包内读取
- **WHEN** 应用打包为 jar 运行
- **THEN** 能正确读取 classpath 下的 prompts/*.txt、schemas/*.json、data/*.json

---

### Requirement: 静态文件服务
系统 SHALL 提供 lufax_dashboard3.html 和 ai_insights.html 的静态文件服务。

#### Scenario: 页面访问
- **WHEN** 访问 GET /lufax_dashboard3.html
- **THEN** 返回对应 HTML 文件内容
- **WHEN** 访问 GET /ai_insights.html
- **THEN** 返回对应 HTML 文件内容

---

## MODIFIED Requirements

### Requirement: 数据库连接
原 Python 使用 SQLite 文件数据库，Java SHALL 使用 PostgreSQL 数据库，连接信息从 application.properties 读取。

### Requirement: 配置管理
原 Python 使用 .env 文件 + os.environ，Java SHALL 使用 application.properties + @Value 注入，支持环境变量覆盖。

## REMOVED Requirements

### Requirement: SQLite 依赖
**Reason**: 迁移到 PostgreSQL
**Migration**: 所有 SQLite 特有语法（AUTOINCREMENT、datetime('now')、INSERT OR REPLACE、PRAGMA）替换为 PostgreSQL 等价写法

### Requirement: Python asyncio
**Reason**: Java 使用 ThreadPoolTaskExecutor
**Migration**: asyncio.Semaphore → java.util.concurrent.Semaphore，asyncio.gather → ExecutorService.invokeAll
