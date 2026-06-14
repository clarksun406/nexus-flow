package com.nexusflow.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataFiatRampOrderRepository extends JpaRepository<FiatRampOrderEntity, String> {
    Optional<FiatRampOrderEntity> findByMerchantIdAndMerchantOrderNo(String merchantId, String merchantOrderNo);
    Optional<FiatRampOrderEntity> findByProviderIdAndProviderOrderId(String providerId, String providerOrderId);
    Optional<FiatRampOrderEntity> findByPaymentId(String paymentId);
}
