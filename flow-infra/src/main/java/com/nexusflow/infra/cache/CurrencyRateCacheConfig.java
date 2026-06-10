package com.nexusflow.infra.cache;

import com.nexusflow.domain.channel.CurrencyRateCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

@Configuration
public class CurrencyRateCacheConfig {

    @Bean(destroyMethod = "close")
    public JedisPool currencyRateJedisPool(
            @Value("${nexusflow.redis.host:localhost}") String host,
            @Value("${nexusflow.redis.port:6379}") int port) {
        return new JedisPool(host, port);
    }

    @Bean
    @ConditionalOnProperty(name = "nexusflow.cache.enabled", havingValue = "true")
    public CurrencyRateCache redisCurrencyRateCache(
            JedisPool currencyRateJedisPool,
            @Value("${nexusflow.cache.rate-ttl-seconds:60}") long rateTtl,
            @Value("${nexusflow.cache.currency-ttl-seconds:300}") long currencyTtl) {
        return new RedisCurrencyRateCache(currencyRateJedisPool, rateTtl, currencyTtl);
    }

    @Bean
    @ConditionalOnMissingBean(CurrencyRateCache.class)
    public CurrencyRateCache noOpCurrencyRateCache() {
        return new NoOpCurrencyRateCache();
    }
}
