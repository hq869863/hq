package org.example.filter;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * @author hq
 */
@Component
@Order(2)
public class TraceIdWebFilter implements WebFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 生成全局唯一追踪ID
        String traceId = UUID.randomUUID().toString().replace("-", "");

        // 将 TraceID 放入请求头，透传给下游（如 Feign 调用的老业务）
        exchange.getRequest().mutate().header(TRACE_ID_HEADER, traceId).build();

        // 同时放入响应头，方便前端或调用方排查问题
        exchange.getResponse().getHeaders().add(TRACE_ID_HEADER, traceId);

        // 可以在这里打印一条入口日志
        System.out.println("[TraceID: " + traceId + "] 收到请求: " + exchange.getRequest().getPath());

        return chain.filter(exchange);
    }
}