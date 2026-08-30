package com.nexusflow.domain.fiat;

import com.nexusflow.common.PageResult;

import java.util.Optional;

public interface FiatRampRepository {
    void save(FiatRampOrder order);
    Optional<FiatRampOrder> findByRampOrderId(String rampOrderId);
    Optional<FiatRampOrder> findByMerchantOrderNo(String merchantId, String merchantOrderNo);
    Optional<FiatRampOrder> findByProviderOrderId(String providerId, String providerOrderId);
    Optional<FiatRampOrder> findByPaymentId(String paymentId);

    /**
     * Paged search for ops views. Null filters mean "no filter on that field".
     * Results are ordered by createTime descending.
     */
    PageResult<FiatRampOrder> search(FiatRampStatus status, String merchantId, int page, int size);
}
