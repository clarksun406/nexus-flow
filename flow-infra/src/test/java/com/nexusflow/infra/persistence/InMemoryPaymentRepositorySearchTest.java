package com.nexusflow.infra.persistence;

import com.nexusflow.common.PageResult;
import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.payment.PaymentStatus;
import com.nexusflow.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryPaymentRepositorySearchTest {

    private static final Instant BASE = Instant.parse("2026-08-30T12:00:00Z");

    private InMemoryPaymentRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPaymentRepository();
    }

    @Test
    void searchFiltersByStatusAndPages() {
        repository.save(payment("exec-1", PaymentStatus.PENDING, 1));
        repository.save(payment("exec-2", PaymentStatus.DETECTED, 2));
        repository.save(payment("exec-3", PaymentStatus.CONFIRMING, 3));

        PageResult<CryptoPayment> pending = repository.search(PaymentStatus.PENDING, 0, 20);
        PageResult<CryptoPayment> all = repository.search(null, 0, 2);
        PageResult<CryptoPayment> secondPage = repository.search(null, 1, 2);

        assertEquals(1, pending.total());
        assertEquals(List.of("exec-1"), pending.items().stream().map(CryptoPayment::getId).toList());

        assertEquals(3, all.total());
        assertEquals(2, all.items().size());
        assertEquals(List.of("exec-3", "exec-2"), all.items().stream().map(CryptoPayment::getId).toList());

        assertEquals(1, secondPage.items().size());
        assertEquals("exec-1", secondPage.items().get(0).getId());
    }

    private CryptoPayment payment(String id, PaymentStatus status, int secondsOffset) {
        return CryptoPayment.reconstitute()
                .id(id)
                .orderId("order-" + id)
                .expected(Money.of("USDT_TRC20", new BigDecimal("10.00")))
                .receivingAddress("ADDR-" + id)
                .status(status)
                .requiredConfirmations(3)
                .createdAt(BASE.plusSeconds(secondsOffset))
                .build();
    }
}
