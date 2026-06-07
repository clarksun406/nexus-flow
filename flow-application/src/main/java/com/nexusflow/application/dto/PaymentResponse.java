package com.nexusflow.application.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Response after creating or querying a payment.
 */
@Value
@Builder
public class PaymentResponse {

    String paymentId;
    String orderId;
    String currency;
    String expectedAmount;
    String receivingAddress;
    String status;
    String txHash;
    Integer confirmations;
    Long createdAt;
    Long detectedAt;
    Long confirmedAt;
}