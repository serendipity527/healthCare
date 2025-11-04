package com.yihu.agent.ai.service;

import com.yihu.agent.ai.config.LangChainConfig;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 对话记忆管理服务
 * 基于内存的会话管理，支持自动清理过期会话
 */
@Slf4j
@Service
public class ChatMemoryService {
    
    /**
     * 会话记忆存储：sessionId -> ChatMemory
     */
    private final Map<String, ChatMemory> sessionMemories = new ConcurrentHashMap<>();
    
    /**
     * 会话最后活跃时间：sessionId -> timestamp
     */
    private final Map<String, Long> sessionLastActivity = new ConcurrentHashMap<>();
    
    private final LangChainConfig langChainConfig;
    
    @Value("${healthcare.chat.session-timeout:30m}")
    private String sessionTimeoutStr;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public ChatMemoryService(LangChainConfig langChainConfig) {
        this.langChainConfig = langChainConfig;
        // 启动定时清理任务，每5分钟清理一次过期会话
        scheduler.scheduleAtFixedRate(this::cleanupExpiredSessions, 5, 5, TimeUnit.MINUTES);
    }
    
    /**
     * 获取或创建会话的 ChatMemory
     */
    public ChatMemory getOrCreateMemory(String sessionId) {
        sessionLastActivity.put(sessionId, System.currentTimeMillis());
        
        return sessionMemories.computeIfAbsent(sessionId, id -> {
            log.info("创建新的会话记忆: sessionId={}", id);
            return langChainConfig.createChatMemory();
        });
    }
    
    /**
     * 添加用户消息到记忆
     */
    public void addUserMessage(String sessionId, String message) {
        ChatMemory memory = getOrCreateMemory(sessionId);
        memory.add(UserMessage.from(message));
        log.debug("添加用户消息: sessionId={}, message={}", sessionId, message);
    }
    
    /**
     * 添加 AI 消息到记忆
     */
    public void addAiMessage(String sessionId, String message) {
        ChatMemory memory = getOrCreateMemory(sessionId);
        memory.add(AiMessage.from(message));
        log.debug("添加AI消息: sessionId={}, message={}", sessionId, message);
    }
    
    /**
     * 清除指定会话的记忆
     */
    public void clearMemory(String sessionId) {
        sessionMemories.remove(sessionId);
        sessionLastActivity.remove(sessionId);
        log.info("清除会话记忆: sessionId={}", sessionId);
    }
    
    /**
     * 获取当前活跃会话数
     */
    public int getActiveSessionCount() {
        return sessionMemories.size();
    }
    
    /**
     * 清理过期的会话
     */
    private void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        long timeoutMillis = parseTimeoutToMillis(sessionTimeoutStr);
        
        sessionLastActivity.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > timeoutMillis) {
                String sessionId = entry.getKey();
                sessionMemories.remove(sessionId);
                log.info("清理过期会话: sessionId={}, 过期时长={}ms", sessionId, now - entry.getValue());
                return true;
            }
            return false;
        });
    }
    
    /**
     * 解析超时时间字符串为毫秒
     */
    private long parseTimeoutToMillis(String timeoutStr) {
        try {
            if (timeoutStr.endsWith("m")) {
                return Long.parseLong(timeoutStr.substring(0, timeoutStr.length() - 1)) * 60 * 1000;
            } else if (timeoutStr.endsWith("h")) {
                return Long.parseLong(timeoutStr.substring(0, timeoutStr.length() - 1)) * 60 * 60 * 1000;
            } else {
                return 30 * 60 * 1000; // 默认 30 分钟
            }
        } catch (Exception e) {
            return 30 * 60 * 1000;
        }
    }
}

