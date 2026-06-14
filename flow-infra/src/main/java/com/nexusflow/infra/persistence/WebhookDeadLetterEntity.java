package com.nexusflow.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "webhook_dead_letters")
public class WebhookDeadLetterEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "delivery_type", length = 40, nullable = false)
    private String deliveryType;

    @Column(name = "target_url", length = 2048, nullable = false)
    private String targetUrl;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "event_id", length = 128)
    private String eventId;

    @Column(name = "event_type", length = 120)
    private String eventType;

    @Column(name = "payment_id", length = 64)
    private String paymentId;

    @Column(name = "order_id", length = 128)
    private String orderId;

    @Column(name = "failure_reason", length = 1024, nullable = false)
    private String failureReason;

    @Column(nullable = false)
    private Integer attempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
