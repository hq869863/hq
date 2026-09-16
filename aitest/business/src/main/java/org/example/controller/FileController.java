package org.example.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

/**
 * @author hq
 */
@RestController
public class FileController {
    private final WebClient aiWebClient;

    public FileController(@Qualifier("ragWebClient")WebClient aiWebClient) {
        this.aiWebClient = aiWebClient;
    }

    /**
     * 流式对话接口（透传给 ai 模块）
     */
    @PostMapping(value = "/load")
    public Flux<String> chatFlux(@RequestParam String path) {
        return aiWebClient.post()
                .uri("/load?path={path}", path)
                .retrieve()
                .bodyToFlux(String.class);
    }

}
