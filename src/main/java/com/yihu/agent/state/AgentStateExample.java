package com.yihu.agent.state;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentStateExample {

    public static void main(String[] args) {
        // 1. 创建初始状态
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("name", "Alice");
        initialData.put("age", 25);
        initialData.put("messages", new ArrayList<>(List.of("Hello")));

        AgentState state = new AgentState(initialData);

        // 2. 获取状态值
        String name = state.<String>value("name").orElse("Unknown");
        Integer age = state.<Integer>value("age").orElse(0);

//        System.out.println("Name: " + name);  // 输出: Name: Alice
//        System.out.println("Age: " + age);    // 输出: Age: 25
//        System.out.println("State: " + state); // 打印整个状态

//        // 3. 更新状态（简单覆盖）
        Map<String, Object> partialUpdate = new HashMap<>();
        partialUpdate.put("age", 26);
        partialUpdate.put("city", "New York");
//
        Map<String, Object> updatedState = AgentState.updateState(
                state,
                partialUpdate,
                null  // 没有 channels
        );
//
//        System.out.println("Updated state: " + updatedState);
//        // 输出: {name=Alice, age=26, city=New York, messages=[Hello]}
//
        // 4. 使用标记删除键
        Map<String, Object> removeUpdate = new HashMap<>();
        removeUpdate.put("city", AgentState.MARK_FOR_REMOVAL);

        Map<String, Object> stateAfterRemoval = AgentState.updateState(
                updatedState,
                removeUpdate,
                null
        );

        System.out.println("After removal: " + stateAfterRemoval);
        // 输出: {name=Alice, age=26, messages=[Hello]}

//        // 5. 使用 Channel 来合并列表（Append Reducer）
//        Map<String, Channel<?>> channels = new HashMap<>();
//        channels.put("messages", Channel.APPEND_CHANNEL);
//
//        Map<String, Object> messageUpdate = new HashMap<>();
//        messageUpdate.put("messages", List.of("World", "!"));
//
//        Map<String, Object> finalState = AgentState.updateState(
//                stateAfterRemoval,
//                messageUpdate,
//                channels
//        );
//
//        System.out.println("Final state: " + finalState);
        // 输出: {name=Alice, age=26, messages=[Hello, World, !]}
    }
}