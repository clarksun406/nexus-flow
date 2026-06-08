package com.nexusflow.application;

import com.nexusflow.domain.event.DomainEventPublisher;
import com.nexusflow.domain.order.OrderRepository;
import com.nexusflow.domain.order.OrderStatus;
import com.nexusflow.domain.order.PaymentOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled job that expires overdue orchestration-layer orders.
 *
 * Orders in WAITING_PAYMENT or PARTIALLY_PAID status that have passed their
 * configured expireTime are marked as EXPIRED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpiryJob {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;

    @Scheduled(fixedDelayString = "${nexusflow.order.expiry.interval-ms:60000}")
    public void expireOverdueOrders() {
        List<PaymentOrder> expirable = orderRepository.findByStatusIn(
                List.of(OrderStatus.WAITING_PAYMENT, OrderStatus.PARTIALLY_PAID));

        for (PaymentOrder order : expirable) {
            try {
                if (order.isExpired()) {
                    order.markExpired();
                    orderRepository.save(order);
                    order.collectEvents().forEach(eventPublisher::publish);
                    log.info("Order expired: paymentId={}, merchantOrderNo={}",
                            order.getPaymentId(), order.getMerchantOrderNo());
                }
            } catch (Exception e) {
                log.error("Failed to expire order {}: {}", order.getPaymentId(), e.getMessage(), e);
            }
        }
    }
}
