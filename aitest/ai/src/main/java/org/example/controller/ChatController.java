package org.example.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author hq
 */
@RestController
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);


    private final ChatClient dashscopeChatClient;
    private final ChatClient ollamaChatClient = null;

    public ChatController(
            @Qualifier("dashscopeChatClient") ChatClient dashscopeChatClient) {
        this.dashscopeChatClient = dashscopeChatClient;
    }

    /**
     * 统一对话接口
     */
    @PostMapping("/chat/{modelType}/{chatId}")
    public String chat(
            @PathVariable("modelType") Integer modelType,
            @PathVariable("chatId") String chatId,
            @RequestBody String userQuery) {

        ChatClient targetClient;
        if (modelType == 1) {
            targetClient = dashscopeChatClient;
        } else if (modelType == 2) {
            targetClient = ollamaChatClient;
        } else {
            return "不支持的模型类型，请传入 1(DashScope) 或 2(Ollama)";
        }

        return targetClient.prompt()
                .user(userQuery)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .content();
    }

    /**
     * 统一对话接口
     */
    @PostMapping("/chat/flux/{modelType}/{chatId}")
    public Flux<String> chatFlux(
            @PathVariable("modelType") Integer modelType,
            @PathVariable("chatId") String chatId,
            @RequestBody String userQuery) throws Exception {


        ChatClient targetClient;
        if (modelType == 1) {
            targetClient = dashscopeChatClient;
        } else if (modelType == 2) {
            targetClient = ollamaChatClient;
        } else {
            throw new Exception("不支持的模型类型，请传入 1(DashScope) 或 2(Ollama)");
        }
        Flux<String> content = targetClient.prompt()
                .user(userQuery)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream().content()
                .doOnNext(chunk -> log.info("接收到内容片段: {}", chunk));
        return content;
    }

}
