//package org.example.filter;
//
//import cn.dev33.satoken.stp.StpUtil;
//import org.springframework.core.annotation.Order;
//import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import org.springframework.web.server.WebFilter;
//import org.springframework.web.server.WebFilterChain;
//import reactor.core.publisher.Mono;
//
//import java.time.Duration;
//
///**
// * @author hq
// */
//@Component
//public class QuotaWebFilter implements WebFilter {
//
//    private final ReactiveStringRedisTemplate redisTemplate;
//
//    public QuotaWebFilter(ReactiveStringRedisTemplate redisTemplate) {
//        this.redisTemplate = redisTemplate;
//    }
//
//    @Override
//    @Order(3)
//    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
//        String userId = String.valueOf(StpUtil.getLoginId());
//        String key = "mcp:quota:" + userId;
//
//        return redisTemplate.opsForValue().get(key)
//                .flatMap(count -> {
//                    if (Integer.parseInt(count) >= 50) {
//                        // 超过免费次数，直接拦截返回
//                        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
//                        return exchange.getResponse().writeWith(
//                                Mono.just(exchange.getResponse().bufferFactory()
//                                        .wrap("今日免费次数已用完，请升级VIP".getBytes()))
//                        );
//                    }
//                    // 未超限，次数+1，并设置过期时间为1天
//                    return redisTemplate.opsForValue().increment(key)
//                            .flatMap(v -> redisTemplate.expire(key, Duration.ofDays(1)))
//                            .then(chain.filter(exchange));
//                })
//                .switchIfEmpty(Mono.defer(() -> {
//                    // 第一次调用，初始化次数为1
//                    return redisTemplate.opsForValue().set(key, "1", Duration.ofDays(1))
//                            .then(chain.filter(exchange));
//                }));
//    }
//}