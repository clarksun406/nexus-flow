package com.nexusflow.application;

import com.nexusflow.domain.shared.Chain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BlockchainCircuitBreaker {

    private final int failureThreshold;
    private final long cooldownSeconds;
    private final Map<Chain, State> states = new ConcurrentHashMap<>();

    public BlockchainCircuitBreaker(@Value("${nexusflow.blockchain.circuit-breaker.failure-threshold:3}") int failureThreshold,
                                    @Value("${nexusflow.blockchain.circuit-breaker.cooldown-seconds:30}") long cooldownSeconds) {
        this.failureThreshold = failureThreshold;
        this.cooldownSeconds = cooldownSeconds;
    }

    public boolean allowRequest(Chain chain) {
        State state = states.get(chain);
        return state == null || state.openUntil == null || !state.openUntil.isAfter(Instant.now());
    }

    public void recordSuccess(Chain chain) {
        states.remove(chain);
    }

    public void recordFailure(Chain chain) {
        states.compute(chain, (ignored, state) -> {
            State current = state != null ? state : new State();
            current.failures++;
            if (current.failures >= failureThreshold) {
                current.openUntil = Instant.now().plusSeconds(cooldownSeconds);
            }
            return current;
        });
    }

    private static final class State {
        private int failures;
        private Instant openUntil;
    }
}
