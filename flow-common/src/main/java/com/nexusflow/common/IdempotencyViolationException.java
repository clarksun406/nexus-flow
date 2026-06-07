package com.nexusflow.common;

/**
 * Thrown when a duplicate/idempotent request is detected.
 */
public class IdempotencyViolationException extends NexusFlowException {

    public IdempotencyViolationException(String orderId) {
        super(ErrorCode.IDEMPOTENCY_VIOLATION, "Duplicate request for order: " + orderId);
    }
}