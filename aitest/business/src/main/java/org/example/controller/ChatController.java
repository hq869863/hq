package org.example.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
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
    @PostMapping(value = "/graph/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public String chatFlux(@RequestBody String userInput, @PathVariable("chatId") String chatId) {
        return aiWebClient.post()
                .uri("/graph/{chatId}", chatId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userInput)
                .retrieve()
                .toString();
    }
}
