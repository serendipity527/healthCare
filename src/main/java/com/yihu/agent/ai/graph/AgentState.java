package com.yihu.agent.ai.graph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LangGraph 状态定义
 * 在状态机的各个节点之间传递的状态对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentState {
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 当前用户输入
     */
    private String userInput;
    
    /**
     * AI 的回复消息
     */
    private String aiResponse;
    
    /**
     * 当前意图类型：GENERAL-通用聊天, MEDICAL-医疗咨询, EMERGENCY-紧急情况
     */
    private IntentType intent;
    
    /**
     * 风险等级：LOW, MEDIUM, HIGH, EMERGENCY
     */
    private String riskLevel;
    
    /**
     * 收集到的症状列表
     */
    @Builder.Default
    private List<String> symptoms = new ArrayList<>();
    
    /**
     * 对话轮次计数
     */
    @Builder.Default
    private Integer conversationTurn = 0;
    
    /**
     * 是否结束对话
     */
    @Builder.Default
    private Boolean finished = false;
    
    /**
     * 下一个要执行的节点
     */
    private String nextNode;
    
    /**
     * 额外的元数据
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * 意图类型枚举
     */
    public enum IntentType {
        GENERAL,    // 通用聊天
        MEDICAL,    // 医疗咨询
        EMERGENCY   // 紧急情况
    }
    
    /**
     * 增加对话轮次
     */
    public void incrementTurn() {
        this.conversationTurn++;
    }
    
    /**
     * 添加症状
     */
    public void addSymptom(String symptom) {
        if (this.symptoms == null) {
            this.symptoms = new ArrayList<>();
        }
        this.symptoms.add(symptom);
    }
    
    /**
     * 添加元数据
     */
    public void putMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
}

