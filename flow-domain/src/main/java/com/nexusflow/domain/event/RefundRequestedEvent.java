package com.nexusflow.domain.event;

import lombok.Getter;

@Getter
public class RefundRequestedEvent extends DomainEvent {

    private final String refundOrderNo;
    private final String paymentId;
    private final String channelId;
    private final String channelRefundId;
    private final String token;
    private final String network;
    private final String refundAmountCrypto;
    private final String toAddress;
    private final String gasNativeCurrency;
    private final String gasLimit;
    private final String gasPrice;
    private final String gasEstimatedFee;

    public RefundRequestedEvent(String refundOrderNo, String paymentId, String channelId,
                                String channelRefundId, String token, String network,
                                String refundAmountCrypto, String toAddress) {
        this(refundOrderNo, paymentId, channelId, channelRefundId, token, network,
                refundAmountCrypto, toAddress, null, null, null, null);
    }

    public RefundRequestedEvent(String refundOrderNo, String paymentId, String channelId,
                                String channelRefundId, String token, String network,
                                String refundAmountCrypto, String toAddress,
                                String gasNativeCurrency, String gasLimit,
                                String gasPrice, String gasEstimatedFee) {
        this.refundOrderNo = refundOrderNo;
        this.paymentId = paymentId;
        this.channelId = channelId;
        this.channelRefundId = channelRefundId;
        this.token = token;
        this.network = network;
        this.refundAmountCrypto = refundAmountCrypto;
        this.toAddress = toAddress;
        this.gasNativeCurrency = gasNativeCurrency;
        this.gasLimit = gasLimit;
        this.gasPrice = gasPrice;
        this.gasEstimatedFee = gasEstimatedFee;
    }

    @Override
    public String eventType() {
        return "crypto.refund.requested";
    }
}
