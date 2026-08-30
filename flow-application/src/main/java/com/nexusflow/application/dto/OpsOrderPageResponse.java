package com.nexusflow.application.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Paged order list for the ops console.
 */
@Value
@Builder
public class OpsOrderPageResponse {
    List<OrderItem> items;
    int page;
    int size;
    long total;

    @Value
    @Builder
    public static class OrderItem {
        String paymentId;
        String merchantId;
        String merchantOrderNo;
        String status;
        String amountFiat;
        String currencyFiat;
        String amountCrypto;
        String currencyCrypto;
        String network;
        String channelId;
        String paidAmountFiat;
        String paidAmountCrypto;
        String txHash;
        Long expireTime;
        Long payTime;
        Long createTime;
        Long updateTime;
    }
}
