package com.nexusflow.domain.fiat;

import java.util.Optional;

public interface FiatRampRepository {
    void save(FiatRampOrder order);
    Optional<FiatRampOrder> findByRampOrderId(String rampOrderId);
    Optional<FiatRampOrder> findByMerchantOrderNo(String merchantId, String merchantOrderNo);
    Optional<FiatRampOrder> findByProviderOrderId(String providerId, String providerOrderId);
    Optional<FiatRampOrder> findByPaymentId(String paymentId);
}
