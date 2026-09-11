package org.example.controller;


import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author hq
 */
@RestController
public class OllamaController {

    @Resource(name = "ollamaChatModel")
//    @Qualifier("ollamaChatModel")
    private ChatModel chatModel;

    @GetMapping("/chat/hello1")
    public String doChat(@RequestParam(name = "msg", defaultValue = "你好") String question) {
        return chatModel.call(question);
    }

    @GetMapping("/chat/stream2")
    public Flux<String> stream(@RequestParam(name = "msg", defaultValue = "你好") String question) {
        return chatModel.stream(question);
    }

}
