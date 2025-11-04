package com.yihu.agent.ai.graph.nodes;

import com.yihu.agent.ai.graph.AgentState;
import com.yihu.agent.ai.service.ChatMemoryService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

/**
 * 通用对话节点
 * 调用 LLM 生成回复
 */
@Slf4j
@Component
public class GeneralChatNode implements Function<AgentState, AgentState> {
    
    private final OpenAiChatModel chatModel;
    private final ChatMemoryService memoryService;
    private final String systemPrompt;
    
    public GeneralChatNode(OpenAiChatModel chatModel, ChatMemoryService memoryService) {
        this.chatModel = chatModel;
        this.memoryService = memoryService;
        this.systemPrompt = loadSystemPrompt();
    }
    
    @Override
    public AgentState apply(AgentState state) {
        log.info("GeneralChatNode: 生成AI回复 - sessionId={}", state.getSessionId());
        
        try {
            // 获取会话记忆
            ChatMemory memory = memoryService.getOrCreateMemory(state.getSessionId());
            
            // 如果是第一轮对话，添加系统提示词
            if (state.getConversationTurn() == 1) {
                memory.add(SystemMessage.from(systemPrompt));
            }
            
            // 添加用户消息
            UserMessage userMessage = UserMessage.from(state.getUserInput());
            memory.add(userMessage);
            
            // 调用 LLM 生成回复 - 传递完整的对话历史
            String aiReply;
            try {
                // 获取所有历史消息
                List<ChatMessage> messages = memory.messages();
                log.debug("发送消息到 LLM，历史消息数量: {}", messages.size());
                
                // 构建包含历史的上下文提示并调用 LLM
                String contextPrompt = buildContextPrompt(messages);
                aiReply = chatModel.chat(contextPrompt);
                log.debug("LLM 回复成功（包含 {} 条历史消息的上下文）", messages.size());
            } catch (Exception e) {
                log.error("调用 LLM 失败", e);
                // 最后的备选方案：使用简单的 chat 方法（无上下文）
                try {
                    aiReply = chatModel.chat(state.getUserInput());
                } catch (Exception e2) {
                    log.error("备选方案也失败", e2);
                    aiReply = "抱歉，我暂时无法回复。关于您提到的问题，我建议您详细描述症状，以便我给出更准确的建议。如果症状严重，请立即就医。";
                }
            }
            
            // 将 AI 回复添加到记忆
            dev.langchain4j.data.message.AiMessage aiMessage = 
                    dev.langchain4j.data.message.AiMessage.from(aiReply);
            memory.add(aiMessage);
            
            // 设置回复到状态
            state.setAiResponse(aiReply);
            state.setIntent(AgentState.IntentType.GENERAL);
            state.setFinished(true);
            
            log.info("GeneralChatNode: 回复生成成功 - length={}", aiReply.length());
            
        } catch (Exception e) {
            log.error("GeneralChatNode: 生成回复时发生错误", e);
            state.setAiResponse("抱歉，我遇到了一些技术问题。请稍后再试，或者如果是紧急情况，请立即拨打急救电话120。");
            state.setFinished(true);
        }
        
        return state;
    }
    
    /**
     * 构建包含历史上下文的提示
     * 当 generate 方法不可用时使用此方法
     */
    private String buildContextPrompt(List<ChatMessage> messages) {
        StringBuilder context = new StringBuilder();
        
        for (ChatMessage message : messages) {
            if (message instanceof SystemMessage) {
                SystemMessage sm = (SystemMessage) message;
                context.append("[系统]：").append(sm.text()).append("\n\n");
            } else if (message instanceof UserMessage) {
                UserMessage um = (UserMessage) message;
                context.append("用户：").append(um.singleText()).append("\n\n");
            } else if (message instanceof dev.langchain4j.data.message.AiMessage) {
                dev.langchain4j.data.message.AiMessage am = 
                    (dev.langchain4j.data.message.AiMessage) message;
                context.append("AI助手：").append(am.text()).append("\n\n");
            }
        }
        
        context.append("请根据以上对话历史，回复用户的最后一个问题。保持对话的连贯性和上下文一致性。");
        
        log.debug("构建的上下文长度: {} 字符", context.length());
        return context.toString();
    }
    
    /**
     * 加载系统提示词
     */
    private String loadSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/system-prompt.txt");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("加载系统提示词失败，使用默认提示词", e);
            return "你是一位专业的医疗健康咨询助手，请提供准确、安全的健康建议。";
        }
    }
}

