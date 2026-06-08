package com.nexusflow.domain.order;

import com.nexusflow.common.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStatusTest {

    @Test
    void waitingPaymentCanTransitionToConfirmed() {
        assertThat(OrderStatus.WAITING_PAYMENT.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
    }

    @Test
    void waitingPaymentCanTransitionToPartiallyPaid() {
        assertThat(OrderStatus.WAITING_PAYMENT.canTransitionTo(OrderStatus.PARTIALLY_PAID)).isTrue();
    }

    @Test
    void waitingPaymentCanTransitionToExpired() {
        assertThat(OrderStatus.WAITING_PAYMENT.canTransitionTo(OrderStatus.EXPIRED)).isTrue();
    }

    @Test
    void waitingPaymentCanTransitionToFailed() {
        assertThat(OrderStatus.WAITING_PAYMENT.canTransitionTo(OrderStatus.FAILED)).isTrue();
    }

    @Test
    void partiallyPaidCanTransitionToConfirmed() {
        assertThat(OrderStatus.PARTIALLY_PAID.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
    }

    @Test
    void confirmedCanTransitionToRefundProcessing() {
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.REFUND_PROCESSING)).isTrue();
    }

    @Test
    void refundProcessingCanTransitionToRefunded() {
        assertThat(OrderStatus.REFUND_PROCESSING.canTransitionTo(OrderStatus.REFUNDED)).isTrue();
    }

    @Test
    void refundProcessingCanTransitionToRefundFailed() {
        assertThat(OrderStatus.REFUND_PROCESSING.canTransitionTo(OrderStatus.REFUND_FAILED)).isTrue();
    }

    @Test
    void refundFailedCanRetryRefundProcessing() {
        assertThat(OrderStatus.REFUND_FAILED.canTransitionTo(OrderStatus.REFUND_PROCESSING)).isTrue();
    }

    @Test
    void waitingPaymentCannotTransitionToRefundProcessing() {
        assertThat(OrderStatus.WAITING_PAYMENT.canTransitionTo(OrderStatus.REFUND_PROCESSING)).isFalse();
    }

    @Test
    void confirmedCannotTransitionToWaitingPayment() {
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.WAITING_PAYMENT)).isFalse();
    }

    @Test
    void refundedIsTerminal() {
        assertThat(OrderStatus.REFUNDED.isTerminal()).isTrue();
    }

    @Test
    void expiredIsTerminal() {
        assertThat(OrderStatus.EXPIRED.isTerminal()).isTrue();
    }

    @Test
    void failedIsTerminal() {
        assertThat(OrderStatus.FAILED.isTerminal()).isTrue();
    }

    @Test
    void confirmedIsNotTerminal() {
        assertThat(OrderStatus.CONFIRMED.isTerminal()).isFalse();
    }

    @Test
    void refundFailedIsNotTerminal() {
        assertThat(OrderStatus.REFUND_FAILED.isTerminal()).isFalse();
    }

    @Test
    void requireTransitionToThrowsOnInvalid() {
        assertThatThrownBy(() -> OrderStatus.WAITING_PAYMENT.requireTransitionTo(OrderStatus.REFUNDED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void requireTransitionToReturnsTarget() {
        OrderStatus result = OrderStatus.WAITING_PAYMENT.requireTransitionTo(OrderStatus.CONFIRMED);
        assertThat(result).isEqualTo(OrderStatus.CONFIRMED);
    }
}
