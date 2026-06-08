package com.nexusflow.domain.order;

import com.nexusflow.common.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowStatusTest {

    @Test
    void initCanTransitionToWaiting() {
        assertThat(FlowStatus.INIT.requireTransitionTo(FlowStatus.WAITING)).isEqualTo(FlowStatus.WAITING);
    }

    @Test
    void waitingCanTransitionToConfirmed() {
        assertThat(FlowStatus.WAITING.requireTransitionTo(FlowStatus.CONFIRMED)).isEqualTo(FlowStatus.CONFIRMED);
    }

    @Test
    void waitingCanTransitionToCancelled() {
        assertThat(FlowStatus.WAITING.requireTransitionTo(FlowStatus.CANCELLED)).isEqualTo(FlowStatus.CANCELLED);
    }

    @Test
    void waitingCanTransitionToFailed() {
        assertThat(FlowStatus.WAITING.requireTransitionTo(FlowStatus.FAILED)).isEqualTo(FlowStatus.FAILED);
    }

    @Test
    void initCannotTransitionToConfirmed() {
        assertThatThrownBy(() -> FlowStatus.INIT.requireTransitionTo(FlowStatus.CONFIRMED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void confirmedCannotTransitionToAnything() {
        assertThatThrownBy(() -> FlowStatus.CONFIRMED.requireTransitionTo(FlowStatus.FAILED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void cancelledCannotTransitionToAnything() {
        assertThatThrownBy(() -> FlowStatus.CANCELLED.requireTransitionTo(FlowStatus.WAITING))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
