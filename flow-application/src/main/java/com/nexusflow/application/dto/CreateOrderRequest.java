package com.nexusflow.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Value;

@Value @Builder
public class CreateOrderRequest {
    @NotBlank String merchantId;
    @NotBlank String merchantOrderNo;
    @Pattern(regexp = "^[0-9]+(\\.[0-9]+)?$", message = "amountFiat must be a positive decimal") String amountFiat;
    String currencyFiat;
    @Pattern(regexp = "^[0-9]+(\\.[0-9]+)?$", message = "amountCrypto must be a positive decimal") String amountCrypto;
    String currencyCrypto;
    String network;
    String preferredChannel;
    String notifyUrl;
    String returnUrl;
    String extend;
}
