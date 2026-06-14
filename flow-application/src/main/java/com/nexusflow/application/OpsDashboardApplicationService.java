package com.nexusflow.application;

import com.nexusflow.application.dto.OpsDashboardResponse;
import com.nexusflow.domain.blockchain.OrphanTransaction;
import com.nexusflow.domain.blockchain.OrphanTransactionRepository;
import com.nexusflow.domain.blockchain.OrphanTransactionStatus;
import com.nexusflow.domain.channel.ChannelAdapter;
import com.nexusflow.domain.order.OrderRepository;
import com.nexusflow.domain.order.OrderStatus;
import com.nexusflow.domain.order.PaymentOrder;
import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.payment.PaymentRepository;
import com.nexusflow.domain.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpsDashboardApplicationService {

    private final List<ChannelAdapter> channelAdapters;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrphanTransactionRepository orphanTransactionRepository;

    @Transactional(readOnly = true)
    public OpsDashboardResponse getDashboard() {
        List<PaymentOrder> orders = orderRepository.findByStatusIn(Arrays.asList(OrderStatus.values()));
        List<CryptoPayment> payments = paymentRepository.findByStatusIn(Arrays.asList(PaymentStatus.values()));
        Map<OrphanTransactionStatus, List<OrphanTransaction>> orphanByStatus = orphanByStatus();

        Map<String, Long> orderCounts = countOrders(orders);
        Map<String, Long> paymentCounts = countPayments(payments);
        Map<String, Long> orphanCounts = countOrphans(orphanByStatus);
        List<OpsDashboardResponse.ChannelHealth> channels = channelAdapters.stream()
                .map(this::toChannelHealth)
                .sorted(Comparator.comparing(OpsDashboardResponse.ChannelHealth::getChannelId))
                .toList();

        OpsDashboardResponse.ReconciliationSummary reconciliation = buildReconciliation(
                orderCounts, paymentCounts, orphanCounts);

        return OpsDashboardResponse.builder()
                .orderStatusCounts(orderCounts)
                .paymentStatusCounts(paymentCounts)
                .orphanStatusCounts(orphanCounts)
                .channels(channels)
                .recentOrders(recentOrders(orders))
                .reconciliation(reconciliation)
                .alerts(buildAlerts(channels, reconciliation))
                .generatedAt(Instant.now().toEpochMilli())
                .build();
    }

    private Map<OrphanTransactionStatus, List<OrphanTransaction>> orphanByStatus() {
        Map<OrphanTransactionStatus, List<OrphanTransaction>> result = new EnumMap<>(OrphanTransactionStatus.class);
        for (OrphanTransactionStatus status : OrphanTransactionStatus.values()) {
            List<OrphanTransaction> transactions = orphanTransactionRepository.findByStatus(status);
            result.put(status, transactions != null ? transactions : List.of());
        }
        return result;
    }

    private Map<String, Long> countOrders(List<PaymentOrder> orders) {
        Map<OrderStatus, Long> counts = initializedOrderCounts();
        for (PaymentOrder order : orders) {
            counts.computeIfPresent(order.getStatus(), (status, count) -> count + 1);
        }
        return stringifyKeys(counts);
    }

    private Map<String, Long> countPayments(List<CryptoPayment> payments) {
        Map<PaymentStatus, Long> counts = initializedPaymentCounts();
        for (CryptoPayment payment : payments) {
            counts.computeIfPresent(payment.getStatus(), (status, count) -> count + 1);
        }
        return stringifyKeys(counts);
    }

    private Map<String, Long> countOrphans(Map<OrphanTransactionStatus, List<OrphanTransaction>> orphanByStatus) {
        Map<OrphanTransactionStatus, Long> counts = new EnumMap<>(OrphanTransactionStatus.class);
        for (OrphanTransactionStatus status : OrphanTransactionStatus.values()) {
            counts.put(status, (long) orphanByStatus.getOrDefault(status, List.of()).size());
        }
        return stringifyKeys(counts);
    }

    private Map<OrderStatus, Long> initializedOrderCounts() {
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            counts.put(status, 0L);
        }
        return counts;
    }

    private Map<PaymentStatus, Long> initializedPaymentCounts() {
        Map<PaymentStatus, Long> counts = new EnumMap<>(PaymentStatus.class);
        for (PaymentStatus status : PaymentStatus.values()) {
            counts.put(status, 0L);
        }
        return counts;
    }

    private <E extends Enum<E>> Map<String, Long> stringifyKeys(Map<E, Long> source) {
        Map<String, Long> result = new java.util.LinkedHashMap<>();
        source.forEach((status, count) -> result.put(status.name(), count));
        return result;
    }

    private OpsDashboardResponse.ChannelHealth toChannelHealth(ChannelAdapter adapter) {
        try {
            boolean healthy = adapter.isHealthy();
            int currencyCount = adapter.getSupportedCurrencies() != null ? adapter.getSupportedCurrencies().size() : 0;
            return OpsDashboardResponse.ChannelHealth.builder()
                    .channelId(adapter.channelId())
                    .displayName(adapter.displayName())
                    .status(healthy ? "UP" : "DOWN")
                    .supportedCurrencyCount(currencyCount)
                    .message(healthy ? "healthy" : "health check returned false")
                    .build();
        } catch (Exception e) {
            return OpsDashboardResponse.ChannelHealth.builder()
                    .channelId(adapter.channelId())
                    .displayName(adapter.displayName())
                    .status("DOWN")
                    .supportedCurrencyCount(0)
                    .message(e.getMessage())
                    .build();
        }
    }

    private List<OpsDashboardResponse.OrderSummary> recentOrders(List<PaymentOrder> orders) {
        return orders.stream()
                .sorted(Comparator.comparing(PaymentOrder::getUpdateTime,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(20)
                .map(this::toOrderSummary)
                .toList();
    }

    private OpsDashboardResponse.OrderSummary toOrderSummary(PaymentOrder order) {
        return OpsDashboardResponse.OrderSummary.builder()
                .paymentId(order.getPaymentId())
                .merchantId(order.getMerchantId())
                .merchantOrderNo(order.getMerchantOrderNo())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .amountFiat(order.getAmountFiat() != null ? order.getAmountFiat().toPlainString() : null)
                .currencyFiat(order.getCurrencyFiat())
                .amountCrypto(order.getAmountCrypto() != null ? order.getAmountCrypto().toPlainString() : null)
                .currencyCrypto(order.getCurrencyCrypto())
                .network(order.getNetwork())
                .channelId(order.getChannelId())
                .txHash(order.getTxHash())
                .createTime(toEpochMillis(order.getCreateTime()))
                .updateTime(toEpochMillis(order.getUpdateTime()))
                .build();
    }

    private OpsDashboardResponse.ReconciliationSummary buildReconciliation(
            Map<String, Long> orderCounts,
            Map<String, Long> paymentCounts,
            Map<String, Long> orphanCounts) {
        return OpsDashboardResponse.ReconciliationSummary.builder()
                .pendingExecutionPayments(value(paymentCounts, PaymentStatus.PENDING.name()))
                .unconfirmedExecutionPayments(value(paymentCounts, PaymentStatus.DETECTED.name())
                        + value(paymentCounts, PaymentStatus.CONFIRMING.name()))
                .unmatchedOrphanTransactions(value(orphanCounts, OrphanTransactionStatus.UNMATCHED.name()))
                .partiallyPaidOrders(value(orderCounts, OrderStatus.PARTIALLY_PAID.name()))
                .refundProcessingOrders(value(orderCounts, OrderStatus.REFUND_PROCESSING.name()))
                .build();
    }

    private List<OpsDashboardResponse.RiskAlert> buildAlerts(
            List<OpsDashboardResponse.ChannelHealth> channels,
            OpsDashboardResponse.ReconciliationSummary reconciliation) {
        List<OpsDashboardResponse.RiskAlert> alerts = new ArrayList<>();
        long downChannels = channels.stream().filter(c -> !"UP".equals(c.getStatus())).count();
        if (downChannels > 0) {
            alerts.add(alert("HIGH", "CHANNEL_DOWN", "One or more channel adapters are unhealthy", downChannels));
        }
        if (reconciliation.getUnmatchedOrphanTransactions() > 0) {
            alerts.add(alert("HIGH", "ORPHAN_TX_UNMATCHED", "Unmatched on-chain transactions need review",
                    reconciliation.getUnmatchedOrphanTransactions()));
        }
        if (reconciliation.getUnconfirmedExecutionPayments() > 0) {
            alerts.add(alert("MEDIUM", "UNCONFIRMED_PAYMENTS", "Execution payments are waiting for confirmations",
                    reconciliation.getUnconfirmedExecutionPayments()));
        }
        if (reconciliation.getPartiallyPaidOrders() > 0) {
            alerts.add(alert("MEDIUM", "PARTIAL_ORDERS", "Partially paid merchant orders need follow-up",
                    reconciliation.getPartiallyPaidOrders()));
        }
        if (alerts.isEmpty()) {
            alerts.add(alert("INFO", "NO_ACTIVE_ALERTS", "No active dashboard alerts", 0));
        }
        return alerts;
    }

    private OpsDashboardResponse.RiskAlert alert(String severity, String code, String message, long count) {
        return OpsDashboardResponse.RiskAlert.builder()
                .severity(severity)
                .code(code)
                .message(message)
                .count(count)
                .build();
    }

    private long value(Map<String, Long> map, String key) {
        return map.getOrDefault(key, 0L);
    }

    private long toEpochMillis(Instant instant) {
        return instant != null ? instant.toEpochMilli() : 0L;
    }
}
