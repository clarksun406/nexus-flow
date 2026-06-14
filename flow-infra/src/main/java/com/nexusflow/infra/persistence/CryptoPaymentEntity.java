package com.nexusflow.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(name = "crypto_payments")
public class CryptoPaymentEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "order_id", length = 128, nullable = false, unique = true)
    private String orderId;

    @Column(length = 32, nullable = false)
    private String currency;

    @Column(name = "expected_amount", precision = 36, scale = 18, nullable = false)
    private BigDecimal expectedAmount;

    @Column(name = "received_amount", precision = 36, scale = 18)
    private BigDecimal receivedAmount;

    @Column(length = 20, nullable = false)
    private String status;

    @Column(name = "receiving_address", length = 128, nullable = false)
    private String receivingAddress;

    @Column(name = "tx_hash", length = 128)
    private String txHash;

    @Column(name = "detected_block_number")
    private Long detectedBlockNumber;

    @Column
    private Integer confirmations;

    @Column(name = "required_confirmations")
    private Integer requiredConfirmations;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "last_failure_reason", length = 512)
    private String lastFailureReason;

    @Column(name = "callback_url", length = 512)
    private String callbackUrl;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "detected_at")
    private Instant detectedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Version
    private Long version;
}
