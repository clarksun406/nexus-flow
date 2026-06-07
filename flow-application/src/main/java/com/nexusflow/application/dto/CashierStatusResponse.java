package com.nexusflow.application.dto;

import lombok.Builder;
import lombok.Value;

@Value @Builder
public class CashierStatusResponse {
    String paymentId;
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
    int transactionCount;
    int requiredConfirmations;
    Long expireTime;
    Long payTime;
    Long confirmTime;
}