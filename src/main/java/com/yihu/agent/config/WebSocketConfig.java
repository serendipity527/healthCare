package com.yihu.agent.config;

import com.yihu.agent.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

// WebSocket 配置类，用于注册自定义 WebSocket 处理器
@Configuration
@EnableWebSocket  // 启用 WebSocket 支持
public class WebSocketConfig implements WebSocketConfigurer {

    // 注入自定义的 ChatWebSocketHandler，处理实际的 websocket 消息
    @Autowired
    private ChatWebSocketHandler chatHandler;

    /**
     * 注册 WebSocket 端点
     * @param registry WebSocketHandlerRegistry
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册 chatHandler 到 /ws/chat 路径
        // .setAllowedOrigins("*") 表示允许所有来源，MVP 阶段方便调试，正式环境建议收敛白名单
        registry.addHandler(chatHandler, "/ws/chat")
                .setAllowedOrigins("*");
    }
}