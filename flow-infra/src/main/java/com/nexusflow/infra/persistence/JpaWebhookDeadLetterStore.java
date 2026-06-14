package com.nexusflow.infra.persistence;

import com.nexusflow.application.WebhookDeadLetter;
import com.nexusflow.application.WebhookDeadLetterStore;
import com.nexusflow.application.WebhookDeadLetterStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaWebhookDeadLetterStore implements WebhookDeadLetterStore {

    private static final int MAX_RECENT_LIMIT = 100;

    private final SpringDataWebhookDeadLetterRepository repository;

    @Override
    public void save(WebhookDeadLetter deadLetter) {
        repository.save(toEntity(deadLetter));
    }

    @Override
    public List<WebhookDeadLetter> findRecent(int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, MAX_RECENT_LIMIT));
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, boundedLimit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<WebhookDeadLetter> findByStatus(WebhookDeadLetterStatus status, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, MAX_RECENT_LIMIT));
        return repository.findByStatusOrderByCreatedAtDesc(status.name(), PageRequest.of(0, boundedLimit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<WebhookDeadLetter> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    WebhookDeadLetterEntity toEntity(WebhookDeadLetter deadLetter) {
        WebhookDeadLetterEntity entity = new WebhookDeadLetterEntity();
        entity.setId(deadLetter.getId());
        entity.setDeliveryType(deadLetter.getDeliveryType());
        entity.setTargetUrl(deadLetter.getTargetUrl());
        entity.setPayload(deadLetter.getPayload());
        entity.setEventId(deadLetter.getEventId());
        entity.setEventType(deadLetter.getEventType());
        entity.setPaymentId(deadLetter.getPaymentId());
        entity.setOrderId(deadLetter.getOrderId());
        entity.setFailureReason(deadLetter.getFailureReason());
        entity.setAttempts(deadLetter.getAttempts());
        entity.setStatus(deadLetter.getStatus() != null
                ? deadLetter.getStatus().name()
                : WebhookDeadLetterStatus.PENDING.name());
        entity.setCreatedAt(deadLetter.getCreatedAt());
        entity.setResolvedAt(deadLetter.getResolvedAt());
        return entity;
    }

    WebhookDeadLetter toDomain(WebhookDeadLetterEntity entity) {
        return WebhookDeadLetter.builder()
                .id(entity.getId())
                .deliveryType(entity.getDeliveryType())
                .targetUrl(entity.getTargetUrl())
                .payload(entity.getPayload())
                .eventId(entity.getEventId())
                .eventType(entity.getEventType())
                .paymentId(entity.getPaymentId())
                .orderId(entity.getOrderId())
                .failureReason(entity.getFailureReason())
                .attempts(entity.getAttempts() != null ? entity.getAttempts() : 0)
                .status(entity.getStatus() != null
                        ? WebhookDeadLetterStatus.valueOf(entity.getStatus())
                        : WebhookDeadLetterStatus.PENDING)
                .createdAt(entity.getCreatedAt())
                .resolvedAt(entity.getResolvedAt())
                .build();
    }
}
