package com.nexusflow.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Value;

/**
 * Command to create a new crypto payment.
 */
@Value
@Builder
public class CreatePaymentCommand {

    @NotBlank
    String orderId;

    @NotBlank
    String currency;  // e.g. "USDT_TRC20"

    @NotBlank
    @Positive
    String amount;    // smallest unit (e.g. "1000000" for 1 USDT with 6 decimals)

    String callbackUrl;
}