package com.nexusflow.application;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class WebhookDeadLetter {
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
    Instant createdAt;
}
