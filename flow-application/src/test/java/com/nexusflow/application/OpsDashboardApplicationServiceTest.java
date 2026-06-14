package com.nexusflow.application;

import com.nexusflow.domain.blockchain.OrphanTransaction;
import com.nexusflow.domain.blockchain.OrphanTransactionRepository;
import com.nexusflow.domain.blockchain.OrphanTransactionStatus;
import com.nexusflow.domain.channel.ChannelAdapter;
import com.nexusflow.domain.channel.CurrencyConfig;
import com.nexusflow.domain.order.OrderRepository;
import com.nexusflow.domain.order.OrderStatus;
import com.nexusflow.domain.order.PaymentOrder;
import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.payment.PaymentRepository;
import com.nexusflow.domain.payment.PaymentStatus;
import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpsDashboardApplicationServiceTest {

    private ChannelAdapter healthyChannel;
    private ChannelAdapter downChannel;
    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private OrphanTransactionRepository orphanTransactionRepository;
    private OpsDashboardApplicationService service;

    @BeforeEach
    void setUp() {
        healthyChannel = mock(ChannelAdapter.class);
        downChannel = mock(ChannelAdapter.class);
        orderRepository = mock(OrderRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        orphanTransactionRepository = mock(OrphanTransactionRepository.class);
        service = new OpsDashboardApplicationService(
                List.of(healthyChannel, downChannel),
                orderRepository,
                paymentRepository,
                orphanTransactionRepository);
    }

    @Test
    void dashboardAggregatesChannelsOrdersPaymentsAndRiskAlerts() {
        when(healthyChannel.channelId()).thenReturn("STUB");
        when(healthyChannel.displayName()).thenReturn("Stub");
        when(healthyChannel.isHealthy()).thenReturn(true);
        when(healthyChannel.getSupportedCurrencies()).thenReturn(List.of(CurrencyConfig.builder()
                .token("USDT").network("TRC20").enabled(true).build()));

        when(downChannel.channelId()).thenReturn("BITMART");
        when(downChannel.displayName()).thenReturn("BitMart");
        when(downChannel.isHealthy()).thenReturn(false);
        when(downChannel.getSupportedCurrencies()).thenReturn(List.of());

        PaymentOrder waiting = order("pay-1", OrderStatus.WAITING_PAYMENT);
        PaymentOrder partial = order("pay-2", OrderStatus.PARTIALLY_PAID);
        when(orderRepository.findByStatusIn(anyCollection())).thenReturn(List.of(waiting, partial));

        CryptoPayment pending = payment("crypto-1", PaymentStatus.PENDING);
        CryptoPayment detected = payment("crypto-2", PaymentStatus.DETECTED);
        when(paymentRepository.findByStatusIn(anyCollection())).thenReturn(List.of(pending, detected));

        when(orphanTransactionRepository.findByStatus(OrphanTransactionStatus.UNMATCHED)).thenReturn(List.of(orphan()));
        when(orphanTransactionRepository.findByStatus(OrphanTransactionStatus.RESOLVED)).thenReturn(List.of());
        when(orphanTransactionRepository.findByStatus(OrphanTransactionStatus.IGNORED)).thenReturn(List.of());

        var dashboard = service.getDashboard();

        assertThat(dashboard.getChannels()).hasSize(2);
        assertThat(dashboard.getChannels()).anySatisfy(channel -> {
            assertThat(channel.getChannelId()).isEqualTo("BITMART");
            assertThat(channel.getStatus()).isEqualTo("DOWN");
        });
        assertThat(dashboard.getOrderStatusCounts()).containsEntry("WAITING_PAYMENT", 1L)
                .containsEntry("PARTIALLY_PAID", 1L);
        assertThat(dashboard.getPaymentStatusCounts()).containsEntry("PENDING", 1L)
                .containsEntry("DETECTED", 1L);
        assertThat(dashboard.getOrphanStatusCounts()).containsEntry("UNMATCHED", 1L);
        assertThat(dashboard.getReconciliation().getUnconfirmedExecutionPayments()).isEqualTo(1);
        assertThat(dashboard.getAlerts()).extracting("code")
                .contains("CHANNEL_DOWN", "ORPHAN_TX_UNMATCHED", "UNCONFIRMED_PAYMENTS", "PARTIAL_ORDERS");
    }

    private PaymentOrder order(String paymentId, OrderStatus status) {
        return PaymentOrder.reconstitute()
                .paymentId(paymentId)
                .merchantId("merchant-1")
                .merchantOrderNo("order-" + paymentId)
                .amountFiat(new BigDecimal("100"))
                .currencyFiat("USD")
                .amountCrypto(new BigDecimal("100"))
                .currencyCrypto("USDT")
                .network("TRC20")
                .exchangeRate(BigDecimal.ONE)
                .channelId("STUB")
                .channelUserId("user-1")
                .status(status)
                .paidAmountCrypto(BigDecimal.ZERO)
                .paidAmountFiat(BigDecimal.ZERO)
                .expireTime(Instant.now().plusSeconds(600))
                .createTime(Instant.now())
                .updateTime(Instant.now())
                .build();
    }

    private CryptoPayment payment(String id, PaymentStatus status) {
        return CryptoPayment.reconstitute()
                .id(id)
                .orderId("order-" + id)
                .expected(Money.of("USDT", BigDecimal.TEN))
                .status(status)
                .receivingAddress("addr")
                .confirmations(0)
                .requiredConfirmations(12)
                .createdAt(Instant.now())
                .build();
    }

    private OrphanTransaction orphan() {
        return OrphanTransaction.builder()
                .id("orphan-1")
                .chain(Chain.ETH)
                .txHash("tx-1")
                .toAddress("0xabc")
                .amount("10")
                .currency("USDT")
                .blockNumber(100L)
                .build();
    }
}
