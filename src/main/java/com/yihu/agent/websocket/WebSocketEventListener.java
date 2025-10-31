package com.yihu.agent.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 事件监听器
 * 
 * 监听用户连接和断开事件，维护在线用户列表
 */
@Component
@Slf4j
public class WebSocketEventListener {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 在线用户集合（线程安全）
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    /**
     * 用户连接事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        Principal user = headerAccessor.getUser();
        if (user != null) {
            String userId = user.getName();
            onlineUsers.add(userId);
            
            log.info("用户上线: {}, 当前在线人数: {}", userId, onlineUsers.size());
            
            // 1. 先给新连接的用户发送当前的在线用户列表
            sendUserListToUser(userId, onlineUsers);
            
            // 2. 再广播用户上线通知给其他人
            broadcastUserStatus(userId, "online", onlineUsers);
        }
    }

    /**
     * 用户断开连接事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        Principal user = headerAccessor.getUser();
        if (user != null) {
            String userId = user.getName();
            onlineUsers.remove(userId);
            
            log.info("用户下线: {}, 当前在线人数: {}", userId, onlineUsers.size());
            
            // 广播用户下线通知
            broadcastUserStatus(userId, "offline", onlineUsers);
        }
    }

    /**
     * 发送在线用户列表给指定用户（用户刚连接时调用）
     */
    private void sendUserListToUser(String userId, Set<String> currentOnlineUsers) {
        Map<String, Object> userListMessage = Map.of(
            "type", "user_list",
            "onlineUsers", currentOnlineUsers,
            "onlineCount", currentOnlineUsers.size()
        );
        
        // 发送给指定用户
        messagingTemplate.convertAndSendToUser(
            userId,
            "/queue/user-list",
            userListMessage
        );
        
        log.debug("已发送在线用户列表给用户: {}, 在线人数: {}", userId, currentOnlineUsers.size());
    }
    
    /**
     * 广播用户状态变化
     */
    private void broadcastUserStatus(String userId, String status, Set<String> currentOnlineUsers) {
        Map<String, Object> statusMessage = Map.of(
            "type", "user_status",
            "userId", userId,
            "status", status,
            "onlineUsers", currentOnlineUsers,
            "onlineCount", currentOnlineUsers.size()
        );
        
        messagingTemplate.convertAndSend("/topic/user-status", statusMessage);
    }

    /**
     * 获取在线用户列表（供其他服务调用）
     */
    public Set<String> getOnlineUsers() {
        return Set.copyOf(onlineUsers);
    }

    /**
     * 获取在线用户数量
     */
    public int getOnlineUserCount() {
        return onlineUsers.size();
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(String userId) {
        return onlineUsers.contains(userId);
    }
}

