package org.example.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author hq
 */
@RestController
public class FileController {
    private final WebClient aiWebClient;

    public FileController(WebClient aiWebClient) {
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
