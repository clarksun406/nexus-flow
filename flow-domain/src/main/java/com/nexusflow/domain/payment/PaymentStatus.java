package com.nexusflow.domain.payment;

import com.nexusflow.common.InvalidStateTransitionException;
import lombok.Getter;

import java.util.Set;

/**
 * Strict state machine for crypto payments.
 *
 * Allowed transitions:
 * CREATED → PENDING → DETECTED → CONFIRMING → CONFIRMED
 *                                         CONFIRMING → FAILED
 *                              PENDING → EXPIRED
 */
@Getter
public enum PaymentStatus {

    CREATED,
    PENDING,
    DETECTED,
    CONFIRMING,
    CONFIRMED,
    FAILED,
    EXPIRED;

    private static final Set<Transition> ALLOWED = Set.of(
            new Transition(CREATED, PENDING),
            new Transition(PENDING, DETECTED),
            new Transition(DETECTED, PENDING),
            new Transition(DETECTED, CONFIRMING),
            new Transition(CONFIRMING, PENDING),
            new Transition(CONFIRMING, CONFIRMED),
            new Transition(CONFIRMING, FAILED),
            new Transition(PENDING, EXPIRED)
    );

    public boolean canTransitionTo(PaymentStatus target) {
        return ALLOWED.contains(new Transition(this, target));
    }

    public PaymentStatus requireTransitionTo(PaymentStatus target) {
        if (!canTransitionTo(target)) {
            throw new InvalidStateTransitionException(this.name(), target.name());
        }
        return target;
    }

    public boolean isTerminal() {
        return this == CONFIRMED || this == FAILED || this == EXPIRED;
    }

    private record Transition(PaymentStatus from, PaymentStatus to) {}
}
