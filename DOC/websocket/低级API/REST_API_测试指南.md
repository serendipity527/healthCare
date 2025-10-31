# WebSocket REST API 测试指南

## 📋 目录
1. [API 概览](#api-概览)
2. [接口详情](#接口详情)
3. [测试示例](#测试示例)
4. [Postman Collection](#postman-collection)
5. [使用场景](#使用场景)

---

## API 概览

WebSocket REST API 允许后端服务通过 HTTP 接口操作 WebSocket 连接，主动推送消息给客户端。

### 🌐 Base URL

```
http://localhost:8080/api/websocket
```

### 📡 可用接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/send` | 发送消息给指定用户 |
| POST | `/broadcast` | 广播消息给所有在线用户 |
| POST | `/batch-send` | 批量发送消息 |
| GET | `/online/{userId}` | 检查用户是否在线 |
| GET | `/stats` | 获取在线用户统计 |

---

## 接口详情

### 1️⃣ 发送消息给指定用户

**功能**：向指定的在线用户发送消息

**请求**：
```http
POST /api/websocket/send
Content-Type: application/json

{
  "userId": "user123",
  "message": "您有新的订单通知"
}
```

**成功响应**（200 OK）：
```json
{
  "success": true,
  "message": "消息已发送",
  "timestamp": 1698765432000
}
```

**失败响应**（404 Not Found）：
```json
{
  "success": false,
  "message": "用户不在线",
  "timestamp": 1698765432000
}
```

**使用场景**：
- 订单状态更新通知
- 私信提醒
- 账户安全提醒

---

### 2️⃣ 广播消息给所有在线用户

**功能**：向所有在线用户发送系统通知

**请求**：
```http
POST /api/websocket/broadcast
Content-Type: application/json

{
  "message": "系统维护通知：服务将于今晚 22:00 进行升级"
}
```

**成功响应**（200 OK）：
```json
{
  "success": true,
  "message": "消息已广播给 5 个用户",
  "timestamp": 1698765432000,
  "data": {
    "receivedCount": 5
  }
}
```

**使用场景**：
- 系统维护通知
- 重要公告
- 活动推广

---

### 3️⃣ 批量发送消息

**功能**：向多个指定用户发送消息

**请求**：
```http
POST /api/websocket/batch-send
Content-Type: application/json

{
  "userIds": ["user001", "user002", "user003"],
  "message": "您有新的活动推荐"
}
```

**成功响应**（200 OK）：
```json
{
  "success": true,
  "message": "发送完成：成功 2，失败 1",
  "timestamp": 1698765432000,
  "data": {
    "totalCount": 3,
    "successCount": 2,
    "failCount": 1
  }
}
```

**使用场景**：
- 群发活动通知
- 批量消息推送
- VIP 用户通知

---

### 4️⃣ 检查用户是否在线

**功能**：查询指定用户的在线状态

**请求**：
```http
GET /api/websocket/online/user123
```

**成功响应**（200 OK）：
```json
{
  "success": true,
  "message": "查询成功",
  "timestamp": 1698765432000,
  "data": {
    "userId": "user123",
    "isOnline": true
  }
}
```

**使用场景**：
- 显示用户在线状态
- 消息发送前检查
- 好友在线状态

---

### 5️⃣ 获取在线用户统计

**功能**：获取当前所有在线用户的统计信息

**请求**：
```http
GET /api/websocket/stats
```

**成功响应**（200 OK）：
```json
{
  "success": true,
  "message": "查询成功",
  "timestamp": 1698765432000,
  "data": {
    "onlineCount": 3,
    "onlineUsers": ["user001", "user002", "user003"]
  }
}
```

**使用场景**：
- 管理后台监控
- 系统统计报表
- 并发用户分析

---

## 测试示例

### 🧪 测试场景 1：完整流程测试

**步骤**：

1. **启动 WebSocket 客户端**
   - 打开 `Front/test-point-to-point.html`
   - 连接 3 个用户：`user001`, `user002`, `user003`

2. **查看在线用户**
   ```bash
   curl http://localhost:8080/api/websocket/stats
   ```
   
   **期望结果**：
   ```json
   {
     "success": true,
     "data": {
       "onlineCount": 3,
       "onlineUsers": ["user001", "user002", "user003"]
     }
   }
   ```

3. **发送消息给单个用户**
   ```bash
   curl -X POST http://localhost:8080/api/websocket/send \
     -H "Content-Type: application/json" \
     -d '{
       "userId": "user001",
       "message": "你好，这是来自后端的消息"
     }'
   ```
   
   **期望结果**：
   - `user001` 的浏览器收到消息
   - API 返回 `{"success": true, "message": "消息已发送"}`

4. **广播消息**
   ```bash
   curl -X POST http://localhost:8080/api/websocket/broadcast \
     -H "Content-Type: application/json" \
     -d '{
       "message": "系统通知：大家好！"
     }'
   ```
   
   **期望结果**：
   - 所有 3 个用户都收到消息
   - API 返回 `{"success": true, "data": {"receivedCount": 3}}`

---

### 🧪 测试场景 2：用户不在线

**步骤**：

1. **发送消息给不存在的用户**
   ```bash
   curl -X POST http://localhost:8080/api/websocket/send \
     -H "Content-Type: application/json" \
     -d '{
       "userId": "user999",
       "message": "测试消息"
     }'
   ```
   
   **期望结果**：
   ```json
   {
     "success": false,
     "message": "用户不在线"
   }
   ```
   HTTP 状态码：404

---

### 🧪 测试场景 3：批量发送（部分用户在线）

**步骤**：

1. **连接 2 个用户**
   - `user001` 在线
   - `user002` 在线

2. **批量发送给 3 个用户（包含 1 个不在线）**
   ```bash
   curl -X POST http://localhost:8080/api/websocket/batch-send \
     -H "Content-Type: application/json" \
     -d '{
       "userIds": ["user001", "user002", "user999"],
       "message": "批量测试消息"
     }'
   ```
   
   **期望结果**：
   ```json
   {
     "success": true,
     "message": "发送完成：成功 2，失败 1",
     "data": {
       "totalCount": 3,
       "successCount": 2,
       "failCount": 1
     }
   }
   ```

---

## Postman Collection

### 📦 导入步骤

1. 打开 Postman
2. 点击 **Import**
3. 选择 **Raw text**
4. 粘贴下面的 JSON
5. 点击 **Continue** → **Import**

### 📄 Collection JSON

```json
{
  "info": {
    "name": "WebSocket REST API",
    "description": "WebSocket 点对点消息系统 REST API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "1. 发送消息给指定用户",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"userId\": \"user001\",\n  \"message\": \"你好，这是一条测试消息\"\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/websocket/send",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "websocket", "send"]
        }
      }
    },
    {
      "name": "2. 广播消息给所有用户",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"message\": \"系统通知：大家好！\"\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/websocket/broadcast",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "websocket", "broadcast"]
        }
      }
    },
    {
      "name": "3. 批量发送消息",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"userIds\": [\"user001\", \"user002\", \"user003\"],\n  \"message\": \"批量消息测试\"\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/websocket/batch-send",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "websocket", "batch-send"]
        }
      }
    },
    {
      "name": "4. 检查用户是否在线",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/api/websocket/online/user001",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "websocket", "online", "user001"]
        }
      }
    },
    {
      "name": "5. 获取在线用户统计",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8080/api/websocket/stats",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "websocket", "stats"]
        }
      }
    }
  ]
}
```

---

## 使用场景

### 💼 场景 1：电商订单通知

```java
@Service
public class OrderService {
    
    @Autowired
    private ChatWebSocketHandler webSocketHandler;
    
    public void notifyOrderStatus(String userId, Order order) {
        String message = String.format(
            "您的订单 %s 已发货，预计 3 天内送达",
            order.getOrderNo()
        );
        
        webSocketHandler.sendToUser(userId, message);
    }
}
```

**REST API 方式**：
```bash
curl -X POST http://localhost:8080/api/websocket/send \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "message": "您的订单 ORDER-2024-001 已发货，预计 3 天内送达"
  }'
```

---

### 💼 场景 2：系统维护通知

```java
@Service
public class SystemMaintenanceService {
    
    @Autowired
    private ChatWebSocketHandler webSocketHandler;
    
    @Scheduled(cron = "0 0 21 * * ?") // 每天晚上 9 点
    public void notifyMaintenance() {
        String message = "系统维护通知：服务将于今晚 22:00 进行升级，预计耗时 1 小时";
        
        int count = webSocketHandler.broadcastToAll(message);
        log.info("维护通知已发送给 {} 个用户", count);
    }
}
```

**REST API 方式**：
```bash
curl -X POST http://localhost:8080/api/websocket/broadcast \
  -H "Content-Type: application/json" \
  -d '{
    "message": "系统维护通知：服务将于今晚 22:00 进行升级，预计耗时 1 小时"
  }'
```

---

### 💼 场景 3：活动推送（精准用户）

```java
@Service
public class MarketingService {
    
    @Autowired
    private ChatWebSocketHandler webSocketHandler;
    
    public void pushCampaign(Campaign campaign) {
        // 获取目标用户列表
        List<String> vipUsers = userRepository.findVipUsers();
        
        String message = String.format(
            "VIP专享活动：%s，限时优惠中！",
            campaign.getTitle()
        );
        
        int successCount = 0;
        for (String userId : vipUsers) {
            if (webSocketHandler.sendToUser(userId, message)) {
                successCount++;
            }
        }
        
        log.info("活动推送完成：目标 {} 人，成功 {} 人", vipUsers.size(), successCount);
    }
}
```

**REST API 方式**：
```bash
curl -X POST http://localhost:8080/api/websocket/batch-send \
  -H "Content-Type: application/json" \
  -d '{
    "userIds": ["vip001", "vip002", "vip003"],
    "message": "VIP专享活动：春季大促，全场 8 折！"
  }'
```

---

### 💼 场景 4：在线客服提醒

```java
@RestController
public class CustomerServiceController {
    
    @Autowired
    private ChatWebSocketHandler webSocketHandler;
    
    @PostMapping("/api/customer-service/notify-agent")
    public ResponseEntity<?> notifyAgent(@RequestParam String agentId, 
                                         @RequestParam String customerId) {
        String message = String.format(
            "新客户咨询：%s 正在等待服务",
            customerId
        );
        
        boolean sent = webSocketHandler.sendToUser(agentId, message);
        
        if (sent) {
            return ResponseEntity.ok("客服已通知");
        } else {
            return ResponseEntity.status(404).body("客服不在线");
        }
    }
}
```

---

## 集成示例

### 🔗 Spring 定时任务

```java
@Component
public class ScheduledTasks {
    
    @Autowired
    private ChatWebSocketHandler webSocketHandler;
    
    // 每小时检查并通知在线用户
    @Scheduled(fixedRate = 3600000)
    public void hourlyNotification() {
        int onlineCount = webSocketHandler.getOnlineUserCount();
        
        if (onlineCount > 0) {
            webSocketHandler.broadcastToAll("温馨提示：建议您每小时休息 5 分钟");
        }
    }
}
```

---

### 🔗 事件监听

```java
@Component
public class UserEventListener {
    
    @Autowired
    private ChatWebSocketHandler webSocketHandler;
    
    @EventListener
    public void handleUserRegistrationEvent(UserRegistrationEvent event) {
        String userId = event.getUserId();
        String message = "欢迎加入！您已成功注册";
        
        // 延迟 3 秒发送欢迎消息（等待用户建立 WebSocket 连接）
        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS)
            .execute(() -> webSocketHandler.sendToUser(userId, message));
    }
}
```

---

## 错误处理

### ❌ 常见错误

| 错误 | 原因 | 解决方案 |
|------|------|---------|
| 400 Bad Request | 请求参数缺失或格式错误 | 检查请求体格式 |
| 404 Not Found | 用户不在线 | 先调用 `/online/{userId}` 检查 |
| 500 Internal Server Error | 服务器内部错误 | 查看服务器日志 |

---

## 性能建议

### ⚡ 1. 批量操作

当需要通知多个用户时，使用 `/batch-send` 而不是多次调用 `/send`：

```bash
# ❌ 不推荐：多次调用
for userId in user001 user002 user003; do
  curl -X POST http://localhost:8080/api/websocket/send \
    -H "Content-Type: application/json" \
    -d "{\"userId\": \"$userId\", \"message\": \"测试\"}"
done

# ✅ 推荐：批量发送
curl -X POST http://localhost:8080/api/websocket/batch-send \
  -H "Content-Type: application/json" \
  -d '{
    "userIds": ["user001", "user002", "user003"],
    "message": "测试"
  }'
```

---

### ⚡ 2. 异步处理

对于大量用户的通知，使用异步处理：

```java
@Service
public class AsyncNotificationService {
    
    @Autowired
    private ChatWebSocketHandler webSocketHandler;
    
    @Async
    public CompletableFuture<Integer> notifyUsersAsync(List<String> userIds, String message) {
        int successCount = 0;
        
        for (String userId : userIds) {
            if (webSocketHandler.sendToUser(userId, message)) {
                successCount++;
            }
        }
        
        return CompletableFuture.completedFuture(successCount);
    }
}
```

---

## 监控和日志

### 📊 查看日志

服务器会记录所有 REST API 调用：

```
2024-10-31 10:23:45 INFO  WebSocketApiController : REST API: 发送消息给用户 user123, 内容: 您有新的订单通知
2024-10-31 10:24:12 INFO  WebSocketApiController : REST API: 广播消息, 内容: 系统维护通知
2024-10-31 10:25:30 INFO  WebSocketApiController : REST API: 批量发送消息给 5 个用户
```

---

## 安全建议

### 🔒 1. 添加认证

```java
@RestController
@RequestMapping("/api/websocket")
public class WebSocketApiController {
    
    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')") // 仅管理员可调用
    public ResponseEntity<?> sendToUser(...) {
        // ...
    }
}
```

---

### 🔒 2. API Key 验证

```java
@PostMapping("/send")
public ResponseEntity<?> sendToUser(
    @RequestHeader("X-API-Key") String apiKey,
    @RequestBody SendMessageRequest request) {
    
    if (!isValidApiKey(apiKey)) {
        return ResponseEntity.status(401).body("无效的 API Key");
    }
    
    // ...
}
```

---

### 🔒 3. 频率限制

```java
@PostMapping("/send")
@RateLimiter(name = "websocketApi", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> sendToUser(...) {
    // ...
}

public ResponseEntity<?> rateLimitFallback(Exception e) {
    return ResponseEntity.status(429).body("请求过于频繁，请稍后再试");
}
```

---

## 总结

✅ REST API 提供了便捷的方式来操作 WebSocket 连接  
✅ 支持单发、广播、批量发送等多种模式  
✅ 可与其他业务系统轻松集成  
✅ 适合系统通知、消息推送等场景  

**相关文件**：
- Controller 实现：`src/main/java/com/yihu/agent/controller/WebSocketApiController.java`
- WebSocket Handler：`src/main/java/com/yihu/agent/websocket/ChatWebSocketHandler.java`

---

**祝你测试愉快！** 🎉

