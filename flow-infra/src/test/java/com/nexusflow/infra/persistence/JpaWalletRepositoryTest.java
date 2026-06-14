package com.nexusflow.infra.persistence;

import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.wallet.Wallet;
import com.nexusflow.domain.wallet.WalletType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaWalletRepositoryTest {

    private SpringDataWalletRepository springDataRepository;
    private JpaWalletRepository repository;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataWalletRepository.class);
        repository = new JpaWalletRepository(springDataRepository);
    }

    @Test
    void saveMapsDomainFieldsToEntity() {
        Wallet wallet = Wallet.builder()
                .id("wallet-1")
                .name("TRON hot")
                .chain(Chain.TRON)
                .type(WalletType.HOT)
                .address("TADDR")
                .encryptedPrivateKey("ciphertext")
                .kmsKeyId("kms-key-1")
                .mpcWalletId("mpc-wallet-1")
                .build();

        repository.save(wallet);

        ArgumentCaptor<WalletEntity> captor = ArgumentCaptor.forClass(WalletEntity.class);
        verify(springDataRepository).save(captor.capture());
        WalletEntity entity = captor.getValue();
        assertEquals("wallet-1", entity.getId());
        assertEquals("TRON hot", entity.getName());
        assertEquals("TRON", entity.getChain());
        assertEquals("HOT", entity.getType());
        assertEquals("TADDR", entity.getAddress());
        assertEquals("ciphertext", entity.getEncryptedPrivateKey());
        assertEquals("kms-key-1", entity.getKmsKeyId());
        assertEquals("mpc-wallet-1", entity.getMpcWalletId());
        assertTrue(entity.isActive());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void savePreservesExistingVersionForUpdates() {
        WalletEntity existing = new WalletEntity();
        existing.setId("wallet-1");
        existing.setVersion(7L);
        when(springDataRepository.findById("wallet-1")).thenReturn(Optional.of(existing));
        Wallet wallet = Wallet.builder()
                .id("wallet-1")
                .name("TRON hot")
                .chain(Chain.TRON)
                .type(WalletType.HOT)
                .address("TADDR")
                .encryptedPrivateKey("ciphertext")
                .build();

        repository.save(wallet);

        ArgumentCaptor<WalletEntity> captor = ArgumentCaptor.forClass(WalletEntity.class);
        verify(springDataRepository).save(captor.capture());
        assertEquals(7L, captor.getValue().getVersion());
    }

    @Test
    void findByIdReconstitutesAllPersistentFields() {
        Instant createdAt = Instant.parse("2026-06-13T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-06-13T00:01:00Z");
        WalletEntity entity = new WalletEntity();
        entity.setId("wallet-1");
        entity.setName("TRON hot");
        entity.setChain("TRON");
        entity.setType("HOT");
        entity.setAddress("TADDR");
        entity.setEncryptedPrivateKey("ciphertext");
        entity.setKmsKeyId("kms-key-1");
        entity.setMpcWalletId("mpc-wallet-1");
        entity.setActive(false);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);
        when(springDataRepository.findById("wallet-1")).thenReturn(Optional.of(entity));

        Optional<Wallet> found = repository.findById("wallet-1");

        assertTrue(found.isPresent());
        Wallet wallet = found.get();
        assertEquals("wallet-1", wallet.getId());
        assertEquals("TRON hot", wallet.getName());
        assertEquals(Chain.TRON, wallet.getChain());
        assertEquals(WalletType.HOT, wallet.getType());
        assertEquals("TADDR", wallet.getAddress());
        assertEquals("ciphertext", wallet.getEncryptedPrivateKey());
        assertEquals("kms-key-1", wallet.getKmsKeyId());
        assertEquals("mpc-wallet-1", wallet.getMpcWalletId());
        assertFalse(wallet.isActive());
        assertEquals(createdAt, wallet.getCreatedAt());
        assertEquals(updatedAt, wallet.getUpdatedAt());
    }

    @Test
    void findActiveByChainDelegatesEnumName() {
        repository.findActiveByChain(Chain.ETH);

        verify(springDataRepository).findFirstByChainAndActive("ETH", true);
    }
}
