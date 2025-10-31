package com.yihu.agent.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * STOMP 聊天控制器 - 支持广播和点对点消息
 * 
 * 核心功能：
 * 1. 广播消息 - 所有订阅者都会收到
 * 2. 点对点私信 - 只有目标用户会收到
 * 3. 消息回执 - 发送方收到确认
 */
@Controller
@Slf4j
public class StompChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 广播消息处理
     * 
     * 客户端发送：stompClient.send("/app/chat", {}, JSON.stringify(message))
     * 客户端订阅：stompClient.subscribe("/topic/messages", callback)
     * 
     * @param message 消息内容
     * @param principal 当前用户身份
     * @return 广播响应
     */
    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public BroadcastMessage handleChatMessage(@Payload ChatMessage message, Principal principal) {
        String sender = principal != null ? principal.getName() : "匿名用户";
        String content = HtmlUtils.htmlEscape(message.content());

        log.info("广播消息 - 发送者: {}, 内容: {}", sender, content);

        return new BroadcastMessage(
            "broadcast",
            sender,
            content,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    /**
     * 点对点私信处理（方式一：使用 @SendToUser）
     * 
     * 客户端发送：stompClient.send("/app/private", {}, JSON.stringify(message))
     * 目标用户订阅：stompClient.subscribe("/user/queue/private", callback)
     * 
     * @param message 私信内容
     * @param principal 当前用户身份
     * @return 私信响应（只发送给目标用户）
     */
    @MessageMapping("/private")
    public void handlePrivateMessage(@Payload PrivateMessage message, Principal principal) {
        String fromUser = principal != null ? principal.getName() : "匿名用户";
        String toUser = message.toUserId();
        String content = HtmlUtils.htmlEscape(message.content());

        log.info("私信消息 - 从: {} 到: {}, 内容: {}", fromUser, toUser, content);

        // 构造响应消息
        PrivateMessageResponse response = new PrivateMessageResponse(
            "private",
            fromUser,
            toUser,
            content,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        // 发送给目标用户（核心方法！）
        // convertAndSendToUser 会自动添加 /user/{toUser} 前缀
        // 所以实际发送到：/user/{toUser}/queue/private
        messagingTemplate.convertAndSendToUser(
            toUser,           // 目标用户ID
            "/queue/private", // 队列路径
            response         // 消息内容
        );

        // 可选：给发送方发送回执
        PrivateMessageResponse receipt = new PrivateMessageResponse(
            "receipt",
            "system",
            fromUser,
            "消息已发送给 " + toUser,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        messagingTemplate.convertAndSendToUser(
            fromUser,
            "/queue/receipt",
            receipt
        );
    }

    /**
     * 群发消息（发送给多个指定用户）
     * 
     * @param message 群发消息内容
     * @param principal 当前用户身份
     */
    @MessageMapping("/group")
    public void handleGroupMessage(@Payload GroupMessage message, Principal principal) {
        String fromUser = principal != null ? principal.getName() : "匿名用户";
        String content = HtmlUtils.htmlEscape(message.content());

        log.info("群发消息 - 发送者: {}, 目标用户: {}, 内容: {}", 
            fromUser, message.toUserIds(), content);

        // 构造响应消息
        PrivateMessageResponse response = new PrivateMessageResponse(
            "group",
            fromUser,
            "group",
            content,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        // 发送给每个目标用户
        int successCount = 0;
        for (String toUser : message.toUserIds()) {
            try {
                messagingTemplate.convertAndSendToUser(
                    toUser,
                    "/queue/private",
                    response
                );
                successCount++;
            } catch (Exception e) {
                log.error("发送群发消息失败，目标用户: {}", toUser, e);
            }
        }

        // 发送回执给发送方
        PrivateMessageResponse receipt = new PrivateMessageResponse(
            "receipt",
            "system",
            fromUser,
            String.format("群发消息已发送给 %d 个用户", successCount),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        messagingTemplate.convertAndSendToUser(
            fromUser,
            "/queue/receipt",
            receipt
        );
    }

    /**
     * 用户状态通知（上线/下线）
     * 可由其他服务调用
     */
    public void notifyUserStatus(String userId, String status) {
        UserStatusNotification notification = new UserStatusNotification(
            "status",
            userId,
            status,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        // 广播给所有订阅者
        messagingTemplate.convertAndSend("/topic/user-status", notification);
    }
}

// ==================== 消息DTO类 ====================

/**
 * 基础聊天消息
 */
record ChatMessage(String content) {}

/**
 * 广播消息响应
 */
record BroadcastMessage(
    String type,
    String sender,
    String content,
    String timestamp
) {}

/**
 * 点对点私信请求
 */
record PrivateMessage(
    String toUserId,
    String content
) {}

/**
 * 点对点私信响应
 */
record PrivateMessageResponse(
    String type,
    String fromUserId,
    String toUserId,
    String content,
    String timestamp
) {}

/**
 * 群发消息
 */
record GroupMessage(
    java.util.List<String> toUserIds,
    String content
) {}

/**
 * 用户状态通知
 */
record UserStatusNotification(
    String type,
    String userId,
    String status,
    String timestamp
) {}
