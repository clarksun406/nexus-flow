package com.nexusflow.domain.order;

import com.nexusflow.common.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    void save(PaymentOrder order);
    Optional<PaymentOrder> findByPaymentId(String paymentId);
    Optional<PaymentOrder> findByMerchantOrderNo(String merchantId, String merchantOrderNo);
    Optional<PaymentOrder> findByChannelOrderId(String channelId, String channelOrderId);
    boolean existsByMerchantOrderNo(String merchantId, String merchantOrderNo);
    List<PaymentOrder> findByStatusIn(Collection<OrderStatus> statuses);

    /**
     * Paged search for ops views. Null filters mean "no filter on that field".
     * Results are ordered by createTime descending.
     */
    PageResult<PaymentOrder> search(OrderStatus status, String merchantId, int page, int size);
}