package com.nexusflow.domain.event;

import com.nexusflow.domain.payment.PaymentStatus;
import lombok.Getter;

/**
 * Emitted when a payment undergoes a state transition.
 */
@Getter
public class PaymentStateChangedEvent extends DomainEvent {

    private final String paymentId;
    private final String orderId;
    private final PaymentStatus previousStatus;
    private final PaymentStatus newStatus;
    private final String txHash;
    private final Integer confirmations;

    public PaymentStateChangedEvent(String paymentId, String orderId,
                                    PaymentStatus previousStatus, PaymentStatus newStatus,
                                    String txHash, Integer confirmations) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.txHash = txHash;
        this.confirmations = confirmations;
    }

    @Override
    public String eventType() {
        return "crypto.payment." + newStatus.name().toLowerCase();
    }

    /**
     * Convenience method: create a detected event.
     */
    public static PaymentStateChangedEvent detected(String paymentId, String orderId,
                                                     PaymentStatus previous, String txHash) {
        return new PaymentStateChangedEvent(paymentId, orderId, previous,
                PaymentStatus.DETECTED, txHash, null);
    }

    /**
     * Convenience method: create a confirmed event.
     */
    public static PaymentStateChangedEvent confirmed(String paymentId, String orderId,
                                                      PaymentStatus previous, String txHash, int confirmations) {
        return new PaymentStateChangedEvent(paymentId, orderId, previous,
                PaymentStatus.CONFIRMED, txHash, confirmations);
    }

    /**
     * Convenience method: create a failed event.
     */
    public static PaymentStateChangedEvent failed(String paymentId, String orderId,
                                                   PaymentStatus previous, String txHash) {
        return new PaymentStateChangedEvent(paymentId, orderId, previous,
                PaymentStatus.FAILED, txHash, null);
    }
}