# PROMPT_RULES.md

## Prompt 规则

### 一、Prompt 文件位置

| 文件名 | 路径 | 用途 |
|--------|------|------|
| `card_insight_v2.txt` | `backend/prompts/card_insight_v2.txt` | 单卡片洞察 Prompt |
| `dashboard_insight_v2.txt` | `backend/prompts/dashboard_insight_v2.txt` | 看板动态解读 Prompt |
| `scenario_insight_v2.txt` | `backend/prompts/scenario_insight_v2.txt` | 经营计算器情景解读 Prompt |

---

### 二、Schema 文件位置

| 文件名 | 路径 | 用途 |
|--------|------|------|
| `card_insight.schema.json` | `backend/schemas/card_insight.schema.json` | 卡片洞察 Schema |
| `dashboard_insight.schema.json` | `backend/schemas/dashboard_insight.schema.json` | 看板洞察 Schema |
| `scenario_insight.schema.json` | `backend/schemas/scenario_insight.schema.json` | 情景洞察 Schema |

---

### 三、模型调用入口

#### 1. LLM 适配器选择

位置：`backend/services/llm_adapter.py`

根据环境变量 `LLM_PROVIDER` 选择适配器：
- `mock`（默认）：MockLLMAdapter
- 其他：待实现

---

#### 2. 调用流程

位置：`backend/services/insight_service.py`

流程：
1. 构建 Fact Pack（`fact_builder.py`）
2. 加载 Prompt
3. 加载 Schema
4. 调用 LLM Adapter
5. 验证输出
6. 返回结果

核心函数：
- `generate_card_insight()`
- `generate_dashboard_insight()`
- `generate_scenario_insight()`

---

### 四、输入字段（Fact Pack）

#### 1. CARD_FACT_PACK（卡片洞察）

由 `backend/services/fact_builder.build_card_fact_pack()` 构建

包含：
- period
- card_id
- card_config
- metric_data
- rule_result

---

#### 2. DASHBOARD_FACT_PACK（看板洞察）

由 `backend/services/fact_builder.build_dashboard_fact_pack()` 构建

包含：
- period
- health_score
- health_label
- dupont（杜邦指标）
- dimension_scores（六维评分）
- root_causes（根因）
- offset_factors（抵消因素）
- rule_result

**重要约束**：
- `health_score`：**LLM 不得重新计算**，必须原样输出
- `dimension_scores[].score`：**LLM 不得重新计算**，必须原样输出
- `dimension_scores[].status`：**LLM 不得随意改变**，必须沿用
- `root_causes`：**LLM 不得改变后端排序**

---

#### 3. SCENARIO_FACT_PACK（情景洞察）

由 `backend/services/fact_builder.build_scenario_fact_pack()` 构建

包含：
- period
- scenario
- scenario_label
- inputs（计算器参数）
- computed（计算结果）
- sensitivity（敏感性分析）

**重要约束**：
- 所有计算结果由前端/后端完成
- **LLM 不得重新计算** ROA、净利润等指标
- LLM 只负责解释已经计算好的结果

---

### 五、输出字段

详见 `backend/schemas/*.schema.json` 和 `.ai/FIELD_SCHEMA.md`

---

### 六、输出格式要求

1. **必须是合法 JSON**
2. **不要输出 Markdown**
3. **不要输出 JSON 以外的任何内容**
4. **必须通过 JSON Schema 校验**
5. **所有数字必须来自 Fact Pack**

---

### 七、质量要求

#### 1. 标准洞察（standard_insight / standard_diagnosis / standard_explanation）

要求：
- 稳定、可审计、可直接展示
- 基于明确的事实和规则
- 不包含猜测性内容
- 长度符合 Schema 限制

---

#### 2. 探索性洞察（exploratory_insights / emerging_findings / scenario_exploration）

要求：
- 允许模型进行深度挖掘
- 必须绑定证据（supporting_facts）
- 必须标注置信度（confidence: high/medium/low）
- 必须列出验证项（validation_needed）
- 长度符合 Schema 限制

---

### 八、禁止生成内容

1. **禁止 LLM 计算任何指标**：
   - ROA
   - 净利润
   - 同比、环比
   - 预算差
   - 红黄绿灯
   - 六维评分
   - 利润贡献
   - 敏感性矩阵

2. **禁止编造数字**：
   - 行业均值
   - 预测值
   - 预算差
   - 红线
   - 任何不在 Fact Pack 中的数字

3. **禁止输出投资建议**

4. **禁止重新计算 health_score、dimension score**

5. **禁止改变 root_causes 排序**

6. **禁止随意改变 dimension status**

7. **dashboard_insight 禁止单独生成「关键矛盾识别」模块**：
   - 不要输出 key_conflicts
   - 不要单独罗列“规模 vs 风险”“成本改善 vs 利润承压”“转型推进 vs 历史包袱”
   - 这类判断只能自然嵌入到六维卡片、根因链、探索性发现中

---

### 九、Prompt 与前端/后端字段的对应关系

#### 1. card_insight

| Prompt 输入 | 来源 | 输出字段 | 前端使用 |
|------------|------|----------|----------|
| card_id | card_config.json | card_id | lufax_dashboard3.html |
| standard_insight | LLM | standard_insight | lufax_dashboard3.html |
| drivers | LLM | drivers | lufax_dashboard3.html |
| watch_items | LLM | watch_items | lufax_dashboard3.html |
| exploratory_insights | LLM | exploratory_insights | lufax_dashboard3.html |

---

#### 2. dashboard_insight

| Prompt 输入 | 来源 | 输出字段 | 前端使用 |
|------------|------|----------|----------|
| health_score | Fact Pack | health_score | ai_insights.html |
| health_label | Fact Pack | health_label | ai_insights.html |
| standard_diagnosis | LLM | standard_diagnosis | ai_insights.html |
| dimensions | LLM + Fact Pack | dimensions | ai_insights.html |
| root_cause_chain | LLM + Fact Pack | root_cause_chain | ai_insights.html |
| offset_factors | LLM + Fact Pack | offset_factors | ai_insights.html |
| emerging_findings | LLM | emerging_findings | ai_insights.html |

---

#### 3. scenario_insight

| Prompt 输入 | 来源 | 输出字段 | 前端使用 |
|------------|------|----------|----------|
| inputs | 前端请求 | - | - |
| computed | 前端/后端计算 | - | - |
| standard_explanation | LLM | standard_explanation | ai_insights.html |
| scenario_exploration | LLM | scenario_exploration | ai_insights.html |

---

### 十、修改 Prompt 后必须检查哪些文件

1. **对应的 Schema 文件**：
   - 修改 `card_insight_v2.txt` → 检查 `card_insight.schema.json`
   - 修改 `dashboard_insight_v2.txt` → 检查 `dashboard_insight.schema.json`
   - 修改 `scenario_insight_v2.txt` → 检查 `scenario_insight.schema.json`

2. **Fact Pack 构建逻辑**：
   - `backend/services/fact_builder.py`

3. **前端渲染逻辑**：
   - `lufax_dashboard3.html`（如果影响卡片洞察）
   - `ai_insights.html`（如果影响看板洞察或情景洞察）

4. **测试输出**：
   - 验证 LLM 输出是否符合 Schema
   - 验证前端是否正常渲染
   - 验证没有编造数字

---

### 十一、Prompt 版本管理

当前版本：`v2`

版本位置：
- Prompt 文件名包含版本（`_v2.txt`）
- 数据库 `prompt_version` 字段
- `backend/services/insight_service.py` 中硬编码

---

### 十二、数字白名单校验

当前未完全实现，但原则上：
- LLM 输出中的所有数字必须能在 Fact Pack 中找到
- 禁止编造任何数字
