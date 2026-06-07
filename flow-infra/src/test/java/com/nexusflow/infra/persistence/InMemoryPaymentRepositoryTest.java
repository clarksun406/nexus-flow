package com.nexusflow.infra.persistence;

import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryPaymentRepositoryTest {

    private InMemoryPaymentRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryPaymentRepository();
    }

    private CryptoPayment payment(String id, String address) {
        return CryptoPayment.builder()
                .id(id).orderId("order-" + id)
                .expected(Money.of("USDT_TRC20", new BigDecimal("100")))
                .receivingAddress(address)
                .requiredConfirmations(3)
                .build();
    }

    @Test
    void findsPendingPaymentByReceivingAddress() {
        CryptoPayment p = payment("1", "TADDR");
        p.markPending();
        repo.save(p);

        Optional<CryptoPayment> found = repo.findPendingByReceivingAddress("TADDR");
        assertTrue(found.isPresent());
        assertEquals("1", found.get().getId());
    }

    @Test
    void ignoresNonPendingPaymentsAtAddress() {
        CryptoPayment p = payment("1", "TADDR"); // status CREATED, never moved to PENDING
        repo.save(p);

        assertFalse(repo.findPendingByReceivingAddress("TADDR").isPresent());
    }

    @Test
    void returnsEmptyWhenNoPaymentAtAddress() {
        CryptoPayment p = payment("1", "TADDR");
        p.markPending();
        repo.save(p);

        assertFalse(repo.findPendingByReceivingAddress("OTHER").isPresent());
    }

    @Test
    void nullAddressReturnsEmpty() {
        assertFalse(repo.findPendingByReceivingAddress(null).isPresent());
    }
}
