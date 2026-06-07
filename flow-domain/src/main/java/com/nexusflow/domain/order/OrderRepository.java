package com.nexusflow.domain.order;

import java.util.Optional;

public interface OrderRepository {
    void save(PaymentOrder order);
    Optional<PaymentOrder> findByPaymentId(String paymentId);
    Optional<PaymentOrder> findByMerchantOrderNo(String merchantId, String merchantOrderNo);
    Optional<PaymentOrder> findByChannelOrderId(String channelId, String channelOrderId);
    boolean existsByMerchantOrderNo(String merchantId, String merchantOrderNo);
}