package com.yihu.agent.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 原生 WebSocket 处理器 - 支持点对点消息
 * 
 * 功能特性：
 * 1. 用户身份管理：通过 userId 映射 WebSocketSession
 * 2. 点对点消息：根据目标用户ID发送私信
 * 3. 广播消息：向所有在线用户广播消息
 * 4. 在线状态：维护用户在线列表
 * 
 * 消息格式：
 * {
 *   "type": "private" | "broadcast",
 *   "fromUserId": "发送者ID",
 *   "toUserId": "接收者ID" (仅private类型需要),
 *   "content": "消息内容"
 * }
 */
@Component
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    // 用户ID -> WebSocketSession 的映射（线程安全）
    private static final ConcurrentHashMap<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    
    // 用于 JSON 序列化/反序列化
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 连接建立成功时调用
     * 从 URL 参数中提取 userId，建立用户映射关系
     * 
     * 连接示例：ws://localhost:8080/ws/chat-raw?userId=user123
     */
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        String userId = getUserIdFromSession(session);
        
        if (userId == null || userId.isEmpty()) {
            log.warn("连接缺少 userId 参数，拒绝连接: {}", session.getId());
            session.sendMessage(new TextMessage("{\"error\": \"连接必须包含 userId 参数\"}"));
            session.close();
            return;
        }
        
        // 如果该用户已经有连接，关闭旧连接
        WebSocketSession oldSession = userSessions.get(userId);
        if (oldSession != null && oldSession.isOpen()) {
            log.info("用户 {} 重复连接，关闭旧连接", userId);
            oldSession.sendMessage(new TextMessage("{\"info\": \"您的账号在其他地方登录\"}"));
            oldSession.close();
        }
        
        // 保存新连接
        userSessions.put(userId, session);
        log.info("用户 {} 连接成功，当前在线人数: {}", userId, userSessions.size());
        
        // 发送欢迎消息和在线用户列表
        WelcomeMessage welcome = new WelcomeMessage(
            "连接成功",
            userId,
            userSessions.size(),
            userSessions.keySet()
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcome)));
        
        // 通知其他用户有新用户上线
        notifyUserOnline(userId);
    }

    /**
     * 处理接收到的文本消息
     * 支持两种消息类型：
     * 1. private - 点对点私信
     * 2. broadcast - 广播消息
     */
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        String payload = message.getPayload();
        String fromUserId = getUserIdFromSession(session);
        
        log.info("收到消息来自 {}: {}", fromUserId, payload);
        
        try {
            // 解析消息
            ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
            
            // 设置发送者ID（防止客户端伪造）
            chatMessage.setFromUserId(fromUserId);
            
            // 根据消息类型分发
            switch (chatMessage.getType()) {
                case "private":
                    handlePrivateMessage(chatMessage);
                    break;
                case "broadcast":
                    handleBroadcastMessage(chatMessage);
                    break;
                default:
                    log.warn("未知的消息类型: {}", chatMessage.getType());
                    session.sendMessage(new TextMessage("{\"error\": \"未知的消息类型\"}"));
            }
        } catch (Exception e) {
            log.error("消息处理失败", e);
            session.sendMessage(new TextMessage("{\"error\": \"消息格式错误: " + e.getMessage() + "\"}"));
        }
    }

    /**
     * 连接关闭时调用
     */
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        String userId = getUserIdFromSession(session);
        
        if (userId != null) {
            userSessions.remove(userId);
            log.info("用户 {} 断开连接，状态: {}，当前在线人数: {}", userId, status, userSessions.size());
            
            // 通知其他用户该用户下线
            notifyUserOffline(userId);
        }
    }

    /**
     * 传输错误处理
     */
    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        String userId = getUserIdFromSession(session);
        log.error("传输错误，用户: {}, 错误: {}", userId, exception.getMessage());
        
        if (session.isOpen()) {
            session.close();
        }
        
        if (userId != null) {
            userSessions.remove(userId);
        }
    }

    // ==================== 核心功能方法 ====================
    
    /**
     * 处理点对点私信
     */
    private void handlePrivateMessage(ChatMessage message) throws IOException {
        String toUserId = message.getToUserId();
        String fromUserId = message.getFromUserId();
        
        if (toUserId == null || toUserId.isEmpty()) {
            log.warn("私信缺少目标用户ID");
            return;
        }
        
        WebSocketSession targetSession = userSessions.get(toUserId);
        
        if (targetSession != null && targetSession.isOpen()) {
            // 发送给目标用户
            MessageResponse response = new MessageResponse(
                "private",
                fromUserId,
                toUserId,
                message.getContent(),
                System.currentTimeMillis()
            );
            targetSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            log.info("私信已发送: {} -> {}", fromUserId, toUserId);
            
            // 可选：给发送者发送已读回执
            WebSocketSession senderSession = userSessions.get(fromUserId);
            if (senderSession != null && senderSession.isOpen()) {
                MessageResponse receipt = new MessageResponse(
                    "receipt",
                    "system",
                    fromUserId,
                    "消息已发送给 " + toUserId,
                    System.currentTimeMillis()
                );
                senderSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(receipt)));
            }
        } else {
            log.warn("目标用户 {} 不在线", toUserId);
            
            // 通知发送者目标用户不在线
            WebSocketSession senderSession = userSessions.get(fromUserId);
            if (senderSession != null && senderSession.isOpen()) {
                MessageResponse error = new MessageResponse(
                    "error",
                    "system",
                    fromUserId,
                    "用户 " + toUserId + " 不在线",
                    System.currentTimeMillis()
                );
                senderSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
            }
        }
    }
    
    /**
     * 处理广播消息
     */
    private void handleBroadcastMessage(ChatMessage message) throws IOException {
        MessageResponse response = new MessageResponse(
            "broadcast",
            message.getFromUserId(),
            "all",
            message.getContent(),
            System.currentTimeMillis()
        );
        
        String jsonMessage = objectMapper.writeValueAsString(response);
        int successCount = 0;
        
        for (Map.Entry<String, WebSocketSession> entry : userSessions.entrySet()) {
            WebSocketSession session = entry.getValue();
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(jsonMessage));
                    successCount++;
                } catch (IOException e) {
                    log.error("广播消息失败，用户: {}", entry.getKey(), e);
                }
            }
        }
        
        log.info("广播消息已发送给 {} 个用户", successCount);
    }
    
    /**
     * 通知其他用户有新用户上线
     */
    private void notifyUserOnline(String userId) throws IOException {
        SystemNotification notification = new SystemNotification(
            "user_online",
            userId + " 上线了",
            userId,
            userSessions.size()
        );
        
        String jsonMessage = objectMapper.writeValueAsString(notification);
        
        for (Map.Entry<String, WebSocketSession> entry : userSessions.entrySet()) {
            // 不通知自己
            if (!entry.getKey().equals(userId) && entry.getValue().isOpen()) {
                entry.getValue().sendMessage(new TextMessage(jsonMessage));
            }
        }
    }

    /**
     * 通知其他用户有用户下线
     */
    private void notifyUserOffline(String userId) throws IOException {
        SystemNotification notification = new SystemNotification(
            "user_offline",
            userId + " 下线了",
            userId,
            userSessions.size()
        );
        
        String jsonMessage = objectMapper.writeValueAsString(notification);
        
        for (Map.Entry<String, WebSocketSession> entry : userSessions.entrySet()) {
            if (entry.getValue().isOpen()) {
                entry.getValue().sendMessage(new TextMessage(jsonMessage));
            }
        }
    }

    // ==================== 公共API方法 ====================
    
    /**
     * 向指定用户发送消息（供外部服务调用）
     * 
     * @param userId 目标用户ID
     * @param content 消息内容
     * @return 是否发送成功
     */
    public boolean sendToUser(String userId, String content) {
        WebSocketSession session = userSessions.get(userId);
        
        if (session != null && session.isOpen()) {
            try {
                MessageResponse response = new MessageResponse(
                    "system",
                    "system",
                    userId,
                    content,
                    System.currentTimeMillis()
                );
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                log.info("系统消息已发送给用户: {}", userId);
                return true;
            } catch (IOException e) {
                log.error("发送消息失败，用户: {}", userId, e);
                return false;
            }
        }
        
        log.warn("用户 {} 不在线，无法发送消息", userId);
        return false;
    }
    
    /**
     * 广播消息给所有在线用户
     * 
     * @param content 消息内容
     * @return 成功发送的用户数量
     */
    public int broadcastToAll(String content) {
        MessageResponse response = new MessageResponse(
            "system_broadcast",
            "system",
            "all",
            content,
            System.currentTimeMillis()
        );
        
        int successCount = 0;
        
        try {
            String jsonMessage = objectMapper.writeValueAsString(response);
            
            for (Map.Entry<String, WebSocketSession> entry : userSessions.entrySet()) {
                if (entry.getValue().isOpen()) {
                    try {
                        entry.getValue().sendMessage(new TextMessage(jsonMessage));
                        successCount++;
                    } catch (IOException e) {
                        log.error("广播失败，用户: {}", entry.getKey(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("广播消息序列化失败", e);
        }
        
        log.info("系统广播已发送给 {} 个用户", successCount);
        return successCount;
    }
    
    /**
     * 获取在线用户列表
     */
    public Map<String, WebSocketSession> getOnlineUsers() {
        return new ConcurrentHashMap<>(userSessions);
    }
    
    /**
     * 获取在线用户数量
     */
    public int getOnlineUserCount() {
        return userSessions.size();
    }
    
    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(String userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

    // ==================== 辅助方法 ====================
    
    /**
     * 从 WebSocketSession 中提取 userId
     * 支持从 URL 查询参数中获取
     * 
     * @param session WebSocket会话
     * @return 用户ID，如果未找到则返回 null
     */
    private String getUserIdFromSession(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri != null) {
                Map<String, String> queryParams = UriComponentsBuilder.fromUri(uri)
                        .build()
                        .getQueryParams()
                        .toSingleValueMap();
                return queryParams.get("userId");
            }
        } catch (Exception e) {
            log.error("提取 userId 失败", e);
        }
        return null;
    }
    
    // ==================== 内部消息类 ====================
    
    /**
     * 客户端发送的消息格式
     * 
     * Note: Getter/Setter 方法供 Jackson 序列化/反序列化使用
     */
    @SuppressWarnings("unused")
    private static class ChatMessage {
        private String type;        // private | broadcast
        private String fromUserId;  // 发送者ID（服务端会覆盖此值）
        private String toUserId;    // 接收者ID（仅private类型需要）
        private String content;     // 消息内容
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getFromUserId() { return fromUserId; }
        public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
        public String getToUserId() { return toUserId; }
        public void setToUserId(String toUserId) { this.toUserId = toUserId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
    
    /**
     * 服务端发送的消息响应格式
     * 
     * Note: Getter 方法供 Jackson 序列化使用
     */
    @SuppressWarnings("unused")
    private static class MessageResponse {
        private final String type;
        private final String fromUserId;
        private final String toUserId;
        private final String content;
        private final long timestamp;
        
        public MessageResponse(String type, String fromUserId, String toUserId, String content, long timestamp) {
            this.type = type;
            this.fromUserId = fromUserId;
            this.toUserId = toUserId;
            this.content = content;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getType() { return type; }
        public String getFromUserId() { return fromUserId; }
        public String getToUserId() { return toUserId; }
        public String getContent() { return content; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * 系统通知消息
     * 
     * Note: Getter 方法供 Jackson 序列化使用
     */
    @SuppressWarnings("unused")
    private static class SystemNotification {
        private final String type;
        private final String message;
        private final String userId;
        private final int onlineCount;
        
        public SystemNotification(String type, String message, String userId, int onlineCount) {
            this.type = type;
            this.message = message;
            this.userId = userId;
            this.onlineCount = onlineCount;
        }
        
        // Getters
        public String getType() { return type; }
        public String getMessage() { return message; }
        public String getUserId() { return userId; }
        public int getOnlineCount() { return onlineCount; }
    }
    
    /**
     * 欢迎消息
     * 
     * Note: Getter 方法供 Jackson 序列化使用
     */
    @SuppressWarnings("unused")
    private static class WelcomeMessage {
        private final String message;
        private final String userId;
        private final int onlineCount;
        private final Object onlineUsers;
        
        public WelcomeMessage(String message, String userId, int onlineCount, Object onlineUsers) {
            this.message = message;
            this.userId = userId;
            this.onlineCount = onlineCount;
            this.onlineUsers = onlineUsers;
        }
        
        // Getters
        public String getMessage() { return message; }
        public String getUserId() { return userId; }
        public int getOnlineCount() { return onlineCount; }
        public Object getOnlineUsers() { return onlineUsers; }
    }
}