package com.nexusflow.domain.event;

/**
 * Port for publishing domain events.
 * Implementation in infrastructure layer.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}