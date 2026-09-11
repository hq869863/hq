package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author hq
 */
@Configuration
public class MyConfig {

    // 注册WebClient 调用ai模块
    @Bean
    public WebClient aiWebClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8008")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}
