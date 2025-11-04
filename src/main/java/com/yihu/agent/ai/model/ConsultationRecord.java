package com.yihu.agent.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 咨询记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationRecord {
    
    /**
     * 记录ID
     */
    private String recordId;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 咨询时间
     */
    private LocalDateTime consultationTime;
    
    /**
     * 主诉症状
     */
    private String chiefComplaint;
    
    /**
     * 症状列表
     */
    private List<String> symptoms;
    
    /**
     * 风险等级：LOW-低风险, MEDIUM-中风险, HIGH-高风险, EMERGENCY-紧急
     */
    private RiskLevel riskLevel;
    
    /**
     * AI 给出的建议
     */
    private String recommendation;
    
    /**
     * 对话摘要
     */
    private String summary;
    
    /**
     * 是否已就医
     */
    private Boolean hasSoughtMedicalCare;
    
    public enum RiskLevel {
        LOW,        // 低风险
        MEDIUM,     // 中风险
        HIGH,       // 高风险
        EMERGENCY   // 紧急
    }
}

