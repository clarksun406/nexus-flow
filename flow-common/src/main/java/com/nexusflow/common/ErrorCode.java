package com.nexusflow.common;

/**
 * Unified error codes for NexusFlow.
 */
public enum ErrorCode {

    // General
    INTERNAL_ERROR("NF-0001", "Internal server error"),
    INVALID_REQUEST("NF-0002", "Invalid request"),
    IDEMPOTENCY_VIOLATION("NF-0003", "Duplicate request detected"),
    UNAUTHORIZED("NF-0004", "Unauthorized"),
    INVALID_SIGNATURE("NF-0005", "Invalid callback signature"),
    FORBIDDEN("NF-0006", "Forbidden"),

    // Payment
    PAYMENT_NOT_FOUND("NF-1001", "Payment not found"),
    PAYMENT_ALREADY_EXISTS("NF-1002", "Payment already exists"),
    INVALID_STATE_TRANSITION("NF-1003", "Invalid payment state transition"),
    PAYMENT_EXPIRED("NF-1004", "Payment has expired"),

    // Wallet
    WALLET_NOT_FOUND("NF-2001", "Wallet not found"),
    INSUFFICIENT_BALANCE("NF-2002", "Insufficient balance"),
    KEY_ENCRYPTION_FAILED("NF-2003", "Key encryption failed"),
    SIGNING_FAILED("NF-2004", "Transaction signing failed"),
    ADDRESS_NOT_AVAILABLE("NF-2005", "No available receiving address"),

    // Blockchain
    CHAIN_UNREACHABLE("NF-3001", "Blockchain node unreachable"),
    TX_VERIFICATION_FAILED("NF-3002", "Transaction verification failed"),
    UNSUPPORTED_CHAIN("NF-3003", "Unsupported blockchain"),
    ORPHAN_TRANSACTION_NOT_FOUND("NF-3004", "Orphan transaction not found"),
    WEBHOOK_DEAD_LETTER_NOT_FOUND("NF-3005", "Webhook dead letter not found"),

    // Orchestration (Order / Channel)
    NO_AVAILABLE_CHANNEL("NF-4001", "No available channel for this request"),
    REFUND_NOT_ALLOWED("NF-4002", "Order status does not allow refund"),
    REFUND_AMOUNT_EXCEEDED("NF-4003", "Refund amount exceeds paid amount"),
    REFUND_NOT_FOUND("NF-4004", "Refund order not found");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
