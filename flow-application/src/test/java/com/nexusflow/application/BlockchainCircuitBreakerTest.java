package com.nexusflow.application;

import com.nexusflow.domain.shared.Chain;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BlockchainCircuitBreakerTest {

    @Test
    void opensAfterThresholdAndResetsOnSuccess() {
        BlockchainCircuitBreaker breaker = new BlockchainCircuitBreaker(2, 60);

        assertThat(breaker.allowRequest(Chain.ETH)).isTrue();

        breaker.recordFailure(Chain.ETH);
        assertThat(breaker.allowRequest(Chain.ETH)).isTrue();

        breaker.recordFailure(Chain.ETH);
        assertThat(breaker.allowRequest(Chain.ETH)).isFalse();

        breaker.recordSuccess(Chain.ETH);
        assertThat(breaker.allowRequest(Chain.ETH)).isTrue();
    }
}
