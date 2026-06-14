package com.nexusflow.infra.persistence;

import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.payment.PaymentStatus;
import com.nexusflow.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaPaymentRepositoryTest {

    private SpringDataCryptoPaymentRepository springDataRepository;
    private JpaPaymentRepository repository;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataCryptoPaymentRepository.class);
        repository = new JpaPaymentRepository(springDataRepository);
    }

    @Test
    void saveMapsDomainFieldsToEntity() {
        CryptoPayment payment = CryptoPayment.builder()
                .id("pay-1")
                .orderId("order-1")
                .expected(Money.of("USDT_TRC20", new BigDecimal("100.00")))
                .receivingAddress("TADDR")
                .requiredConfirmations(3)
                .callbackUrl("https://merchant.example/callback")
                .build();
        payment.markPending();
        payment.markDetected("tx-1", Money.of("USDT_TRC20", new BigDecimal("100.00")));
        payment.updateConfirmations(1);

        repository.save(payment);

        ArgumentCaptor<CryptoPaymentEntity> captor = ArgumentCaptor.forClass(CryptoPaymentEntity.class);
        verify(springDataRepository).save(captor.capture());
        CryptoPaymentEntity entity = captor.getValue();
        assertEquals("pay-1", entity.getId());
        assertEquals("order-1", entity.getOrderId());
        assertEquals("USDT_TRC20", entity.getCurrency());
        assertEquals(new BigDecimal("100.00"), entity.getExpectedAmount());
        assertEquals(new BigDecimal("100.00"), entity.getReceivedAmount());
        assertEquals("CONFIRMING", entity.getStatus());
        assertEquals("TADDR", entity.getReceivingAddress());
        assertEquals("tx-1", entity.getTxHash());
        assertEquals(1, entity.getConfirmations());
        assertEquals(3, entity.getRequiredConfirmations());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getDetectedAt());
    }

    @Test
    void savePreservesExistingVersionForUpdates() {
        CryptoPaymentEntity existing = new CryptoPaymentEntity();
        existing.setId("pay-1");
        existing.setVersion(7L);
        when(springDataRepository.findById("pay-1")).thenReturn(Optional.of(existing));
        CryptoPayment payment = CryptoPayment.builder()
                .id("pay-1")
                .orderId("order-1")
                .expected(Money.of("USDT_TRC20", new BigDecimal("100.00")))
                .receivingAddress("TADDR")
                .build();

        repository.save(payment);

        ArgumentCaptor<CryptoPaymentEntity> captor = ArgumentCaptor.forClass(CryptoPaymentEntity.class);
        verify(springDataRepository).save(captor.capture());
        assertEquals(7L, captor.getValue().getVersion());
    }

    @Test
    void findByIdReconstitutesAllPersistentFields() {
        Instant createdAt = Instant.parse("2026-06-13T00:00:00Z");
        Instant detectedAt = Instant.parse("2026-06-13T00:01:00Z");
        CryptoPaymentEntity entity = new CryptoPaymentEntity();
        entity.setId("pay-1");
        entity.setOrderId("order-1");
        entity.setCurrency("USDT_TRC20");
        entity.setExpectedAmount(new BigDecimal("100.00"));
        entity.setReceivedAmount(new BigDecimal("50.00"));
        entity.setStatus("DETECTED");
        entity.setReceivingAddress("TADDR");
        entity.setTxHash("tx-1");
        entity.setConfirmations(0);
        entity.setRequiredConfirmations(3);
        entity.setCallbackUrl("https://merchant.example/callback");
        entity.setCreatedAt(createdAt);
        entity.setDetectedAt(detectedAt);
        when(springDataRepository.findById("pay-1")).thenReturn(Optional.of(entity));

        Optional<CryptoPayment> found = repository.findById("pay-1");

        assertTrue(found.isPresent());
        CryptoPayment payment = found.get();
        assertEquals("pay-1", payment.getId());
        assertEquals("order-1", payment.getOrderId());
        assertEquals(Money.of("USDT_TRC20", new BigDecimal("100.00")), payment.getExpected());
        assertEquals(Money.of("USDT_TRC20", new BigDecimal("50.00")), payment.getReceived());
        assertEquals(PaymentStatus.DETECTED, payment.getStatus());
        assertEquals("TADDR", payment.getReceivingAddress());
        assertEquals("tx-1", payment.getTxHash());
        assertEquals(0, payment.getConfirmations());
        assertEquals(3, payment.getRequiredConfirmations());
        assertEquals("https://merchant.example/callback", payment.getCallbackUrl());
        assertEquals(createdAt, payment.getCreatedAt());
        assertEquals(detectedAt, payment.getDetectedAt());
    }

    @Test
    void findByStatusInDelegatesStatusNames() {
        repository.findByStatusIn(List.of(PaymentStatus.DETECTED, PaymentStatus.CONFIRMING));

        verify(springDataRepository).findByStatusIn(List.of("DETECTED", "CONFIRMING"));
    }
}
