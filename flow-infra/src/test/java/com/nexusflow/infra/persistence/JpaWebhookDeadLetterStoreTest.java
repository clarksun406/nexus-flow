package com.nexusflow.infra.persistence;

import com.nexusflow.application.WebhookDeadLetter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaWebhookDeadLetterStoreTest {

    private SpringDataWebhookDeadLetterRepository springDataRepository;
    private JpaWebhookDeadLetterStore store;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataWebhookDeadLetterRepository.class);
        store = new JpaWebhookDeadLetterStore(springDataRepository);
    }

    @Test
    void saveMapsAllFields() {
        Instant createdAt = Instant.parse("2026-06-14T00:00:00Z");
        WebhookDeadLetter deadLetter = WebhookDeadLetter.builder()
                .id("dlq-1")
                .deliveryType("CRYPTO_PAYMENT")
                .targetUrl("https://merchant.example/callback")
                .payload("{\"payment_id\":\"pay-1\"}")
                .eventId("event-1")
                .eventType("crypto.payment.detected")
                .paymentId("pay-1")
                .orderId("order-1")
                .failureReason("read timed out")
                .attempts(4)
                .createdAt(createdAt)
                .build();

        store.save(deadLetter);

        ArgumentCaptor<WebhookDeadLetterEntity> captor = ArgumentCaptor.forClass(WebhookDeadLetterEntity.class);
        verify(springDataRepository).save(captor.capture());
        WebhookDeadLetterEntity entity = captor.getValue();
        assertEquals("dlq-1", entity.getId());
        assertEquals("CRYPTO_PAYMENT", entity.getDeliveryType());
        assertEquals("https://merchant.example/callback", entity.getTargetUrl());
        assertEquals("{\"payment_id\":\"pay-1\"}", entity.getPayload());
        assertEquals("event-1", entity.getEventId());
        assertEquals("crypto.payment.detected", entity.getEventType());
        assertEquals("pay-1", entity.getPaymentId());
        assertEquals("order-1", entity.getOrderId());
        assertEquals("read timed out", entity.getFailureReason());
        assertEquals(4, entity.getAttempts());
        assertEquals(createdAt, entity.getCreatedAt());
    }

    @Test
    void findRecentBoundsLimitAndMapsResults() {
        WebhookDeadLetterEntity entity = new WebhookDeadLetterEntity();
        entity.setId("dlq-1");
        entity.setDeliveryType("ORDER");
        entity.setTargetUrl("https://merchant.example/order");
        entity.setPayload("{}");
        entity.setFailureReason("blocked");
        entity.setAttempts(0);
        entity.setCreatedAt(Instant.parse("2026-06-14T00:00:00Z"));
        when(springDataRepository.findAllByOrderByCreatedAtDesc(org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(entity));

        List<WebhookDeadLetter> found = store.findRecent(1000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(springDataRepository).findAllByOrderByCreatedAtDesc(pageable.capture());
        assertEquals(100, pageable.getValue().getPageSize());
        assertEquals(1, found.size());
        assertEquals("dlq-1", found.get(0).getId());
        assertEquals("ORDER", found.get(0).getDeliveryType());
        assertEquals("blocked", found.get(0).getFailureReason());
    }
}
