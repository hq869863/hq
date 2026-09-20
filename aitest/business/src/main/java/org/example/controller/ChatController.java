package org.example.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * @author hq
 */
@RestController
public class ChatController {
    private final WebClient aiWebClient;

    public ChatController(@Qualifier("aiWebClient") WebClient aiWebClient) {
        this.aiWebClient = aiWebClient;
    }

    /**
     * 对话接口（透传给 ai 模块）
     */
    @PostMapping(value = "/graph/{chatId}")
    public String chat(@RequestBody String userInput, @PathVariable("chatId") String chatId) {
        return aiWebClient.post()
                .uri("/graph/{chatId}", chatId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userInput)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    @GetMapping(value = "/test/{chatId}")
    public String test(@PathVariable("chatId") String chatId) {
        return "yes !" + chatId;
    }
}
