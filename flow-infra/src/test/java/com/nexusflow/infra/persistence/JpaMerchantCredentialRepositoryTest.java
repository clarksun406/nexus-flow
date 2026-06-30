package com.nexusflow.infra.persistence;

import com.nexusflow.domain.merchant.MerchantApiKey;
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
    private JpaMerchantCredentialRepository repository;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataMerchantCredentialRepository.class);
        repository = new JpaMerchantCredentialRepository(springDataRepository);
    }

    @Test
    void findActiveByKeyHashMapsMerchantIdentity() {
        Instant now = Instant.parse("2026-06-30T00:00:00Z");
        MerchantCredentialEntity credential = new MerchantCredentialEntity();
        credential.setCredentialId("cred-1");
        credential.setKeyPrefix("nfp_live_1234");
        MerchantProfileEntity profile = new MerchantProfileEntity();
        profile.setMerchantId("merchant-1");
        profile.setMerchantCode("merchant-code-1");
        when(springDataRepository.findActiveCredentialWithMerchant("hash-1", now))
                .thenReturn(Optional.of(new Object[] { credential, profile }));

        Optional<MerchantApiKey> found = repository.findActiveByKeyHash("hash-1", now);

        assertTrue(found.isPresent());
        assertEquals("merchant-1", found.get().getMerchantId());
        assertEquals("merchant-code-1", found.get().getMerchantCode());
        assertEquals("nfp_live_1234", found.get().getKeyPrefix());
        verify(springDataRepository).findActiveCredentialWithMerchant("hash-1", now);
    }

    @Test
    void findActiveByKeyHashReturnsEmptyWhenNotFound() {
        Instant now = Instant.parse("2026-06-30T00:00:00Z");
        when(springDataRepository.findActiveCredentialWithMerchant("unknown-hash", now))
                .thenReturn(Optional.empty());

        Optional<MerchantApiKey> found = repository.findActiveByKeyHash("unknown-hash", now);

        assertTrue(found.isEmpty());
        verify(springDataRepository).findActiveCredentialWithMerchant("unknown-hash", now);
    }
}
