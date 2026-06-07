package com.nexusflow.domain.event;

import com.nexusflow.domain.order.OrderStatus;
import lombok.Getter;

@Getter
public class OrderEvent extends DomainEvent {

    private final String paymentId;
    private final String orderId;
    private final String merchantId;
    private final String channelId;
    private final OrderStatus previousStatus;
    private final OrderStatus newStatus;
    private final String txHash;
    private final String paidCrypto;
    private final String paidFiat;
    private final String pendingAmount;

    public OrderEvent(String paymentId, String orderId, String merchantId,
                      String channelId, OrderStatus prev, OrderStatus next,
                      String txHash, String paidCrypto, String paidFiat, String pending) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.merchantId = merchantId;
        this.channelId = channelId;
        this.previousStatus = prev;
        this.newStatus = next;
        this.txHash = txHash;
        this.paidCrypto = paidCrypto;
        this.paidFiat = paidFiat;
        this.pendingAmount = pending;
    }

    @Override
    public String eventType() {
        return "nexusflow.order." + newStatus.name().toLowerCase();
    }
}