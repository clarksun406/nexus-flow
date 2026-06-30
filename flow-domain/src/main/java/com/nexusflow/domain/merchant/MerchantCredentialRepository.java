package com.nexusflow.domain.merchant;

import java.time.Instant;
import java.util.Optional;

public interface MerchantCredentialRepository {
    Optional<MerchantApiKey> findActiveByKeyHash(String keyHash, Instant now);
}
