package com.nexusflow.domain.event;

/**
 * Idempotency guard for inbound external events (e.g. channel callbacks).
 *
 * Channels may deliver the same callback more than once; consumers use this
 * to ensure each unique event is processed exactly once.
 */
public interface ProcessedEventStore {

    /**
     * Atomically record that {@code eventId} has been seen.
     *
     * @return {@code true} if this is the first time the event is recorded (caller should process it);
     *         {@code false} if it was already recorded (caller should skip it).
     */
    boolean markProcessed(String eventId);
}
