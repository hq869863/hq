package org.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPooled;

/**
 * @author hq
 */
@Component
public class RedisConnectionChecker implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RedisConnectionChecker.class);

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Override
    public void run(String... args) {
        log.info("========== Redis 连接检查 ==========");
        log.info("连接地址: {}:{}", redisHost, redisPort);

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(5);
        poolConfig.setMaxIdle(2);

        try (JedisPool jedisPool = new JedisPool(poolConfig, redisHost, redisPort, 2000, redisPassword)) {
            try (Jedis jedis = jedisPool.getResource()) {
                // 测试连通性
                String ping = jedis.ping();
                log.info("PING 结果: {}", ping);

                // 打印模块列表
                var modules = jedis.moduleList();
                log.info("已加载模块数量: {}", modules.size());
                for (var module : modules) {
                    log.info("  - {}", module);
                }

                // 测试 RediSearch
                try {
                    var ftList = jedis.moduleList();
                    log.info("FT._LIST 结果: {}", ftList);
                    log.info("✅ RediSearch 可用!");
                } catch (Exception e) {
                    log.error("❌ RediSearch 不可用: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("❌ Redis 连接失败: {}", e.getMessage(), e);
        }
        log.info("====================================");
    }
}