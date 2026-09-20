package org.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

/**
 * @author hq
 */
@Configuration
public class VectorStoreConfig {
    // 向量存储 VectorStore
//    @Bean
//    public JedisConnectionFactory jedisConnectionFactory() {
//        // jedis
//        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6379);
//        config.setPassword("123456");
//        return new JedisConnectionFactory(config);
//    }

    @Bean
    public JedisConnectionFactory jedisConnectionFactory(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${spring.data.redis.password}") String password) {

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        config.setPassword(password);
        return new JedisConnectionFactory(config);
    }
}
