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
    private Integer confirmations;
    private Integer requiredConfirmations;

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
        this.callbackUrl = callbackUrl;
        this.status = PaymentStatus.CREATED;
        this.createdAt = Instant.now();
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
        this.txHash = txHash;
        this.received = receivedAmount;
        this.detectedAt = Instant.now();
        transitionTo(PaymentStatus.DETECTED, txHash, null);
    }

    /**
     * Update confirmation count. Auto-transitions to CONFIRMED when threshold reached.
     *
     * @return true if payment is now CONFIRMED
     */
    public boolean updateConfirmations(int count) {
        this.confirmations = count;
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
        transitionTo(PaymentStatus.FAILED, txHash, null);
    }

    /**
     * Mark payment as expired.
     */
    public void markExpired() {
        this.expiredAt = Instant.now();
        transitionTo(PaymentStatus.EXPIRED, null, null);
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