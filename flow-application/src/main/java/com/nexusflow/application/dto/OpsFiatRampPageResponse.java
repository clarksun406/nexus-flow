package com.nexusflow.application.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Paged fiat ramp order list for the ops console.
 */
@Value
@Builder
public class OpsFiatRampPageResponse {
    List<FiatRampItem> items;
    int page;
    int size;
    long total;

    @Value
    @Builder
    public static class FiatRampItem {
        String rampOrderId;
        String merchantId;
        String merchantOrderNo;
        String paymentId;
        String direction;
        String providerId;
        String providerOrderId;
        String status;
        String fiatAmount;
        String fiatCurrency;
        String cryptoAmount;
        String token;
        String network;
        String exchangeRate;
        Long createTime;
        Long updateTime;
    }
}
