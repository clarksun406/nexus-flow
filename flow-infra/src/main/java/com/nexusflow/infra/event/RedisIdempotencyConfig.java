package com.nexusflow.infra.event;

import com.nexusflow.domain.event.ProcessedEventStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

/**
 * Activates the Redis-backed {@link ProcessedEventStore} when
 * {@code nexusflow.idempotency.store=redis}. Otherwise {@link InMemoryProcessedEventStore} is used.
 */
@Configuration
@ConditionalOnProperty(name = "nexusflow.idempotency.store", havingValue = "redis")
public class RedisIdempotencyConfig {

    @Bean(destroyMethod = "close")
    public JedisPool idempotencyJedisPool(@Value("${nexusflow.redis.host:localhost}") String host,
                                          @Value("${nexusflow.redis.port:6379}") int port) {
        return new JedisPool(host, port);
    }

    @Bean
    public ProcessedEventStore redisProcessedEventStore(
            JedisPool idempotencyJedisPool,
            @Value("${nexusflow.idempotency.ttl-seconds:86400}") long ttlSeconds) {
        return new RedisProcessedEventStore(idempotencyJedisPool, ttlSeconds);
    }
}
