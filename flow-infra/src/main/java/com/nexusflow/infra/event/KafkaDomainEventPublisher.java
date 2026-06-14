package com.nexusflow.infra.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexusflow.domain.event.DomainEvent;
import com.nexusflow.domain.event.DomainEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "nexusflow.events.publisher", havingValue = "kafka")
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topicPrefix;

    public KafkaDomainEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                     ObjectMapper objectMapper,
                                     @Value("${nexusflow.events.kafka.topic-prefix:}") String topicPrefix) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicPrefix = topicPrefix;
    }

    @Override
    public void publish(DomainEvent event) {
        String topic = topicFor(event);
        String payload = serialize(event);
        kafkaTemplate.send(topic, event.getEventId(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish domain event to Kafka: topic={}, type={}, eventId={}",
                                topic, event.eventType(), event.getEventId(), ex);
                    } else {
                        log.debug("Published domain event to Kafka: topic={}, type={}, eventId={}",
                                topic, event.eventType(), event.getEventId());
                    }
                });
    }

    private String topicFor(DomainEvent event) {
        return topicPrefix + event.eventType();
    }

    private String serialize(DomainEvent event) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("event_id", event.getEventId());
            node.put("event_type", event.eventType());
            node.put("event_class", event.getClass().getName());
            node.put("occurred_at", event.getOccurredAt().toEpochMilli());
            node.set("payload", objectMapper.valueToTree(event));
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize domain event " + event.getEventId(), e);
        }
    }
}
