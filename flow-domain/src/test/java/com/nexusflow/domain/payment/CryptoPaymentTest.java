package com.nexusflow.domain.payment;

import com.nexusflow.common.InvalidStateTransitionException;
import com.nexusflow.domain.event.DomainEvent;
import com.nexusflow.domain.event.PaymentStateChangedEvent;
import com.nexusflow.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoPaymentTest {

    private CryptoPayment newPayment() {
        return CryptoPayment.builder()
                .id("pay-1")
                .orderId("order-1")
                .expected(Money.of("USDT_TRC20", new BigDecimal("100")))
                .receivingAddress("TADDR")
                .requiredConfirmations(3)
                .build();
    }

    @Test
    void newPaymentStartsInCreatedState() {
        CryptoPayment p = newPayment();
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(p.getCreatedAt()).isNotNull();
        assertThat(p.getRequiredConfirmations()).isEqualTo(3);
    }

    @Test
    void fullHappyPathTransitionsToConfirmed() {
        CryptoPayment p = newPayment();

        p.markPending();
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PENDING);

        p.markDetected("tx-hash", Money.of("USDT_TRC20", new BigDecimal("100")));
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.DETECTED);
        assertThat(p.getTxHash()).isEqualTo("tx-hash");
        assertThat(p.getReceived().getAmount()).isEqualByComparingTo("100");
        assertThat(p.getDetectedAt()).isNotNull();

        // first confirmation moves DETECTED -> CONFIRMING, not yet confirmed
        assertThat(p.updateConfirmations(1)).isFalse();
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.CONFIRMING);

        // reaching required confirmations confirms it
        assertThat(p.updateConfirmations(3)).isTrue();
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(p.getConfirmations()).isEqualTo(3);
        assertThat(p.getConfirmedAt()).isNotNull();
    }

    @Test
    void pendingCanExpire() {
        CryptoPayment p = newPayment();
        p.markPending();
        p.markExpired();
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(p.getExpiredAt()).isNotNull();
    }

    @Test
    void illegalTransitionIsRejected() {
        CryptoPayment p = newPayment();
        // CREATED cannot jump straight to DETECTED
        assertThatThrownBy(() -> p.markDetected("tx", Money.of("USDT_TRC20", BigDecimal.ONE)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void eachTransitionEmitsAndClearsDomainEvent() {
        CryptoPayment p = newPayment();
        p.markPending();

        List<DomainEvent> events = p.collectEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(PaymentStateChangedEvent.class);
        PaymentStateChangedEvent e = (PaymentStateChangedEvent) events.get(0);
        assertThat(e.getPreviousStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(e.getNewStatus()).isEqualTo(PaymentStatus.PENDING);

        // events are cleared after collection
        assertThat(p.collectEvents()).isEmpty();
    }
}
