package com.nexusflow.infra.persistence;

import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.payment.PaymentStatus;
import com.nexusflow.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Test
    void findByStatusInReturnsOnlyMatchingStatuses() {
        CryptoPayment pending = payment("1", "A");
        pending.markPending();
        repo.save(pending);

        CryptoPayment created = payment("2", "B"); // stays CREATED
        repo.save(created);

        CryptoPayment detected = payment("3", "C");
        detected.markPending();
        detected.markDetected("tx", Money.of("USDT_TRC20", BigDecimal.ONE));
        repo.save(detected);

        List<String> ids = repo.findByStatusIn(List.of(PaymentStatus.PENDING, PaymentStatus.DETECTED))
                .stream().map(CryptoPayment::getId).sorted().collect(Collectors.toList());

        assertEquals(List.of("1", "3"), ids);
    }

    @Test
    void findByStatusInEmptyReturnsEmpty() {
        CryptoPayment pending = payment("1", "A");
        pending.markPending();
        repo.save(pending);

        assertTrue(repo.findByStatusIn(List.of()).isEmpty());
    }
}
