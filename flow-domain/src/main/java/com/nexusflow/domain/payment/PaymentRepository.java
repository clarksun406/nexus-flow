package com.nexusflow.domain.payment;

import java.util.Optional;

/**
 * Repository port for CryptoPayment aggregate.
 */
public interface PaymentRepository {

    void save(CryptoPayment payment);

    Optional<CryptoPayment> findById(String id);

    Optional<CryptoPayment> findByOrderId(String orderId);

    Optional<CryptoPayment> findByTxHash(String txHash);

    /**
     * Find a payment in PENDING status awaiting funds at the given receiving address.
     * Used to match incoming on-chain transactions to expected payments.
     */
    Optional<CryptoPayment> findPendingByReceivingAddress(String receivingAddress);

    boolean existsByOrderId(String orderId);
}