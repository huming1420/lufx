# Checklist: Python FastAPI → Java Spring Boot 迁移

## 工程骨架
- [ ] pom.xml 使用 Spring Boot 1.5.9.RELEASE parent
- [ ] pom.xml java.version=1.8
- [ ] pom.xml 包含 mybatis-spring-boot-starter 1.3.x
- [ ] pom.xml 包含 postgresql JDBC 驱动
- [ ] pom.xml 包含 json-schema-validator (networknt 1.0.x)
- [ ] pom.xml 无 JDK 9+ 依赖冲突
- [ ] DashboardApplication.java 可正常启动
- [ ] application.properties 包含数据库连接配置
- [ ] application.properties 包含 LLM 配置项
- [ ] server.port=8000

## API 兼容性
- [ ] GET /api/health 返回与 Python 一致的 JSON 结构
- [ ] GET /api/dashboard/data 返回与 Python 一致的 JSON 结构
- [ ] GET /api/insights/page-state?period= 返回与 Python 一致
- [ ] GET /api/insights/cards?period= 返回与 Python 一致
- [ ] GET /api/insights/dashboard?period= 返回与 Python 一致
- [ ] POST /api/insights/jobs 请求/响应与 Python 一致
- [ ] GET /api/insights/jobs/{job_id} 返回与 Python 一致
- [ ] POST /api/insights/card 请求/响应与 Python 一致
- [ ] POST /api/insights/dashboard 请求/响应与 Python 一致
- [ ] POST /api/insights/scenario 请求/响应与 Python 一致
- [ ] POST /api/insights/manual-override 请求/响应与 Python 一致
- [ ] POST /api/upload/excel 请求/响应与 Python 一致
- [ ] GET /lufax_dashboard3.html 返回 HTML
- [ ] GET /ai_insights.html 返回 HTML
- [ ] GET /favicon.ico 返回 204

## 数据库
- [ ] init_postgres.sql 可在 PostgreSQL 10+ 执行
- [ ] insight_result 表包含唯一约束 (period, metric_version, prompt_version, insight_type, card_id)
- [ ] insight_result 表使用 ON CONFLICT ... DO UPDATE
- [ ] insight_job 表结构正确
- [ ] insight_job_item 表包含外键
- [ ] 索引正确创建
- [ ] 无 SQLite 特有语法残留

## MyBatis
- [ ] InsightResultMapper namespace 正确
- [ ] InsightResultMapper.xml 包含 upsert (INSERT ON CONFLICT)
- [ ] InsightResultMapper.xml 包含缓存查询
- [ ] InsightResultMapper.xml 包含 scenario 缓存匹配查询
- [ ] InsightJobMapper namespace 正确
- [ ] InsightJobItemMapper namespace 正确
- [ ] resultMap 字段映射正确

## 业务逻辑
- [ ] RuleEngineService 信号灯判定与 Python 一致
- [ ] RuleEngineService 维度评分与 Python 一致
- [ ] RuleEngineService 根因排序与 Python 一致
- [ ] RuleEngineService 情景测算与 Python 一致
- [ ] FactBuilderService 三种 Fact Pack 构建与 Python 一致
- [ ] FactBuilderService format_display_value 与 Python 一致
- [ ] InsightService LLM 调用流程与 Python 一致
- [ ] InsightService JSON Schema 校验保留
- [ ] InsightService 缓存读取逻辑与 Python 一致
- [ ] InsightJobService 任务状态机正确 (pending/running/finished/failed/ready)
- [ ] InsightJobService 并发控制 (Semaphore=2)
- [ ] InsightJobService 重试降级逻辑正确
- [ ] InsightJobService page_state 聚合含超时校准

## LLM 适配器
- [ ] LlmAdapter 接口定义正确
- [ ] MockLlmAdapter 生成确定性输出
- [ ] ExternalLlmAdapter 支持 OpenAI 协议
- [ ] ExternalLlmAdapter 支持 PAIC Agents 协议
- [ ] API Key 从配置/环境变量读取
- [ ] API Key 不在代码中硬编码
- [ ] API Key 不返回给前端

## 资源文件
- [ ] prompts/card_insight_v2.txt 在 classpath 可读
- [ ] prompts/dashboard_insight_v2.txt 在 classpath 可读
- [ ] prompts/scenario_insight_v2.txt 在 classpath 可读
- [ ] schemas/card_insight.schema.json 在 classpath 可读
- [ ] schemas/dashboard_insight.schema.json 在 classpath 可读
- [ ] schemas/scenario_insight.schema.json 在 classpath 可读
- [ ] data/card_config.json 在 classpath 可读
- [ ] data/dashboard_metrics_mock.json 在 classpath 可读
- [ ] lufax_dashboard3.html 可通过 GET 访问
- [ ] ai_insights.html 可通过 GET 访问

## 安全和合规
- [ ] 无 JDK 9+ 语法（var、record、sealed class 等）
- [ ] 无 Spring Boot 2.x/3.x 特性
- [ ] 无 Lombok 依赖
- [ ] scenario hash 计算与 Python 一致（JSON sort_keys 行为）
- [ ] metric_version hash 计算与 Python 一致
- [ ] 浮点容差比较使用 BigDecimal 或容差 0.02
