package com.nexusflow.application.dto;

import lombok.Builder;
import lombok.Value;

@Value @Builder
public class CashierSubmitResponse {
    String flowNo;
    String paymentId;
    String token;
    String network;
    String cryptoAmount;
    String fiatAmount;
    String fiatCurrency;
    String exchangeRate;
    String channelId;
    String payAddress;
    String memo;
    int requiredConfirmations;
    Long expireTime;
    String status;
}