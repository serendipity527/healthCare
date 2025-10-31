# 🚀 WebSocket 点对点消息 - 快速开始

## 📝 简介

本项目实现了完整的 **WebSocket 点对点消息系统**，基于 **原生 WebSocket** 方式，支持：

✅ **点对点私信**  
✅ **广播消息**  
✅ **在线状态管理**  
✅ **REST API 集成**  
✅ **完整的测试页面**

---

## 🎯 5 分钟快速上手

### 步骤 1️⃣：启动服务器

```bash
# 在项目根目录执行
mvn spring-boot:run
```

**验证服务启动成功**：
```bash
# 应该返回在线用户统计
curl http://localhost:8080/api/websocket/stats
```

---

### 步骤 2️⃣：打开测试页面

**方法 A**：直接打开 HTML（推荐）
```bash
# 用浏览器打开
Front/test-point-to-point.html
```

**方法 B**：启动 HTTP 服务器
```bash
cd Front
python -m http.server 8000
# 然后访问 http://localhost:8000/test-point-to-point.html
```

---

### 步骤 3️⃣：测试点对点消息

1. **打开第一个浏览器标签页**
   - 用户ID：`Alice`
   - 点击"连接"

2. **打开第二个浏览器标签页**
   - 用户ID：`Bob`
   - 点击"连接"

3. **Alice 发送私信给 Bob**
   - 在 Alice 的标签页：
     - 目标用户ID：`Bob`
     - 消息内容：`你好，Bob！`
     - 点击"发送"
   
   - **期望结果**：Bob 收到消息

4. **测试广播**
   - 在任意标签页输入广播消息：`大家好！`
   - 点击"广播"
   
   - **期望结果**：所有标签页都收到消息

---

## 📚 核心概念

### 🔑 用户身份识别

通过 URL 参数传递 `userId`：

```javascript
// 连接时指定用户ID
const ws = new WebSocket('ws://localhost:8080/ws/chat-raw?userId=user123');
```

**服务端会**：
1. 提取 `userId`
2. 建立 `userId` → `WebSocketSession` 映射
3. 防止重复登录（同一 userId 只保留最新连接）

---

### 📨 消息格式

#### 1. 发送私信（客户端 → 服务端）

```json
{
  "type": "private",
  "toUserId": "user002",
  "content": "你好，这是私信"
}
```

#### 2. 发送广播（客户端 → 服务端）

```json
{
  "type": "broadcast",
  "content": "大家好！"
}
```

#### 3. 接收消息（服务端 → 客户端）

```json
{
  "type": "private",
  "fromUserId": "user001",
  "toUserId": "user002",
  "content": "你好，这是私信",
  "timestamp": 1698765432000
}
```

---

## 🛠️ 核心实现

### 后端核心类

| 类 | 路径 | 说明 |
|----|------|------|
| `ChatWebSocketHandler` | `src/main/java/com/yihu/agent/websocket/` | WebSocket 消息处理器 |
| `RawWebSocketConfig` | `src/main/java/com/yihu/agent/config/` | WebSocket 配置 |
| `WebSocketApiController` | `src/main/java/com/yihu/agent/controller/` | REST API 接口 |

---

### 关键数据结构

```java
// 用户映射：userId -> WebSocketSession
private static final ConcurrentHashMap<String, WebSocketSession> userSessions 
    = new ConcurrentHashMap<>();
```

**为什么用 ConcurrentHashMap？**
- ✅ 线程安全
- ✅ 高并发性能好
- ✅ 支持多用户同时操作

---

### 核心方法

```java
// 1️⃣ 发送消息给指定用户
public boolean sendToUser(String userId, String content) {
    WebSocketSession session = userSessions.get(userId);
    if (session != null && session.isOpen()) {
        session.sendMessage(new TextMessage(content));
        return true;
    }
    return false;
}

// 2️⃣ 广播给所有用户
public int broadcastToAll(String content) {
    int count = 0;
    for (WebSocketSession session : userSessions.values()) {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(content));
            count++;
        }
    }
    return count;
}

// 3️⃣ 检查用户是否在线
public boolean isUserOnline(String userId) {
    WebSocketSession session = userSessions.get(userId);
    return session != null && session.isOpen();
}
```

---

## 🌐 使用方式

### 方式一：WebSocket 客户端

**适用场景**：前端实时通信

```javascript
// 1. 建立连接
const ws = new WebSocket('ws://localhost:8080/ws/chat-raw?userId=user123');

// 2. 监听消息
ws.onmessage = function(event) {
    const data = JSON.parse(event.data);
    console.log('收到消息:', data);
};

// 3. 发送私信
ws.send(JSON.stringify({
    type: 'private',
    toUserId: 'user456',
    content: 'Hello!'
}));

// 4. 发送广播
ws.send(JSON.stringify({
    type: 'broadcast',
    content: '大家好！'
}));
```

---

### 方式二：REST API

**适用场景**：后端服务推送消息

#### 1. 发送消息给指定用户

```bash
curl -X POST http://localhost:8080/api/websocket/send \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "message": "您有新的订单通知"
  }'
```

#### 2. 广播消息

```bash
curl -X POST http://localhost:8080/api/websocket/broadcast \
  -H "Content-Type: application/json" \
  -d '{
    "message": "系统维护通知"
  }'
```

#### 3. 查询在线用户

```bash
curl http://localhost:8080/api/websocket/stats
```

---

### 方式三：Spring Bean 注入

**适用场景**：业务逻辑集成

```java
@Service
public class OrderService {
    
    @Autowired
    private ChatWebSocketHandler webSocketHandler;
    
    public void notifyOrderCreated(String userId, Order order) {
        String message = String.format(
            "订单创建成功：%s",
            order.getOrderNo()
        );
        
        webSocketHandler.sendToUser(userId, message);
    }
}
```

---

## 🧪 测试指南

### 测试场景 1：双人对话

1. **打开两个浏览器标签页**
   - 标签页 A：userId = `Alice`
   - 标签页 B：userId = `Bob`

2. **Alice 发私信给 Bob**
   - 目标用户ID：`Bob`
   - 消息：`你好，Bob！`

3. **Bob 回复 Alice**
   - 目标用户ID：`Alice`
   - 消息：`你好，Alice！`

---

### 测试场景 2：多人广播

1. **打开三个浏览器标签页**
   - user001, user002, user003

2. **任意用户发送广播**
   - 消息：`大家好！`

3. **验证**
   - 所有标签页都收到消息

---

### 测试场景 3：REST API 推送

1. **打开浏览器连接为 user123**

2. **使用 curl 发送消息**
   ```bash
   curl -X POST http://localhost:8080/api/websocket/send \
     -H "Content-Type: application/json" \
     -d '{"userId": "user123", "message": "来自后端的消息"}'
   ```

3. **验证**
   - 浏览器收到消息

---

## 📖 完整文档

| 文档 | 路径 | 说明 |
|------|------|------|
| **WebSocket 点对点消息使用指南** | `DOC/WebSocket点对点消息使用指南.md` | 完整的功能说明和 API 文档 |
| **REST API 测试指南** | `DOC/REST_API_测试指南.md` | REST API 接口详情和测试示例 |
| **测试页面** | `Front/test-point-to-point.html` | 可视化测试工具 |

---

## 🔧 常用命令

### 启动服务

```bash
# Maven
mvn spring-boot:run

# Java
java -jar target/healthCare-0.0.1-SNAPSHOT.jar
```

---

### 查看日志

```bash
# 实时查看日志
tail -f logs/application.log

# 搜索 WebSocket 相关日志
grep "WebSocket" logs/application.log
```

---

### 测试 REST API

```bash
# 查看在线用户
curl http://localhost:8080/api/websocket/stats

# 发送消息
curl -X POST http://localhost:8080/api/websocket/send \
  -H "Content-Type: application/json" \
  -d '{"userId": "user123", "message": "测试消息"}'

# 广播消息
curl -X POST http://localhost:8080/api/websocket/broadcast \
  -H "Content-Type: application/json" \
  -d '{"message": "系统通知"}'
```

---

## 🎨 架构图

```
┌─────────────────────────────────────────────────────────┐
│                     客户端层                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ 浏览器 A  │  │ 浏览器 B  │  │ 浏览器 C  │             │
│  │(user001) │  │(user002) │  │(user003) │             │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘             │
└───────┼─────────────┼─────────────┼────────────────────┘
        │ WebSocket   │             │
        │             │             │
┌───────▼─────────────▼─────────────▼────────────────────┐
│                   Spring Boot 服务器                     │
│  ┌─────────────────────────────────────────────────┐   │
│  │       ChatWebSocketHandler                      │   │
│  │  ┌──────────────────────────────────────────┐  │   │
│  │  │  ConcurrentHashMap<String, Session>      │  │   │
│  │  │  user001 -> Session1                     │  │   │
│  │  │  user002 -> Session2                     │  │   │
│  │  │  user003 -> Session3                     │  │   │
│  │  └──────────────────────────────────────────┘  │   │
│  │                                                 │   │
│  │  • handleTextMessage()  处理消息               │   │
│  │  • sendToUser()         点对点发送             │   │
│  │  • broadcastToAll()     广播                   │   │
│  └─────────────────────────────────────────────────┘   │
│                                                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │       WebSocketApiController (REST API)         │   │
│  │  • POST /api/websocket/send                     │   │
│  │  • POST /api/websocket/broadcast                │   │
│  │  • GET  /api/websocket/stats                    │   │
│  └─────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────┘
```

---

## 💡 设计亮点

### 1️⃣ 用户身份管理

- ✅ 从 URL 参数提取 userId
- ✅ 防止重复登录
- ✅ 自动清理断开连接

### 2️⃣ 消息路由

- ✅ 点对点私信
- ✅ 广播消息
- ✅ 发送回执
- ✅ 错误提示

### 3️⃣ 在线状态

- ✅ 实时维护在线列表
- ✅ 上下线通知
- ✅ 在线检查 API

### 4️⃣ 线程安全

- ✅ ConcurrentHashMap
- ✅ 无锁设计
- ✅ 高并发支持

### 5️⃣ 扩展性

- ✅ REST API 集成
- ✅ Spring Bean 注入
- ✅ 事件监听支持

---

## 🚀 进阶功能

### 1. 消息持久化

```java
@Service
public class MessagePersistenceService {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private ChatWebSocketHandler webSocketHandler;
    
    public void sendAndSave(String fromUserId, String toUserId, String content) {
        // 1. 保存到数据库
        Message message = new Message(fromUserId, toUserId, content);
        messageRepository.save(message);
        
        // 2. 实时推送
        webSocketHandler.sendToUser(toUserId, content);
    }
}
```

---

### 2. 离线消息

```java
@Override
public void afterConnectionEstablished(WebSocketSession session) {
    String userId = getUserIdFromSession(session);
    
    // 推送离线消息
    List<Message> offlineMessages = messageService.getOfflineMessages(userId);
    for (Message msg : offlineMessages) {
        sendToUser(userId, msg.getContent());
    }
    
    // 标记为已读
    messageService.markAsRead(userId);
}
```

---

### 3. 心跳保活

**客户端**：
```javascript
setInterval(() => {
    if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'ping' }));
    }
}, 30000); // 每 30 秒
```

**服务端**：
```java
if ("ping".equals(message.getType())) {
    session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
    return;
}
```

---

### 4. 群组聊天

```java
// 群组管理
private static final ConcurrentHashMap<String, Set<String>> groups = new ConcurrentHashMap<>();

public void sendToGroup(String groupId, String message) {
    Set<String> members = groups.get(groupId);
    if (members != null) {
        for (String userId : members) {
            sendToUser(userId, message);
        }
    }
}
```

---

## ❓ 常见问题

### Q1: 为什么选择原生 WebSocket 而不是 STOMP？

**原生 WebSocket**：
- ✅ 简单直观
- ✅ 灵活性高
- ✅ 适合点对点场景
- ✅ 性能开销小

**STOMP**：
- ✅ 协议标准
- ✅ 支持消息代理（RabbitMQ）
- ✅ 适合复杂场景
- ✅ 易于集群部署

---

### Q2: 如何集成用户认证？

```java
private String getUserIdFromSession(WebSocketSession session) {
    // 从 Token 解析用户ID
    String token = session.getHandshakeHeaders().getFirst("Authorization");
    return jwtUtils.parseToken(token).getUserId();
}
```

---

### Q3: 如何支持集群部署？

使用 **Redis 发布/订阅**：

```java
// 发送消息时，发布到 Redis
redisTemplate.convertAndSend("websocket:messages", message);

// 订阅 Redis，接收其他服务器的消息
@MessageListener
public void onMessage(Message message) {
    // 如果用户连接在本服务器，则推送
    if (webSocketHandler.isUserOnline(userId)) {
        webSocketHandler.sendToUser(userId, content);
    }
}
```

---

## 📞 技术支持

- **详细文档**：`DOC/WebSocket点对点消息使用指南.md`
- **REST API**：`DOC/REST_API_测试指南.md`
- **测试页面**：`Front/test-point-to-point.html`

---

## ✅ 下一步

1. ✅ 阅读完整文档
2. ✅ 运行测试页面
3. ✅ 尝试 REST API
4. ✅ 集成到你的业务系统

---

**祝你使用愉快！如有问题，欢迎反馈！** 🎉

