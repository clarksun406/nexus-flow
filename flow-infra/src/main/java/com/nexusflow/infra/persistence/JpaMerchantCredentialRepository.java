package com.nexusflow.infra.persistence;

import com.nexusflow.domain.merchant.MerchantApiKey;
import com.nexusflow.domain.merchant.MerchantCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaMerchantCredentialRepository implements MerchantCredentialRepository {

    private final SpringDataMerchantCredentialRepository repository;

    @Override
    public Optional<MerchantApiKey> findActiveByKeyHash(String keyHash, Instant now) {
        return repository.findActiveCredentialWithMerchant(keyHash, now)
                .map(row -> {
                    MerchantCredentialEntity credential = (MerchantCredentialEntity) row[0];
                    MerchantProfileEntity profile = (MerchantProfileEntity) row[1];
                    return MerchantApiKey.builder()
                            .merchantId(profile.getMerchantId())
                            .merchantCode(profile.getMerchantCode())
                            .keyPrefix(credential.getKeyPrefix())
                            .build();
                });
    }
}
