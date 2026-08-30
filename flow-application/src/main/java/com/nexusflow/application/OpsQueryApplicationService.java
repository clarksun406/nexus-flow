package com.nexusflow.application;

import com.nexusflow.application.dto.OpsFiatRampPageResponse;
import com.nexusflow.application.dto.OpsOrderPageResponse;
import com.nexusflow.application.dto.OpsPaymentPageResponse;
import com.nexusflow.common.PageResult;
import com.nexusflow.domain.fiat.FiatRampOrder;
import com.nexusflow.domain.fiat.FiatRampRepository;
import com.nexusflow.domain.fiat.FiatRampStatus;
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

/**
 * Paged query service backing the ops console list views (orders, execution
 * payments, fiat ramp orders).
 */
@Service
@RequiredArgsConstructor
public class OpsQueryApplicationService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final FiatRampRepository fiatRampRepository;

    @Transactional(readOnly = true)
    public OpsOrderPageResponse searchOrders(OrderStatus status, String merchantId, int page, int size) {
        PageResult<PaymentOrder> result = orderRepository.search(status, merchantId, page, size);
        return OpsOrderPageResponse.builder()
                .items(result.items().stream().map(this::toOrderItem).toList())
                .page(result.page())
                .size(result.size())
                .total(result.total())
                .build();
    }

    @Transactional(readOnly = true)
    public OpsPaymentPageResponse searchPayments(PaymentStatus status, int page, int size) {
        PageResult<CryptoPayment> result = paymentRepository.search(status, page, size);
        return OpsPaymentPageResponse.builder()
                .items(result.items().stream().map(this::toPaymentItem).toList())
                .page(result.page())
                .size(result.size())
                .total(result.total())
                .build();
    }

    @Transactional(readOnly = true)
    public OpsFiatRampPageResponse searchFiatRampOrders(FiatRampStatus status, String merchantId, int page, int size) {
        PageResult<FiatRampOrder> result = fiatRampRepository.search(status, merchantId, page, size);
        return OpsFiatRampPageResponse.builder()
                .items(result.items().stream().map(this::toFiatRampItem).toList())
                .page(result.page())
                .size(result.size())
                .total(result.total())
                .build();
    }

    private OpsOrderPageResponse.OrderItem toOrderItem(PaymentOrder order) {
        return OpsOrderPageResponse.OrderItem.builder()
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
                .paidAmountFiat(order.getPaidAmountFiat() != null ? order.getPaidAmountFiat().toPlainString() : null)
                .paidAmountCrypto(order.getPaidAmountCrypto() != null
                        ? order.getPaidAmountCrypto().toPlainString() : null)
                .txHash(order.getTxHash())
                .expireTime(toEpochMillis(order.getExpireTime()))
                .payTime(toEpochMillis(order.getPayTime()))
                .createTime(toEpochMillis(order.getCreateTime()))
                .updateTime(toEpochMillis(order.getUpdateTime()))
                .build();
    }

    private OpsPaymentPageResponse.PaymentItem toPaymentItem(CryptoPayment payment) {
        return OpsPaymentPageResponse.PaymentItem.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .currency(payment.getExpected() != null ? payment.getExpected().getCurrency() : null)
                .status(payment.getStatus() != null ? payment.getStatus().name() : null)
                .expectedAmount(payment.getExpected() != null && payment.getExpected().getAmount() != null
                        ? payment.getExpected().getAmount().toPlainString() : null)
                .receivedAmount(payment.getReceived() != null && payment.getReceived().getAmount() != null
                        ? payment.getReceived().getAmount().toPlainString() : null)
                .receivingAddress(payment.getReceivingAddress())
                .txHash(payment.getTxHash())
                .confirmations(payment.getConfirmations())
                .requiredConfirmations(payment.getRequiredConfirmations())
                .lastFailureReason(payment.getLastFailureReason())
                .createdAt(toEpochMillis(payment.getCreatedAt()))
                .detectedAt(toEpochMillis(payment.getDetectedAt()))
                .confirmedAt(toEpochMillis(payment.getConfirmedAt()))
                .build();
    }

    private OpsFiatRampPageResponse.FiatRampItem toFiatRampItem(FiatRampOrder order) {
        return OpsFiatRampPageResponse.FiatRampItem.builder()
                .rampOrderId(order.getRampOrderId())
                .merchantId(order.getMerchantId())
                .merchantOrderNo(order.getMerchantOrderNo())
                .paymentId(order.getPaymentId())
                .direction(order.getDirection() != null ? order.getDirection().name() : null)
                .providerId(order.getProviderId())
                .providerOrderId(order.getProviderOrderId())
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .fiatAmount(order.getFiatAmount() != null ? order.getFiatAmount().toPlainString() : null)
                .fiatCurrency(order.getFiatCurrency())
                .cryptoAmount(order.getCryptoAmount() != null ? order.getCryptoAmount().toPlainString() : null)
                .token(order.getToken())
                .network(order.getNetwork())
                .exchangeRate(order.getExchangeRate() != null ? order.getExchangeRate().toPlainString() : null)
                .createTime(toEpochMillis(order.getCreateTime()))
                .updateTime(toEpochMillis(order.getUpdateTime()))
                .build();
    }

    private Long toEpochMillis(Instant instant) {
        return instant != null ? instant.toEpochMilli() : null;
    }
}
