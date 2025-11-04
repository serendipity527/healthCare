package com.yihu.agent.ai.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * LangChain4j 配置类
 * 配置 ChatModel 和 ChatMemory
 * 支持从 application.yml 读取配置
 */
@Configuration
public class LangChainConfig {
    
    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;
    
    @Value("${langchain4j.open-ai.chat-model.base-url:https://api.openai.com/v1}")
    private String baseUrl;
    
    @Value("${langchain4j.open-ai.chat-model.model-name:gpt-4}")
    private String modelName;
    
    @Value("${langchain4j.open-ai.chat-model.temperature:0.7}")
    private Double temperature;
    
    @Value("${langchain4j.open-ai.chat-model.timeout:60s}")
    private String timeout;
    
    @Value("${langchain4j.open-ai.chat-model.max-retries:3}")
    private Integer maxRetries;
    
    @Value("${langchain4j.open-ai.chat-model.log-requests:true}")
    private Boolean logRequests;
    
    @Value("${langchain4j.open-ai.chat-model.log-responses:true}")
    private Boolean logResponses;
    
    @Value("${healthcare.chat.memory.max-messages:10}")
    private Integer maxMessages;
    
    /**
     * 配置 OpenAI Compatible ChatModel
     * 支持 OpenAI API 和兼容的服务（如阿里云 DashScope）
     * 
     * @Primary 标记为主要 Bean，解决与 Spring Boot Starter 自动配置的冲突
     */
    @Bean
    @Primary
    public OpenAiChatModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(parseDuration(timeout))
                .maxRetries(maxRetries)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }
    
    /**
     * ChatMemory 工厂方法
     * 为每个会话创建独立的记忆实例
     */
    public ChatMemory createChatMemory() {
        return MessageWindowChatMemory.withMaxMessages(maxMessages);
    }
    
    /**
     * 解析时间字符串为 Duration
     */
    private Duration parseDuration(String durationStr) {
        try {
            if (durationStr.endsWith("s")) {
                return Duration.ofSeconds(Long.parseLong(durationStr.substring(0, durationStr.length() - 1)));
            } else if (durationStr.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(durationStr.substring(0, durationStr.length() - 1)));
            } else {
                return Duration.ofSeconds(60); // 默认 60 秒
            }
        } catch (Exception e) {
            return Duration.ofSeconds(60);
        }
    }
}

