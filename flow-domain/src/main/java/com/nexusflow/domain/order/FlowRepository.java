package com.nexusflow.domain.order;

import java.util.List;
import java.util.Optional;

public interface FlowRepository {
    void save(PaymentFlow flow);
    Optional<PaymentFlow> findByFlowNo(String flowNo);
    List<PaymentFlow> findByPaymentId(String paymentId);
    Optional<PaymentFlow> findActiveByPaymentId(String paymentId);
}