package com.nexusflow.application.dto;

import lombok.Builder;
import lombok.Value;

@Value @Builder
public class OrderResponse {
    String merchantId;
    String paymentId;
    String merchantOrderNo;
    String status;
    String amountFiat;
    String currencyFiat;
    String amountCrypto;
    String currencyCrypto;
    String network;
    String exchangeRate;
    String channelId;
    String channelName;
    String payAddress;
    String memo;
    String paidAmountCrypto;
    String paidAmountFiat;
    String pendingAmount;
    String txHash;
    String payUrl;
    Long expireTime;
    Long payTime;
    Long confirmTime;
    Long createTime;
}