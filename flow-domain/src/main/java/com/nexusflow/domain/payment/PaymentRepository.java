package com.nexusflow.domain.payment;

import com.nexusflow.common.PageResult;

import java.util.Collection;
import java.util.List;
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

    /**
     * Find all payments currently in any of the given statuses.
     * Used by reconciliation (DETECTED/CONFIRMING) and expiry (PENDING) jobs.
     */
    List<CryptoPayment> findByStatusIn(Collection<PaymentStatus> statuses);

    boolean existsByOrderId(String orderId);

    /**
     * Paged search for ops views. Null status means "no status filter".
     * Results are ordered by createdAt descending.
     */
    PageResult<CryptoPayment> search(PaymentStatus status, int page, int size);
}