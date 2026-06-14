package com.nexusflow.application.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WebhookDeadLetterResponse {
    String id;
    String deliveryType;
    String targetUrl;
    String payload;
    String eventId;
    String eventType;
    String paymentId;
    String orderId;
    String failureReason;
    int attempts;
    String status;
    Long createdAt;
    Long resolvedAt;
}
