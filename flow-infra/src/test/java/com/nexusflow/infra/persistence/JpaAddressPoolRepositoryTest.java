package com.nexusflow.infra.persistence;

import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.wallet.AddressPoolEntry;
import com.nexusflow.domain.wallet.AddressPoolStatus;
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

class JpaAddressPoolRepositoryTest {

    private SpringDataAddressPoolRepository springDataRepository;
    private JpaAddressPoolRepository repository;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataAddressPoolRepository.class);
        repository = new JpaAddressPoolRepository(springDataRepository);
    }

    @Test
    void saveMapsDomainFieldsAndPreservesVersion() {
        AddressPoolEntryEntity existing = new AddressPoolEntryEntity();
        existing.setId("addr-1");
        existing.setVersion(5L);
        when(springDataRepository.findById("addr-1")).thenReturn(Optional.of(existing));
        AddressPoolEntry entry = AddressPoolEntry.builder()
                .id("addr-1")
                .chain(Chain.TRON)
                .address("TADDR")
                .encryptedPrivateKey("cipher")
                .derivationPath("m/44'/195'/0'/0/0")
                .derivationIndex(0)
                .build();
        entry.assignTo("pay-1");

        repository.save(entry);

        ArgumentCaptor<AddressPoolEntryEntity> captor = ArgumentCaptor.forClass(AddressPoolEntryEntity.class);
        verify(springDataRepository).save(captor.capture());
        AddressPoolEntryEntity entity = captor.getValue();
        assertEquals("TRON", entity.getChain());
        assertEquals("TADDR", entity.getAddress());
        assertEquals("ASSIGNED", entity.getStatus());
        assertEquals("pay-1", entity.getAssignedPaymentId());
        assertEquals(5L, entity.getVersion());
    }

    @Test
    void findFirstAvailableReconstitutesDomain() {
        Instant created = Instant.parse("2026-06-13T00:00:00Z");
        AddressPoolEntryEntity entity = new AddressPoolEntryEntity();
        entity.setId("addr-1");
        entity.setChain("TRON");
        entity.setAddress("TADDR");
        entity.setEncryptedPrivateKey("cipher");
        entity.setDerivationPath("m/44'/195'/0'/0/0");
        entity.setDerivationIndex(0);
        entity.setStatus("AVAILABLE");
        entity.setCreatedAt(created);
        entity.setUpdatedAt(created);
        when(springDataRepository.lockFirstAvailableByChain("TRON", "AVAILABLE"))
                .thenReturn(Optional.of(entity));

        Optional<AddressPoolEntry> found = repository.findFirstAvailableByChain(Chain.TRON);

        assertTrue(found.isPresent());
        assertEquals(AddressPoolStatus.AVAILABLE, found.get().getStatus());
        assertEquals("TADDR", found.get().getAddress());
        assertEquals(created, found.get().getCreatedAt());
    }
}
