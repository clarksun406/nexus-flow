package com.nexusflow.domain.refund;

import com.nexusflow.common.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundStatusTest {

    @Test
    void processingCanTransitionToSuccess() {
        assertThat(RefundStatus.PROCESSING.requireTransitionTo(RefundStatus.SUCCESS)).isEqualTo(RefundStatus.SUCCESS);
    }

    @Test
    void processingCanTransitionToFailed() {
        assertThat(RefundStatus.PROCESSING.requireTransitionTo(RefundStatus.FAILED)).isEqualTo(RefundStatus.FAILED);
    }

    @Test
    void successIsTerminal() {
        assertThat(RefundStatus.SUCCESS.isTerminal()).isTrue();
    }

    @Test
    void failedIsTerminal() {
        assertThat(RefundStatus.FAILED.isTerminal()).isTrue();
    }

    @Test
    void processingIsNotTerminal() {
        assertThat(RefundStatus.PROCESSING.isTerminal()).isFalse();
    }

    @Test
    void successCannotTransitionToAnything() {
        assertThatThrownBy(() -> RefundStatus.SUCCESS.requireTransitionTo(RefundStatus.PROCESSING))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void failedCannotTransitionToAnything() {
        assertThatThrownBy(() -> RefundStatus.FAILED.requireTransitionTo(RefundStatus.SUCCESS))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
