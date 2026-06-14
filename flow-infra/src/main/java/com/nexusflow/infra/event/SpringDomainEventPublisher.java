package com.nexusflow.infra.event;

import com.nexusflow.domain.event.DomainEvent;
import com.nexusflow.domain.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring-based implementation of DomainEventPublisher.
 *
 * Publishes domain events via Spring's ApplicationEventPublisher,
 * enabling in-process event handling (Phase 1).
 * Phase 2+ can add Kafka publishing here.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "nexusflow.events.publisher", havingValue = "spring", matchIfMissing = true)
@RequiredArgsConstructor
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher springPublisher;

    @Override
    public void publish(DomainEvent event) {
        log.debug("Publishing domain event: type={}, eventId={}", event.eventType(), event.getEventId());
        springPublisher.publishEvent(event);
    }
}
