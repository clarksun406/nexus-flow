package com.nexusflow.application;

import com.nexusflow.application.dto.PaymentResponse;

import java.time.Instant;
import java.util.Optional;

public interface PaymentIdempotencyStore {

    Optional<StoredPaymentResponse> find(String key);

    boolean reserve(String key, String requestHash, Instant expiresAt);

    void complete(String key, PaymentResponse response);

    void delete(String key);

    record StoredPaymentResponse(String requestHash, PaymentResponse response) {
        public boolean isCompleted() {
            return response != null;
        }
    }
}
