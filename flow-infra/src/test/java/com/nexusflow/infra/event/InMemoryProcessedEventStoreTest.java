package com.nexusflow.infra.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryProcessedEventStoreTest {

    private InMemoryProcessedEventStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryProcessedEventStore();
    }

    @Test
    void firstMarkReturnsTrueDuplicateReturnsFalse() {
        assertTrue(store.markProcessed("evt-1"), "first occurrence should be claimed");
        assertFalse(store.markProcessed("evt-1"), "duplicate should be rejected");
    }

    @Test
    void distinctEventsAreIndependent() {
        assertTrue(store.markProcessed("evt-1"));
        assertTrue(store.markProcessed("evt-2"));
    }

    @Test
    void concurrentMarksClaimEventExactlyOnce() throws InterruptedException {
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger winners = new AtomicInteger();
        var done = new java.util.concurrent.CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    if (store.markProcessed("same-event")) {
                        winners.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS), "all workers should finish");
        pool.shutdownNow();

        assertEquals(1, winners.get(), "exactly one thread should claim the event");
    }
}
