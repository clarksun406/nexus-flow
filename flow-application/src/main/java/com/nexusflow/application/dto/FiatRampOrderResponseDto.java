package com.nexusflow.application.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FiatRampOrderResponseDto {
    String rampOrderId;
    String merchantId;
    String merchantOrderNo;
    String paymentId;
    String direction;
    String providerId;
    String providerOrderId;
    String quoteId;
    String fiatAmount;
    String fiatCurrency;
    String cryptoAmount;
    String token;
    String network;
    String exchangeRate;
    String feeAmountFiat;
    String walletAddress;
    String checkoutUrl;
    String fiatTransferId;
    String cryptoTxHash;
    String status;
    String failureReason;
    Long expireTime;
    Long completeTime;
    Long createTime;
}
