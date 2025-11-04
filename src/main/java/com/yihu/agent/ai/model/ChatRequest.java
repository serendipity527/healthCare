package com.yihu.agent.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 对话请求消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户输入的消息内容
     */
    private String message;
    
    /**
     * 会话ID，用于区分不同的对话会话
     */
    private String sessionId;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    public ChatRequest(String userId, String message, String sessionId) {
        this.userId = userId;
        this.message = message;
        this.sessionId = sessionId;
        this.timestamp = System.currentTimeMillis();
    }
}

