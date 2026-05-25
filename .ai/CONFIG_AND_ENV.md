# CONFIG_AND_ENV.md

## 配置与环境变量

### 一、环境变量

#### 1. .env 文件位置

位置：项目根目录（`c:\great\陆控\.env`）

**重要**：
- .env 文件**不要提交到 Git**
- 不要包含真实的 API Key
- 生产环境使用环境变量注入

---

#### 2. 当前环境变量

| 配置项 | 来源文件 | 用途 | 默认值 | 是否必需 | 注意事项 |
|--------|----------|------|--------|----------|----------|
| OPENAI_API_KEY | .env | LLM API Key | - | 否 | 敏感信息，不要提交 |
| OPENAI_BASE_URL | .env | LLM Base URL | - | 否 | - |
| OPENAI_MODEL | .env | LLM 模型名称 | - | 否 | - |

---

### 二、配置文件

#### 1. backend/data/card_config.json

位置：`backend/data/card_config.json`

用途：卡片配置

详细字段见 `.ai/FIELD_SCHEMA.md`

---

#### 2. backend/data/dashboard_metrics_mock.json

位置：`backend/data/dashboard_metrics_mock.json`

用途：Mock 指标数据

说明：
- 当前数据来源
- 后续替换为真实数据库

---

### 三、LLM 提供者配置

位置：`backend/services/llm_adapter.py`

通过环境变量 `LLM_PROVIDER` 选择：
- `mock`（默认）：使用 MockLLMAdapter
- 其他：待实现

---

### 四、数据库配置

位置：`backend/sql/init.sql`

数据库文件：`backend/data/insight.db`

说明：
- SQLite，无需额外配置
- 自动初始化
- 文件存储在 `backend/data/` 目录

---

### 五、服务器配置

位置：`start.bat`

启动命令：
```batch
python -m uvicorn backend.server:app --host 0.0.0.0 --port 8000
```

配置：
- Host: `0.0.0.0`
- Port: `8000`

---

### 六、常见环境变量不生效问题

#### 1. 检查 .env 文件位置

确保 .env 在项目根目录，不在 backend/ 目录。

#### 2. 检查 python-dotenv 是否安装

启动脚本会自动安装，但可以手动检查：
```bash
pip install python-dotenv
```

#### 3. 重启服务器

修改 .env 后必须重启服务器才能生效。

#### 4. 检查加载代码

位置：`backend/server.py` 开头：
```python
from dotenv import load_dotenv
load_dotenv(Path(__file__).resolve().parents[1] / ".env", override=True)
```

确保路径正确。

---

### 七、配置项检查清单

修改配置后检查：
- [ ] .env 文件没有提交到 Git
- [ ] 没有暴露真实 API Key
- [ ] 服务器已重启
- [ ] LLM 调用正常
- [ ] 数据库连接正常
