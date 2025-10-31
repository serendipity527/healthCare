# WebSocket 点对点消息使用指南

## 📋 目录
1. [功能概述](#功能概述)
2. [实现原理](#实现原理)
3. [快速开始](#快速开始)
4. [API 文档](#api-文档)
5. [消息格式](#消息格式)
6. [测试指南](#测试指南)
7. [常见问题](#常见问题)

---

## 功能概述

本项目实现了基于**原生 WebSocket** 的点对点消息系统，具备以下功能：

### ✨ 核心特性

- ✅ **用户身份管理**：通过 `userId` 唯一标识每个连接
- ✅ **点对点私信**：用户之间发送一对一消息
- ✅ **广播消息**：向所有在线用户发送消息
- ✅ **在线状态管理**：实时维护用户在线列表
- ✅ **上下线通知**：自动通知其他用户状态变化
- ✅ **消息回执**：发送方收到发送状态反馈
- ✅ **防重复登录**：同一用户多次连接会关闭旧连接
- ✅ **错误处理**：完善的异常处理和错误提示

---

## 实现原理

### 🏗️ 架构设计

```
┌─────────────┐         ┌──────────────────┐         ┌─────────────┐
│  客户端 A   │ ──ws──> │  ChatWebSocket   │ <──ws── │  客户端 B   │
│ (user001)   │         │     Handler      │         │ (user002)   │
└─────────────┘         └──────────────────┘         └─────────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │  ConcurrentHashMap   │
                    │  userId -> Session   │
                    └──────────────────────┘
```

### 🔑 关键技术点

1. **用户映射**：使用 `ConcurrentHashMap<String, WebSocketSession>` 维护用户ID到会话的映射
2. **URL 参数传递**：从 WebSocket 连接 URL 中提取 `userId`（例如：`ws://localhost:8080/ws/chat-raw?userId=user123`）
3. **消息路由**：根据消息类型（`private` / `broadcast`）路由到不同的处理方法
4. **JSON 序列化**：使用 Jackson ObjectMapper 处理消息的序列化/反序列化
5. **线程安全**：所有共享数据结构使用并发安全的集合类

---

## 快速开始

### 🚀 启动服务器

1. **确保项目已启动**
   ```bash
   mvn spring-boot:run
   ```

2. **验证 WebSocket 端点**
   - 端点地址：`ws://localhost:8080/ws/chat-raw`
   - 配置文件：`src/main/java/com/yihu/agent/config/RawWebSocketConfig.java`

### 🧪 使用测试页面

1. **打开测试页面**
   - 浏览器访问：`Front/test-point-to-point.html`
   - 或者启动一个简单的 HTTP 服务器：
     ```bash
     # 在 Front 目录下执行
     python -m http.server 8000
     # 然后访问 http://localhost:8000/test-point-to-point.html
     ```

2. **多用户测试**
   - 打开**多个浏览器标签页**或**不同浏览器**
   - 每个页面使用不同的 `userId` 连接（例如：user001, user002, Alice, Bob）

### 📝 基本使用流程

```javascript
// 1. 建立连接（包含 userId）
const ws = new WebSocket('ws://localhost:8080/ws/chat-raw?userId=user001');

// 2. 监听连接打开
ws.onopen = function() {
    console.log('连接成功');
};

// 3. 发送私信
ws.send(JSON.stringify({
    type: 'private',
    toUserId: 'user002',
    content: 'Hello, 这是私信!'
}));

// 4. 发送广播
ws.send(JSON.stringify({
    type: 'broadcast',
    content: '大家好！'
}));

// 5. 接收消息
ws.onmessage = function(event) {
    const data = JSON.parse(event.data);
    console.log('收到消息:', data);
};
```

---

## API 文档

### 📡 WebSocket 端点

**端点**：`ws://localhost:8080/ws/chat-raw`

**连接参数**：
- `userId`（必填）：用户唯一标识符

**连接示例**：
```
ws://localhost:8080/ws/chat-raw?userId=user123
```

---

### 📤 客户端发送消息格式

#### 1. 私信消息

```json
{
    "type": "private",
    "toUserId": "目标用户ID",
    "content": "消息内容"
}
```

**示例**：
```json
{
    "type": "private",
    "toUserId": "user002",
    "content": "你好，这是一条私信"
}
```

#### 2. 广播消息

```json
{
    "type": "broadcast",
    "content": "消息内容"
}
```

**示例**：
```json
{
    "type": "broadcast",
    "content": "大家好！"
}
```

---

### 📥 服务端响应消息格式

#### 1. 连接成功（欢迎消息）

```json
{
    "message": "连接成功",
    "userId": "当前用户ID",
    "onlineCount": 3,
    "onlineUsers": ["user001", "user002", "user003"]
}
```

#### 2. 私信消息

```json
{
    "type": "private",
    "fromUserId": "发送者ID",
    "toUserId": "接收者ID",
    "content": "消息内容",
    "timestamp": 1698765432000
}
```

#### 3. 发送回执

```json
{
    "type": "receipt",
    "fromUserId": "system",
    "toUserId": "当前用户ID",
    "content": "消息已发送给 user002",
    "timestamp": 1698765432000
}
```

#### 4. 广播消息

```json
{
    "type": "broadcast",
    "fromUserId": "发送者ID",
    "toUserId": "all",
    "content": "消息内容",
    "timestamp": 1698765432000
}
```

#### 5. 用户上线通知

```json
{
    "type": "user_online",
    "message": "user002 上线了",
    "userId": "user002",
    "onlineCount": 3
}
```

#### 6. 用户下线通知

```json
{
    "type": "user_offline",
    "message": "user002 下线了",
    "userId": "user002",
    "onlineCount": 2
}
```

#### 7. 错误消息

```json
{
    "type": "error",
    "fromUserId": "system",
    "toUserId": "当前用户ID",
    "content": "用户 user999 不在线",
    "timestamp": 1698765432000
}
```

#### 8. 系统消息

```json
{
    "type": "system",
    "fromUserId": "system",
    "toUserId": "当前用户ID",
    "content": "系统通知内容",
    "timestamp": 1698765432000
}
```

---

## 消息格式

### 📊 消息类型总览

| 消息类型 | 方向 | 说明 |
|---------|------|------|
| `private` | 双向 | 点对点私信 |
| `broadcast` | 双向 | 广播消息 |
| `receipt` | 服务端→客户端 | 发送回执 |
| `error` | 服务端→客户端 | 错误提示 |
| `system` | 服务端→客户端 | 系统消息 |
| `user_online` | 服务端→客户端 | 用户上线通知 |
| `user_offline` | 服务端→客户端 | 用户下线通知 |
| `system_broadcast` | 服务端→客户端 | 系统广播 |

---

## 测试指南

### 🧪 测试场景 1：双人私信

**步骤**：

1. **打开两个浏览器标签页**
   - 标签页 A：userId = `Alice`
   - 标签页 B：userId = `Bob`

2. **连接 WebSocket**
   - 两个标签页都点击"连接"按钮

3. **验证上线通知**
   - 观察是否收到对方上线的系统通知

4. **发送私信**
   - 在 Alice 的标签页：
     - 目标用户ID：`Bob`
     - 消息内容：`你好，Bob！`
     - 点击"发送"

5. **验证消息接收**
   - Bob 的标签页应该在"私信对话"区域收到消息
   - Alice 的标签页应该收到发送成功的回执

---

### 🧪 测试场景 2：多人广播

**步骤**：

1. **打开三个浏览器标签页**
   - 标签页 1：userId = `user001`
   - 标签页 2：userId = `user002`
   - 标签页 3：userId = `user003`

2. **全部连接**
   - 观察在线用户列表是否正确显示 3 人

3. **发送广播**
   - 在任意标签页的"广播消息"输入框输入：`大家好！`
   - 点击"广播"按钮

4. **验证**
   - 所有标签页（包括发送者）都应该在"广播消息"区域收到消息

---

### 🧪 测试场景 3：防重复登录

**步骤**：

1. **第一个标签页**
   - userId = `user001`
   - 点击"连接"

2. **第二个标签页**
   - 使用**相同的** userId = `user001`
   - 点击"连接"

3. **验证**
   - 第一个标签页应该收到"您的账号在其他地方登录"提示并断开连接
   - 第二个标签页正常连接

---

### 🧪 测试场景 4：用户不在线

**步骤**：

1. **连接为 user001**

2. **发送私信给不存在的用户**
   - 目标用户ID：`user999`（不存在）
   - 消息内容：`Hello`
   - 点击"发送"

3. **验证**
   - 应该收到错误提示：`用户 user999 不在线`

---

### 🧪 测试场景 5：断线重连

**步骤**：

1. **用户 A 连接**

2. **用户 B 连接**

3. **用户 A 断开连接**
   - 点击"断开"按钮

4. **验证**
   - 用户 B 应该收到"user_A 下线了"的系统通知
   - 在线用户列表应该更新

5. **用户 A 重新连接**

6. **验证**
   - 用户 B 应该收到"user_A 上线了"的系统通知

---

## 后端 API（供其他服务调用）

### 🔌 Spring Bean 注入

```java
@Service
public class YourService {
    
    @Autowired
    private ChatWebSocketHandler webSocketHandler;
    
    // 你的业务逻辑...
}
```

### 📤 发送消息给特定用户

```java
/**
 * 向指定用户发送消息
 * @param userId 目标用户ID
 * @param content 消息内容
 * @return 是否发送成功
 */
boolean success = webSocketHandler.sendToUser("user123", "您有新的系统通知");
```

**示例**：
```java
@RestController
public class NotificationController {
    
    @Autowired
    private ChatWebSocketHandler webSocketHandler;
    
    @PostMapping("/api/notify")
    public ResponseEntity<?> notifyUser(@RequestParam String userId, 
                                        @RequestParam String message) {
        boolean sent = webSocketHandler.sendToUser(userId, message);
        
        if (sent) {
            return ResponseEntity.ok("消息已发送");
        } else {
            return ResponseEntity.status(404).body("用户不在线");
        }
    }
}
```

---

### 📢 广播消息给所有用户

```java
/**
 * 广播消息给所有在线用户
 * @param content 消息内容
 * @return 成功接收的用户数量
 */
int count = webSocketHandler.broadcastToAll("系统维护通知：服务将于今晚 22:00 进行升级");
```

---

### 👥 获取在线用户信息

```java
// 获取在线用户数量
int onlineCount = webSocketHandler.getOnlineUserCount();

// 检查用户是否在线
boolean isOnline = webSocketHandler.isUserOnline("user123");

// 获取所有在线用户（返回 Map<String, WebSocketSession>）
Map<String, WebSocketSession> users = webSocketHandler.getOnlineUsers();
```

---

## 常见问题

### ❓ Q1: 连接时提示"连接必须包含 userId 参数"

**原因**：WebSocket 连接 URL 中缺少 `userId` 参数

**解决**：
```javascript
// ❌ 错误
new WebSocket('ws://localhost:8080/ws/chat-raw')

// ✅ 正确
new WebSocket('ws://localhost:8080/ws/chat-raw?userId=user123')
```

---

### ❓ Q2: 发送私信没有反应

**检查清单**：
1. 目标用户ID是否正确
2. 目标用户是否在线（查看在线用户列表）
3. 消息格式是否正确
4. 查看浏览器控制台是否有错误信息

---

### ❓ Q3: 同一用户多次连接会怎样？

**行为**：
- 旧连接会收到"您的账号在其他地方登录"提示
- 旧连接会被自动关闭
- 新连接正常建立

这是**防重复登录**机制，确保每个 userId 只有一个活动连接。

---

### ❓ Q4: 如何实现消息历史记录？

当前实现是**纯内存**方式，不持久化消息。如需消息历史，建议：

1. **数据库存储**
   ```java
   @Service
   public class MessageService {
       
       @Autowired
       private MessageRepository messageRepository;
       
       public void saveMessage(String from, String to, String content) {
           // 保存到数据库
       }
       
       public List<Message> getHistory(String userId1, String userId2) {
           // 查询历史消息
       }
   }
   ```

2. **在 Handler 中集成**
   ```java
   @Autowired
   private MessageService messageService;
   
   private void handlePrivateMessage(ChatMessage message) throws IOException {
       // 先保存消息
       messageService.saveMessage(
           message.getFromUserId(), 
           message.getToUserId(), 
           message.getContent()
       );
       
       // 再发送消息
       // ...
   }
   ```

---

### ❓ Q5: 如何处理离线消息？

当前实现**不支持离线消息**。如需此功能：

**方案 1：数据库存储 + 上线推送**
```java
// 用户上线时，推送离线消息
@Override
public void afterConnectionEstablished(WebSocketSession session) {
    String userId = getUserIdFromSession(session);
    
    // 查询离线消息
    List<Message> offlineMessages = messageService.getOfflineMessages(userId);
    
    // 推送离线消息
    for (Message msg : offlineMessages) {
        sendToUser(userId, msg.getContent());
    }
    
    // 标记为已读
    messageService.markAsRead(userId);
}
```

**方案 2：消息队列（RabbitMQ / Kafka）**
- 发送消息时入队
- 用户上线时消费队列

---

### ❓ Q6: 如何集成用户认证（JWT / Session）？

**方案 1：从 JWT Token 提取用户信息**
```java
private String getUserIdFromSession(WebSocketSession session) {
    // 从 URL 参数或 Header 中获取 Token
    String token = extractToken(session);
    
    // 解析 JWT
    String userId = jwtUtils.parseToken(token).getUserId();
    
    return userId;
}
```

**方案 2：使用 Spring Security**
```java
@Override
public void afterConnectionEstablished(WebSocketSession session) {
    // 获取已认证的用户
    Principal principal = session.getPrincipal();
    String userId = principal.getName();
    
    // ...
}
```

---

### ❓ Q7: 如何扩展到多服务器（集群部署）？

当前实现是**单机版**，如需集群部署：

**方案 1：使用 Redis 发布/订阅**
```java
@Service
public class RedisMessagePublisher {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public void publishMessage(String channel, Object message) {
        redisTemplate.convertAndSend(channel, message);
    }
}

@Service
public class RedisMessageSubscriber implements MessageListener {
    
    @Autowired
    private ChatWebSocketHandler handler;
    
    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 接收来自其他服务器的消息
        String userId = ...;
        String content = ...;
        
        // 如果用户连接在本服务器，则推送
        if (handler.isUserOnline(userId)) {
            handler.sendToUser(userId, content);
        }
    }
}
```

**方案 2：使用 STOMP + 外部消息代理（RabbitMQ / ActiveMQ）**
- 参考 `StompWebSocketConfig.java` 中的注释
- 启用 `enableStompBrokerRelay` 替代 `enableSimpleBroker`

---

## 性能优化建议

### 🚀 1. 连接池优化

```properties
# application.properties
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=10
```

### 🚀 2. 心跳保活

客户端定时发送心跳：
```javascript
setInterval(() => {
    if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'ping' }));
    }
}, 30000); // 每 30 秒一次
```

服务端处理心跳：
```java
if ("ping".equals(chatMessage.getType())) {
    session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
    return;
}
```

### 🚀 3. 消息压缩

```java
// 启用 WebSocket 压缩扩展
registry.addHandler(chatHandler, "/ws/chat-raw")
       .setAllowedOrigins("*")
       .setHandshakeHandler(new DefaultHandshakeHandler() {
           @Override
           protected void upgradeHttpToWebSocket(...) {
               // 启用 permessage-deflate 压缩
           }
       });
```

---

## 安全建议

### 🔒 1. 限制跨域

```java
// 生产环境配置
registry.addHandler(chatHandler, "/ws/chat-raw")
       .setAllowedOrigins("https://yourdomain.com");
```

### 🔒 2. 用户认证

- 使用 JWT Token 或 Session 验证用户身份
- 不要直接信任客户端传递的 `userId`

### 🔒 3. 消息内容过滤

```java
// 防止 XSS 攻击
import org.springframework.web.util.HtmlUtils;

String content = HtmlUtils.htmlEscape(message.getContent());
```

### 🔒 4. 频率限制

```java
// 防止消息轰炸
private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

private boolean checkRateLimit(String userId) {
    RateLimiter limiter = rateLimiters.computeIfAbsent(
        userId, 
        k -> RateLimiter.create(10.0) // 每秒最多 10 条消息
    );
    return limiter.tryAcquire();
}
```

---

## 总结

本实现提供了一个**完整的、生产级的** WebSocket 点对点消息系统，包含：

✅ 用户管理  
✅ 私信功能  
✅ 广播功能  
✅ 在线状态  
✅ 完善的错误处理  
✅ 详细的测试页面  
✅ 灵活的后端 API  

**适用场景**：
- 在线客服系统
- 即时通讯应用
- 实时通知推送
- 多人协作工具
- 在线教育平台

**扩展方向**：
- 消息持久化
- 离线消息
- 群组聊天
- 文件传输
- 音视频通话（WebRTC）

---

## 相关文件

- 后端实现：`src/main/java/com/yihu/agent/websocket/ChatWebSocketHandler.java`
- 配置文件：`src/main/java/com/yihu/agent/config/RawWebSocketConfig.java`
- 测试页面：`Front/test-point-to-point.html`

---

**祝你使用愉快！如有问题，欢迎提 Issue！** 🎉

