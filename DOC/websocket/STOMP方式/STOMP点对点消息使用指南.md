# STOMP 点对点消息使用指南

## 📋 目录
1. [STOMP 简介](#stomp-简介)
2. [实现原理](#实现原理)
3. [快速开始](#快速开始)
4. [消息格式](#消息格式)
5. [订阅路径说明](#订阅路径说明)
6. [测试指南](#测试指南)
7. [与原生WebSocket对比](#与原生websocket对比)

---

## STOMP 简介

### 什么是 STOMP？

**STOMP（Simple Text Oriented Messaging Protocol）** 是一个简单的文本消息协议，专为消息中间件设计。

### 为什么选择 STOMP？

| 特性 | STOMP | 原生 WebSocket |
|------|-------|----------------|
| **协议标准** | ✅ 标准协议 | ❌ 自定义协议 |
| **消息路由** | ✅ 内置路由机制 | ❌ 需要手动实现 |
| **点对点** | ✅ 原生支持 `/user` | ❌ 需要维护映射 |
| **集群支持** | ✅ 易于集成消息代理 | ❌ 需要 Redis 等 |
| **客户端库** | ✅ 丰富的客户端库 | ❌ 需要自己封装 |
| **学习曲线** | ⚠️ 需要理解概念 | ✅ 简单直接 |

---

## 实现原理

### 核心架构

```
┌─────────────────────────────────────────────────────────────┐
│                      STOMP 架构                              │
└─────────────────────────────────────────────────────────────┘

客户端 A                                           客户端 B
  │                                                   │
  ├─ Connect(userId=Alice) ──────────────────────────┤
  │                                                   │
  ├─ Subscribe(/user/queue/private) ────────────────┤
  │                                                   │
  ├─ Subscribe(/topic/messages) ────────────────────┤
  │                                                   │
  ├─ Send(/app/private, toUserId=Bob) ──>           │
  │                       │                           │
  │                       ↓                           │
  │              ┌─────────────────┐                 │
  │              │ StompController │                 │
  │              └────────┬────────┘                 │
  │                       ↓                           │
  │          SimpMessagingTemplate                   │
  │                       │                           │
  │     convertAndSendToUser(Bob, "/queue/private")  │
  │                       │                           │
  │                       └─────────────────────────>│
  │                                                   │
  │                                                  Bob 收到
```

### 关键概念

#### 1️⃣ **目的地（Destination）**

| 前缀 | 用途 | 示例 |
|------|------|------|
| `/app` | 客户端发送消息 | `/app/chat`, `/app/private` |
| `/topic` | 广播消息（一对多） | `/topic/messages` |
| `/queue` | 队列消息（点对点） | `/queue/private` |
| `/user` | 用户专属队列 | `/user/queue/private` |

#### 2️⃣ **消息流向**

**广播消息流程：**
```
客户端 A
  └─> Send to: /app/chat
       └─> 路由到 @MessageMapping("/chat")
            └─> @SendTo("/topic/messages")
                 └─> 所有订阅 /topic/messages 的客户端收到
```

**点对点消息流程：**
```
客户端 A (Alice)
  └─> Send to: /app/private
       └─> 路由到 @MessageMapping("/private")
            └─> convertAndSendToUser(Bob, "/queue/private")
                 └─> 发送到 /user/Bob/queue/private
                      └─> 只有 Bob 收到
```

---

## 快速开始

### 步骤 1️⃣：启动服务器

```bash
mvn spring-boot:run
```

### 步骤 2️⃣：打开测试页面

浏览器打开：`Front/test-stomp-p2p.html`

### 步骤 3️⃣：连接 STOMP

**JavaScript 代码：**

```javascript
// 1. 创建 SockJS 连接
const socket = new SockJS('http://localhost:8080/ws/chat-stomp');

// 2. 创建 STOMP 客户端
const stompClient = Stomp.over(socket);

// 3. 连接到服务器（传递用户ID）
stompClient.connect(
    { userId: 'Alice' },  // 连接头参数
    function(frame) {
        console.log('连接成功:', frame);
        
        // 4. 订阅消息
        subscribeMessages();
    }
);
```

### 步骤 4️⃣：订阅消息

```javascript
// 订阅广播消息
stompClient.subscribe('/topic/messages', function(message) {
    console.log('广播:', message.body);
});

// 订阅私信
stompClient.subscribe('/user/queue/private', function(message) {
    console.log('私信:', message.body);
});

// 订阅回执
stompClient.subscribe('/user/queue/receipt', function(message) {
    console.log('回执:', message.body);
});
```

### 步骤 5️⃣：发送消息

```javascript
// 发送广播
stompClient.send('/app/chat', {}, JSON.stringify({
    content: '大家好！'
}));

// 发送私信
stompClient.send('/app/private', {}, JSON.stringify({
    toUserId: 'Bob',
    content: '你好，Bob！'
}));
```

---

## 消息格式

### 📤 客户端发送

#### 1. 广播消息

```javascript
// 发送到 /app/chat
stompClient.send('/app/chat', {}, JSON.stringify({
    content: "大家好！"
}));
```

**服务端处理：**
```java
@MessageMapping("/chat")
@SendTo("/topic/messages")
public BroadcastMessage handleChatMessage(@Payload ChatMessage message) {
    // 返回值会被广播到 /topic/messages
}
```

#### 2. 点对点私信

```javascript
// 发送到 /app/private
stompClient.send('/app/private', {}, JSON.stringify({
    toUserId: "Bob",
    content: "你好，Bob！"
}));
```

**服务端处理：**
```java
@MessageMapping("/private")
public void handlePrivateMessage(@Payload PrivateMessage message) {
    messagingTemplate.convertAndSendToUser(
        message.getToUserId(),
        "/queue/private",
        response
    );
}
```

#### 3. 群发消息

```javascript
// 发送到 /app/group
stompClient.send('/app/group', {}, JSON.stringify({
    toUserIds: ["Alice", "Bob", "Charlie"],
    content: "群组消息"
}));
```

---

### 📥 服务端响应

#### 1. 广播消息响应

```json
{
    "type": "broadcast",
    "sender": "Alice",
    "content": "大家好！",
    "timestamp": "2024-10-31 10:30:00"
}
```

**客户端接收：**
```javascript
stompClient.subscribe('/topic/messages', function(message) {
    const data = JSON.parse(message.body);
    console.log(data.sender + ': ' + data.content);
});
```

#### 2. 私信响应

```json
{
    "type": "private",
    "fromUserId": "Alice",
    "toUserId": "Bob",
    "content": "你好，Bob！",
    "timestamp": "2024-10-31 10:31:00"
}
```

**客户端接收：**
```javascript
stompClient.subscribe('/user/queue/private', function(message) {
    const data = JSON.parse(message.body);
    console.log('来自 ' + data.fromUserId + ': ' + data.content);
});
```

#### 3. 消息回执

```json
{
    "type": "receipt",
    "fromUserId": "system",
    "toUserId": "Alice",
    "content": "消息已发送给 Bob",
    "timestamp": "2024-10-31 10:31:01"
}
```

---

## 订阅路径说明

### 📡 订阅路径详解

| 路径 | 作用 | 谁会收到 | 示例 |
|------|------|---------|------|
| `/topic/messages` | 广播消息 | 所有订阅者 | 公告、通知 |
| `/user/queue/private` | 私信 | 当前用户 | 一对一聊天 |
| `/user/queue/receipt` | 回执 | 当前用户 | 发送确认 |
| `/topic/user-status` | 用户状态 | 所有订阅者 | 上下线通知 |

### 🔑 `/user` 前缀的魔法

**客户端订阅：**
```javascript
stompClient.subscribe('/user/queue/private', callback);
```

**实际订阅路径：**
```
/user/{当前用户ID}/queue/private
```

**Spring 自动处理：**
- 从 `Principal` 获取当前用户ID
- 自动添加到路径中
- 确保消息只发送给目标用户

**服务端发送：**
```java
messagingTemplate.convertAndSendToUser(
    "Bob",              // 目标用户ID
    "/queue/private",   // 队列路径
    message            // 消息内容
);
```

**实际发送路径：**
```
/user/Bob/queue/private
```

---

## 测试指南

### 🧪 测试场景 1：双人私聊

**步骤：**

1. **打开两个浏览器标签页**
   - 标签页 A：userId = `Alice`
   - 标签页 B：userId = `Bob`

2. **连接并验证订阅**
   - 两个标签页都点击"连接"
   - 查看"当前订阅"列表，应该有：
     - `/topic/messages`
     - `/user/queue/private`
     - `/user/queue/receipt`

3. **Alice 发送私信给 Bob**
   - 在 Alice 的标签页：
     - 目标用户ID：`Bob`
     - 消息内容：`你好，Bob！`
     - 点击"发送"

4. **验证消息接收**
   - Bob 的标签页应该在"私信对话"区域收到消息
   - Alice 的标签页应该收到回执："消息已发送给 Bob"

---

### 🧪 测试场景 2：多人广播

**步骤：**

1. **打开三个浏览器标签页**
   - user001, user002, user003

2. **全部连接**

3. **任意用户发送广播**
   - 消息内容：`大家好！`
   - 点击"广播"

4. **验证**
   - 所有标签页（包括发送者）都在"广播消息"区域收到消息
   - 发送者的消息显示在右侧（绿色背景）
   - 其他用户的消息显示在左侧（蓝色背景）

---

### 🧪 测试场景 3：STOMP 连接头

**JavaScript 控制台测试：**

```javascript
// 查看连接头
stompClient.connect(
    { 
        userId: 'Alice',
        customHeader: 'value' 
    },
    function(frame) {
        console.log('连接成功');
        console.log('Frame:', frame);
    }
);
```

**服务端接收：**
```java
@Override
public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message);
    String userId = accessor.getFirstNativeHeader("userId");
    // userId = "Alice"
}
```

---

### 🧪 测试场景 4：消息回执

**步骤：**

1. **连接为 Alice**

2. **发送私信给 Bob**（Bob 在线）
   - 观察"私信对话"区域
   - 应该收到系统消息："消息已发送给 Bob"

3. **发送私信给不存在的用户**
   - 目标用户ID：`user999`
   - 发送消息
   - 应该收到系统消息："消息已发送给 user999"
   - （注意：当前实现不检查用户是否在线）

---

## 后端API使用

### 🔌 注入 SimpMessagingTemplate

```java
@Service
public class NotificationService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    // 你的业务逻辑...
}
```

### 📤 发送消息给指定用户

```java
/**
 * 发送私信给指定用户
 */
public void sendToUser(String userId, String message) {
    PrivateMessageResponse response = new PrivateMessageResponse(
        "system",
        "system",
        userId,
        message,
        LocalDateTime.now().toString()
    );
    
    messagingTemplate.convertAndSendToUser(
        userId,
        "/queue/private",
        response
    );
}
```

**使用示例：**
```java
@RestController
public class OrderController {
    
    @Autowired
    private NotificationService notificationService;
    
    @PostMapping("/api/orders")
    public Order createOrder(@RequestBody Order order) {
        // 创建订单逻辑...
        
        // 通知用户
        notificationService.sendToUser(
            order.getUserId(),
            "订单创建成功：" + order.getOrderNo()
        );
        
        return order;
    }
}
```

---

### 📢 广播消息给所有用户

```java
/**
 * 广播系统通知
 */
public void broadcastNotification(String message) {
    SystemNotification notification = new SystemNotification(
        "system",
        message,
        LocalDateTime.now().toString()
    );
    
    messagingTemplate.convertAndSend("/topic/messages", notification);
}
```

**使用示例：**
```java
@Component
public class MaintenanceScheduler {
    
    @Autowired
    private NotificationService notificationService;
    
    @Scheduled(cron = "0 0 21 * * ?")
    public void notifyMaintenance() {
        notificationService.broadcastNotification(
            "系统维护通知：服务将于今晚 22:00 进行升级"
        );
    }
}
```

---

### 👥 群发消息

```java
/**
 * 发送给多个指定用户
 */
public void sendToMultipleUsers(List<String> userIds, String message) {
    PrivateMessageResponse response = new PrivateMessageResponse(
        "system",
        "system",
        "group",
        message,
        LocalDateTime.now().toString()
    );
    
    for (String userId : userIds) {
        messagingTemplate.convertAndSendToUser(
            userId,
            "/queue/private",
            response
        );
    }
}
```

---

## 与原生WebSocket对比

### 代码对比

#### 原生 WebSocket

```java
// 维护用户映射
private static final ConcurrentHashMap<String, WebSocketSession> userSessions;

// 发送消息
public void sendToUser(String userId, String message) {
    WebSocketSession session = userSessions.get(userId);
    if (session != null && session.isOpen()) {
        session.sendMessage(new TextMessage(message));
    }
}
```

**客户端：**
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/chat?userId=Alice');

ws.send(JSON.stringify({
    type: 'private',
    toUserId: 'Bob',
    content: 'Hello'
}));
```

#### STOMP 方式

```java
// 无需维护映射，Spring 自动管理

@Autowired
private SimpMessagingTemplate messagingTemplate;

// 发送消息
public void sendToUser(String userId, String message) {
    messagingTemplate.convertAndSendToUser(
        userId,
        "/queue/private",
        message
    );
}
```

**客户端：**
```javascript
const socket = new SockJS('http://localhost:8080/ws/chat-stomp');
const stompClient = Stomp.over(socket);

stompClient.connect({ userId: 'Alice' }, function() {
    stompClient.subscribe('/user/queue/private', callback);
    
    stompClient.send('/app/private', {}, JSON.stringify({
        toUserId: 'Bob',
        content: 'Hello'
    }));
});
```

---

### 优缺点对比

| 特性 | 原生 WebSocket | STOMP |
|------|----------------|-------|
| **实现复杂度** | 需要手动管理 | Spring 自动处理 ✅ |
| **用户映射** | 手动维护 Map | 自动管理 ✅ |
| **消息路由** | 自己实现 | 内置路由 ✅ |
| **集群支持** | 需要 Redis | 易集成 MQ ✅ |
| **性能** | 略高 ✅ | 略低（协议开销） |
| **灵活性** | 完全自定义 ✅ | 受协议约束 |
| **学习曲线** | 简单 ✅ | 需要学习 STOMP |
| **客户端** | 任意 | 需要 STOMP 库 |

---

### 选择建议

**选择原生 WebSocket 如果：**
- ✅ 项目简单，消息量不大
- ✅ 需要完全自定义协议
- ✅ 性能要求极高
- ✅ 不需要集群部署

**选择 STOMP 如果：**
- ✅ 企业级应用
- ✅ 需要点对点消息
- ✅ 计划集群部署
- ✅ 需要与消息代理集成（RabbitMQ/ActiveMQ）
- ✅ 团队熟悉 Spring 生态

---

## 高级特性

### 1️⃣ 集成 RabbitMQ

```java
@Configuration
@EnableWebSocketMessageBroker
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 使用外部消息代理
        registry.enableStompBrokerRelay("/topic", "/queue")
            .setRelayHost("localhost")
            .setRelayPort(61613)
            .setClientLogin("guest")
            .setClientPasscode("guest");
    }
}
```

**优势：**
- ✅ 支持集群部署
- ✅ 消息持久化
- ✅ 更高的吞吐量

---

### 2️⃣ 消息拦截器

```java
@Override
public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new ChannelInterceptor() {
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message);
            
            // 拦截所有消息
            if (StompCommand.SEND.equals(accessor.getCommand())) {
                // 验证、日志、统计等
            }
            
            return message;
        }
    });
}
```

---

### 3️⃣ 异常处理

```java
@Controller
public class StompChatController {
    
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Exception e) {
        return "错误: " + e.getMessage();
    }
}
```

---

## 常见问题

### Q1: 为什么订阅 `/user/queue/private` 而不是 `/queue/private`？

**答：**`/user` 是 Spring WebSocket 的特殊前缀，表示"用户专属队列"。

- 客户端订阅：`/user/queue/private`
- Spring 自动转换为：`/user/{userId}/queue/private`
- 确保消息只发送给特定用户

---

### Q2: 如何获取当前在线用户列表？

**答：**STOMP 本身不维护在线用户列表，有两种方案：

**方案一：使用事件监听**
```java
@Component
public class WebSocketEventListener {
    
    private Set<String> onlineUsers = new ConcurrentHashSet<>();
    
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = accessor.getUser().getName();
        onlineUsers.add(userId);
    }
    
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = accessor.getUser().getName();
        onlineUsers.remove(userId);
    }
}
```

**方案二：使用 SimpUserRegistry**
```java
@Autowired
private SimpUserRegistry userRegistry;

public Set<String> getOnlineUsers() {
    return userRegistry.getUsers().stream()
        .map(SimpUser::getName)
        .collect(Collectors.toSet());
}
```

---

### Q3: 消息没有收到怎么办？

**检查清单：**

1. ✅ 用户是否已连接？
2. ✅ 用户是否订阅了正确的路径？
3. ✅ 服务端是否发送到正确的路径？
4. ✅ userId 是否正确传递？
5. ✅ 查看浏览器控制台和服务器日志

**调试方法：**
```javascript
// 启用 STOMP 调试输出
stompClient.debug = function(str) {
    console.log('STOMP:', str);
};
```

---

## 总结

✅ **STOMP 提供了企业级的消息传递解决方案**  
✅ **Spring 自动管理用户会话和消息路由**  
✅ **易于集成外部消息代理（RabbitMQ/ActiveMQ）**  
✅ **适合复杂的聊天应用和实时通知系统**  

**相关文件：**
- 配置：`src/main/java/com/yihu/agent/config/StompWebSocketConfig.java`
- 控制器：`src/main/java/com/yihu/agent/controller/StompChatController.java`
- 测试页面：`Front/test-stomp-p2p.html`

---

**祝你使用愉快！** 🎉

