package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author hq
 */
@Configuration
public class WebClientConfig {

    /**
     * 调用 ai 模块的 WebClient
     */
    @Bean("aiWebClient")
    public WebClient aiWebClient() {
        return WebClient.builder()
                .baseUrl("http://ai-deployment:8008")
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                        .build())
                .build();
    }

    /**
     * 调用 rag 模块的 WebClient
     */
    @Bean("ragWebClient")
    public WebClient ragWebClient() {
        return WebClient.builder()
                .baseUrl("http://rag-deployment:8009")
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                        .build())
                .build();
    }
}
