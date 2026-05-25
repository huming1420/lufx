# Tasks: Python FastAPI → Java Spring Boot 迁移

- [ ] Task 1: 创建 Maven 工程骨架
  - [ ] 1.1: 创建 pom.xml（Spring Boot 1.5.9.RELEASE + JDK 1.8 + MyBatis 1.3.x + PostgreSQL + Jackson + json-schema-validator）
  - [ ] 1.2: 创建 DashboardApplication.java 启动类
  - [ ] 1.3: 创建 application.properties（数据库连接、LLM 配置、端口 8000）
  - [ ] 1.4: 创建配置类：MyBatisConfig、AsyncConfig、LlmConfig、WebMvcConfig

- [ ] Task 2: 创建数据模型类
  - [ ] 2.1: 创建 Entity 类：InsightResult、InsightJob、InsightJobItem
  - [ ] 2.2: 创建 Request 类：CardInsightRequest、DashboardInsightRequest、ScenarioInsightRequest、CreateJobRequest、ManualOverrideRequest、UploadExcelRequest
  - [ ] 2.3: 创建 Response/DTO 类：HealthResponse、DashboardDataResponse、PageStateResponse、CardInsightDto、JobStatusDto、ScenarioInsightDto

- [ ] Task 3: 创建 PostgreSQL 初始化 SQL
  - [ ] 3.1: 创建 init_postgres.sql（3 张表 + 索引，SQLite→PostgreSQL 语法适配）

- [ ] Task 4: 创建 MyBatis Mapper
  - [ ] 4.1: 创建 InsightResultMapper.java 接口 + InsightResultMapper.xml
  - [ ] 4.2: 创建 InsightJobMapper.java 接口 + InsightJobMapper.xml
  - [ ] 4.3: 创建 InsightJobItemMapper.java 接口 + InsightJobItemMapper.xml

- [ ] Task 5: 创建工具类
  - [ ] 5.1: 创建 JsonUtils（JSON 解析/序列化/Markdown 清理/字段提取）
  - [ ] 5.2: 创建 ResourceUtils（classpath 资源读取）
  - [ ] 5.3: 创建 HashUtils（SHA-256/SHA-1 哈希计算）
  - [ ] 5.4: 创建 TimeUtils（ISO 时间戳生成、时间差计算）
  - [ ] 5.5: 创建 ValidationUtils（JSON Schema 校验 + fallback）

- [ ] Task 6: 创建 LLM 适配器
  - [ ] 6.1: 创建 LlmAdapter 接口
  - [ ] 6.2: 创建 MockLlmService（确定性 mock 输出，迁移 DIMENSION_MODEL 常量）
  - [ ] 6.3: 创建 MockLlmAdapter（委托 MockLlmService）
  - [ ] 6.4: 创建 ExternalLlmAdapter（OpenAI + PAIC Agents 协议，RestTemplate 实现）

- [ ] Task 7: 创建 RuleEngineService
  - [ ] 7.1: 迁移信号灯判定逻辑（_determine_status）
  - [ ] 7.2: 迁移维度评分逻辑（_score_dimension）
  - [ ] 7.3: 迁移根因排序逻辑（_rank_root_causes）
  - [ ] 7.4: 迁移情景测算逻辑（_compute_scenario）
  - [ ] 7.5: 迁移跨维度信号逻辑（_detect_cross_signals）
  - [ ] 7.6: 迁移规则结果生成（generate_rules）

- [ ] Task 8: 创建 FactBuilderService
  - [ ] 8.1: 迁移 format_display_value / format_display_yoy 等格式化方法
  - [ ] 8.2: 迁移 build_card_fact_pack 逻辑
  - [ ] 8.3: 迁移 build_dashboard_fact_pack 逻辑
  - [ ] 8.4: 迁移 build_scenario_fact_pack 逻辑
  - [ ] 8.5: 迁移 _normalize_scenario_inputs 简写映射

- [ ] Task 9: 创建 InsightService
  - [ ] 9.1: 迁移 generate_insight 主逻辑（LLM 调用 + Schema 校验 + 结果落库）
  - [ ] 9.2: 迁移 _validate_output（Schema 校验 + fallback）
  - [ ] 9.3: 迁移缓存读取和写入逻辑

- [ ] Task 10: 创建 InsightJobService
  - [ ] 10.1: 迁移 create_job 逻辑（任务创建 + 子项初始化）
  - [ ] 10.2: 迁移 execute_job 逻辑（ThreadPoolTaskExecutor + Semaphore 并发控制）
  - [ ] 10.3: 迁移重试和降级逻辑（mock fallback）
  - [ ] 10.4: 迁移 get_job 逻辑
  - [ ] 10.5: 迁移 _warm_default_scenario_cache 后台预热逻辑
  - [ ] 10.6: 迁移 page_state 聚合逻辑（含超时校准）

- [ ] Task 11: 创建 Controller 层
  - [ ] 11.1: 创建 HealthController（GET /api/health）
  - [ ] 11.2: 创建 DashboardController（GET /api/dashboard/data）
  - [ ] 11.3: 创建 InsightController（所有 /api/insights/* 路由 + POST /api/upload/excel）
  - [ ] 11.4: 创建 PageController（GET /lufax_dashboard3.html, GET /ai_insights.html, GET /favicon.ico）

- [ ] Task 12: 迁移资源文件
  - [ ] 12.1: 复制 prompts/*.txt 到 src/main/resources/prompts/
  - [ ] 12.2: 复制 schemas/*.json 到 src/main/resources/schemas/
  - [ ] 12.3: 复制 data/*.json 到 src/main/resources/data/
  - [ ] 12.4: 复制 lufax_dashboard3.html 和 ai_insights.html 到 src/main/resources/static/

- [ ] Task 13: 集成验证和修复
  - [ ] 13.1: 验证 Maven 构建（mvn clean compile）
  - [ ] 13.2: 验证 API 路径与 Python 一致
  - [ ] 13.3: 验证请求/响应字段与 Python 一致
  - [ ] 13.4: 验证 JDK 1.8 语法兼容性
  - [ ] 13.5: 验证 PostgreSQL SQL 可执行
  - [ ] 13.6: 验证 resources 文件可从 jar 包内读取

# Task Dependencies
- Task 1 → Task 2, 3, 4, 5（骨架先建）
- Task 2 → Task 4, 7, 8, 9, 10, 11（模型类先建）
- Task 3 → Task 4（SQL 先建，Mapper 依赖表结构）
- Task 5 → Task 6, 9（工具类先建）
- Task 6 → Task 9, 10（LLM 适配器先建）
- Task 7 → Task 8（规则引擎先建，FactBuilder 依赖规则结果）
- Task 8 → Task 9（FactBuilder 先建，InsightService 依赖 Fact Pack）
- Task 9 → Task 10（InsightService 先建，JobService 依赖）
- Task 4, 9, 10 → Task 11（Mapper + Service 先建，Controller 才能写）
- Task 12 可与 Task 2-11 并行
- Task 13 在所有其他 Task 完成后执行
