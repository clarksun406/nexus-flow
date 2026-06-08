package com.nexusflow.application;

import com.nexusflow.application.dto.*;
import com.nexusflow.common.ErrorCode;
import com.nexusflow.common.NexusFlowException;
import com.nexusflow.common.PaymentNotFoundException;
import com.nexusflow.domain.channel.ChannelAdapter;
import com.nexusflow.domain.channel.ChannelRouter;
import com.nexusflow.domain.channel.ChannelUser;
import com.nexusflow.domain.channel.DepositAddress;
import com.nexusflow.domain.event.DomainEventPublisher;
import com.nexusflow.domain.event.ProcessedEventStore;
import com.nexusflow.domain.order.*;
import com.nexusflow.domain.refund.RefundOrder;
import com.nexusflow.domain.refund.RefundRepository;
import com.nexusflow.domain.refund.RefundStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Core payment orchestrator — manages the full lifecycle of a merchant payment order.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrchestrator {

    private final ChannelRouter channelRouter;
    private final List<ChannelAdapter> channelAdapters;
    private final OrderRepository orderRepository;
    private final FlowRepository flowRepository;
    private final RefundRepository refundRepository;
    private final DomainEventPublisher eventPublisher;
    private final WebhookService webhookService;
    private final ProcessedEventStore processedEventStore;

    // ── Create Order ──

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest req) {
        if (orderRepository.existsByMerchantOrderNo(req.getMerchantId(), req.getMerchantOrderNo())) {
            throw new NexusFlowException(ErrorCode.PAYMENT_ALREADY_EXISTS,
                    "Duplicate order: " + req.getMerchantOrderNo());
        }

        // Route to best channel
        var routeReq = ChannelRouter.RouteRequest.builder()
                .merchantId(req.getMerchantId())
                .currencyFiat(req.getCurrencyFiat())
                .preferredChannelId(req.getPreferredChannel()).build();
        List<ChannelAdapter> channels = channelRouter.route(routeReq);
        if (channels.isEmpty()) throw new NexusFlowException(ErrorCode.NO_AVAILABLE_CHANNEL, "No channel available");

        ChannelAdapter channel = channels.get(0);
        log.info("Routing order {} to channel {}", req.getMerchantOrderNo(), channel.channelId());

        // Ensure buyer account on channel
        ChannelUser channelUser = channel.openUser(req.getMerchantId(), req.getMerchantOrderNo());

        // Get exchange rate
        var rate = channel.getExchangeRate("USDT", "TRC20", req.getCurrencyFiat());
        BigDecimal amountCrypto = new BigDecimal(req.getAmountFiat()).divide(rate.getPrice(), 6, RoundingMode.HALF_UP);

        // Create order
        Instant expireTime = Instant.now().plusSeconds(30 * 60); // 30 min default
        PaymentOrder order = PaymentOrder.builder()
                .paymentId(UUID.randomUUID().toString())
                .merchantId(req.getMerchantId())
                .merchantOrderNo(req.getMerchantOrderNo())
                .amountFiat(new BigDecimal(req.getAmountFiat()))
                .currencyFiat(req.getCurrencyFiat())
                .amountCrypto(amountCrypto)
                .currencyCrypto(rate.getToken())
                .network(rate.getNetwork())
                .exchangeRate(rate.getPrice())
                .channelId(channel.channelId())
                .channelUserId(channelUser.getChannelUserId())
                .notifyUrl(req.getNotifyUrl())
                .returnUrl(req.getReturnUrl())
                .extendData(req.getExtend())
                .expireTime(expireTime)
                .build();

        orderRepository.save(order);
        order.collectEvents().forEach(eventPublisher::publish);

        String payUrl = "https://cashier.nexusflow.com/checkout?payment_id=" + order.getPaymentId();
        log.info("Order created: paymentId={}, channel={}", order.getPaymentId(), channel.channelId());

        return toResponse(order, channel.displayName(), payUrl);
    }

    // ── Cashier Submit ──

    @Transactional
    public CashierSubmitResponse submitPayment(CashierSubmitRequest req) {
        PaymentOrder order = orderRepository.findByPaymentId(req.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(req.getPaymentId()));

        if (order.isExpired()) throw new NexusFlowException(ErrorCode.PAYMENT_EXPIRED, "Order expired");
        if (order.getStatus() != OrderStatus.WAITING_PAYMENT && order.getStatus() != OrderStatus.PARTIALLY_PAID) {
            throw new NexusFlowException(ErrorCode.INVALID_STATE_TRANSITION, "Order not in payable state");
        }

        // Cancel any active flow
        flowRepository.findActiveByPaymentId(req.getPaymentId())
                .ifPresent(f -> { f.markCancelled(); flowRepository.save(f); });

        // Resolve channel
        String channelId = req.getChannelId() != null ? req.getChannelId() : order.getChannelId();
        ChannelAdapter channel = resolveChannel(channelId);

        // Create deposit address via channel
        var depReq = ChannelAdapter.CreateDepositRequest.builder()
                .merchantId(order.getMerchantId())
                .channelUserId(order.getChannelUserId())
                .token(req.getToken()).network(req.getNetwork())
                .cryptoAmount(order.getAmountCrypto())
                .orderId(order.getPaymentId())
                .notifyUrl("https://api.nexusflow.com/callback/" + channelId + "/payment")
                .build();

        DepositAddress deposit = channel.createDepositAddress(depReq);

        // Create flow
        PaymentFlow flow = PaymentFlow.builder()
                .flowNo("FLW" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .paymentId(order.getPaymentId())
                .channelId(channelId)
                .token(req.getToken()).network(req.getNetwork())
                .cryptoAmount(order.getAmountCrypto())
                .fiatAmount(order.getAmountFiat())
                .fiatCurrency(order.getCurrencyFiat())
                .exchangeRate(order.getExchangeRate())
                .payAddress(deposit.getAddress())
                .memo(deposit.getMemo())
                .build();
        flow.markWaiting();
        flowRepository.save(flow);

        order.assignDepositAddress(deposit.getAddress(), deposit.getMemo(), deposit.getChannelOrderId());
        orderRepository.save(order);

        return CashierSubmitResponse.builder()
                .flowNo(flow.getFlowNo())
                .paymentId(order.getPaymentId())
                .token(req.getToken()).network(req.getNetwork())
                .cryptoAmount(order.getAmountCrypto().toPlainString())
                .fiatAmount(order.getAmountFiat().toPlainString())
                .fiatCurrency(order.getCurrencyFiat())
                .exchangeRate(order.getExchangeRate().toPlainString())
                .channelId(channelId)
                .payAddress(deposit.getAddress())
                .memo(deposit.getMemo())
                .requiredConfirmations(deposit.getRequiredConfirmations())
                .expireTime(order.getExpireTime().toEpochMilli())
                .status("WAITING")
                .build();
    }

    // ── Handle Channel Callback ──

    @Transactional
    public void handlePaymentCallback(String channelId, String channelOrderId,
                                       String txHash, String paidCrypto, String paidFiat, String eventId) {
        PaymentOrder order = orderRepository.findByChannelOrderId(channelId, channelOrderId)
                .orElseThrow(() -> logAndThrow("Order not found for channelOrderId: " + channelOrderId));

        // Dedup by eventId — channels may deliver the same callback more than once.
        // Done after resolving the order so an early "order not found" retry isn't permanently swallowed.
        if (eventId != null && !processedEventStore.markProcessed(eventId)) {
            log.info("Duplicate channel callback ignored: eventId={}, channelOrderId={}", eventId, channelOrderId);
            return;
        }

        BigDecimal cryptoAmt;
        try {
            cryptoAmt = new BigDecimal(paidCrypto);
        } catch (NumberFormatException e) {
            throw new NexusFlowException(ErrorCode.INVALID_REQUEST, "Invalid paidCrypto value: " + paidCrypto);
        }
        BigDecimal fiatAmt;
        if (paidFiat != null) {
            try {
                fiatAmt = new BigDecimal(paidFiat);
            } catch (NumberFormatException e) {
                throw new NexusFlowException(ErrorCode.INVALID_REQUEST, "Invalid paidFiat value: " + paidFiat);
            }
        } else {
            fiatAmt = cryptoAmt.divide(order.getExchangeRate(), 2, RoundingMode.HALF_UP);
        }

        if (cryptoAmt.compareTo(order.getAmountCrypto()) >= 0) {
            order.markConfirmed(txHash, cryptoAmt, fiatAmt);
            log.info("Payment confirmed: paymentId={}, txHash={}", order.getPaymentId(), txHash);
        } else {
            order.markPartiallyPaid(txHash, cryptoAmt, fiatAmt);
            log.info("Payment partially paid: paymentId={}, paid={}, expected={}",
                    order.getPaymentId(), paidCrypto, order.getAmountCrypto());
        }

        orderRepository.save(order);

        // Update active flow
        flowRepository.findActiveByPaymentId(order.getPaymentId())
                .ifPresent(f -> { f.markConfirmed(txHash, cryptoAmt); flowRepository.save(f); });

        // Publish events + fire webhook
        List<com.nexusflow.domain.event.DomainEvent> events = order.collectEvents();
        events.forEach(eventPublisher::publish);

        if (order.getNotifyUrl() != null) {
            webhookService.notifyMerchant(order, events);
        }
    }

    // ── Query ──

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String paymentId) {
        PaymentOrder order = orderRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        return toResponse(order, order.getChannelId(), null);
    }

    @Transactional(readOnly = true)
    public CashierStatusResponse getCashierStatus(String paymentId) {
        PaymentOrder order = orderRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        List<PaymentFlow> flows = flowRepository.findByPaymentId(paymentId);
        return CashierStatusResponse.builder()
                .paymentId(order.getPaymentId())
                .status(order.getStatus().name())
                .amountFiat(order.getAmountFiat().toPlainString())
                .currencyFiat(order.getCurrencyFiat())
                .amountCrypto(order.getAmountCrypto().toPlainString())
                .currencyCrypto(order.getCurrencyCrypto())
                .network(order.getNetwork())
                .exchangeRate(order.getExchangeRate().toPlainString())
                .channelId(order.getChannelId())
                .payAddress(order.getPayAddress())
                .memo(order.getMemo())
                .paidAmountCrypto(order.getPaidAmountCrypto().toPlainString())
                .paidAmountFiat(order.getPaidAmountFiat().toPlainString())
                .pendingAmount(order.getStatus() == OrderStatus.PARTIALLY_PAID
                        ? order.getAmountCrypto().subtract(order.getPaidAmountCrypto()).toPlainString() : null)
                .txHash(order.getTxHash())
                .transactionCount(flows.size())
                .expireTime(order.getExpireTime().toEpochMilli())
                .payTime(order.getPayTime() != null ? order.getPayTime().toEpochMilli() : null)
                .confirmTime(order.getConfirmTime() != null ? order.getConfirmTime().toEpochMilli() : null)
                .build();
    }

    // ── Refund ──

    @Transactional
    public RefundResponseDto refund(RefundRequestDto req) {
        PaymentOrder order = orderRepository.findByMerchantOrderNo(req.getMerchantId(), req.getMerchantOrderNo())
                .orElseThrow(() -> new PaymentNotFoundException(req.getMerchantOrderNo()));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new NexusFlowException(ErrorCode.REFUND_NOT_ALLOWED, "Order not confirmed");
        }

        BigDecimal refundFiat = new BigDecimal(req.getRefundAmountFiat());
        BigDecimal refundCrypto = refundFiat.divide(order.getExchangeRate(), 6, RoundingMode.HALF_UP);

        if (refundFiat.compareTo(order.getPaidAmountFiat()) > 0) {
            throw new NexusFlowException(ErrorCode.REFUND_AMOUNT_EXCEEDED, "Refund exceeds paid amount");
        }

        // Create refund order
        RefundOrder refund = RefundOrder.builder()
                .refundOrderNo(req.getRefundOrderNo())
                .paymentId(order.getPaymentId())
                .refundAmountFiat(refundFiat)
                .refundAmountCrypto(refundCrypto)
                .exchangeRate(order.getExchangeRate())
                .token(order.getCurrencyCrypto())
                .network(order.getNetwork())
                .toAddress(order.getPayAddress()) // channel will use original source address
                .notifyUrl(req.getNotifyUrl())
                .build();
        refundRepository.save(refund);

        order.markRefundProcessing();
        orderRepository.save(order);
        order.collectEvents().forEach(eventPublisher::publish);

        log.info("Refund initiated: refundOrderNo={}, amount={}", req.getRefundOrderNo(), refundFiat);

        return RefundResponseDto.builder()
                .refundOrderNo(refund.getRefundOrderNo())
                .paymentId(order.getPaymentId())
                .status(refund.getStatus().name())
                .refundAmountFiat(refundFiat.toPlainString())
                .refundAmountCrypto(refundCrypto.toPlainString())
                .exchangeRate(order.getExchangeRate().toPlainString())
                .token(order.getCurrencyCrypto())
                .network(order.getNetwork())
                .createTime(refund.getCreateTime().toEpochMilli())
                .build();
    }

    @Transactional
    public void handleRefundCallback(String channelRefundId, String status, String txHash) {
        RefundOrder refund = refundRepository.findByChannelRefundId(channelRefundId)
                .orElseThrow(() -> new NexusFlowException(ErrorCode.REFUND_NOT_FOUND, "Refund not found: " + channelRefundId));

        PaymentOrder order = orderRepository.findByPaymentId(refund.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(refund.getPaymentId()));

        if ("SUCCESS".equalsIgnoreCase(status)) {
            refund.markSuccess(txHash);
            order.markRefunded();
        } else {
            refund.markFailed();
            order.markRefundFailed();
        }

        refundRepository.save(refund);
        orderRepository.save(order);
        order.collectEvents().forEach(eventPublisher::publish);
    }

    // ── Helpers ──

    private ChannelAdapter resolveChannel(String channelId) {
        return channelAdapters.stream()
                .filter(a -> a.channelId().equalsIgnoreCase(channelId))
                .findFirst()
                .orElseThrow(() -> new NexusFlowException(ErrorCode.NO_AVAILABLE_CHANNEL,
                        "No channel adapter for channelId: " + channelId));
    }

    private OrderResponse toResponse(PaymentOrder o, String channelName, String payUrl) {
        return OrderResponse.builder()
                .paymentId(o.getPaymentId()).merchantOrderNo(o.getMerchantOrderNo())
                .status(o.getStatus().name())
                .amountFiat(o.getAmountFiat().toPlainString()).currencyFiat(o.getCurrencyFiat())
                .amountCrypto(o.getAmountCrypto().toPlainString()).currencyCrypto(o.getCurrencyCrypto())
                .network(o.getNetwork()).exchangeRate(o.getExchangeRate().toPlainString())
                .channelId(o.getChannelId()).channelName(channelName)
                .payAddress(o.getPayAddress()).memo(o.getMemo())
                .paidAmountCrypto(o.getPaidAmountCrypto().toPlainString())
                .paidAmountFiat(o.getPaidAmountFiat().toPlainString())
                .txHash(o.getTxHash()).payUrl(payUrl)
                .expireTime(o.getExpireTime().toEpochMilli())
                .payTime(o.getPayTime() != null ? o.getPayTime().toEpochMilli() : null)
                .confirmTime(o.getConfirmTime() != null ? o.getConfirmTime().toEpochMilli() : null)
                .createTime(o.getCreateTime().toEpochMilli()).build();
    }

    private NexusFlowException logAndThrow(String msg) {
        log.error(msg);
        return new NexusFlowException(ErrorCode.PAYMENT_NOT_FOUND, msg);
    }
}