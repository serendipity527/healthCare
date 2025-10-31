package com.yihu.agent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // 👈 **魔法开关！** // 它启用了 STOMP 消息代理功能。
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理（“邮局”的内部规则）
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // 1. 设置“应用目的地”前缀 (客户端 -> 服务器)
        // 告诉 Spring，凡是目的地以 "/app" 开头的消息，
        // 都应该被路由到 @MessageMapping 注解的方法。
        registry.setApplicationDestinationPrefixes("/app");

        // 2. 设置“消息代理”目的地 (服务器 -> 客户端)
        // 告诉 Spring，凡是目的地以 "/topic" 或 "/queue" 开头的消息，
        // 都应该路由到 STOMP 的内置内存代理 (Broker) 上。
        // 代理会负责将消息广播给所有订阅了这些主题的客户端。
        registry.enableSimpleBroker("/topic", "/queue");

        // (可选) 如果你用 RabbitMQ/ActiveMQ 替换内存代理，就这样写:
        // registry.enableStompBrokerRelay("/topic", "/queue")
        //     .setRelayHost("localhost")
        //     .setRelayPort(61613)
        //     .setClientLogin("guest")
        //     .setClientPasscode("guest");
    }

    /**
     * 注册 STOMP 端点（“邮局”的入口大门）
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        // 1. 注册一个 STOMP 端点
        // 这是客户端用来连接 WebSocket 服务器的 HTTP URL。
        // 客户端将首先连接到这个 URL 来进行 WebSocket 握手。
        registry.addEndpoint("/ws/chat-stomp")
                // 允许所有来源访问（开发环境配置，生产环境请设置具体域名）
                // 使用 setAllowedOriginPatterns 支持更灵活的跨域配置，包括 localhost:*
                .setAllowedOriginPatterns("*")
                // 2. 启用 SockJS 备选方案
                // .withSockJS() 是一个关键的"降级"选项。
                // 如果浏览器不支持原生 WebSocket，它会自动切换到
                // 长轮询 (Long Polling) 等技术，来模拟 WebSocket 行为。
                // 这极大地提高了浏览器的兼容性。
                .withSockJS();
    }
}