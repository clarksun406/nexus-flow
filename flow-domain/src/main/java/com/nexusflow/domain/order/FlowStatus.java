package com.nexusflow.domain.order;

import com.nexusflow.common.InvalidStateTransitionException;
import lombok.Getter;

import java.util.Set;

@Getter
public enum FlowStatus {

    INIT,
    WAITING,
    CONFIRMED,
    CANCELLED,
    FAILED;

    private static final Set<Transition> ALLOWED = Set.of(
            t(INIT, WAITING),
            t(WAITING, CONFIRMED),
            t(WAITING, CANCELLED),
            t(WAITING, FAILED)
    );

    public FlowStatus requireTransitionTo(FlowStatus target) {
        if (!ALLOWED.contains(new Transition(this, target))) {
            throw new InvalidStateTransitionException(this.name(), target.name());
        }
        return target;
    }

    private static Transition t(FlowStatus from, FlowStatus to) {
        return new Transition(from, to);
    }

    private record Transition(FlowStatus from, FlowStatus to) {}
}