package com.nexusflow.application.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class OpsDashboardResponse {
    Map<String, Long> orderStatusCounts;
    Map<String, Long> paymentStatusCounts;
    Map<String, Long> orphanStatusCounts;
    List<ChannelHealth> channels;
    List<OrderSummary> recentOrders;
    ReconciliationSummary reconciliation;
    List<RiskAlert> alerts;
    long generatedAt;

    @Value
    @Builder
    public static class ChannelHealth {
        String channelId;
        String displayName;
        String status;
        int supportedCurrencyCount;
        String message;
    }

    @Value
    @Builder
    public static class OrderSummary {
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
        String txHash;
        long createTime;
        long updateTime;
    }

    @Value
    @Builder
    public static class ReconciliationSummary {
        long pendingExecutionPayments;
        long unconfirmedExecutionPayments;
        long unmatchedOrphanTransactions;
        long partiallyPaidOrders;
        long refundProcessingOrders;
    }

    @Value
    @Builder
    public static class RiskAlert {
        String severity;
        String code;
        String message;
        long count;
    }
}
