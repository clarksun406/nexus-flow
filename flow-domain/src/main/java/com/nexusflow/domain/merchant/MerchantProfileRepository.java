package com.nexusflow.domain.merchant;

import java.util.List;
import java.util.Optional;

public interface MerchantProfileRepository {

    List<MerchantProfile> findAll();

    Optional<MerchantProfile> findById(String merchantId);

    MerchantProfile save(MerchantProfile profile);
}
