package com.nexusflow.infra.persistence;

import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.wallet.MnemonicBackup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaMnemonicStoreTest {

    private SpringDataMnemonicBackupRepository springDataRepository;
    private JpaMnemonicStore store;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataMnemonicBackupRepository.class);
        store = new JpaMnemonicStore(springDataRepository);
    }

    @Test
    void saveMapsDomainFieldsAndPreservesVersion() {
        MnemonicBackupEntity existing = new MnemonicBackupEntity();
        existing.setId("mn-1");
        existing.setVersion(3L);
        when(springDataRepository.findById("mn-1")).thenReturn(Optional.of(existing));
        MnemonicBackup backup = MnemonicBackup.builder()
                .id("mn-1")
                .walletId("wallet-1")
                .chain(Chain.TRON)
                .encryptedMnemonic("cipher")
                .derivationPath("m/44'/195'/0'/0/0")
                .build();

        store.save(backup);

        ArgumentCaptor<MnemonicBackupEntity> captor = ArgumentCaptor.forClass(MnemonicBackupEntity.class);
        verify(springDataRepository).save(captor.capture());
        MnemonicBackupEntity entity = captor.getValue();
        assertEquals("wallet-1", entity.getWalletId());
        assertEquals("TRON", entity.getChain());
        assertEquals("cipher", entity.getEncryptedMnemonic());
        assertEquals("m/44'/195'/0'/0/0", entity.getDerivationPath());
        assertEquals(3L, entity.getVersion());
    }

    @Test
    void findByWalletIdReconstitutesDomain() {
        Instant created = Instant.parse("2026-06-13T00:00:00Z");
        MnemonicBackupEntity entity = new MnemonicBackupEntity();
        entity.setId("mn-1");
        entity.setWalletId("wallet-1");
        entity.setChain("TRON");
        entity.setEncryptedMnemonic("cipher");
        entity.setDerivationPath("m/44'/195'/0'/0/0");
        entity.setCreatedAt(created);
        when(springDataRepository.findByWalletId("wallet-1")).thenReturn(Optional.of(entity));

        Optional<MnemonicBackup> found = store.findByWalletId("wallet-1");

        assertTrue(found.isPresent());
        assertEquals("mn-1", found.get().getId());
        assertEquals(Chain.TRON, found.get().getChain());
        assertEquals(created, found.get().getCreatedAt());
    }
}
