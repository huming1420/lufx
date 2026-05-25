# AGENT_START_HERE.md

## 欢迎

这是陆金所控股经营分析看板项目的 AI 助手入口文档。

## 📋 任务执行流程

每次执行任务时，请严格按照以下步骤操作：

### 1. 先看 .ai/ROUTING.md
这是任务路由表，告诉您：
- 任务需要读哪些索引文件
- 需要看哪些源码文件
- 禁止读哪些文件

### 2. 再看 .ai/FILE_OWNERSHIP.md
这是关键文件所有权表，告诉您：
- 每个文件是做什么的
- 可以改哪些内容
- 禁止改哪些内容
- 关联哪些文件

### 3. 按需读取相关索引
根据您的任务类型，可能需要读取：
- `.ai/PROJECT_CONTEXT.md` - 了解项目背景
- `.ai/ARCHITECTURE.md` - 了解技术架构
- `.ai/MODULE_MAP.md` - 了解模块结构
- `.ai/DATA_FLOW.md` - 了解数据流
- `.ai/API_MAP.md` - 了解 API
- `.ai/FIELD_SCHEMA.md` - 了解字段结构
- `.ai/UI_RULES.md` - 了解 UI 规则
- `.ai/BUSINESS_RULES.md` - 了解业务规则
- `.ai/PROMPT_RULES.md` - 了解 Prompt
- `.ai/CONFIG_AND_ENV.md` - 了解配置
- `.ai/DEPLOYMENT.md` - 了解部署
- `.ai/TEST_RULES.md` - 了解测试
- `.ai/KNOWN_ISSUES.md` - 了解已知问题

### 4. 最后读取必要的源码
只读取您任务真正需要的源码文件，不要全项目扫描。

### 5. 修改前的输出
在开始修改代码前，请先输出：
```
本次任务类型：[类型]
需要读取的索引文件：[列表]
需要读取的源码文件：[列表]
预计修改的文件：[列表]
是否影响字段结构：[是/否]
是否影响 API：[是/否]
是否影响 UI：[是/否]
是否影响配置：[是/否]
是否影响部署：[是/否]
```

### 6. 修改后的输出
修改完成后，请输出：
```
实际修改的文件：[列表]
修改位置：[行号/函数名]
修改内容：[简要说明]
修改原因：[为什么改]
影响范围：[影响哪些模块]
验证结果：[测试/验证情况]
```

### 7. 更新任务日志
如果修改了代码，请更新 `.ai/TASK_LOG.md`，记录您的修改。

### 8. 更新索引文件
如果您的修改涉及：
- 字段变化 → 更新 `.ai/FIELD_SCHEMA.md`
- API 变化 → 更新 `.ai/API_MAP.md`
- UI 变化 → 更新 `.ai/UI_RULES.md`
- 业务规则变化 → 更新 `.ai/BUSINESS_RULES.md`
- Prompt 变化 → 更新 `.ai/PROMPT_RULES.md`
- 配置变化 → 更新 `.ai/CONFIG_AND_ENV.md`
- 部署变化 → 更新 `.ai/DEPLOYMENT.md`

## ⚠️ 重要约束

1. **不要全项目扫描**：只读取任务必需的文件
2. **不要编造文件**：索引文件只记录项目真实存在的内容
3. **不要破坏架构**：保持"计算和解释分离"原则
4. **不要让 LLM 计算**：LLM 只负责解释，不负责计算指标
5. **不要暴露密钥**：不要把真实的 API Key 提交到仓库
6. **不要改索引以外的**：除非明确需要，不要修改索引规则

## 📁 索引文件说明

| 文件 | 说明 | 何时看 |
|------|------|--------|
| `.ai/AGENT_START_HERE.md` | 本文档 | 每次任务 |
| `.ai/PROJECT_CONTEXT.md` | 项目上下文 | 首次接触项目 |
| `.ai/ARCHITECTURE.md` | 技术架构 | 技术决策时 |
| `.ai/MODULE_MAP.md` | 模块映射 | 修改模块时 |
| `.ai/FILE_OWNERSHIP.md` | 文件所有权 | 修改任何文件前 |
| `.ai/ROUTING.md` | 任务路由 | 每次任务开始 |
| `.ai/DATA_FLOW.md` | 数据流 | 数据相关任务 |
| `.ai/API_MAP.md` | API 映射 | API 相关任务 |
| `.ai/FIELD_SCHEMA.md` | 字段 Schema | 字段相关任务 |
| `.ai/UI_RULES.md` | UI 规则 | UI 相关任务 |
| `.ai/BUSINESS_RULES.md` | 业务规则 | 业务逻辑任务 |
| `.ai/PROMPT_RULES.md` | Prompt 规则 | Prompt 相关任务 |
| `.ai/CONFIG_AND_ENV.md` | 配置说明 | 配置相关任务 |
| `.ai/DEPLOYMENT.md` | 部署说明 | 部署相关任务 |
| `.ai/TEST_RULES.md` | 测试规则 | 测试相关任务 |
| `.ai/TASK_LOG.md` | 任务日志 | 修改代码后 |
| `.ai/KNOWN_ISSUES.md` | 已知问题 | 排查问题时 |


## 📚 深入阅读

- 详细项目说明请见 `项目背景及工程介绍.md`
- 代码位于 `backend/` 目录
- 前端页面位于根目录 `lufax_dashboard3.html` 和 `ai_insights.html`
