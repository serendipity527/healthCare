# application.yml 配置说明

## 📋 配置文件位置
`src/main/resources/application.yml`

## 🔧 配置项详解

### 1. LangChain4j AI 配置

#### 基本配置
```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: sk-43070f4cd1074965a93a03d6d5333cd8
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      model-name: qwen-flash
```

| 配置项 | 说明 | 默认值 | 示例 |
|--------|------|--------|------|
| `api-key` | API密钥 | - | `sk-xxx` |
| `base-url` | API端点地址 | `https://api.openai.com/v1` | 阿里云地址 |
| `model-name` | 模型名称 | `gpt-4` | `qwen-flash` |

#### 高级配置
```yaml
langchain4j:
  open-ai:
    chat-model:
      temperature: 0.7
      timeout: 60s
      max-retries: 3
      log-requests: true
      log-responses: true
```

| 配置项 | 说明 | 范围/格式 | 建议值 |
|--------|------|-----------|--------|
| `temperature` | 随机性控制 | 0-2 | 0.7（平衡）|
| `timeout` | 超时时间 | 如 `60s`, `2m` | `60s` |
| `max-retries` | 重试次数 | 0-10 | 3 |
| `log-requests` | 记录请求 | true/false | 开发环境 true |
| `log-responses` | 记录响应 | true/false | 开发环境 true |

### 2. HealthCare 应用配置

#### 对话记忆配置
```yaml
healthcare:
  chat:
    memory:
      max-messages: 10
    session-timeout: 30m
```

| 配置项 | 说明 | 默认值 | 建议范围 |
|--------|------|--------|----------|
| `max-messages` | 保留消息数 | 10 | 5-20 |
| `session-timeout` | 会话超时 | 30m | 15m-60m |

#### 紧急关键词配置
```yaml
healthcare:
  emergency:
    keywords: 胸痛,心脏疼,呼吸困难,大出血,昏迷,窒息,中风,失明
```

**说明**: 逗号分隔的关键词列表，用于识别紧急医疗情况（阶段2会用到）

### 3. 服务器配置
```yaml
server:
  port: 8080
```

### 4. 日志配置
```yaml
logging:
  level:
    root: INFO
    com.yihu.agent: DEBUG
    dev.langchain4j: DEBUG
```

| 日志级别 | 说明 | 适用场景 |
|----------|------|----------|
| `TRACE` | 最详细 | 深度调试 |
| `DEBUG` | 调试信息 | 开发环境 |
| `INFO` | 一般信息 | 生产环境 |
| `WARN` | 警告信息 | 生产环境 |
| `ERROR` | 错误信息 | 所有环境 |

## 🌍 多环境配置

### 开发环境 (application-dev.yml)
```yaml
langchain4j:
  open-ai:
    chat-model:
      log-requests: true
      log-responses: true
      timeout: 120s  # 开发环境可以更长

logging:
  level:
    com.yihu.agent: DEBUG
```

### 生产环境 (application-prod.yml)
```yaml
langchain4j:
  open-ai:
    chat-model:
      log-requests: false
      log-responses: false
      timeout: 30s   # 生产环境更严格

logging:
  level:
    root: WARN
    com.yihu.agent: INFO
```

**激活方式**:
```bash
# 开发环境
java -jar app.jar --spring.profiles.active=dev

# 生产环境
java -jar app.jar --spring.profiles.active=prod
```

## 🔐 安全最佳实践

### 1. 敏感信息外部化
**不推荐** (直接写在配置文件):
```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: sk-43070f4cd1074965a93a03d6d5333cd8  # ❌ 容易泄露
```

**推荐** (使用环境变量):
```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: ${LANGCHAIN_API_KEY}  # ✅ 从环境变量读取
```

设置环境变量:
```powershell
# Windows PowerShell
$env:LANGCHAIN_API_KEY="sk-43070f4cd1074965a93a03d6d5333cd8"
```

```bash
# Linux/Mac
export LANGCHAIN_API_KEY="sk-43070f4cd1074965a93a03d6d5333cd8"
```

### 2. 使用 Spring Cloud Config
对于生产环境，建议使用配置中心管理敏感信息。

## 🚀 支持的 AI 服务提供商

### OpenAI (官方)
```yaml
langchain4j:
  open-ai:
    chat-model:
      base-url: https://api.openai.com/v1
      model-name: gpt-4
      api-key: sk-your-openai-key
```

### 阿里云通义千问 (DashScope)
```yaml
langchain4j:
  open-ai:
    chat-model:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      model-name: qwen-flash  # 或 qwen-plus, qwen-max
      api-key: sk-your-dashscope-key
```

**可用模型**:
- `qwen-flash`: 快速响应，适合实时对话
- `qwen-plus`: 平衡性能和质量
- `qwen-max`: 最高质量，较慢
- `qwen-turbo`: 高性价比

### Azure OpenAI
```yaml
langchain4j:
  open-ai:
    chat-model:
      base-url: https://your-resource.openai.azure.com/
      model-name: gpt-4
      api-key: your-azure-key
```

## 📝 配置模板参考

### 最小配置（仅必需项）
```yaml
spring:
  application:
    name: healthCare

langchain4j:
  open-ai:
    chat-model:
      api-key: your-api-key
      base-url: your-api-endpoint
      model-name: your-model
```

### 完整配置（所有选项）
```yaml
spring:
  application:
    name: healthCare

langchain4j:
  open-ai:
    chat-model:
      api-key: ${LANGCHAIN_API_KEY}
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      model-name: qwen-flash
      temperature: 0.7
      timeout: 60s
      max-retries: 3
      log-requests: true
      log-responses: true

healthcare:
  chat:
    memory:
      max-messages: 10
    session-timeout: 30m
  emergency:
    keywords: 胸痛,心脏疼,呼吸困难,大出血,昏迷,窒息,中风,失明,心肌梗死,休克,抽搐,吐血,咳血

server:
  port: 8080

logging:
  level:
    root: INFO
    com.yihu.agent: DEBUG
    dev.langchain4j: DEBUG
```

## 🔍 故障排查

### 问题1: API Key 无效
**症状**: 启动失败或调用时报 401 错误

**检查**:
1. API Key 是否正确配置
2. API Key 是否有效（未过期）
3. API Key 是否有足够的配额

### 问题2: 连接超时
**症状**: 请求一直等待，最后超时

**解决**:
1. 增加 `timeout` 配置
2. 检查网络连接
3. 确认 `base-url` 地址正确

### 问题3: 模型不存在
**症状**: 报错模型名称无效

**解决**:
1. 确认 `model-name` 拼写正确
2. 检查该模型是否在当前服务商可用
3. 查看服务商文档确认模型名称

## 📚 相关文档

- [LangChain4j 配置文档](https://docs.langchain4j.dev/)
- [Spring Boot 配置文档](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
- [阿里云 DashScope 文档](https://help.aliyun.com/zh/dashscope/)

---

**最后更新**: 2025-11-04

