package com.nexusflow.application;

import com.nexusflow.application.dto.WebhookDeadLetterResponse;
import com.nexusflow.common.InvalidStateTransitionException;
import com.nexusflow.common.WebhookDeadLetterNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WebhookDeadLetterApplicationService {

    private final WebhookDeadLetterStore deadLetterStore;
    private final WebhookClient webhookClient;

    @Transactional(readOnly = true)
    public List<WebhookDeadLetterResponse> list(WebhookDeadLetterStatus status, int limit) {
        return deadLetterStore.findByStatus(status, limit).stream()
                .map(this::toResponse)
                .toList();
    }

    public WebhookDeadLetterResponse replay(String id) {
        WebhookDeadLetter deadLetter = findRequired(id);
        requirePending(deadLetter);

        WebhookDeliveryResult result;
        try {
            result = webhookClient.sendWithRetry(deadLetter.getTargetUrl(), deadLetter.getPayload());
        } catch (RuntimeException e) {
            result = WebhookDeliveryResult.failed(0, e.getMessage());
        }
        int totalAttempts = deadLetter.getAttempts() + (result != null ? result.attempts() : 0);
        WebhookDeadLetter updated;
        if (result != null && result.success()) {
            updated = copy(deadLetter)
                    .attempts(totalAttempts)
                    .status(WebhookDeadLetterStatus.REPLAYED)
                    .resolvedAt(Instant.now())
                    .build();
        } else {
            updated = copy(deadLetter)
                    .attempts(totalAttempts)
                    .failureReason(result != null && result.lastError() != null
                            ? result.lastError()
                            : "Webhook replay failed")
                    .build();
        }
        deadLetterStore.save(updated);
        return toResponse(updated);
    }

    @Transactional
    public WebhookDeadLetterResponse ignore(String id) {
        WebhookDeadLetter deadLetter = findRequired(id);
        requirePending(deadLetter);
        WebhookDeadLetter updated = copy(deadLetter)
                .status(WebhookDeadLetterStatus.IGNORED)
                .resolvedAt(Instant.now())
                .build();
        deadLetterStore.save(updated);
        return toResponse(updated);
    }

    private WebhookDeadLetter findRequired(String id) {
        return deadLetterStore.findById(id)
                .orElseThrow(() -> new WebhookDeadLetterNotFoundException(id));
    }

    private void requirePending(WebhookDeadLetter deadLetter) {
        if (deadLetter.getStatus() != WebhookDeadLetterStatus.PENDING) {
            throw new InvalidStateTransitionException(deadLetter.getStatus().name(), "PENDING_ACTION");
        }
    }

    private WebhookDeadLetter.WebhookDeadLetterBuilder copy(WebhookDeadLetter deadLetter) {
        return WebhookDeadLetter.builder()
                .id(deadLetter.getId())
                .deliveryType(deadLetter.getDeliveryType())
                .targetUrl(deadLetter.getTargetUrl())
                .payload(deadLetter.getPayload())
                .eventId(deadLetter.getEventId())
                .eventType(deadLetter.getEventType())
                .paymentId(deadLetter.getPaymentId())
                .orderId(deadLetter.getOrderId())
                .failureReason(deadLetter.getFailureReason())
                .attempts(deadLetter.getAttempts())
                .status(deadLetter.getStatus())
                .createdAt(deadLetter.getCreatedAt())
                .resolvedAt(deadLetter.getResolvedAt());
    }

    private WebhookDeadLetterResponse toResponse(WebhookDeadLetter deadLetter) {
        return WebhookDeadLetterResponse.builder()
                .id(deadLetter.getId())
                .deliveryType(deadLetter.getDeliveryType())
                .targetUrl(deadLetter.getTargetUrl())
                .payload(deadLetter.getPayload())
                .eventId(deadLetter.getEventId())
                .eventType(deadLetter.getEventType())
                .paymentId(deadLetter.getPaymentId())
                .orderId(deadLetter.getOrderId())
                .failureReason(deadLetter.getFailureReason())
                .attempts(deadLetter.getAttempts())
                .status(deadLetter.getStatus() != null ? deadLetter.getStatus().name() : null)
                .createdAt(toEpochMillis(deadLetter.getCreatedAt()))
                .resolvedAt(toEpochMillis(deadLetter.getResolvedAt()))
                .build();
    }

    private Long toEpochMillis(Instant instant) {
        return instant != null ? instant.toEpochMilli() : null;
    }
}
