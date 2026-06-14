package com.nexusflow.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataCryptoPaymentRepository extends JpaRepository<CryptoPaymentEntity, String> {

    Optional<CryptoPaymentEntity> findByOrderId(String orderId);

    Optional<CryptoPaymentEntity> findFirstByTxHash(String txHash);

    Optional<CryptoPaymentEntity> findFirstByReceivingAddressAndStatus(String receivingAddress, String status);

    List<CryptoPaymentEntity> findByStatusIn(Collection<String> statuses);

    boolean existsByOrderId(String orderId);
}
