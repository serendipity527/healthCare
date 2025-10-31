package com.yihu.agent.controller;

import lombok.Getter;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.util.HtmlUtils;

@Controller
public class StompChatController {
    /**
     * 最简单的广播功能
     * <p>
     * 1. @MessageMapping("/chat"):
     * 客户端通过 stompClient.send("/app/chat", ...) 发送消息到这里。
     * ( "/app" 是您在配置中设置的 setApplicationDestinationPrefixes )
     * <p>
     * 2. @SendTo("/topic/messages"):
     * 方法的返回值 (SimpleResponse 对象) 会被自动广播到 "/topic/messages" 主题。
     * ( "/topic" 是您在配置中设置的 enableSimpleBroker )
     * <p>
     * 客户端需要通过 stompClient.subscribe("/topic/messages", ...) 来订阅。
     *
     * @param message 客户端发送的消息 (JSON 会被自动反序列化为 SimpleMessage 对象)
     * @return SimpleResponse 对象 (会被序列化为 JSON 并广播)
     */
    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public SimpleResponse handleChatMessage(@Payload SimpleMessage message) {
        // 1. 从消息中获取内容
        String sender = HtmlUtils.htmlEscape(message.sender());
        String content = HtmlUtils.htmlEscape(message.content());

        // 2. 构造广播的响应
        String responseContent = String.format("来自 %s 的消息: %s", sender, content);

        // 3. 返回的对象会被自动广播
        return new SimpleResponse(responseContent);
    }

}


/**
 * DTO: 客户端发送来的消息 (简化版) - 使用 Record
 */
record SimpleMessage(String sender, String content) { }

/**
 * DTO: 服务器广播出去的响应 (简化版) - 使用 Record
 */
record SimpleResponse(String content) { }
// --- 数据传输对象 (DTOs) ---
// 为了演示方便放在一起。

///**
// * DTO: 客户端发送来的消息 (简化版)
// */
//class SimpleMessage {
//    private String sender;    // 发送者
//    private String content;   // 消息内容
//
//    // Getters 和 Setters
//    public String getSender() { return sender; }
//    public void setSender(String sender) { this.sender = sender; }
//    public String getContent() { return content; }
//    public void setContent(String content) { this.content = content; }
//}
//
///**
// * DTO: 服务器广播出去的响应 (简化版)
// */
//class SimpleResponse {
//    private String content;
//
//    public SimpleResponse(String content) {
//        this.content = content;
//    }
//
//    // Getters 和 Setters
//    public String getContent() { return content; }
//    public void setContent(String content) { this.content = content; }
//}
