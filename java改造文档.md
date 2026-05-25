你是一名资深 Java 后端架构师。请将当前工程中的 Python 后端服务完整迁移为 Java 后端服务。

一、迁移目标

当前项目后端由 Python FastAPI 实现，主要代码位于 backend/ 目录。请将其迁移为 Java 后端，要求保持原有前端页面可继续调用，不改变现有 API 路径、请求参数、响应字段和业务语义。

目标技术栈必须严格使用：

- JDK：1.8
- Spring Boot：1.5.9.RELEASE
- Maven：3.6.2
- ORM/DAO：MyBatis
- 数据库：PostgreSQL 10+
- JSON 处理：Jackson
- HTTP 客户端：优先使用 RestTemplate 或 Java 8 兼容方案
- 不使用 Spring WebFlux
- 不使用高版本 Spring Boot 特性
- 不使用 JDK 9+ 语法
- 不引入 Kotlin、Scala、Lombok，除非明确说明必要性

二、当前 Python 后端需要迁移的核心能力

请重点阅读并迁移以下内容：

1. API 入口
- backend/server.py
- 迁移为 Spring Boot Controller
- 保持现有 API 路径、HTTP 方法、请求体、查询参数、响应 JSON 结构不变

2. 洞察服务主逻辑
- backend/services/insight_service.py
- 迁移为 Java Service
- 保留 JSON Schema 校验逻辑
- 保留缓存读取、LLM 调用、结果落库、错误处理流程

3. Fact Pack 构建逻辑
- backend/services/fact_builder.py
- 迁移为 Java Service
- 保留现有字段结构和计算结果
- 严禁让 LLM 重新计算指标
- LLM 只能基于 Fact Pack 做解释

4. 洞察任务服务
- backend/services/insight_job_service.py
- 迁移为 Java Service
- 当前 Python 使用后台任务处理批量洞察生成
- Java 端可用 ThreadPoolTaskExecutor 或 Spring @Async，但必须兼容 Spring Boot 1.5.9

5. 数据访问层
- backend/services/insight_repo.py
- backend/sql/init.sql
- 当前 SQLite 需要迁移为 PostgreSQL 10+
- 使用 MyBatis Mapper XML 或注解均可，但优先 Mapper XML，便于维护 SQL
- 保持表结构语义不变
- SQLite 语法需要转换为 PostgreSQL 语法

6. LLM 适配器
- backend/services/llm_adapter.py
- backend/services/mock_llm.py
- 迁移为 Java Interface + 实现类
- 至少保留 MockLLMAdapter
- 外部 LLM 配置从 application.properties 或环境变量读取
- 不允许把 API Key 写死在代码中
- 不允许将 API Key 返回给前端

7. Prompt 和 Schema 文件
- backend/prompts/*.txt
- backend/schemas/*.json
- 这些文件应迁移到 src/main/resources 下
- Java 代码从 classpath 读取
- 保持 Prompt 内容和 Schema 结构不变

8. Mock 数据文件
- backend/data/card_config.json
- backend/data/dashboard_metrics_mock.json
- 迁移到 src/main/resources/data/
- Java 后端读取方式需兼容打包后的 jar 运行

三、必须保持兼容的 API

请完整迁移以下接口，并保持路径、方法、入参、出参兼容：

- GET /api/health
- GET /api/dashboard/data
- GET /api/insights/page-state
- GET /api/insights/cards
- GET /api/insights/dashboard
- POST /api/insights/jobs
- GET /api/insights/jobs/{job_id}
- POST /api/insights/card
- POST /api/insights/dashboard
- POST /api/insights/scenario
- POST /api/insights/manual-override
- POST /api/upload/excel
- GET /lufax_dashboard3.html
- GET /ai_insights.html
- GET /favicon.ico

重要：前端 HTML 不应因为后端迁移而修改 API 调用逻辑。除非发现原前端调用存在明显错误，否则 Java 后端必须兼容原调用。

四、Java 工程结构要求

请生成标准 Maven 工程，建议结构如下：

src/main/java/com/lufax/dashboard/
  DashboardApplication.java

  controller/
    HealthController.java
    DashboardController.java
    InsightController.java
    PageController.java

  service/
    InsightService.java
    FactBuilderService.java
    InsightJobService.java
    RuleEngineService.java

  llm/
    LlmAdapter.java
    MockLlmAdapter.java
    ExternalLlmAdapter.java

  repository/
    InsightResultMapper.java
    InsightJobMapper.java
    InsightJobItemMapper.java

  model/
    request/
    response/
    entity/
    dto/

  config/
    MyBatisConfig.java
    AsyncConfig.java
    LlmConfig.java
    WebMvcConfig.java

  util/
    JsonUtils.java
    ResourceUtils.java
    HashUtils.java
    TimeUtils.java
    ValidationUtils.java

src/main/resources/
  application.properties
  mapper/
    InsightResultMapper.xml
    InsightJobMapper.xml
    InsightJobItemMapper.xml
  sql/
    init_postgres.sql
  prompts/
    card_insight_v2.txt
    dashboard_insight_v2.txt
    scenario_insight_v2.txt
  schemas/
    card_insight.schema.json
    dashboard_insight.schema.json
    scenario_insight.schema.json
  data/
    card_config.json
    dashboard_metrics_mock.json
  static/
    lufax_dashboard3.html
    ai_insights.html

五、数据库迁移要求

将 SQLite 表迁移为 PostgreSQL 10+：

- insight_result
- insight_job
- insight_job_item

要求：

1. 生成 PostgreSQL 初始化 SQL：src/main/resources/sql/init_postgres.sql
2. 主键、自增、唯一约束、索引必须保留
3. JSON 字段可以先用 TEXT 保存，避免过度改造
4. datetime('now') 需要改为 PostgreSQL 兼容写法，例如 now()
5. INSERT OR REPLACE 等 SQLite 写法必须改为 PostgreSQL ON CONFLICT
6. 需要给出 SQLite 到 PostgreSQL 的字段映射说明
7. 不要直接依赖原 insight.db 文件

六、pom.xml 版本约束

请生成完整 pom.xml，必须兼容以下版本：

- spring-boot-starter-parent：1.5.9.RELEASE
- java.version：1.8
- mybatis-spring-boot-starter：建议使用 1.3.x
- postgresql JDBC 驱动：兼容 PostgreSQL 10+
- jackson：使用 Spring Boot 1.5.9 默认兼容版本，除非必须覆盖
- json-schema-validator：选择 Java 8 可用版本，或提供明确替代方案

不要引入与 Spring Boot 1.5.9 明显冲突的高版本依赖。

七、业务原则必须保留

1. 计算和解释分离
- 后端/前端负责确定性计算
- LLM 只负责解释，不负责计算
- LLM 输出中的数字必须来自 Fact Pack
- 不得让 LLM 编造预算差、同比、环比、ROA、不良率等指标

2. Fact Pack 是 LLM 的唯一业务输入
- LLM 不直接读取原始数据库
- LLM 不直接读取前端页面
- LLM 不自行推导指标

3. JSON Schema 校验必须保留
- card_insight.schema.json
- dashboard_insight.schema.json
- scenario_insight.schema.json
- LLM 输出不通过 Schema 校验时，接口应返回与原 Python 逻辑兼容的错误响应

4. 缓存机制必须保留
- 洞察结果按 period、metric_version、prompt_version、insight_type、card_id 等维度缓存
- force_refresh=true 时应绕过缓存重新生成

5. 任务状态机必须保留
- pending
- running
- finished
- failed
- ready

八、迁移方式要求

请不要机械地逐行翻译 Python 代码，而是做等价的 Java 工程化迁移。

要求分三步执行：

第一步：阅读和梳理
输出：
- 当前 Python 后端模块清单
- API 清单
- 数据表清单
- 配置文件清单
- Prompt / Schema / Mock 数据清单
- Python 模块到 Java 类的映射表
- 可能存在的迁移风险

第二步：生成 Java 工程
输出并创建：
- pom.xml
- Spring Boot 启动类
- Controller
- Service
- Mapper
- Entity / DTO / Request / Response
- application.properties
- MyBatis XML
- PostgreSQL 初始化 SQL
- Resource 读取工具
- JSON Schema 校验工具
- LLM Adapter
- Mock LLM Adapter

第三步：校验和修复
完成后必须检查：
- 是否存在 JDK 1.8 不兼容语法
- 是否误用了 Spring Boot 2.x / 3.x 特性
- Maven 依赖是否和 Spring Boot 1.5.9 冲突
- API 路径是否与原 Python 一致
- 请求和响应字段是否与原 Python 一致
- PostgreSQL SQL 是否可执行
- MyBatis Mapper namespace / id / resultMap 是否正确
- resources 文件是否能在 jar 包中读取
- 前端 HTML 是否能继续访问原接口

九、输出格式要求

每次修改前，请先输出：

本次任务类型：
需要读取的文件：
预计新增文件：
预计修改文件：
是否影响 API：
是否影响数据库：
是否影响前端：
主要风险：

每次修改后，请输出：

实际新增文件：
实际修改文件：
核心修改点：
Python 到 Java 的对应关系：
数据库迁移说明：
API 兼容性说明：
配置项说明：
验证结果：
仍需人工确认的问题：

十、严禁事项

- 严禁升级到 Spring Boot 2.x 或 3.x
- 严禁使用 JDK 9+ 语法
- 严禁改变现有 API 路径
- 严禁改变前端请求字段
- 严禁删除 JSON Schema 校验
- 严禁让 LLM 负责计算指标
- 严禁把 API Key 写入代码
- 严禁将 API Key 返回给前端
- 严禁忽略 PostgreSQL 和 SQLite 的 SQL 差异
- 严禁只生成空壳 Controller，不迁移业务逻辑
- 严禁只写说明文档而不生成可运行代码

十一、最终交付物

最终必须交付：

1. 可运行的 Java Maven 工程
2. 完整 pom.xml
3. Spring Boot 1.5.9 启动类
4. Controller / Service / Mapper / DTO / Entity
5. PostgreSQL 初始化 SQL
6. application.properties 示例
7. Prompt、Schema、Mock 数据资源迁移
8. 与原 Python API 对齐的接口说明
9. Python 文件到 Java 文件的迁移映射表
10. 启动命令
11. 本地验证命令
12. 已知差异和风险清单

十二、启动与验证要求

请确保最终项目可以通过以下方式启动：

mvn clean package
java -jar target/*.jar

或：

mvn spring-boot:run

启动后至少验证：

GET /api/health
GET /api/dashboard/data
GET /api/insights/cards
POST /api/insights/card
POST /api/insights/dashboard
POST /api/insights/scenario
GET /lufax_dashboard3.html
GET /ai_insights.html

请优先保证后端 API 兼容和业务逻辑完整，再考虑代码美化。