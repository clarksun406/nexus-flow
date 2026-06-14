package com.nexusflow.infra.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.application.PaymentIdempotencyStore;
import com.nexusflow.application.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaPaymentIdempotencyStore implements PaymentIdempotencyStore {

    private final SpringDataIdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<StoredPaymentResponse> find(String key) {
        Optional<IdempotencyKeyEntity> found = repository.findById(key);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        IdempotencyKeyEntity entity = found.get();
        if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(Instant.now())) {
            repository.deleteById(key);
            return Optional.empty();
        }
        return Optional.of(new StoredPaymentResponse(
                entity.getRequestHash(),
                deserialize(entity.getResponse())));
    }

    @Override
    public boolean reserve(String key, String requestHash, Instant expiresAt) {
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setIdempotencyKey(key);
        entity.setRequestHash(requestHash);
        entity.setCreatedAt(Instant.now());
        entity.setExpiresAt(expiresAt);
        try {
            repository.save(entity);
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Override
    public void complete(String key, PaymentResponse response) {
        IdempotencyKeyEntity entity = repository.findById(key)
                .orElseThrow(() -> new IllegalStateException("Missing idempotency reservation: " + key));
        entity.setResponse(serialize(response));
        repository.save(entity);
    }

    @Override
    public void delete(String key) {
        repository.deleteById(key);
    }

    private String serialize(PaymentResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize idempotent response", e);
        }
    }

    private PaymentResponse deserialize(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(response, PaymentResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize idempotent response", e);
        }
    }
}
