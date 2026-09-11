package org.example.controller;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author hq
 */
@RestController
public class TestController {
    @Autowired
    private ChatModel chatModel;

    @PostMapping("/test")
    public Flux<String> simpleChat() {
        Prompt prompt = new Prompt("你好");
        return chatModel.stream(prompt).mapNotNull(chatResponse -> {
                    if (chatResponse.getResult() != null &&
                            chatResponse.getResult().getOutput() != null) {
                        return chatResponse.getResult().getOutput().getText();
                    }
                    return "";
                })
                .filter(text -> !text.isEmpty());
    }

}
