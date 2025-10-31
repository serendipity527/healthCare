# STOMP WebSocket 连接测试指南

## 🔧 修复内容总结

### 后端修改
✅ **StompWebSocketConfig.java**
- 添加了 `.setAllowedOrigins("*")` 允许跨域访问
- 确保了 SockJS 支持已启用

### 前端修改
✅ **index.html - STOMP 连接部分**
- 修正了订阅频道：`/topic/messages`（与后端 `@SendTo` 一致）
- 修正了发送目的地：`/app/chat`（与后端 `@MessageMapping` 一致）
- 修正了消息格式：`{sender, content}`（与后端 `SimpleMessage` 一致）
- 添加了详细的调试日志
- 增强了错误处理

## 📋 配置对应关系

| 配置项 | 后端 | 前端 | 状态 |
|--------|------|------|------|
| **STOMP 端点** | `/ws/chat-stomp` | `http://localhost:8080/ws/chat-stomp` | ✅ |
| **应用前缀** | `/app` | `/app/chat` | ✅ |
| **订阅频道** | `/topic/messages` | `/topic/messages` | ✅ |
| **消息格式** | `SimpleMessage(sender, content)` | `{sender, content}` | ✅ |
| **响应格式** | `SimpleResponse(content)` | `{content}` | ✅ |

## 🚀 测试步骤

### 1. 启动后端服务
```bash
# 在项目根目录执行
mvn spring-boot:run
```

或在 IDEA 中运行 `HealthCareApplication`

### 2. 打开前端页面
在浏览器中访问：
```
http://localhost:8080/index.html
```

### 3. 测试 STOMP 连接

#### 步骤 A：连接
1. 点击顶部 **"STOMP 协议"** 标签
2. 输入用户名（例如：`张三`）
3. 点击 **"连接 (STOMP)"** 按钮
4. 观察消息窗口应显示：
   ```
   🔄 正在通过 STOMP 连接到 http://localhost:8080/ws/chat-stomp...
   ✅ STOMP 连接成功！
   👤 当前用户: 张三
   ← 来自 张三 的消息: 加入了聊天室
   ```

#### 步骤 B：发送消息
1. 在输入框中输入消息（例如：`你好，世界！`）
2. 点击 **"发送"** 按钮或按 Enter
3. 观察消息窗口应显示：
   ```
   → 你好，世界！
   ← 来自 张三 的消息: 你好，世界！
   ```

#### 步骤 C：多用户测试
1. 在另一个浏览器标签页打开同样的页面
2. 使用不同的用户名连接（例如：`李四`）
3. 在任一页面发送消息
4. 两个页面都应该能收到消息

### 4. 查看控制台日志

#### 浏览器控制台（F12）
应该看到类似的日志：
```
SockJS connection opened
STOMP Debug: connected to server ...
STOMP Connected: ...
Join message sent
Sending message: {sender: "张三", content: "你好，世界！"}
Received message: ...
```

#### 后端控制台
应该看到类似的日志：
```
STOMP 收到消息: 加入了聊天室
STOMP 收到消息: 你好，世界！
```

## ❌ 常见问题排查

### 问题 1：CORS 跨域错误
**症状**：浏览器控制台显示
```
Access to XMLHttpRequest at 'http://localhost:8080/ws/chat-stomp/info?t=...' 
from origin 'http://localhost:63342' has been blocked by CORS policy
```

**解决方案**：
1. ✅ 确认已添加 `WebMvcConfig.java` 配置文件
2. ✅ 确认 `StompWebSocketConfig.java` 使用了 `.setAllowedOriginPatterns("*")`
3. ✅ **重启后端应用**（配置修改必须重启）
4. ✅ 清除浏览器缓存或使用无痕模式
5. ✅ 推荐直接访问 `http://localhost:8080/index.html`（同源，无 CORS 问题）

**详细说明**：查看 `CORS_问题解决方案.md`

### 问题 2：无法连接到 STOMP
**症状**：点击连接后显示 "❌ STOMP 连接失败"

**解决方案**：
1. 确认后端服务已启动（检查端口 8080）
2. 检查浏览器控制台的错误信息
3. 确认防火墙未阻止 8080 端口
4. 尝试清除浏览器缓存并刷新页面

### 问题 3：连接成功但收不到消息
**症状**：显示连接成功，但发送消息后没有响应

**检查清单**：
- [ ] 订阅频道是否正确：`/topic/messages`
- [ ] 发送目的地是否正确：`/app/chat`
- [ ] 消息格式是否正确：`{sender, content}`
- [ ] 查看后端控制台是否有错误

### 问题 4：后端收到消息但前端不显示
**原因**：可能是消息解析错误

**解决方案**：
1. 打开浏览器控制台（F12）
2. 查看是否有 "Failed to parse message" 错误
3. 检查后端返回的 JSON 格式是否为 `{content: "..."}`

## 🔍 调试技巧

### 启用详细日志
前端代码已经包含了详细的调试日志，所有日志都会输出到浏览器控制台。

### 测试原生 WebSocket
如果 STOMP 有问题，可以先测试原生 WebSocket：
1. 切换到 **"原生 WebSocket"** 标签
2. 点击 **"连接 (原生)"**
3. 发送消息测试

如果原生 WebSocket 可以工作，说明基础网络连接正常，问题出在 STOMP 配置上。

## 📝 关键代码片段

### 前端 STOMP 连接
```javascript
// 连接地址
const stompWsUrl = "http://localhost:8080/ws/chat-stomp";

// 创建 SockJS 连接
const socket = new SockJS(stompWsUrl);
stompClient = StompJs.Stomp.over(socket);

// 订阅消息
stompClient.subscribe('/topic/messages', function(messageOutput) {
    const payload = JSON.parse(messageOutput.body);
    console.log(payload.content);
});

// 发送消息
stompClient.publish({
    destination: '/app/chat',
    body: JSON.stringify({
        sender: 'username',
        content: 'message'
    })
});
```

### 后端 STOMP 控制器
```java
@MessageMapping("/chat")  // 接收 /app/chat
@SendTo("/topic/messages")  // 广播到 /topic/messages
public SimpleResponse handleChatMessage(@Payload SimpleMessage message) {
    String responseContent = String.format("来自 %s 的消息: %s", 
        message.sender(), message.content());
    return new SimpleResponse(responseContent);
}
```

## ✅ 成功标志

当看到以下现象时，表示 STOMP 连接完全正常：

1. ✅ 连接成功提示
2. ✅ 自动收到 "加入聊天室" 的广播消息
3. ✅ 发送消息后立即收到格式化的回复
4. ✅ 多个客户端可以互相看到消息
5. ✅ 浏览器控制台没有错误信息

## 📞 需要帮助？

如果仍然无法连接，请提供以下信息：
- 浏览器控制台的完整错误日志
- 后端控制台的日志输出
- 浏览器类型和版本
- 是否使用了代理或特殊网络环境

