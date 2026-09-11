package org.example.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author hq
 */
@RestController
public class ChatController {
    private final WebClient aiWebClient;

    public ChatController(WebClient aiWebClient) {
        this.aiWebClient = aiWebClient;
    }

    /**
     * 流式对话接口（透传给 ai 模块）
     */
    @PostMapping(value = "/flux/{modelType}/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatFlux(@PathVariable("modelType") Integer modelType,
                                 @PathVariable("chatId") String chatId,
                                 @RequestBody String userQuery) {
        return aiWebClient.post()
                .uri("/chat/flux/{modelType}/{chatId}", modelType, chatId)
                .bodyValue(userQuery)
                .retrieve()
                .bodyToFlux(String.class);
    }

    /**
     * 普通对话接口（透传给 ai 模块）
     */
    @PostMapping("/sync/{modelType}/{chatId}")
    public Mono<String> chat(@PathVariable("modelType") Integer modelType,
                             @PathVariable("chatId") String chatId,
                             @RequestBody String userQuery) {
        return aiWebClient.post()
                .uri("/chat/{modelType}/{chatId}", modelType, chatId)
                .bodyValue(userQuery)
                .retrieve()
                .bodyToMono(String.class);
    }
}
