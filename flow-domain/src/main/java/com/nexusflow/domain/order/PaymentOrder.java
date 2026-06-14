package com.nexusflow.domain.order;

import com.nexusflow.domain.event.DomainEvent;
import com.nexusflow.domain.event.OrderEvent;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * PaymentOrder aggregate root — orchestration layer.
 */
@Getter
public class PaymentOrder {

    private String paymentId;
    private String merchantId;
    private String merchantOrderNo;
    private BigDecimal amountFiat;
    private String currencyFiat;
    private BigDecimal amountCrypto;
    private String currencyCrypto;
    private String network;
    private BigDecimal exchangeRate;
    private String channelId;
    private String channelUserId;
    private String channelOrderId;
    private OrderStatus status;
    private String payAddress;
    private String memo;
    private BigDecimal paidAmountCrypto;
    private BigDecimal paidAmountFiat;
    private String txHash;
    @Setter private String notifyUrl;
    @Setter private String returnUrl;
    private String extendData;
    private Instant expireTime;
    private Instant payTime;
    private Instant confirmTime;
    private Instant createTime;
    private Instant updateTime;

    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    @Builder
    public PaymentOrder(String paymentId, String merchantId, String merchantOrderNo,
                        BigDecimal amountFiat, String currencyFiat,
                        BigDecimal amountCrypto, String currencyCrypto, String network,
                        BigDecimal exchangeRate, String channelId, String channelUserId,
                        String notifyUrl, String returnUrl, String extendData, Instant expireTime) {
        this.paymentId = paymentId;
        this.merchantId = merchantId;
        this.merchantOrderNo = merchantOrderNo;
        this.amountFiat = amountFiat;
        this.currencyFiat = currencyFiat;
        this.amountCrypto = amountCrypto;
        this.currencyCrypto = currencyCrypto;
        this.network = network;
        this.exchangeRate = exchangeRate;
        this.channelId = channelId;
        this.channelUserId = channelUserId;
        this.notifyUrl = notifyUrl;
        this.returnUrl = returnUrl;
        this.extendData = extendData;
        this.expireTime = expireTime;
        this.status = OrderStatus.WAITING_PAYMENT;
        this.paidAmountCrypto = BigDecimal.ZERO;
        this.paidAmountFiat = BigDecimal.ZERO;
        this.createTime = Instant.now();
        this.updateTime = Instant.now();
    }

    public void bindChannel(String channelId, String channelUserId) {
        this.channelId = channelId;
        this.channelUserId = channelUserId;
        touch();
    }

    public void assignDepositAddress(String payAddress, String memo, String channelOrderId) {
        this.payAddress = payAddress;
        this.memo = memo;
        this.channelOrderId = channelOrderId;
        touch();
    }

    public void markConfirmed(String txHash, BigDecimal paidCrypto, BigDecimal paidFiat) {
        this.txHash = txHash;
        this.paidAmountCrypto = paidCrypto;
        this.paidAmountFiat = paidFiat;
        this.payTime = Instant.now();
        this.confirmTime = Instant.now();
        transition(OrderStatus.CONFIRMED, txHash, paidCrypto, paidFiat, null);
    }

    public void markPartiallyPaid(String txHash, BigDecimal cumulativeCrypto, BigDecimal cumulativeFiat) {
        this.txHash = txHash;
        this.paidAmountCrypto = cumulativeCrypto;
        this.paidAmountFiat = cumulativeFiat;
        transition(OrderStatus.PARTIALLY_PAID, txHash, cumulativeCrypto, cumulativeFiat,
                amountCrypto.subtract(cumulativeCrypto).toPlainString());
    }

    public void markExpired() {
        transition(OrderStatus.EXPIRED, txHash, paidAmountCrypto, paidAmountFiat, null);
    }

    public void markRefundProcessing() {
        transition(OrderStatus.REFUND_PROCESSING, txHash, paidAmountCrypto, paidAmountFiat, null);
    }

    public void markRefunded() {
        transition(OrderStatus.REFUNDED, txHash, paidAmountCrypto, paidAmountFiat, null);
    }

    public void markRefundFailed() {
        transition(OrderStatus.REFUND_FAILED, txHash, paidAmountCrypto, paidAmountFiat, null);
    }

    public void markFailed() {
        transition(OrderStatus.FAILED, txHash, paidAmountCrypto, paidAmountFiat, null);
    }

    public List<DomainEvent> collectEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    public boolean isExpired() {
        return expireTime != null && Instant.now().isAfter(expireTime);
    }

    private void transition(OrderStatus target, String th, BigDecimal pc, BigDecimal pf, String pending) {
        OrderStatus prev = this.status;
        this.status = prev.requireTransitionTo(target);
        touch();
        domainEvents.add(new OrderEvent(paymentId, merchantOrderNo, merchantId,
                channelId, prev, target, th,
                pc != null ? pc.toPlainString() : null,
                pf != null ? pf.toPlainString() : null, pending));
    }

    /**
     * Full-args builder for reconstituting a PaymentOrder from persistence.
     * Not for public use — only for repository mapping.
     */
    @Builder(builderMethodName = "reconstitute", builderClassName = "PaymentOrderReconstituteBuilder")
    private PaymentOrder(String paymentId, String merchantId, String merchantOrderNo,
                         BigDecimal amountFiat, String currencyFiat,
                         BigDecimal amountCrypto, String currencyCrypto, String network,
                         BigDecimal exchangeRate, String channelId, String channelUserId,
                         String channelOrderId, OrderStatus status,
                         String payAddress, String memo,
                         BigDecimal paidAmountCrypto, BigDecimal paidAmountFiat,
                         String txHash, String notifyUrl, String returnUrl, String extendData,
                         Instant expireTime, Instant payTime, Instant confirmTime,
                         Instant createTime, Instant updateTime) {
        this.paymentId = paymentId;
        this.merchantId = merchantId;
        this.merchantOrderNo = merchantOrderNo;
        this.amountFiat = amountFiat;
        this.currencyFiat = currencyFiat;
        this.amountCrypto = amountCrypto;
        this.currencyCrypto = currencyCrypto;
        this.network = network;
        this.exchangeRate = exchangeRate;
        this.channelId = channelId;
        this.channelUserId = channelUserId;
        this.channelOrderId = channelOrderId;
        this.status = status;
        this.payAddress = payAddress;
        this.memo = memo;
        this.paidAmountCrypto = paidAmountCrypto;
        this.paidAmountFiat = paidAmountFiat;
        this.txHash = txHash;
        this.notifyUrl = notifyUrl;
        this.returnUrl = returnUrl;
        this.extendData = extendData;
        this.expireTime = expireTime;
        this.payTime = payTime;
        this.confirmTime = confirmTime;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    private void touch() { this.updateTime = Instant.now(); }
}
