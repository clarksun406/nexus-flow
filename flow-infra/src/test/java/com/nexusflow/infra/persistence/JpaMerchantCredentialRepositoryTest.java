package com.nexusflow.infra.persistence;

import com.nexusflow.domain.merchant.MerchantApiKey;
import com.nexusflow.domain.merchant.MerchantProfile;
import com.nexusflow.domain.merchant.MerchantProfileRepository;
import com.nexusflow.domain.merchant.MerchantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaMerchantCredentialRepositoryTest {

    private SpringDataMerchantCredentialRepository springDataRepository;
    private MerchantProfileRepository profileRepository;
    private JpaMerchantCredentialRepository repository;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataMerchantCredentialRepository.class);
        profileRepository = mock(MerchantProfileRepository.class);
        repository = new JpaMerchantCredentialRepository(springDataRepository, profileRepository);
    }

    @Test
    void findActiveByKeyHashMapsMerchantIdentity() {
        Instant now = Instant.parse("2026-06-30T00:00:00Z");
        MerchantCredentialEntity credential = new MerchantCredentialEntity();
        credential.setCredentialId("cred-1");
        credential.setMerchantId("merchant-1");
        credential.setKeyPrefix("nfp_live_1234");
        when(springDataRepository.findActiveByKeyHash("hash-1", now))
                .thenReturn(Optional.of(credential));
        when(profileRepository.findById("merchant-1")).thenReturn(Optional.of(
                MerchantProfile.builder()
                        .merchantId("merchant-1")
                        .merchantCode("merchant-code-1")
                        .displayName("Merchant One")
                        .status(MerchantStatus.ACTIVE)
                        .build()));

        Optional<MerchantApiKey> found = repository.findActiveByKeyHash("hash-1", now);

        assertTrue(found.isPresent());
        assertEquals("merchant-1", found.get().getMerchantId());
        assertEquals("merchant-code-1", found.get().getMerchantCode());
        assertEquals("nfp_live_1234", found.get().getKeyPrefix());
        verify(springDataRepository).findActiveByKeyHash("hash-1", now);
    }

    @Test
    void findActiveByKeyHashReturnsEmptyWhenCredentialNotFound() {
        Instant now = Instant.parse("2026-06-30T00:00:00Z");
        when(springDataRepository.findActiveByKeyHash("unknown-hash", now))
                .thenReturn(Optional.empty());

        Optional<MerchantApiKey> found = repository.findActiveByKeyHash("unknown-hash", now);

        assertTrue(found.isEmpty());
        verify(springDataRepository).findActiveByKeyHash("unknown-hash", now);
    }

    @Test
    void findActiveByKeyHashReturnsEmptyWhenMerchantDisabled() {
        Instant now = Instant.parse("2026-06-30T00:00:00Z");
        MerchantCredentialEntity credential = new MerchantCredentialEntity();
        credential.setCredentialId("cred-1");
        credential.setMerchantId("merchant-1");
        credential.setKeyPrefix("nfp_live_1234");
        when(springDataRepository.findActiveByKeyHash("hash-1", now))
                .thenReturn(Optional.of(credential));
        when(profileRepository.findById("merchant-1")).thenReturn(Optional.of(
                MerchantProfile.builder()
                        .merchantId("merchant-1")
                        .merchantCode("merchant-code-1")
                        .displayName("Merchant One")
                        .status(MerchantStatus.DISABLED)
                        .build()));

        Optional<MerchantApiKey> found = repository.findActiveByKeyHash("hash-1", now);

        assertTrue(found.isEmpty());
    }
}
