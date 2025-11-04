package com.yihu.agent.ai.graph.nodes;

import com.yihu.agent.ai.graph.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * 初始节点
 * 接收用户输入，进行基础处理
 */
@Slf4j
@Component
public class InitialNode implements Function<AgentState, AgentState> {
    
    @Override
    public AgentState apply(AgentState state) {
        log.info("InitialNode: 处理用户输入 - sessionId={}, input={}", 
                state.getSessionId(), state.getUserInput());
        
        // 增加对话轮次
        state.incrementTurn();
        
        // 基础文本清理
        String cleanedInput = state.getUserInput().trim();
        state.setUserInput(cleanedInput);
        
        // 记录时间戳
        state.putMetadata("inputReceivedAt", System.currentTimeMillis());
        
        log.debug("InitialNode: 处理完成 - turn={}", state.getConversationTurn());
        
        return state;
    }
}

