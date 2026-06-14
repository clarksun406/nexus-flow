package com.nexusflow.domain.payment;

import com.nexusflow.domain.event.DomainEvent;
import com.nexusflow.domain.event.PaymentStateChangedEvent;
import com.nexusflow.domain.shared.Money;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * CryptoPayment aggregate root.
 *
 * Encapsulates the full lifecycle of an on-chain payment:
 * creation → detection → confirmation → terminal state.
 */
@Getter
@Accessors(chain = true)
public class CryptoPayment {

    private String id;
    private String orderId;
    private Money expected;
    private Money received;
    private PaymentStatus status;
    private String receivingAddress;
    private String txHash;
    private Long detectedBlockNumber;
    private Integer confirmations;
    private Integer requiredConfirmations;
    private Integer retryCount;
    private Instant nextRetryAt;
    private String lastFailureReason;

    @Setter
    private String callbackUrl;

    private Instant createdAt;
    private Instant detectedAt;
    private Instant confirmedAt;
    private Instant expiredAt;

    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    @Builder
    public CryptoPayment(String id, String orderId, Money expected,
                         String receivingAddress, Integer requiredConfirmations,
                         String callbackUrl) {
        this.id = id;
        this.orderId = orderId;
        this.expected = expected;
        this.receivingAddress = receivingAddress;
        this.requiredConfirmations = requiredConfirmations != null ? requiredConfirmations : 12;
        this.confirmations = 0;
        this.retryCount = 0;
        this.callbackUrl = callbackUrl;
        this.status = PaymentStatus.CREATED;
        this.createdAt = Instant.now();
    }

    /**
     * Full-args builder for reconstituting a CryptoPayment from persistence.
     * Not for public use - only for repository mapping.
     */
    @Builder(builderMethodName = "reconstitute", builderClassName = "CryptoPaymentReconstituteBuilder")
    private CryptoPayment(String id, String orderId, Money expected, Money received,
                          PaymentStatus status, String receivingAddress, String txHash,
                          Long detectedBlockNumber, Integer confirmations, Integer requiredConfirmations,
                          Integer retryCount, Instant nextRetryAt, String lastFailureReason,
                          String callbackUrl, Instant createdAt, Instant detectedAt,
                          Instant confirmedAt, Instant expiredAt) {
        this.id = id;
        this.orderId = orderId;
        this.expected = expected;
        this.received = received;
        this.status = status;
        this.receivingAddress = receivingAddress;
        this.txHash = txHash;
        this.detectedBlockNumber = detectedBlockNumber;
        this.confirmations = confirmations;
        this.requiredConfirmations = requiredConfirmations;
        this.retryCount = retryCount != null ? retryCount : 0;
        this.nextRetryAt = nextRetryAt;
        this.lastFailureReason = lastFailureReason;
        this.callbackUrl = callbackUrl;
        this.createdAt = createdAt;
        this.detectedAt = detectedAt;
        this.confirmedAt = confirmedAt;
        this.expiredAt = expiredAt;
    }

    // ─── State transitions ───

    /**
     * Mark payment as pending (waiting for on-chain activity).
     */
    public void markPending() {
        transitionTo(PaymentStatus.PENDING, null, null);
    }

    /**
     * Mark payment as detected (transaction seen on-chain).
     */
    public void markDetected(String txHash, Money receivedAmount) {
        markDetected(txHash, receivedAmount, null);
    }

    public void markDetected(String txHash, Money receivedAmount, Long blockNumber) {
        this.txHash = txHash;
        this.received = receivedAmount;
        this.detectedBlockNumber = blockNumber;
        this.detectedAt = Instant.now();
        resetRetryState();
        transitionTo(PaymentStatus.DETECTED, txHash, null);
    }

    /**
     * Update confirmation count. Auto-transitions to CONFIRMED when threshold reached.
     *
     * @return true if payment is now CONFIRMED
     */
    public boolean updateConfirmations(int count) {
        this.confirmations = count;
        resetRetryState();
        if (status == PaymentStatus.DETECTED && count > 0) {
            transitionTo(PaymentStatus.CONFIRMING, txHash, count);
        } else if (status == PaymentStatus.CONFIRMING && count >= requiredConfirmations) {
            this.confirmedAt = Instant.now();
            transitionTo(PaymentStatus.CONFIRMED, txHash, count);
            return true;
        }
        return false;
    }

    /**
     * Mark payment as failed.
     */
    public void markFailed(String reason) {
        this.lastFailureReason = reason;
        transitionTo(PaymentStatus.FAILED, txHash, null);
    }

    /**
     * Mark payment as expired.
     */
    public void markExpired() {
        this.expiredAt = Instant.now();
        transitionTo(PaymentStatus.EXPIRED, null, null);
    }

    public void rollbackAfterReorg() {
        this.txHash = null;
        this.received = null;
        this.detectedBlockNumber = null;
        this.detectedAt = null;
        this.confirmations = 0;
        resetRetryState();
        transitionTo(PaymentStatus.PENDING, null, null);
    }

    public void recordRetryFailure(String reason, Instant nextRetryAt) {
        this.retryCount = (retryCount != null ? retryCount : 0) + 1;
        this.lastFailureReason = reason;
        this.nextRetryAt = nextRetryAt;
    }

    public boolean isRetryDue(Instant now) {
        return nextRetryAt == null || !nextRetryAt.isAfter(now);
    }

    public void resetRetryState() {
        this.retryCount = 0;
        this.nextRetryAt = null;
        this.lastFailureReason = null;
    }

    /**
     * Collect and clear domain events.
     */
    public List<DomainEvent> collectEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    private void transitionTo(PaymentStatus target, String txHash, Integer confirmations) {
        PaymentStatus previous = this.status;
        this.status = previous.requireTransitionTo(target);
        domainEvents.add(new PaymentStateChangedEvent(
                id, orderId, previous, target, txHash, confirmations));
    }
}
