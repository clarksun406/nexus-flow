package com.nexusflow.domain.refund;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
public class RefundOrder {

    private String refundOrderNo;
    private String paymentId;
    private String channelRefundId;
    private BigDecimal refundAmountFiat;
    private BigDecimal refundAmountCrypto;
    private BigDecimal exchangeRate;
    private String token;
    private String network;
    private String toAddress;
    private String txHash;
    private String notifyUrl;
    private RefundStatus status;
    private Instant createTime;
    private Instant confirmTime;
    private Instant updateTime;

    @Builder
    public RefundOrder(String refundOrderNo, String paymentId,
                       BigDecimal refundAmountFiat, BigDecimal refundAmountCrypto,
                       BigDecimal exchangeRate, String token, String network,
                       String toAddress, String notifyUrl) {
        this.refundOrderNo = refundOrderNo;
        this.paymentId = paymentId;
        this.refundAmountFiat = refundAmountFiat;
        this.refundAmountCrypto = refundAmountCrypto;
        this.exchangeRate = exchangeRate;
        this.token = token;
        this.network = network;
        this.toAddress = toAddress;
        this.notifyUrl = notifyUrl;
        this.status = RefundStatus.PROCESSING;
        this.createTime = Instant.now();
        this.updateTime = Instant.now();
    }

    public void bindChannelRefund(String id) { this.channelRefundId = id; touch(); }
    public void markSuccess(String txHash) {
        this.txHash = txHash;
        this.status = status.requireTransitionTo(RefundStatus.SUCCESS);
        this.confirmTime = Instant.now(); touch();
    }
    public void markFailed() { this.status = status.requireTransitionTo(RefundStatus.FAILED); touch(); }
    /**
     * Full-args builder for reconstituting a RefundOrder from persistence.
     */
    @Builder(builderMethodName = "reconstitute")
    private RefundOrder(String refundOrderNo, String paymentId, String channelRefundId,
                        BigDecimal refundAmountFiat, BigDecimal refundAmountCrypto,
                        BigDecimal exchangeRate, String token, String network,
                        String toAddress, String txHash, String notifyUrl,
                        RefundStatus status, Instant createTime, Instant confirmTime,
                        Instant updateTime) {
        this.refundOrderNo = refundOrderNo;
        this.paymentId = paymentId;
        this.channelRefundId = channelRefundId;
        this.refundAmountFiat = refundAmountFiat;
        this.refundAmountCrypto = refundAmountCrypto;
        this.exchangeRate = exchangeRate;
        this.token = token;
        this.network = network;
        this.toAddress = toAddress;
        this.txHash = txHash;
        this.notifyUrl = notifyUrl;
        this.status = status;
        this.createTime = createTime;
        this.confirmTime = confirmTime;
        this.updateTime = updateTime;
    }

    private void touch() { this.updateTime = Instant.now(); }
}