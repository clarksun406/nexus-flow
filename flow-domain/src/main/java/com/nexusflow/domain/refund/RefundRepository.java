package com.nexusflow.domain.refund;

import java.util.Optional;

public interface RefundRepository {
    void save(RefundOrder order);
    Optional<RefundOrder> findByRefundOrderNo(String refundOrderNo);
    Optional<RefundOrder> findByChannelRefundId(String channelRefundId);
}