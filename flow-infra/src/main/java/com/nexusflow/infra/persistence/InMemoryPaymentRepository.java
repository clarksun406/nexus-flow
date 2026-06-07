package com.nexusflow.infra.persistence;

import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.payment.PaymentRepository;
import com.nexusflow.domain.payment.PaymentStatus;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory payment repository for Phase 1 MVP.
 * Replace with JPA/MyBatis-Plus implementation for production.
 */
@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<String, CryptoPayment> byId = new ConcurrentHashMap<>();
    private final Map<String, CryptoPayment> byOrderId = new ConcurrentHashMap<>();
    private final Map<String, CryptoPayment> byTxHash = new ConcurrentHashMap<>();

    @Override
    public void save(CryptoPayment payment) {
        byId.put(payment.getId(), payment);
        byOrderId.put(payment.getOrderId(), payment);
        if (payment.getTxHash() != null) {
            byTxHash.put(payment.getTxHash(), payment);
        }
    }

    @Override
    public Optional<CryptoPayment> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<CryptoPayment> findByOrderId(String orderId) {
        return Optional.ofNullable(byOrderId.get(orderId));
    }

    @Override
    public Optional<CryptoPayment> findByTxHash(String txHash) {
        return Optional.ofNullable(byTxHash.get(txHash));
    }

    @Override
    public Optional<CryptoPayment> findPendingByReceivingAddress(String receivingAddress) {
        if (receivingAddress == null) {
            return Optional.empty();
        }
        return byId.values().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .filter(p -> receivingAddress.equals(p.getReceivingAddress()))
                .findFirst();
    }

    @Override
    public boolean existsByOrderId(String orderId) {
        return byOrderId.containsKey(orderId);
    }
}