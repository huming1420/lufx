# DEPLOYMENT.md

## 部署

### 一、启动命令

#### 1. Windows（推荐）

使用启动脚本：
```batch
start.bat
```

脚本会：
1. 检查 Python
2. 安装依赖（fastapi, uvicorn, python-dotenv）
3. 启动服务器
4. 自动打开浏览器

---

#### 2. 手动启动

```bash
# 安装依赖
pip install fastapi uvicorn python-dotenv

# 启动服务器
python -m uvicorn backend.server:app --host 0.0.0.0 --port 8000
```

---

### 二、构建命令

**无需构建**：
- Python 代码直接运行
- HTML 文件直接访问

---

### 三、测试命令

当前无自动化测试。

---

### 四、Docker 方式

当前无 Docker 配置。

---

### 五、docker-compose 方式

当前无 docker-compose 配置。

---

### 六、Nginx 方式

当前无 Nginx 配置。

---

### 七、CI/CD 方式

当前无 CI/CD 配置。

---

### 八、静态资源构建方式

**无需构建**：
- HTML、CSS、JS 直接使用
- ECharts 使用 CDN

---

### 九、日志位置

当前无日志文件配置，日志输出到控制台。

---

### 十、常见部署失败原因

#### 1. Python 未安装

错误：`python not found`

解决：安装 Python 3.12+，添加到 PATH

---

#### 2. 端口被占用

错误：`Address already in use`

解决：
- 修改 start.bat 中的端口号
- 或关闭占用 8000 端口的程序

---

#### 3. 依赖安装失败

错误：`pip install failed`

解决：
- 检查网络连接
- 换 pip 源
- 手动安装依赖

---

#### 4. .env 文件缺失

错误：环境变量未加载

解决：复制 .env.example（如果有）或手动创建 .env

---

#### 5. 数据库文件权限

错误：`database is locked` 或 `permission denied`

解决：
- 检查 `backend/data/` 目录权限
- 确保没有其他进程打开 insight.db

---

### 十一、页面不更新 / 缓存不更新排查路径

#### 1. 前端缓存

硬刷新：
- Windows: `Ctrl + F5`
- Mac: `Cmd + Shift + R`

---

#### 2. 后端洞察缓存

使用 `force_refresh=true` 参数：
- 调用 `POST /api/insights/jobs` 时设置 `force_refresh=true`
- 或调用 `POST /api/insights/card` 时会重新生成（如果缓存不存在）

---

#### 3. 清除数据库缓存

使用 `clear_cache.py`（如果有）或：
- 删除 `backend/data/insight.db`
- 重启服务器（会自动重新初始化）

---

#### 4. 检查数据版本

确保 `metric_version` 和 `prompt_version` 正确。

---

### 十二、部署检查清单

部署后检查：
- [ ] 服务器正常启动
- [ ] 访问 http://localhost:8000/lufax_dashboard3.html 正常
- [ ] 访问 http://localhost:8000/ai_insights.html 正常
- [ ] /api/health 返回正常
- [ ] 数据库文件已创建
- [ ] 洞察生成正常（测试 Mock LLM）
- [ ] 没有暴露 API Key
