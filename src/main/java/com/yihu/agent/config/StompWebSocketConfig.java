package com.yihu.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP WebSocket 配置 - 支持点对点消息
 * 
 * 核心功能：
 * 1. 配置消息代理（/topic 广播，/queue 点对点）
 * 2. 配置用户目的地前缀（/user）
 * 3. 用户身份认证（从连接参数获取 userId）
 * 4. 支持 SockJS 降级方案
 */
@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理
     * 
     * /topic - 用于广播消息（一对多）
     * /queue - 用于点对点消息（一对一）
     * /user - 用户专属队列前缀
     */
    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        // 1. 设置应用目的地前缀（客户端发送消息时使用）
        registry.setApplicationDestinationPrefixes("/app");

        // 2. 启用简单消息代理
        // /topic - 广播消息（所有订阅者都会收到）
        // /queue - 点对点消息（只有目标用户会收到）
        registry.enableSimpleBroker("/topic", "/queue");

        // 3. 设置用户目的地前缀（重要！用于点对点消息）
        // 客户端订阅：/user/queue/private
        // 实际路径：/user/{userId}/queue/private
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * 注册 STOMP 端点
     */
    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        // AI 对话端点
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        // 保留原有的聊天端点（兼容性）
        registry.addEndpoint("/ws/chat-stomp")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * 配置客户端入站通道拦截器
     * 用于从连接参数中提取用户身份信息
     */
    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // 从 STOMP 连接头中获取 userId
                    String userId = accessor.getFirstNativeHeader("userId");
                    
                    if (userId != null && !userId.isEmpty()) {
                        // 设置用户身份（关键！）
                        accessor.setUser(() -> userId);
                        log.info("STOMP 用户连接: {}", userId);
                    } else {
                        log.warn("STOMP 连接缺少 userId 参数");
                    }
                }
                
                return message;
            }
        });
    }
}