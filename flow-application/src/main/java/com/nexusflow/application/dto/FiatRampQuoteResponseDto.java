package com.nexusflow.application.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FiatRampQuoteResponseDto {
    String quoteId;
    String providerId;
    String direction;
    String fiatAmount;
    String fiatCurrency;
    String cryptoAmount;
    String token;
    String network;
    String exchangeRate;
    String feeAmountFiat;
    Long expiresAt;
}
