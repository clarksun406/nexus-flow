package com.nexusflow.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Command to create a new crypto payment.
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
public class CreatePaymentCommand {

    @NotBlank
    String orderId;

    @NotBlank
    String currency;  // e.g. "USDT_TRC20"

    @NotBlank
    @Pattern(regexp = "^(?=.*[1-9])[0-9]+(\\.[0-9]+)?$", message = "amount must be a positive decimal")
    String amount;    // smallest unit (e.g. "1000000" for 1 USDT with 6 decimals)

    String callbackUrl;

    String idempotencyKey;
}
