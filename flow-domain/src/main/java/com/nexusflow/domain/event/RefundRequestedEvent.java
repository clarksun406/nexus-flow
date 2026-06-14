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

    public RefundRequestedEvent(String refundOrderNo, String paymentId, String channelId,
                                String channelRefundId, String token, String network,
                                String refundAmountCrypto, String toAddress) {
        this.refundOrderNo = refundOrderNo;
        this.paymentId = paymentId;
        this.channelId = channelId;
        this.channelRefundId = channelRefundId;
        this.token = token;
        this.network = network;
        this.refundAmountCrypto = refundAmountCrypto;
        this.toAddress = toAddress;
    }

    @Override
    public String eventType() {
        return "crypto.refund.requested";
    }
}
