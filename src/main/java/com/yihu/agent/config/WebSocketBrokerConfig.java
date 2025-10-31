package com.yihu.agent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // 👈 核心注解：启用 WebSocket 消息代理
public class WebSocketBrokerConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 1. 配置消息代理 (Message Broker)
        //    启用一个简单的基于内存的消息代理，并设置目标前缀
        //    客户端订阅这些前缀时，消息会路由到代理
        registry.enableSimpleBroker("/topic", "/queue");

        // 2. 设置应用目标前缀 (Application Destination Prefix)
        //    客户端发送消息到 STOMP 代理时，需要带上这个前缀 (例如 /app/hello)
        //    这些消息会被路由到 @MessageMapping 注解的方法
        registry.setApplicationDestinationPrefixes("/app");

        // (可选) 如果使用 /queue，可以设置用户目标前缀，用于点对点消息
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 3. 注册 STOMP 端点 (Endpoint)
        //    这是客户端用于连接到 WebSocket 服务器的端点
        registry.addEndpoint("/gs-guide-websocket") // 你的连接端点 URL
//                .withSockJS(); // 👈 启用 SockJS 作为备选方案，以防浏览器不支持 WebSocket
        ;
    }
}
