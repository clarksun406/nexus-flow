package com.nexusflow.domain.fiat;

import com.nexusflow.common.InvalidStateTransitionException;

import java.util.Set;

public enum FiatRampStatus {
    CREATED,
    PENDING_PAYMENT,
    PROCESSING,
    COMPLETED,
    FAILED,
    EXPIRED,
    CANCELLED;

    private static final Set<Transition> ALLOWED = Set.of(
            t(CREATED, PENDING_PAYMENT),
            t(CREATED, PROCESSING),
            t(CREATED, FAILED),
            t(CREATED, EXPIRED),
            t(CREATED, CANCELLED),
            t(PENDING_PAYMENT, PROCESSING),
            t(PENDING_PAYMENT, COMPLETED),
            t(PENDING_PAYMENT, FAILED),
            t(PENDING_PAYMENT, EXPIRED),
            t(PENDING_PAYMENT, CANCELLED),
            t(PROCESSING, COMPLETED),
            t(PROCESSING, FAILED),
            t(PROCESSING, EXPIRED),
            t(PROCESSING, CANCELLED)
    );

    public FiatRampStatus requireTransitionTo(FiatRampStatus target) {
        if (!ALLOWED.contains(new Transition(this, target))) {
            throw new InvalidStateTransitionException(this.name(), target.name());
        }
        return target;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == EXPIRED || this == CANCELLED;
    }

    private static Transition t(FiatRampStatus from, FiatRampStatus to) {
        return new Transition(from, to);
    }

    private record Transition(FiatRampStatus from, FiatRampStatus to) {}
}
