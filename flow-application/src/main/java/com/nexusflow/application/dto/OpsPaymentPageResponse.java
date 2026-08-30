package com.nexusflow.application.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Paged execution-layer payment list for the ops console.
 */
@Value
@Builder
public class OpsPaymentPageResponse {
    List<PaymentItem> items;
    int page;
    int size;
    long total;

    @Value
    @Builder
    public static class PaymentItem {
        String id;
        String orderId;
        String currency;
        String status;
        String expectedAmount;
        String receivedAmount;
        String receivingAddress;
        String txHash;
        Integer confirmations;
        Integer requiredConfirmations;
        String lastFailureReason;
        Long createdAt;
        Long detectedAt;
        Long confirmedAt;
    }
}
