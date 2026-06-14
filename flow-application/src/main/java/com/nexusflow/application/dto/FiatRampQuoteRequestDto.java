package com.nexusflow.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class FiatRampQuoteRequestDto {
    @NotBlank String merchantId;
    @NotBlank String direction;
    @Pattern(regexp = "^(?=.*[1-9])[0-9]+(\\.[0-9]+)?$", message = "fiatAmount must be a positive decimal") String fiatAmount;
    String fiatCurrency;
    @Pattern(regexp = "^(?=.*[1-9])[0-9]+(\\.[0-9]+)?$", message = "cryptoAmount must be a positive decimal") String cryptoAmount;
    @NotBlank String token;
    @NotBlank String network;
    String walletAddress;
    String country;
    String paymentMethod;
    String preferredGateway;
}
