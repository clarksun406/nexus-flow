package com.nexusflow.infra.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the SET-NX dedup contract against a mocked Jedis (no live Redis).
 */
class RedisProcessedEventStoreTest {

    private JedisPool pool;
    private Jedis jedis;
    private RedisProcessedEventStore store;

    @BeforeEach
    void setUp() {
        pool = mock(JedisPool.class);
        jedis = mock(Jedis.class);
        when(pool.getResource()).thenReturn(jedis);
        store = new RedisProcessedEventStore(pool, 3600);
    }

    @Test
    void firstOccurrenceReturnsTrueWhenKeyIsSet() {
        when(jedis.set(eq("nexusflow:idemp:evt-1"), eq("1"), any(SetParams.class))).thenReturn("OK");

        assertTrue(store.markProcessed("evt-1"));
    }

    @Test
    void duplicateReturnsFalseWhenKeyAlreadyExists() {
        when(jedis.set(eq("nexusflow:idemp:evt-1"), eq("1"), any(SetParams.class))).thenReturn(null);

        assertFalse(store.markProcessed("evt-1"));
    }

    @Test
    void releasesConnectionBackToPool() {
        when(jedis.set(anyString(), anyString(), any(SetParams.class))).thenReturn("OK");

        store.markProcessed("evt-1");

        verify(jedis).close(); // try-with-resources returns the connection
    }
}
