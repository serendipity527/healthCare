package com.yihu.agent.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 对话响应消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * AI 回复的消息内容
     */
    private String message;
    
    /**
     * 消息类型：NORMAL-正常回复, EMERGENCY-紧急警告, THINKING-思考中, ERROR-错误
     */
    private MessageType type;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 额外的元数据（如风险等级、建议等）
     */
    private Object metadata;
    
    public enum MessageType {
        NORMAL,      // 正常回复
        EMERGENCY,   // 紧急警告
        THINKING,    // 思考中
        ERROR        // 错误
    }
    
    /**
     * 创建正常回复
     */
    public static ChatResponse normal(String sessionId, String message) {
        return ChatResponse.builder()
                .sessionId(sessionId)
                .message(message)
                .type(MessageType.NORMAL)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建紧急警告
     */
    public static ChatResponse emergency(String sessionId, String message) {
        return ChatResponse.builder()
                .sessionId(sessionId)
                .message(message)
                .type(MessageType.EMERGENCY)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建思考中状态
     */
    public static ChatResponse thinking(String sessionId) {
        return ChatResponse.builder()
                .sessionId(sessionId)
                .message("正在思考...")
                .type(MessageType.THINKING)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建错误响应
     */
    public static ChatResponse error(String sessionId, String errorMessage) {
        return ChatResponse.builder()
                .sessionId(sessionId)
                .message(errorMessage)
                .type(MessageType.ERROR)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}

