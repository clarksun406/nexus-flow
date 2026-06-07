package com.nexusflow.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Value;

@Value @Builder
public class CreateOrderRequest {
    @NotBlank String merchantId;
    @NotBlank String merchantOrderNo;
    @NotBlank @Positive String amountFiat;
    @NotBlank String currencyFiat;
    String preferredChannel;
    String notifyUrl;
    String returnUrl;
    String extend;
}