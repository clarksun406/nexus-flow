package com.nexusflow.application;

import com.nexusflow.application.dto.*;
import com.nexusflow.common.NexusFlowException;
import com.nexusflow.domain.channel.ChannelAdapter;
import com.nexusflow.domain.channel.ChannelRouter;
import com.nexusflow.domain.channel.ChannelUser;
import com.nexusflow.domain.channel.CurrencyRateCache;
import com.nexusflow.domain.channel.DepositAddress;
import com.nexusflow.domain.channel.ExchangeRate;
import com.nexusflow.domain.event.DomainEventPublisher;
import com.nexusflow.domain.event.ProcessedEventStore;
import com.nexusflow.domain.order.FlowRepository;
import com.nexusflow.domain.order.OrderRepository;
import com.nexusflow.domain.order.OrderStatus;
import com.nexusflow.domain.order.PaymentFlow;
import com.nexusflow.domain.order.PaymentOrder;
import com.nexusflow.domain.refund.RefundOrder;
import com.nexusflow.domain.refund.RefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentOrchestratorTest {

    private ChannelRouter channelRouter;
    private ChannelAdapter stubChannel;
    private OrderRepository orderRepository;
    private FlowRepository flowRepository;
    private RefundRepository refundRepository;
    private DomainEventPublisher eventPublisher;
    private WebhookService webhookService;
    private ProcessedEventStore processedEventStore;
    private CurrencyRateCache currencyRateCache;

    private PaymentOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        channelRouter = mock(ChannelRouter.class);
        stubChannel = mock(ChannelAdapter.class);
        orderRepository = mock(OrderRepository.class);
        flowRepository = mock(FlowRepository.class);
        refundRepository = mock(RefundRepository.class);
        eventPublisher = mock(DomainEventPublisher.class);
        webhookService = mock(WebhookService.class);
        processedEventStore = mock(ProcessedEventStore.class);
        currencyRateCache = mock(CurrencyRateCache.class);

        orchestrator = new PaymentOrchestrator(
                channelRouter, List.of(stubChannel), orderRepository, flowRepository,
                refundRepository, eventPublisher, webhookService, processedEventStore, currencyRateCache);
    }

    private PaymentOrder waitingOrder() {
        return PaymentOrder.builder()
                .paymentId("pay-1").merchantId("m-1").merchantOrderNo("ord-1")
                .amountFiat(new BigDecimal("100")).currencyFiat("USD")
                .amountCrypto(new BigDecimal("100")).currencyCrypto("USDT")
                .network("TRC20").exchangeRate(BigDecimal.ONE)
                .channelId("STUB").channelUserId("u-1")
                .expireTime(Instant.now().plusSeconds(600))
                .build();
    }

    private PaymentOrder waitingOrderWithRate(String amountFiat, String amountCrypto, String exchangeRate) {
        return PaymentOrder.builder()
                .paymentId("pay-1").merchantId("m-1").merchantOrderNo("ord-1")
                .amountFiat(new BigDecimal(amountFiat)).currencyFiat("USD")
                .amountCrypto(new BigDecimal(amountCrypto)).currencyCrypto("BTC")
                .network("BTC").exchangeRate(new BigDecimal(exchangeRate))
                .channelId("STUB").channelUserId("u-1")
                .expireTime(Instant.now().plusSeconds(600))
                .build();
    }

    // ── Create Order ──

    @Test
    void createOrderHappyPath() {
        when(orderRepository.existsByMerchantOrderNo("m-1", "ord-1")).thenReturn(false);
        when(channelRouter.route(any())).thenReturn(List.of(stubChannel));
        when(stubChannel.channelId()).thenReturn("STUB");
        when(stubChannel.openUser("m-1", "ord-1")).thenReturn(
                ChannelUser.builder().channelUserId("cu-1").channelId("STUB").newlyCreated(true).build());
        when(currencyRateCache.getExchangeRate(stubChannel, "USDT", "TRC20", "USD")).thenReturn(
                ExchangeRate.builder().token("USDT").network("TRC20")
                        .price(new BigDecimal("1.0002")).quoteCurrency("USD").timestamp(Instant.now()).build());

        CreateOrderRequest req = CreateOrderRequest.builder()
                .merchantId("m-1").merchantOrderNo("ord-1")
                .amountFiat("100").currencyFiat("USD").build();

        OrderResponse resp = orchestrator.createOrder(req);

        assertThat(resp.getPaymentId()).isNotBlank();
        assertThat(resp.getStatus()).isEqualTo("WAITING_PAYMENT");
        assertThat(resp.getAmountFiat()).isEqualTo("100");
        assertThat(resp.getChannelId()).isEqualTo("STUB");
        verify(orderRepository).save(any(PaymentOrder.class));
    }

    @Test
    void createOrderWithCryptoAmountUsesRequestedAssetAndDerivesFiatAmount() {
        when(orderRepository.existsByMerchantOrderNo("m-1", "ord-crypto")).thenReturn(false);
        when(channelRouter.route(any())).thenReturn(List.of(stubChannel));
        when(stubChannel.channelId()).thenReturn("STUB");
        when(stubChannel.openUser("m-1", "ord-crypto")).thenReturn(
                ChannelUser.builder().channelUserId("cu-1").channelId("STUB").newlyCreated(true).build());
        when(currencyRateCache.getExchangeRate(stubChannel, "USDC", "ERC20", "USD")).thenReturn(
                ExchangeRate.builder().token("USDC").network("ERC20")
                        .price(new BigDecimal("1.0000")).quoteCurrency("USD").timestamp(Instant.now()).build());

        CreateOrderRequest req = CreateOrderRequest.builder()
                .merchantId("m-1").merchantOrderNo("ord-crypto")
                .amountCrypto("25.5").currencyCrypto("usdc").network("erc20").build();

        OrderResponse resp = orchestrator.createOrder(req);

        ArgumentCaptor<ChannelRouter.RouteRequest> routeCaptor = ArgumentCaptor.forClass(ChannelRouter.RouteRequest.class);
        verify(channelRouter).route(routeCaptor.capture());
        assertThat(routeCaptor.getValue().getToken()).isEqualTo("USDC");
        assertThat(routeCaptor.getValue().getNetwork()).isEqualTo("ERC20");
        assertThat(routeCaptor.getValue().getCurrencyFiat()).isEqualTo("USD");

        assertThat(resp.getAmountCrypto()).isEqualTo("25.5");
        assertThat(new BigDecimal(resp.getAmountFiat())).isEqualByComparingTo("25.50");
        assertThat(resp.getCurrencyCrypto()).isEqualTo("USDC");
        assertThat(resp.getNetwork()).isEqualTo("ERC20");
    }

    @Test
    void createOrderRejectsDuplicate() {
        when(orderRepository.existsByMerchantOrderNo("m-1", "ord-1")).thenReturn(true);

        CreateOrderRequest req = CreateOrderRequest.builder()
                .merchantId("m-1").merchantOrderNo("ord-1")
                .amountFiat("100").currencyFiat("USD").build();

        assertThatThrownBy(() -> orchestrator.createOrder(req))
                .isInstanceOf(NexusFlowException.class);
    }

    @Test
    void createOrderRejectsMixedFiatAndCryptoAmountInputs() {
        when(orderRepository.existsByMerchantOrderNo("m-1", "ord-mixed")).thenReturn(false);

        CreateOrderRequest req = CreateOrderRequest.builder()
                .merchantId("m-1").merchantOrderNo("ord-mixed")
                .amountFiat("100").currencyFiat("USD")
                .amountCrypto("100").currencyCrypto("USDT").network("TRC20").build();

        assertThatThrownBy(() -> orchestrator.createOrder(req))
                .isInstanceOf(NexusFlowException.class)
                .hasMessageContaining("either amountFiat/currencyFiat or amountCrypto");

        verify(channelRouter, never()).route(any());
        verify(orderRepository, never()).save(any(PaymentOrder.class));
    }

    @Test
    void createOrderFailsWhenNoChannelAvailable() {
        when(orderRepository.existsByMerchantOrderNo("m-1", "ord-1")).thenReturn(false);
        when(channelRouter.route(any())).thenReturn(List.of());

        CreateOrderRequest req = CreateOrderRequest.builder()
                .merchantId("m-1").merchantOrderNo("ord-1")
                .amountFiat("100").currencyFiat("USD").build();

        assertThatThrownBy(() -> orchestrator.createOrder(req))
                .isInstanceOf(NexusFlowException.class);
    }

    // ── Submit Payment ──

    @Test
    void submitPaymentUsesAddressReturnedByChannelAdapter() {
        PaymentOrder order = waitingOrder();
        when(orderRepository.findByPaymentId("pay-1")).thenReturn(Optional.of(order));
        when(flowRepository.findActiveByPaymentId("pay-1")).thenReturn(Optional.empty());
        when(stubChannel.channelId()).thenReturn("STUB");
        when(stubChannel.createDepositAddress(any())).thenReturn(
                DepositAddress.builder()
                        .address("0xREAL_ADDRESS").memo("memo-1")
                        .channelOrderId("co-1").requiredConfirmations(5).build());

        CashierSubmitRequest req = CashierSubmitRequest.builder()
                .paymentId("pay-1").token("USDT").network("TRC20").channelId("STUB").build();

        CashierSubmitResponse resp = orchestrator.submitPayment(req);

        verify(stubChannel).createDepositAddress(any());
        assertThat(resp.getPayAddress()).isEqualTo("0xREAL_ADDRESS");
        assertThat(resp.getRequiredConfirmations()).isEqualTo(5);
        assertThat(order.getPayAddress()).isEqualTo("0xREAL_ADDRESS");

        ArgumentCaptor<PaymentFlow> flowCaptor = ArgumentCaptor.forClass(PaymentFlow.class);
        verify(flowRepository).save(flowCaptor.capture());
        assertThat(flowCaptor.getValue().getPayAddress()).isEqualTo("0xREAL_ADDRESS");
    }

    @Test
    void submitPaymentFailsWhenNoAdapterMatchesChannelId() {
        PaymentOrder order = waitingOrder();
        when(orderRepository.findByPaymentId("pay-1")).thenReturn(Optional.of(order));
        when(flowRepository.findActiveByPaymentId("pay-1")).thenReturn(Optional.empty());
        when(stubChannel.channelId()).thenReturn("STUB");

        CashierSubmitRequest req = CashierSubmitRequest.builder()
                .paymentId("pay-1").token("USDT").network("TRC20").channelId("UNKNOWN").build();

        assertThatThrownBy(() -> orchestrator.submitPayment(req))
                .isInstanceOf(NexusFlowException.class);
    }

    // ── Handle Channel Callback ──

    @Test
    void duplicateCallbackIsProcessedOnlyOnce() {
        PaymentOrder order = waitingOrder();
        when(orderRepository.findByChannelOrderId("STUB", "co-1")).thenReturn(Optional.of(order));
        when(flowRepository.findActiveByPaymentId("pay-1")).thenReturn(Optional.empty());
        when(processedEventStore.markProcessed("evt-1")).thenReturn(true, false);

        orchestrator.handlePaymentCallback("STUB", "co-1", "tx-1", "100", null, "evt-1");
        orchestrator.handlePaymentCallback("STUB", "co-1", "tx-1", "100", null, "evt-1");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void callbackForUnknownOrderDoesNotConsumeEventId() {
        when(orderRepository.findByChannelOrderId(eq("STUB"), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                orchestrator.handlePaymentCallback("STUB", "missing", "tx-1", "100", null, "evt-1"))
                .isInstanceOf(NexusFlowException.class);

        verify(processedEventStore, never()).markProcessed(anyString());
    }

    @Test
    void partialPaymentMarksPartiallyPaid() {
        PaymentOrder order = waitingOrder();
        when(orderRepository.findByChannelOrderId("STUB", "co-1")).thenReturn(Optional.of(order));
        when(flowRepository.findActiveByPaymentId("pay-1")).thenReturn(Optional.empty());
        when(processedEventStore.markProcessed("evt-2")).thenReturn(true);

        // Pay less than expected (50 out of 100)
        orchestrator.handlePaymentCallback("STUB", "co-1", "tx-1", "50", "50", "evt-2");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIALLY_PAID);
        assertThat(order.getPaidAmountCrypto()).isEqualByComparingTo("50");
    }

    @Test
    void callbackWithoutPaidFiatDerivesFiatByMultiplyingExchangeRate() {
        PaymentOrder order = waitingOrderWithRate("20000", "1", "20000");
        when(orderRepository.findByChannelOrderId("STUB", "co-1")).thenReturn(Optional.of(order));
        when(flowRepository.findActiveByPaymentId("pay-1")).thenReturn(Optional.empty());
        when(processedEventStore.markProcessed("evt-3")).thenReturn(true);

        orchestrator.handlePaymentCallback("STUB", "co-1", "tx-1", "1", null, "evt-3");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getPaidAmountFiat()).isEqualByComparingTo("20000.00");
    }

    // ── Refund ──

    @Test
    void refundHappyPath() {
        PaymentOrder order = waitingOrder();
        // Mark order as CONFIRMED first
        order.markConfirmed("tx-1", new BigDecimal("100"), new BigDecimal("100"));
        order.collectEvents(); // clear events

        when(orderRepository.findByMerchantOrderNo("m-1", "ord-1")).thenReturn(Optional.of(order));

        RefundRequestDto req = RefundRequestDto.builder()
                .merchantId("m-1").merchantOrderNo("ord-1")
                .refundOrderNo("ref-1").refundAmountFiat("50")
                .notifyUrl("https://example.com/callback").build();

        RefundResponseDto resp = orchestrator.refund(req);

        assertThat(resp.getRefundOrderNo()).isEqualTo("ref-1");
        assertThat(resp.getStatus()).isEqualTo("PROCESSING");
        assertThat(resp.getRefundAmountFiat()).isEqualTo("50");
        verify(refundRepository).save(any(RefundOrder.class));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_PROCESSING);
    }

    @Test
    void refundRejectsNonConfirmedOrder() {
        PaymentOrder order = waitingOrder(); // WAITING_PAYMENT
        when(orderRepository.findByMerchantOrderNo("m-1", "ord-1")).thenReturn(Optional.of(order));

        RefundRequestDto req = RefundRequestDto.builder()
                .merchantId("m-1").merchantOrderNo("ord-1")
                .refundOrderNo("ref-1").refundAmountFiat("50").build();

        assertThatThrownBy(() -> orchestrator.refund(req))
                .isInstanceOf(NexusFlowException.class);
    }

    @Test
    void refundRejectsExceedingPaidAmount() {
        PaymentOrder order = waitingOrder();
        order.markConfirmed("tx-1", new BigDecimal("100"), new BigDecimal("100"));
        order.collectEvents();
        when(orderRepository.findByMerchantOrderNo("m-1", "ord-1")).thenReturn(Optional.of(order));

        RefundRequestDto req = RefundRequestDto.builder()
                .merchantId("m-1").merchantOrderNo("ord-1")
                .refundOrderNo("ref-1").refundAmountFiat("200").build();

        assertThatThrownBy(() -> orchestrator.refund(req))
                .isInstanceOf(NexusFlowException.class);
    }

    // ── Handle Refund Callback ──

    @Test
    void refundCallbackSuccessMarksRefunded() {
        // Set up order in REFUND_PROCESSING
        PaymentOrder order = waitingOrder();
        order.markConfirmed("tx-1", new BigDecimal("100"), new BigDecimal("100"));
        order.markRefundProcessing();
        order.collectEvents();

        RefundOrder refund = RefundOrder.builder()
                .refundOrderNo("ref-1").paymentId("pay-1")
                .refundAmountFiat(new BigDecimal("50"))
                .refundAmountCrypto(new BigDecimal("50"))
                .exchangeRate(BigDecimal.ONE)
                .token("USDT").network("TRC20").toAddress("addr").build();

        when(refundRepository.findByChannelRefundId("ch-ref-1")).thenReturn(Optional.of(refund));
        when(orderRepository.findByPaymentId("pay-1")).thenReturn(Optional.of(order));

        orchestrator.handleRefundCallback("ch-ref-1", "SUCCESS", "tx-ref-1");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        verify(refundRepository).save(refund);
        verify(orderRepository).save(order);
    }

    @Test
    void refundCallbackFailureMarksRefundFailed() {
        PaymentOrder order = waitingOrder();
        order.markConfirmed("tx-1", new BigDecimal("100"), new BigDecimal("100"));
        order.markRefundProcessing();
        order.collectEvents();

        RefundOrder refund = RefundOrder.builder()
                .refundOrderNo("ref-1").paymentId("pay-1")
                .refundAmountFiat(new BigDecimal("50"))
                .refundAmountCrypto(new BigDecimal("50"))
                .exchangeRate(BigDecimal.ONE)
                .token("USDT").network("TRC20").toAddress("addr").build();

        when(refundRepository.findByChannelRefundId("ch-ref-1")).thenReturn(Optional.of(refund));
        when(orderRepository.findByPaymentId("pay-1")).thenReturn(Optional.of(order));

        orchestrator.handleRefundCallback("ch-ref-1", "FAILED", null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_FAILED);
    }

    @Test
    void refundCallbackUnknownStatusTreatsAsFailure() {
        PaymentOrder order = waitingOrder();
        order.markConfirmed("tx-1", new BigDecimal("100"), new BigDecimal("100"));
        order.markRefundProcessing();
        order.collectEvents();

        RefundOrder refund = RefundOrder.builder()
                .refundOrderNo("ref-1").paymentId("pay-1")
                .refundAmountFiat(new BigDecimal("50"))
                .refundAmountCrypto(new BigDecimal("50"))
                .exchangeRate(BigDecimal.ONE)
                .token("USDT").network("TRC20").toAddress("addr").build();

        when(refundRepository.findByChannelRefundId("ch-ref-1")).thenReturn(Optional.of(refund));
        when(orderRepository.findByPaymentId("pay-1")).thenReturn(Optional.of(order));

        orchestrator.handleRefundCallback("ch-ref-1", "UNKNOWN_STATUS", null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_FAILED);
    }
}
