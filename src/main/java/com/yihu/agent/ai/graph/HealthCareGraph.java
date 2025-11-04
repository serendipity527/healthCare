package com.yihu.agent.ai.graph;

import com.yihu.agent.ai.graph.nodes.GeneralChatNode;
import com.yihu.agent.ai.graph.nodes.InitialNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * HealthCare LangGraph 状态机
 * V1 简化版：Initial -> GeneralChat -> END
 * 
 * 注意：当前版本 (1.7.1) 暂时简化为直接调用节点
 * 后续版本将完整实现 LangGraph 状态机
 */
@Slf4j
@Component
public class HealthCareGraph {
    
    private final InitialNode initialNode;
    private final GeneralChatNode generalChatNode;
    
    public HealthCareGraph(InitialNode initialNode, GeneralChatNode generalChatNode) {
        this.initialNode = initialNode;
        this.generalChatNode = generalChatNode;
    }
    
    /**
     * 执行图并返回结果
     * V1 简化实现：顺序调用节点
     */
    public AgentState execute(AgentState initialState) {
        try {
            log.info("开始执行状态图 - sessionId={}", initialState.getSessionId());
            
            // 1. 执行 Initial 节点
            AgentState state = initialNode.apply(initialState);
            log.debug("InitialNode 执行完成");
            
            // 2. 执行 GeneralChat 节点
            state = generalChatNode.apply(state);
            log.debug("GeneralChatNode 执行完成");
            
            // 3. 标记完成
            state.setFinished(true);
            
            log.info("状态图执行完成 - sessionId={}", state.getSessionId());
            return state;
            
        } catch (Exception e) {
            log.error("执行状态图时发生错误", e);
            
            // 返回错误状态
            initialState.setAiResponse("抱歉，系统遇到了问题。如有紧急情况请立即拨打120。");
            initialState.setFinished(true);
            return initialState;
        }
    }
}

