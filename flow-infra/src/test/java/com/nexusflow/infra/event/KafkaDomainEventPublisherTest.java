package com.nexusflow.infra.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.nexusflow.domain.event.PaymentStateChangedEvent;
import com.nexusflow.domain.payment.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaDomainEventPublisherTest {

    private KafkaTemplate<String, String> kafkaTemplate;
    private ObjectMapper objectMapper;
    private KafkaDomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        objectMapper = JsonMapper.builder().findAndAddModules().build();
        publisher = new KafkaDomainEventPublisher(kafkaTemplate, objectMapper, "");
    }

    @Test
    void publishesDomainEventEnvelopeToEventTypeTopic() throws Exception {
        when(kafkaTemplate.send(eq("crypto.payment.detected"), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        PaymentStateChangedEvent event = PaymentStateChangedEvent.detected(
                "pay-1", "order-1", PaymentStatus.PENDING, "tx-1");

        publisher.publish(event);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("crypto.payment.detected"), keyCaptor.capture(), payloadCaptor.capture());

        JsonNode json = objectMapper.readTree(payloadCaptor.getValue());
        assertEquals(event.getEventId(), keyCaptor.getValue());
        assertEquals(event.getEventId(), json.get("event_id").asText());
        assertEquals("crypto.payment.detected", json.get("event_type").asText());
        assertEquals(PaymentStateChangedEvent.class.getName(), json.get("event_class").asText());
        assertEquals(event.getOccurredAt().toEpochMilli(), json.get("occurred_at").asLong());
        assertEquals("pay-1", json.get("payload").get("paymentId").asText());
        assertEquals("DETECTED", json.get("payload").get("newStatus").asText());
    }
}
