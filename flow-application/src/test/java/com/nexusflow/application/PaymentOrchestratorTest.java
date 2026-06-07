package com.nexusflow.application;

import com.nexusflow.application.dto.CashierSubmitRequest;
import com.nexusflow.application.dto.CashierSubmitResponse;
import com.nexusflow.common.NexusFlowException;
import com.nexusflow.domain.channel.ChannelAdapter;
import com.nexusflow.domain.channel.ChannelRouter;
import com.nexusflow.domain.channel.DepositAddress;
import com.nexusflow.domain.event.DomainEventPublisher;
import com.nexusflow.domain.event.ProcessedEventStore;
import com.nexusflow.domain.order.FlowRepository;
import com.nexusflow.domain.order.OrderRepository;
import com.nexusflow.domain.order.OrderStatus;
import com.nexusflow.domain.order.PaymentFlow;
import com.nexusflow.domain.order.PaymentOrder;
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

        orchestrator = new PaymentOrchestrator(
                channelRouter, List.of(stubChannel), orderRepository, flowRepository,
                refundRepository, eventPublisher, webhookService, processedEventStore);
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

    // ── Bug 2: deposit address must come from the channel adapter, not a hardcoded value ──

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

        // the real channel was consulted
        verify(stubChannel).createDepositAddress(any());
        // response, order, and flow all carry the channel-provided address
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

    // ── Bug 3: duplicate channel callbacks (same eventId) must be processed once ──

    @Test
    void duplicateCallbackIsProcessedOnlyOnce() {
        PaymentOrder order = waitingOrder();
        when(orderRepository.findByChannelOrderId("STUB", "co-1")).thenReturn(Optional.of(order));
        when(flowRepository.findActiveByPaymentId("pay-1")).thenReturn(Optional.empty());
        // first delivery is new, second is a duplicate
        when(processedEventStore.markProcessed("evt-1")).thenReturn(true, false);

        orchestrator.handlePaymentCallback("STUB", "co-1", "tx-1", "100", null, "evt-1");
        orchestrator.handlePaymentCallback("STUB", "co-1", "tx-1", "100", null, "evt-1");

        // order confirmed and persisted exactly once despite two deliveries
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void callbackForUnknownOrderDoesNotConsumeEventId() {
        when(orderRepository.findByChannelOrderId(eq("STUB"), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                orchestrator.handlePaymentCallback("STUB", "missing", "tx-1", "100", null, "evt-1"))
                .isInstanceOf(NexusFlowException.class);

        // dedup must not run before the order is resolved
        verify(processedEventStore, never()).markProcessed(anyString());
    }
}
