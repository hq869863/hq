package org.example.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author hq
 */
@RestController
public class FileController {
    private final WebClient ragWebClient;

    public FileController(@Qualifier("ragWebClient") WebClient ragWebClient) {
        this.ragWebClient = ragWebClient;
    }

    /**
     * 流式对话接口（透传给 ai 模块）
     */
    @GetMapping(value = "/load")
    public String chatFlux() {
        return ragWebClient.get()
                .uri("/load")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

}
