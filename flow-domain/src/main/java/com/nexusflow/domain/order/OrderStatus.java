package com.nexusflow.domain.order;

import com.nexusflow.common.InvalidStateTransitionException;
import lombok.Getter;

import java.util.Set;

/**
 * Order status state machine for the orchestration layer.
 *
 * WAITING_PAYMENT → CONFIRMED (fully paid)
 *                 → PARTIALLY_PAID (underpaid) → CONFIRMED (top-up)
 *                 → EXPIRED (timeout)
 * CONFIRMED → REFUND_PROCESSING → REFUNDED / REFUND_FAILED
 */
@Getter
public enum OrderStatus {

    WAITING_PAYMENT,
    PARTIALLY_PAID,
    CONFIRMED,
    EXPIRED,
    REFUND_PROCESSING,
    REFUNDED,
    REFUND_FAILED,
    FAILED;

    private static final Set<Transition> ALLOWED = Set.of(
            t(WAITING_PAYMENT, CONFIRMED),
            t(WAITING_PAYMENT, PARTIALLY_PAID),
            t(WAITING_PAYMENT, EXPIRED),
            t(WAITING_PAYMENT, FAILED),
            t(PARTIALLY_PAID, CONFIRMED),
            t(PARTIALLY_PAID, EXPIRED),
            t(PARTIALLY_PAID, FAILED),
            t(CONFIRMED, REFUND_PROCESSING),
            t(REFUND_PROCESSING, REFUNDED),
            t(REFUND_PROCESSING, REFUND_FAILED)
    );

    public boolean canTransitionTo(OrderStatus target) {
        return ALLOWED.contains(new Transition(this, target));
    }

    public OrderStatus requireTransitionTo(OrderStatus target) {
        if (!canTransitionTo(target)) {
            throw new InvalidStateTransitionException(this.name(), target.name());
        }
        return target;
    }

    public boolean isTerminal() {
        return this == CONFIRMED || this == EXPIRED || this == REFUNDED
                || this == REFUND_FAILED || this == FAILED;
    }

    private static Transition t(OrderStatus from, OrderStatus to) {
        return new Transition(from, to);
    }

    private record Transition(OrderStatus from, OrderStatus to) {}
}