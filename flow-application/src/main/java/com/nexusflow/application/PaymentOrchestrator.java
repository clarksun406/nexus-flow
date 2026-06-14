package com.nexusflow.application;

import com.nexusflow.application.dto.*;
import com.nexusflow.common.ErrorCode;
import com.nexusflow.common.NexusFlowException;
import com.nexusflow.common.PaymentNotFoundException;
import com.nexusflow.domain.channel.ChannelAdapter;
import com.nexusflow.domain.channel.ChannelRefund;
import com.nexusflow.domain.channel.ChannelRouter;
import com.nexusflow.domain.channel.ChannelUser;
import com.nexusflow.domain.channel.CurrencyRateCache;
import com.nexusflow.domain.channel.DepositAddress;
import com.nexusflow.domain.channel.ExchangeRate;
import com.nexusflow.domain.event.DomainEventPublisher;
import com.nexusflow.domain.event.ProcessedEventStore;
import com.nexusflow.domain.event.RefundRequestedEvent;
import com.nexusflow.domain.gas.GasEstimate;
import com.nexusflow.domain.gas.GasEstimateRequest;
import com.nexusflow.domain.gas.GasEstimator;
import com.nexusflow.domain.gas.GasOperation;
import com.nexusflow.domain.order.*;
import com.nexusflow.domain.refund.RefundOrder;
import com.nexusflow.domain.refund.RefundRepository;
import com.nexusflow.domain.refund.RefundStatus;
import com.nexusflow.domain.shared.Chain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final CurrencyRateCache currencyRateCache;
    private final GasEstimator gasEstimator;

    @Value("${nexusflow.cashier.base-url:/checkout.html}")
    private String cashierBaseUrl = "/checkout.html";

    // ── Create Order ──

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest req) {
        if (orderRepository.existsByMerchantOrderNo(req.getMerchantId(), req.getMerchantOrderNo())) {
            throw new NexusFlowException(ErrorCode.PAYMENT_ALREADY_EXISTS,
                    "Duplicate order: " + req.getMerchantOrderNo());
        }

        OrderPricing pricing = resolvePricing(req);

        // Route to best channel
        var routeReq = ChannelRouter.RouteRequest.builder()
                .merchantId(req.getMerchantId())
                .token(pricing.token)
                .network(pricing.network)
                .currencyFiat(pricing.quoteCurrency)
                .preferredChannelId(req.getPreferredChannel()).build();
        List<ChannelAdapter> channels = channelRouter.route(routeReq);
        if (channels.isEmpty()) throw new NexusFlowException(ErrorCode.NO_AVAILABLE_CHANNEL, "No channel available");

        ChannelAdapter channel = channels.get(0);
        log.info("Routing order {} to channel {}", req.getMerchantOrderNo(), channel.channelId());

        // Ensure buyer account on channel
        ChannelUser channelUser = channel.openUser(req.getMerchantId(), req.getMerchantOrderNo());

        // Get exchange rate (cached)
        ExchangeRate rate = currencyRateCache.getExchangeRate(
                channel, pricing.token, pricing.network, pricing.quoteCurrency);
        validateRate(rate, pricing.token, pricing.network, pricing.quoteCurrency);

        BigDecimal amountCrypto = pricing.cryptoDenominated
                ? pricing.amount
                : pricing.amount.divide(rate.getPrice(), 6, RoundingMode.HALF_UP);
        BigDecimal amountFiat = pricing.cryptoDenominated
                ? pricing.amount.multiply(rate.getPrice()).setScale(2, RoundingMode.HALF_UP)
                : pricing.amount;
        String token = hasText(rate.getToken()) ? normalizeCode(rate.getToken()) : pricing.token;
        String network = hasText(rate.getNetwork()) ? normalizeCode(rate.getNetwork()) : pricing.network;

        // Create order
        Instant expireTime = Instant.now().plusSeconds(30 * 60); // 30 min default
        PaymentOrder order = PaymentOrder.builder()
                .paymentId(UUID.randomUUID().toString())
                .merchantId(req.getMerchantId())
                .merchantOrderNo(req.getMerchantOrderNo())
                .amountFiat(amountFiat)
                .currencyFiat(pricing.quoteCurrency)
                .amountCrypto(amountCrypto)
                .currencyCrypto(token)
                .network(network)
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

        String payUrl = buildCashierPayUrl(order.getPaymentId());
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
            fiatAmt = cryptoAmt.multiply(order.getExchangeRate()).setScale(2, RoundingMode.HALF_UP);
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

        ChannelAdapter channel = resolveChannel(order.getChannelId());
        String requestedToAddress = hasText(req.getToAddress()) ? req.getToAddress().trim() : null;
        String toAddress = requestedToAddress != null
                ? requestedToAddress
                : ("SELF_HOSTED_NODE".equalsIgnoreCase(channel.channelId()) ? null : order.getPayAddress());
        ChannelRefund channelRefund = channel.refund(ChannelAdapter.RefundRequest.builder()
                .channelOrderId(hasText(order.getChannelOrderId()) ? order.getChannelOrderId() : order.getPaymentId())
                .channelUserId(order.getChannelUserId())
                .refundCryptoAmount(refundCrypto)
                .token(order.getCurrencyCrypto())
                .network(order.getNetwork())
                .toAddress(toAddress)
                .notifyUrl(req.getNotifyUrl())
                .refundOrderNo(req.getRefundOrderNo())
                .build());
        if (channelRefund == null || !hasText(channelRefund.getChannelRefundId())) {
            throw new NexusFlowException(ErrorCode.INTERNAL_ERROR,
                    "Channel refund did not return a channelRefundId");
        }

        RefundOrder refund = RefundOrder.builder()
                .refundOrderNo(req.getRefundOrderNo())
                .paymentId(order.getPaymentId())
                .refundAmountFiat(refundFiat)
                .refundAmountCrypto(refundCrypto)
                .exchangeRate(order.getExchangeRate())
                .token(order.getCurrencyCrypto())
                .network(order.getNetwork())
                .toAddress(toAddress)
                .notifyUrl(req.getNotifyUrl())
                .build();
        refund.bindChannelRefund(channelRefund.getChannelRefundId());
        refundRepository.save(refund);

        order.markRefundProcessing();
        orderRepository.save(order);
        order.collectEvents().forEach(eventPublisher::publish);
        GasEstimate gasEstimate = estimateSelfHostedRefundGas(channel.channelId(), refund);
        eventPublisher.publish(new RefundRequestedEvent(
                refund.getRefundOrderNo(),
                refund.getPaymentId(),
                channel.channelId(),
                channelRefund.getChannelRefundId(),
                refund.getToken(),
                refund.getNetwork(),
                refund.getRefundAmountCrypto().toPlainString(),
                refund.getToAddress(),
                gasEstimate != null ? gasEstimate.getNativeCurrency() : null,
                gasEstimate != null ? Long.toString(gasEstimate.getGasLimit()) : null,
                gasEstimate != null ? gasEstimate.getGasPrice().toPlainString() : null,
                gasEstimate != null ? gasEstimate.getEstimatedFee().toPlainString() : null));

        log.info("Refund initiated: refundOrderNo={}, amount={}", req.getRefundOrderNo(), refundFiat);

        return RefundResponseDto.builder()
                .refundOrderNo(refund.getRefundOrderNo())
                .paymentId(order.getPaymentId())
                .channelRefundId(refund.getChannelRefundId())
                .status(refund.getStatus().name())
                .refundAmountFiat(refundFiat.toPlainString())
                .refundAmountCrypto(refundCrypto.toPlainString())
                .exchangeRate(order.getExchangeRate().toPlainString())
                .token(order.getCurrencyCrypto())
                .network(order.getNetwork())
                .toAddress(refund.getToAddress())
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

    private GasEstimate estimateSelfHostedRefundGas(String channelId, RefundOrder refund) {
        if (!"SELF_HOSTED_NODE".equalsIgnoreCase(channelId)) {
            return null;
        }
        try {
            return gasEstimator.estimate(GasEstimateRequest.builder()
                    .chain(Chain.fromCurrency(refund.getToken() + "_" + refund.getNetwork()))
                    .token(refund.getToken())
                    .network(refund.getNetwork())
                    .operation(GasOperation.REFUND)
                    .amount(refund.getRefundAmountCrypto())
                    .toAddress(refund.getToAddress())
                    .build());
        } catch (RuntimeException e) {
            log.warn("Failed to estimate gas for self-hosted refund: refundOrderNo={}, token={}, network={}, reason={}",
                    refund.getRefundOrderNo(), refund.getToken(), refund.getNetwork(), e.getMessage());
            return null;
        }
    }

    private OrderPricing resolvePricing(CreateOrderRequest req) {
        boolean hasFiatAmount = hasText(req.getAmountFiat());
        boolean hasCryptoAmount = hasText(req.getAmountCrypto());
        boolean hasCryptoAsset = hasText(req.getCurrencyCrypto()) || hasText(req.getNetwork());

        if (hasFiatAmount && (hasCryptoAmount || hasCryptoAsset)) {
            throw invalidRequest("Provide either amountFiat/currencyFiat or amountCrypto/currencyCrypto/network, not both");
        }
        if (hasFiatAmount) {
            if (!hasText(req.getCurrencyFiat())) {
                throw invalidRequest("currencyFiat is required when amountFiat is provided");
            }
            return new OrderPricing(
                    parsePositiveDecimal(req.getAmountFiat(), "amountFiat"),
                    "USDT",
                    "TRC20",
                    normalizeCode(req.getCurrencyFiat()),
                    false);
        }
        if (hasCryptoAmount) {
            if (!hasText(req.getCurrencyCrypto()) || !hasText(req.getNetwork())) {
                throw invalidRequest("currencyCrypto and network are required when amountCrypto is provided");
            }
            String quoteCurrency = hasText(req.getCurrencyFiat()) ? normalizeCode(req.getCurrencyFiat()) : "USD";
            return new OrderPricing(
                    parsePositiveDecimal(req.getAmountCrypto(), "amountCrypto"),
                    normalizeCode(req.getCurrencyCrypto()),
                    normalizeCode(req.getNetwork()),
                    quoteCurrency,
                    true);
        }
        throw invalidRequest("amountFiat or amountCrypto is required");
    }

    private void validateRate(ExchangeRate rate, String token, String network, String quoteCurrency) {
        if (rate == null || rate.getPrice() == null || rate.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new NexusFlowException(ErrorCode.NO_AVAILABLE_CHANNEL,
                    "No valid exchange rate for " + token + "/" + network + " quoted in " + quoteCurrency);
        }
    }

    private BigDecimal parsePositiveDecimal(String value, String fieldName) {
        try {
            BigDecimal parsed = new BigDecimal(value);
            if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
                throw invalidRequest(fieldName + " must be greater than zero");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw invalidRequest(fieldName + " must be a valid decimal");
        }
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String buildCashierPayUrl(String paymentId) {
        String baseUrl = hasText(cashierBaseUrl) ? cashierBaseUrl.trim() : "/checkout.html";
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "payment_id=" + URLEncoder.encode(paymentId, StandardCharsets.UTF_8);
    }

    private NexusFlowException invalidRequest(String message) {
        return new NexusFlowException(ErrorCode.INVALID_REQUEST, message);
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

    private static class OrderPricing {
        private final BigDecimal amount;
        private final String token;
        private final String network;
        private final String quoteCurrency;
        private final boolean cryptoDenominated;

        private OrderPricing(BigDecimal amount, String token, String network,
                             String quoteCurrency, boolean cryptoDenominated) {
            this.amount = amount;
            this.token = token;
            this.network = network;
            this.quoteCurrency = quoteCurrency;
            this.cryptoDenominated = cryptoDenominated;
        }
    }
}
