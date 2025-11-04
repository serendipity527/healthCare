package com.yihu.agent.controller;

import com.yihu.agent.ai.model.ChatRequest;
import com.yihu.agent.ai.model.ChatResponse;
import com.yihu.agent.ai.service.HealthCareAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * AI 对话 WebSocket 控制器
 * 处理客户端的 AI 对话请求
 */
@Slf4j
@Controller
public class AiChatController {
    
    private final HealthCareAgentService agentService;
    private final SimpMessagingTemplate messagingTemplate;
    
    public AiChatController(HealthCareAgentService agentService, 
                           SimpMessagingTemplate messagingTemplate) {
        this.agentService = agentService;
        this.messagingTemplate = messagingTemplate;
    }
    
    /**
     * 处理 AI 对话消息
     * 客户端发送到: /app/chat/ai
     * 服务器回复到: /user/queue/ai-reply
     */
    @MessageMapping("/chat/ai")
    public void handleAiChat(ChatRequest request) {
        log.info("收到 AI 对话请求 - userId={}, sessionId={}, message={}", 
                request.getUserId(), request.getSessionId(), request.getMessage());
        
        try {
            // 先发送"思考中"状态
            ChatResponse thinkingResponse = ChatResponse.thinking(request.getSessionId());
            sendToUser(request.getUserId(), thinkingResponse);
            
            // 处理消息并生成回复
            ChatResponse response = agentService.processMessage(request);
            
            // 发送 AI 回复
            sendToUser(request.getUserId(), response);
            
            log.info("AI 回复已发送 - userId={}, sessionId={}", 
                    request.getUserId(), request.getSessionId());
            
        } catch (Exception e) {
            log.error("处理 AI 对话时发生错误", e);
            
            // 发送错误响应
            ChatResponse errorResponse = ChatResponse.error(
                    request.getSessionId(),
                    "抱歉，处理您的消息时出现了问题。请稍后再试。"
            );
            sendToUser(request.getUserId(), errorResponse);
        }
    }
    
    /**
     * 发送消息给指定用户
     */
    private void sendToUser(String userId, ChatResponse response) {
        String destination = "/queue/ai-reply";
        messagingTemplate.convertAndSendToUser(userId, destination, response);
        log.debug("消息已发送 - userId={}, destination={}", userId, destination);
    }
}

