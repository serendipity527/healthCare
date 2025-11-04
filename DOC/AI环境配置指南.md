# AI 功能环境配置指南

## 1. OpenAI API Key 配置

AI 对话功能需要配置 OpenAI API Key 才能正常工作。有以下几种配置方式：

### 方式一：设置系统环境变量（推荐）

#### Windows (PowerShell)
```powershell
# 临时设置（仅当前会话有效）
$env:OPENAI_API_KEY="your-actual-api-key-here"

# 永久设置（需要管理员权限）
[System.Environment]::SetEnvironmentVariable('OPENAI_API_KEY', 'your-actual-api-key-here', 'User')
```

#### Windows (CMD)
```cmd
# 临时设置（仅当前会话有效）
set OPENAI_API_KEY=your-actual-api-key-here
```

#### Linux/Mac
```bash
# 临时设置（仅当前终端会话有效）
export OPENAI_API_KEY="your-actual-api-key-here"

# 永久设置（添加到 ~/.bashrc 或 ~/.zshrc）
echo 'export OPENAI_API_KEY="your-actual-api-key-here"' >> ~/.bashrc
source ~/.bashrc
```

### 方式二：在 application.properties 中直接配置（不推荐，仅用于开发测试）

修改 `src/main/resources/application.properties`：
```properties
langchain4j.open-ai.chat-model.api-key=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

⚠️ **警告**：不要将包含真实 API Key 的配置文件提交到 Git 仓库！

### 方式三：使用 application-local.properties（推荐用于本地开发）

1. 创建 `src/main/resources/application-local.properties` 文件
2. 添加以下内容：
```properties
langchain4j.open-ai.chat-model.api-key=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```
3. 在 `.gitignore` 中添加：
```
application-local.properties
```
4. 启动应用时指定 profile：
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## 2. 获取 OpenAI API Key

1. 访问 [OpenAI Platform](https://platform.openai.com/)
2. 注册/登录账号
3. 进入 [API Keys](https://platform.openai.com/api-keys) 页面
4. 点击 "Create new secret key" 创建新的 API Key
5. 复制并安全保存该 Key（只会显示一次）

## 3. 验证配置

启动应用后，查看日志中是否有 LangChain4j 相关的初始化信息：
```
INFO  : LangChain4j initialized successfully
INFO  : OpenAI chat model configured with model: gpt-4
```

## 4. 配置项说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `langchain4j.open-ai.chat-model.api-key` | OpenAI API Key | - |
| `langchain4j.open-ai.chat-model.model-name` | 使用的模型名称 | gpt-4 |
| `langchain4j.open-ai.chat-model.temperature` | 温度参数（0-2），越高越随机 | 0.7 |
| `langchain4j.open-ai.chat-model.timeout` | API 调用超时时间 | 60s |
| `langchain4j.open-ai.chat-model.max-retries` | 失败重试次数 | 3 |
| `healthcare.chat.memory.max-messages` | 保留的最大对话轮次 | 10 |
| `healthcare.chat.session-timeout` | 会话超时时间 | 30m |

## 5. 常见问题

### Q: API Key 配置后仍然报错 "Invalid API Key"
**A**: 检查以下几点：
- API Key 是否正确复制（没有多余空格）
- API Key 是否已激活且有可用额度
- 环境变量是否生效（重启 IDE 或终端）

### Q: 调用超时怎么办？
**A**: 可以增加超时时间：
```properties
langchain4j.open-ai.chat-model.timeout=120s
```

### Q: 想使用其他模型（如 GPT-3.5）怎么办？
**A**: 修改配置：
```properties
langchain4j.open-ai.chat-model.model-name=gpt-3.5-turbo
```

## 6. 安全建议

1. ✅ **使用环境变量**：避免在代码中硬编码 API Key
2. ✅ **添加 .gitignore**：确保不提交敏感配置文件
3. ✅ **定期轮换**：定期更换 API Key
4. ✅ **设置使用限额**：在 OpenAI 控制台设置月度使用限额
5. ❌ **不要共享**：不要将 API Key 分享给他人或公开发布

## 7. 下一步

配置完成后，可以继续进行：
- [阶段 1: MVP 开发](./AI_Development_Manual_V1.md#阶段-1-mvp---基础链路打通)
- 运行测试验证环境配置是否成功

