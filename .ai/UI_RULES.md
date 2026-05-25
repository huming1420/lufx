# UI_RULES.md

## UI 规则

### 一、页面结构

#### 1. lufax_dashboard3.html（主经营看板）

页面结构：
- 顶部导航
- 核心指标卡片
- 各业务模块卡片
- 每个卡片包含图表和 AI 洞察

重要元素 ID：
- `insight-profit` - 净利润洞察
- `insight-scale` - 贷款规模洞察
- `insight-enr` - 余额结构洞察
- `insight-disburse` - 新增放款洞察
- `insight-roa` - ROA 拆解洞察
- `insight-pricing` - 定价洞察
- `insight-risk` - 风险成本洞察
- `insight-funding` - 资金成本洞察
- `insight-opex` - 业务成本洞察


---

#### 2. ai_insights.html（AI 经营洞察页）

页面结构：
- 顶部导航
- Tab 切换（动态解读 / 经营计算器）
- 动态解读 Tab：
  - 综合评分卡片
  - 六维诊断卡片网格
  - 深度分析框
  - 根因链
  - 关键抵消因素
  - 探索性发现
- 经营计算器 Tab：
  - 公式说明
  - 情景按钮
  - 参数滑块
  - 结果卡片
  - 敏感性表
  - AI 解读框

重要元素 ID：
- `ai-score-circle-wrap` - 评分圆环
- `ai-score-num` - 评分数字
- `ai-health-label` - 健康标签
- `ai-health-summary` - 健康摘要
- `ai-verdict-tag` - 结论标签
- `ai-summary-text` - 摘要文本
- `ai-dims-grid` - 六维卡片网格
- `ai-detail-box` - 深度分析框
- `ai-detail-icon` - 深度分析图标
- `ai-detail-dim-name` - 维度名称
- `ai-detail-role` - 角色标签
- `ai-detail-content` - 深度内容
- `ai-detail-next-validation` - 下一步验证
- `ai-cause-chain` - 根因链
- `ai-offset-factors` - 抵消因素
- `ai-emerging-box` - 探索性发现框
- `ai-emerging-findings` - 探索性发现

---

### 二、组件层级

#### 1. 六维诊断卡片

结构：
```
.ai-dim-card (active)
├── .ai-dim-top
│   ├── .ai-dim-name
│   └── .ai-dim-score-badge
├── .ai-dim-bar-wrap
│   └── .ai-dim-bar (width: X%)
└── .ai-dim-text
```

点击事件：切换激活状态，显示深度分析

---

#### 2. 探索性发现卡片

结构：
```
.emerging-item
├── .emerging-type (color: purple/red/yellow/orange/accent)
├── .emerging-finding
└── .emerging-meta
```

---

### 三、视觉风格

#### 1. 颜色规范

CSS 变量定义在 `:root`：

| 变量名 | 颜色值 | 用途 |
|--------|--------|------|
| `--bg` | `#0f1117` | 背景色 |
| `--surface` | `#1a1d27` | 表面色 |
| `--surface2` | `#22263a` | 表面色 2 |
| `--surface3` | `#2a2e44` | 表面色 3 |
| `--border` | `#2e3354` | 边框色 |
| `--accent` | `#4f8ef7` | 主色调（蓝） |
| `--accent2` | `#7c5cfc` | 主色调 2（紫） |
| `--green` | `#34d399` | 绿色 |
| `--red` | `#f87171` | 红色 |
| `--yellow` | `#fbbf24` | 黄色 |
| `--orange` | `#fb923c` | 橙色 |
| `--purple` | `#a78bfa` | 紫色 |
| `--text` | `#e2e8f0` | 文本色 |
| `--muted` | `#94a3b8` | 次要文本色 |

---

#### 2. 状态颜色映射

| 状态 | 颜色 | 说明 |
|------|------|------|
| `red` | `var(--red)` | 风险/警告 |
| `yellow` | `var(--yellow)` | 中性/观察 |
| `green` | `var(--green)` | 良好/健康 |
| `accent` | `var(--accent)` | 强调 |
| `purple` | `var(--purple)` | 特殊 |

---

#### 3. 字体规范

- 字体族：`"PingFang SC", "Microsoft YaHei", sans-serif`
- 无特殊字体大小规范，使用相对单位

---

#### 4. 间距规范

- 无特殊间距规范，使用相对单位

---

### 四、卡片/表格/表单/弹窗规范

#### 1. 评分圆环

SVG 实现：
- 背景圆环：stroke `var(--border)`
- 进度圆环：stroke `url(#scoreGrad)`
- 渐变：从 red 到 yellow 到 green
- stroke-dasharray: `188.5`
- stroke-dashoffset: 计算值

---

#### 2. 结论标签

三种样式：
- `.ai-verdict.good` - 绿色背景
- `.ai-verdict.warn` - 黄色背景
- `.ai-verdict.poor` - 红色背景

---

#### 3. 情景按钮

三种样式：
- `.ai-sc-btn.bear` - 悲观（红）
- `.ai-sc-btn.base` - 基准（黄）
- `.ai-sc-btn.bull` - 乐观（绿）

激活状态：
- 加深饱和度
- 实边框
- 阴影

---

#### 4. 维度角色标签

三种样式：
- `.ai-dim-role.negative` - 红色
- `.ai-dim-role.neutral` - 黄色
- `.ai-dim-role.positive` - 绿色

---

#### 5. 探索发现类型标签

六种样式：
- `.emerging-type.contradiction` - 紫色
- `.emerging-type.hidden_risk` - 红色
- `.emerging-type.quality_issue` - 黄色
- `.emerging-type.offset_failure` - 橙色
- `.emerging-type.structural_shift` - 蓝色
- `.emerging-type.second_order_effect` - 紫色

---

### 五、响应式规则

- `.ai-interpret-wrap`: 大屏 2 列，小屏 1 列
- `.ai-dims-grid`: 大屏 3 列，中屏 2 列
- `.ai-pred-layout`: 大屏 2 列，小屏 1 列
- `.ai-kpi-row`: 大屏 3 列，小屏 1 列

---

### 六、加载态规则

当前无特殊加载态实现。

---

### 七、空状态规则

当前无特殊空状态实现。

---

### 八、错误态规则

当前无特殊错误态实现。

---

### 九、禁止出现的 UI 问题

1. **不要破坏原有 UI 样式**：保持现有 CSS 类和结构
2. **不要删除 ECharts 图表逻辑**：图表是核心功能
3. **不要破坏前端确定性计算**：经营计算器的计算逻辑必须保留
4. **不要随意改变 ID**：前端通过 ID 定位元素
5. **不要改变 CSS 变量定义**：颜色规范统一

---

### 十、常见 UI 修改应该改哪些文件

| 修改需求 | 修改文件 | 注意事项 |
|----------|----------|----------|
| 修改页面文案 | `lufax_dashboard3.html`, `ai_insights.html` | 只改静态文案，动态文案来自 API |
| 修改颜色 | `ai_insights.html`（CSS 变量） | 保持颜色语义一致 |
| 添加新卡片 | `lufax_dashboard3.html`, `backend/data/card_config.json` | HTML 和配置同步 |
| 修改图表 | `lufax_dashboard3.html`（ECharts 配置） | 不要删除现有图表 |
| 修改计算器参数 | `ai_insights.html`, `backend/services/fact_builder.py` | 前后端参数一致 |
| 添加新 Tab | `ai_insights.html` | 保持 Tab 切换逻辑 |

---

### 十一、前端 API 调用规范

1. 使用 `fetch` API
2. 使用相对路径（不要硬编码域名）
3. Content-Type: `application/json`
4. 错误处理：检查 `response.ok`
5. 不要在前端暴露 LLM API Key

---

### 十二、前端计算规范

**重要**：经营计算器的所有计算必须在前端完成，LLM 只负责解释计算结果。

计算涉及的字段：
- `roa = pricing_rate - credit_loss_rate - funding_cost_rate - sales_cost_rate - opex_rate - tax_other_rate`
- `net_profit = anr × roa / 100`

不要修改或删除这些计算逻辑。
