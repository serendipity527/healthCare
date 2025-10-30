package com.yihu.agent.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

// websocket/ChatWebSocketHandler.java
@Component
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {
    // 用于存储所有已连接的 session
    // 需要使用线程安全的集合
    private static final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 1. 连接建立成功
        sessions.add(session);
        System.out.println("新连接加入: " + session.getId());

        // 你可以在这里给刚连接的客户端发送一条欢迎消息
        // session.sendMessage(new TextMessage("欢迎连接 WebSocket 服务！"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 2. 收到客户端发来的文本消息
        String payload = message.getPayload(); // 获取消息内容
        System.out.println("收到消息来自 " + session.getId() + ": " + payload);

        // 示例：将收到的消息广播给所有连接的客户端（包括发送者自己）
        broadcastMessage(new TextMessage("广播: " + payload));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 3. 连接关闭
        sessions.remove(session);
        System.out.println("连接关闭: " + session.getId() + "，状态: " + status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // 4. 发生传输错误
        System.err.println("发生错误: " + session.getId() + "，错误: " + exception.getMessage());
        if (session.isOpen()) {
            session.close();
        }
        sessions.remove(session);
    }

    /**
     * 辅助方法：向所有连接的客户端广播消息
     */
    public void broadcastMessage(TextMessage message) throws IOException {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        }
    }

    /**
     * 辅助方法：向特定 session 发送消息（如果需要点对点）
     */
    public void sendMessageToSession(String sessionId, TextMessage message) throws IOException {
        for (WebSocketSession session : sessions) {
            if (session.getId().equals(sessionId) && session.isOpen()) {
                session.sendMessage(message);
                break;
            }
        }
    }
}