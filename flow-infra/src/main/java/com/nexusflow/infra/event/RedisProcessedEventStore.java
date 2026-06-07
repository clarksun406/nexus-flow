package com.nexusflow.infra.event;

import com.nexusflow.domain.event.ProcessedEventStore;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

/**
 * Redis-backed idempotency guard for production / multi-instance deployments.
 *
 * Uses an atomic {@code SET key 1 NX EX <ttl>}: the key is created only if absent, so exactly one
 * caller observes the "first" outcome even under concurrency or across instances. A TTL bounds
 * memory growth (events older than the window are no longer deduped — size the TTL accordingly).
 *
 * Wired by {@link RedisIdempotencyConfig} when {@code nexusflow.idempotency.store=redis}.
 */
@Slf4j
public class RedisProcessedEventStore implements ProcessedEventStore {

    private static final String KEY_PREFIX = "nexusflow:idemp:";

    private final JedisPool jedisPool;
    private final long ttlSeconds;

    public RedisProcessedEventStore(JedisPool jedisPool, long ttlSeconds) {
        this.jedisPool = jedisPool;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public boolean markProcessed(String eventId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.set(KEY_PREFIX + eventId, "1",
                    SetParams.setParams().nx().ex(ttlSeconds));
            return "OK".equals(result); // null when the key already existed
        }
    }
}
