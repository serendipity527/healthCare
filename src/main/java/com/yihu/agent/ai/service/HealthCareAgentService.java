package com.yihu.agent.ai.service;

import com.yihu.agent.ai.graph.AgentState;
import com.yihu.agent.ai.graph.HealthCareGraph;
import com.yihu.agent.ai.model.ChatRequest;
import com.yihu.agent.ai.model.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * HealthCare AI Agent 核心服务
 * 处理用户消息，调用 LangGraph 生成回复
 */
@Slf4j
@Service
public class HealthCareAgentService {
    
    private final HealthCareGraph healthCareGraph;
    private final ChatMemoryService memoryService;
    
    public HealthCareAgentService(HealthCareGraph healthCareGraph, 
                                   ChatMemoryService memoryService) {
        this.healthCareGraph = healthCareGraph;
        this.memoryService = memoryService;
    }
    
    /**
     * 处理用户消息并生成回复
     */
    public ChatResponse processMessage(ChatRequest request) {
        log.info("处理用户消息 - userId={}, sessionId={}, message={}", 
                request.getUserId(), request.getSessionId(), request.getMessage());
        
        try {
            // 构建初始状态
            AgentState initialState = AgentState.builder()
                    .userId(request.getUserId())
                    .sessionId(request.getSessionId())
                    .userInput(request.getMessage())
                    .conversationTurn(0)
                    .finished(false)
                    .build();
            
            // 执行状态图
            AgentState finalState = healthCareGraph.execute(initialState);
            
            // 构建响应
            ChatResponse response = buildResponse(finalState);
            
            log.info("消息处理完成 - sessionId={}, responseType={}", 
                    request.getSessionId(), response.getType());
            
            return response;
            
        } catch (Exception e) {
            log.error("处理消息时发生错误", e);
            return ChatResponse.error(
                    request.getSessionId(),
                    "抱歉，我暂时无法回复。如有紧急情况，请立即拨打急救电话120。"
            );
        }
    }
    
    /**
     * 根据最终状态构建响应
     */
    private ChatResponse buildResponse(AgentState state) {
        ChatResponse.MessageType type = determineMessageType(state);
        
        return ChatResponse.builder()
                .sessionId(state.getSessionId())
                .message(state.getAiResponse())
                .type(type)
                .timestamp(System.currentTimeMillis())
                .metadata(state.getMetadata())
                .build();
    }
    
    /**
     * 确定消息类型
     */
    private ChatResponse.MessageType determineMessageType(AgentState state) {
        if (state.getIntent() == AgentState.IntentType.EMERGENCY) {
            return ChatResponse.MessageType.EMERGENCY;
        }
        return ChatResponse.MessageType.NORMAL;
    }
    
    /**
     * 清除指定会话的记忆
     */
    public void clearSession(String sessionId) {
        memoryService.clearMemory(sessionId);
        log.info("会话已清除 - sessionId={}", sessionId);
    }
    
    /**
     * 获取活跃会话数
     */
    public int getActiveSessionCount() {
        return memoryService.getActiveSessionCount();
    }
}

