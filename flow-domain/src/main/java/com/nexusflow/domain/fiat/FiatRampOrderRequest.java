package com.nexusflow.domain.fiat;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class FiatRampOrderRequest {
    String merchantId;
    String merchantOrderNo;
    String paymentId;
    FiatRampDirection direction;
    String quoteId;
    BigDecimal fiatAmount;
    String fiatCurrency;
    BigDecimal cryptoAmount;
    String token;
    String network;
    String walletAddress;
    String notifyUrl;
    String returnUrl;
    String customerReference;
}
