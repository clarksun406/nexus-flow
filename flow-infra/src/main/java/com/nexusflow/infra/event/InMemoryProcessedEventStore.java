package com.nexusflow.infra.event;

import com.nexusflow.domain.event.ProcessedEventStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory idempotency guard (default). Data is lost on restart and not shared across instances.
 * Set {@code nexusflow.idempotency.store=redis} to use {@link RedisProcessedEventStore} instead.
 */
@Component
@ConditionalOnProperty(name = "nexusflow.idempotency.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryProcessedEventStore implements ProcessedEventStore {

    private final Set<String> processed = ConcurrentHashMap.newKeySet();

    @Override
    public boolean markProcessed(String eventId) {
        return processed.add(eventId);
    }
}
