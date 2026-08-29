package com.nexusflow.infra.persistence;

import com.nexusflow.domain.merchant.MerchantApiKey;
import com.nexusflow.domain.merchant.MerchantCredentialRepository;
import com.nexusflow.domain.merchant.MerchantProfileRepository;
import com.nexusflow.domain.merchant.MerchantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaMerchantCredentialRepository implements MerchantCredentialRepository {

    private final SpringDataMerchantCredentialRepository repository;
    private final MerchantProfileRepository merchantProfileRepository;

    @Override
    public Optional<MerchantApiKey> findActiveByKeyHash(String keyHash, Instant now) {
        return repository.findActiveByKeyHash(keyHash, now)
                .flatMap(credential -> merchantProfileRepository.findById(credential.getMerchantId())
                        .filter(profile -> profile.getStatus() == MerchantStatus.ACTIVE)
                        .map(profile -> MerchantApiKey.builder()
                                .merchantId(profile.getMerchantId())
                                .merchantCode(profile.getMerchantCode())
                                .keyPrefix(credential.getKeyPrefix())
                                .build()));
    }
}
