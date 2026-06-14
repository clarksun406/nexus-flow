package com.nexusflow.domain.fiat;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class FiatRampQuoteRequest {
    String merchantId;
    FiatRampDirection direction;
    BigDecimal fiatAmount;
    String fiatCurrency;
    BigDecimal cryptoAmount;
    String token;
    String network;
    String walletAddress;
    String country;
    String paymentMethod;
}
