package com.nexusflow.domain.fiat;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class FiatRampQuote {
    String quoteId;
    String providerId;
    FiatRampDirection direction;
    BigDecimal fiatAmount;
    String fiatCurrency;
    BigDecimal cryptoAmount;
    String token;
    String network;
    BigDecimal exchangeRate;
    BigDecimal feeAmountFiat;
    Instant expiresAt;
}
