package com.nexusflow.infra.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.application.PaymentIdempotencyStore;
import com.nexusflow.application.dto.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaPaymentIdempotencyStoreTest {

    private SpringDataIdempotencyKeyRepository springDataRepository;
    private JpaPaymentIdempotencyStore store;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataIdempotencyKeyRepository.class);
        store = new JpaPaymentIdempotencyStore(springDataRepository, new ObjectMapper());
    }

    @Test
    void reserveCreatesPendingRecord() {
        Instant expiresAt = Instant.parse("2026-06-15T00:00:00Z");

        assertTrue(store.reserve("key-1", "hash-1", expiresAt));

        ArgumentCaptor<IdempotencyKeyEntity> captor = ArgumentCaptor.forClass(IdempotencyKeyEntity.class);
        verify(springDataRepository).save(captor.capture());
        IdempotencyKeyEntity entity = captor.getValue();
        assertEquals("key-1", entity.getIdempotencyKey());
        assertEquals("hash-1", entity.getRequestHash());
        assertEquals(expiresAt, entity.getExpiresAt());
        assertNull(entity.getResponse());
    }

    @Test
    void reserveReturnsFalseOnDuplicateKey() {
        when(springDataRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertFalse(store.reserve("key-1", "hash-1", Instant.now()));
    }

    @Test
    void findDeserializesCompletedResponse() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .paymentId("pay-1")
                .orderId("order-1")
                .status("PENDING")
                .build();
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setIdempotencyKey("key-1");
        entity.setRequestHash("hash-1");
        entity.setResponse(new ObjectMapper().writeValueAsString(response));
        entity.setExpiresAt(Instant.now().plusSeconds(60));
        when(springDataRepository.findById("key-1")).thenReturn(Optional.of(entity));

        Optional<PaymentIdempotencyStore.StoredPaymentResponse> found = store.find("key-1");

        assertTrue(found.isPresent());
        assertEquals("hash-1", found.get().requestHash());
        assertEquals("pay-1", found.get().response().getPaymentId());
        assertEquals("order-1", found.get().response().getOrderId());
    }

    @Test
    void findDeletesExpiredRecord() {
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setIdempotencyKey("key-1");
        entity.setExpiresAt(Instant.now().minusSeconds(1));
        when(springDataRepository.findById("key-1")).thenReturn(Optional.of(entity));

        assertTrue(store.find("key-1").isEmpty());
        verify(springDataRepository).deleteById("key-1");
    }

    @Test
    void completeStoresSerializedResponse() {
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setIdempotencyKey("key-1");
        entity.setRequestHash("hash-1");
        when(springDataRepository.findById("key-1")).thenReturn(Optional.of(entity));
        PaymentResponse response = PaymentResponse.builder()
                .paymentId("pay-1")
                .orderId("order-1")
                .status("PENDING")
                .build();

        store.complete("key-1", response);

        ArgumentCaptor<IdempotencyKeyEntity> captor = ArgumentCaptor.forClass(IdempotencyKeyEntity.class);
        verify(springDataRepository).save(captor.capture());
        assertTrue(captor.getValue().getResponse().contains("\"paymentId\":\"pay-1\""));
    }
}
