package com.nexusflow.domain.refund;

import com.nexusflow.common.InvalidStateTransitionException;
import lombok.Getter;

import java.util.Set;

@Getter
public enum RefundStatus {

    PROCESSING,
    SUCCESS,
    FAILED;

    private static final Set<Transition> ALLOWED = Set.of(
            t(PROCESSING, SUCCESS),
            t(PROCESSING, FAILED)
    );

    public RefundStatus requireTransitionTo(RefundStatus target) {
        if (!ALLOWED.contains(new Transition(this, target))) {
            throw new InvalidStateTransitionException(this.name(), target.name());
        }
        return target;
    }

    public boolean isTerminal() { return this == SUCCESS || this == FAILED; }

    private static Transition t(RefundStatus from, RefundStatus to) {
        return new Transition(from, to);
    }

    private record Transition(RefundStatus from, RefundStatus to) {}
}