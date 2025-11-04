package com.yihu.agent.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户健康档案
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserHealthProfile {
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 年龄
     */
    private String age;
    
    /**
     * 性别：MALE-男, FEMALE-女, OTHER-其他
     */
    private Gender gender;
    
    /**
     * 过敏史
     */
    @Builder.Default
    private List<String> allergies = new ArrayList<>();
    
    /**
     * 是否怀孕
     */
    private Boolean isPregnant;
    
    /**
     * 慢性病列表
     */
    @Builder.Default
    private List<String> chronicDiseases = new ArrayList<>();
    
    /**
     * 历史咨询记录
     */
    @Builder.Default
    private List<ConsultationRecord> history = new ArrayList<>();
    
    /**
     * 当前用药情况
     */
    @Builder.Default
    private List<String> currentMedications = new ArrayList<>();
    
    /**
     * 性别枚举
     */
    public enum Gender {
        MALE("男"),
        FEMALE("女"),
        OTHER("其他");
        
        private final String displayName;
        
        Gender(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 添加咨询记录
     */
    public void addConsultationRecord(ConsultationRecord record) {
        if (this.history == null) {
            this.history = new ArrayList<>();
        }
        this.history.add(record);
    }
}

