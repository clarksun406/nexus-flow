package com.nexusflow.infra.cache;

import com.nexusflow.domain.channel.ChannelAdapter;
import com.nexusflow.domain.channel.CurrencyConfig;
import com.nexusflow.domain.channel.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies RedisCurrencyRateCache read-through and write-through contracts
 * against a mocked Jedis pool (no live Redis required).
 */
class RedisCurrencyRateCacheTest {

    private JedisPool pool;
    private Jedis jedis;
    private ChannelAdapter adapter;
    private RedisCurrencyRateCache cache;

    @BeforeEach
    void setUp() {
        pool = mock(JedisPool.class);
        jedis = mock(Jedis.class);
        adapter = mock(ChannelAdapter.class);
        when(pool.getResource()).thenReturn(jedis);
        when(adapter.channelId()).thenReturn("STUB");
        cache = new RedisCurrencyRateCache(pool, 60, 300);
    }

    @Test
    void cacheMissFetchesFromAdapterAndWritesToRedis() {
        ExchangeRate rate = ExchangeRate.builder()
                .token("USDT").network("TRC20")
                .price(new BigDecimal("1.0005"))
                .quoteCurrency("USD")
                .timestamp(Instant.now())
                .build();
        when(jedis.get(anyString())).thenReturn(null);
        when(adapter.getExchangeRate("USDT", "TRC20", "USD")).thenReturn(rate);

        ExchangeRate result = cache.getExchangeRate(adapter, "USDT", "TRC20", "USD");

        assertNotNull(result);
        assertEquals(new BigDecimal("1.0005"), result.getPrice());
        verify(adapter).getExchangeRate("USDT", "TRC20", "USD");
        verify(jedis).setex(anyString(), eq(60L), anyString());
    }

    @Test
    void cacheHitReturnsWithoutCallingAdapter() {
        // JSON format produced by manual serialization (epoch millis timestamp)
        String json = "{\"token\":\"USDT\",\"network\":\"TRC20\",\"price\":\"1.0003\",\"quoteCurrency\":\"USD\",\"timestamp\":1704067200000}";
        when(jedis.get(anyString())).thenReturn(json);

        ExchangeRate result = cache.getExchangeRate(adapter, "USDT", "TRC20", "USD");

        assertNotNull(result);
        assertEquals(new BigDecimal("1.0003"), result.getPrice());
        verify(adapter, never()).getExchangeRate(anyString(), anyString(), anyString());
    }

    @Test
    void supportedCurrenciesCacheMissFetchesAndWrites() {
        List<CurrencyConfig> currencies = List.of(
                CurrencyConfig.builder().token("USDT").network("TRC20").decimals(6)
                        .minDeposit(new BigDecimal("1")).requiredConfirmations(1).enabled(true).build()
        );
        when(jedis.get(anyString())).thenReturn(null);
        when(adapter.getSupportedCurrencies()).thenReturn(currencies);

        List<CurrencyConfig> result = cache.getSupportedCurrencies(adapter);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("USDT", result.get(0).getToken());
        verify(adapter).getSupportedCurrencies();
        verify(jedis).setex(anyString(), eq(300L), anyString());
    }

    @Test
    void redisReadFailureFallsBackToAdapter() {
        ExchangeRate rate = ExchangeRate.builder()
                .token("BTC").network("BTC")
                .price(new BigDecimal("65000"))
                .quoteCurrency("USD")
                .timestamp(Instant.now())
                .build();
        when(pool.getResource()).thenThrow(new RuntimeException("connection refused"));
        when(adapter.getExchangeRate("BTC", "BTC", "USD")).thenReturn(rate);

        ExchangeRate result = cache.getExchangeRate(adapter, "BTC", "BTC", "USD");

        assertNotNull(result);
        assertEquals(new BigDecimal("65000"), result.getPrice());
    }
}
