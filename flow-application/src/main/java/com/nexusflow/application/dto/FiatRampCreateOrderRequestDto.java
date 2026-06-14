package com.nexusflow.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class FiatRampCreateOrderRequestDto {
    @NotBlank String merchantId;
    @NotBlank String merchantOrderNo;
    String paymentId;
    @NotBlank String direction;
    String quoteId;
    @Pattern(regexp = "^(?=.*[1-9])[0-9]+(\\.[0-9]+)?$", message = "fiatAmount must be a positive decimal") String fiatAmount;
    String fiatCurrency;
    @Pattern(regexp = "^(?=.*[1-9])[0-9]+(\\.[0-9]+)?$", message = "cryptoAmount must be a positive decimal") String cryptoAmount;
    @NotBlank String token;
    @NotBlank String network;
    String walletAddress;
    String notifyUrl;
    String returnUrl;
    String customerReference;
    String preferredGateway;
}
