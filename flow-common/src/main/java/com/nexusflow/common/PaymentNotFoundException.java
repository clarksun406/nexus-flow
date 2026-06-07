package com.nexusflow.common;

/**
 * Thrown when a payment is not found.
 */
public class PaymentNotFoundException extends NexusFlowException {

    public PaymentNotFoundException(String paymentId) {
        super(ErrorCode.PAYMENT_NOT_FOUND, "Payment not found: " + paymentId);
    }
}