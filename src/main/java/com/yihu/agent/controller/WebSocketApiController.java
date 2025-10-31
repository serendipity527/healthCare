package com.yihu.agent.controller;

import com.yihu.agent.websocket.ChatWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket API 控制器
 * 
 * 提供 REST API 来操作 WebSocket 连接和发送消息
 * 
 * 使用场景：
 * 1. 后端服务主动推送消息给用户
 * 2. 系统通知
 * 3. 管理员广播消息
 * 4. 查询在线用户
 */
@RestController
@RequestMapping("/api/websocket")
@CrossOrigin(origins = "*")
@Slf4j
public class WebSocketApiController {

    @Autowired
    private ChatWebSocketHandler webSocketHandler;

    /**
     * 发送消息给指定用户
     * 
     * POST /api/websocket/send
     * {
     *   "userId": "user123",
     *   "message": "您有新的订单通知"
     * }
     * 
     * @param request 请求体
     * @return 发送结果
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendToUser(@RequestBody SendMessageRequest request) {
        log.info("REST API: 发送消息给用户 {}, 内容: {}", request.getUserId(), request.getMessage());
        
        if (request.getUserId() == null || request.getUserId().isEmpty()) {
            return ResponseEntity.badRequest().body(createResponse(false, "userId 不能为空", null));
        }
        
        if (request.getMessage() == null || request.getMessage().isEmpty()) {
            return ResponseEntity.badRequest().body(createResponse(false, "message 不能为空", null));
        }
        
        boolean success = webSocketHandler.sendToUser(request.getUserId(), request.getMessage());
        
        if (success) {
            return ResponseEntity.ok(createResponse(true, "消息已发送", null));
        } else {
            return ResponseEntity.status(404).body(createResponse(false, "用户不在线", null));
        }
    }

    /**
     * 广播消息给所有在线用户
     * 
     * POST /api/websocket/broadcast
     * {
     *   "message": "系统维护通知：服务将于今晚 22:00 进行升级"
     * }
     * 
     * @param request 请求体
     * @return 发送结果
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> broadcast(@RequestBody BroadcastRequest request) {
        log.info("REST API: 广播消息, 内容: {}", request.getMessage());
        
        if (request.getMessage() == null || request.getMessage().isEmpty()) {
            return ResponseEntity.badRequest().body(createResponse(false, "message 不能为空", null));
        }
        
        int count = webSocketHandler.broadcastToAll(request.getMessage());
        
        Map<String, Object> data = new HashMap<>();
        data.put("receivedCount", count);
        
        return ResponseEntity.ok(createResponse(
            true, 
            String.format("消息已广播给 %d 个用户", count), 
            data
        ));
    }

    /**
     * 检查用户是否在线
     * 
     * GET /api/websocket/online/{userId}
     * 
     * @param userId 用户ID
     * @return 在线状态
     */
    @GetMapping("/online/{userId}")
    public ResponseEntity<Map<String, Object>> checkUserOnline(@PathVariable String userId) {
        log.info("REST API: 检查用户在线状态: {}", userId);
        
        boolean isOnline = webSocketHandler.isUserOnline(userId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("isOnline", isOnline);
        
        return ResponseEntity.ok(createResponse(true, "查询成功", data));
    }

    /**
     * 获取在线用户统计
     * 
     * GET /api/websocket/stats
     * 
     * @return 在线用户统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("REST API: 获取在线用户统计");
        
        int onlineCount = webSocketHandler.getOnlineUserCount();
        var onlineUsers = webSocketHandler.getOnlineUsers().keySet();
        
        Map<String, Object> data = new HashMap<>();
        data.put("onlineCount", onlineCount);
        data.put("onlineUsers", onlineUsers);
        
        return ResponseEntity.ok(createResponse(true, "查询成功", data));
    }

    /**
     * 批量发送消息
     * 
     * POST /api/websocket/batch-send
     * {
     *   "userIds": ["user001", "user002", "user003"],
     *   "message": "您有新的活动推荐"
     * }
     * 
     * @param request 请求体
     * @return 发送结果
     */
    @PostMapping("/batch-send")
    public ResponseEntity<Map<String, Object>> batchSend(@RequestBody BatchSendRequest request) {
        log.info("REST API: 批量发送消息给 {} 个用户", request.getUserIds().size());
        
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            return ResponseEntity.badRequest().body(createResponse(false, "userIds 不能为空", null));
        }
        
        if (request.getMessage() == null || request.getMessage().isEmpty()) {
            return ResponseEntity.badRequest().body(createResponse(false, "message 不能为空", null));
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (String userId : request.getUserIds()) {
            boolean success = webSocketHandler.sendToUser(userId, request.getMessage());
            if (success) {
                successCount++;
            } else {
                failCount++;
            }
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("totalCount", request.getUserIds().size());
        data.put("successCount", successCount);
        data.put("failCount", failCount);
        
        return ResponseEntity.ok(createResponse(
            true, 
            String.format("发送完成：成功 %d，失败 %d", successCount, failCount), 
            data
        ));
    }

    // ==================== 辅助方法 ====================
    
    /**
     * 创建统一的响应格式
     */
    private Map<String, Object> createResponse(boolean success, String message, Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        
        if (data != null) {
            response.put("data", data);
        }
        
        return response;
    }

    // ==================== 请求DTO ====================
    
    /**
     * 发送消息请求
     */
    public static class SendMessageRequest {
        private String userId;
        private String message;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * 广播消息请求
     */
    public static class BroadcastRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * 批量发送消息请求
     */
    public static class BatchSendRequest {
        private java.util.List<String> userIds;
        private String message;

        public java.util.List<String> getUserIds() {
            return userIds;
        }

        public void setUserIds(java.util.List<String> userIds) {
            this.userIds = userIds;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}

