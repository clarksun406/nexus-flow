package com.nexusflow.infra.event;

import com.nexusflow.domain.event.ProcessedEventStore;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory idempotency guard for Phase 1 MVP.
 * Replace with a Redis/PostgreSQL-backed implementation (with TTL) for production
 * and multi-instance deployments.
 */
@Component
public class InMemoryProcessedEventStore implements ProcessedEventStore {

    private final Set<String> processed = ConcurrentHashMap.newKeySet();

    @Override
    public boolean markProcessed(String eventId) {
        return processed.add(eventId);
    }
}
