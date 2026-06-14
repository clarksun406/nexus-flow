package com.nexusflow.infra.persistence;

import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.wallet.AddressPoolEntry;
import com.nexusflow.domain.wallet.AddressPoolRepository;
import com.nexusflow.domain.wallet.AddressPoolStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaAddressPoolRepository implements AddressPoolRepository {

    private final SpringDataAddressPoolRepository repository;

    @Override
    public void save(AddressPoolEntry entry) {
        AddressPoolEntryEntity entity = toEntity(entry);
        repository.findById(entry.getId())
                .map(AddressPoolEntryEntity::getVersion)
                .ifPresent(entity::setVersion);
        repository.save(entity);
    }

    @Override
    public Optional<AddressPoolEntry> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<AddressPoolEntry> findByAddress(String address) {
        return repository.findByAddress(address).map(this::toDomain);
    }

    @Override
    public Optional<AddressPoolEntry> findFirstAvailableByChain(Chain chain) {
        return repository.findFirstByChainAndStatusOrderByDerivationIndexAsc(
                chain.name(), AddressPoolStatus.AVAILABLE.name()).map(this::toDomain);
    }

    @Override
    public long countAvailableByChain(Chain chain) {
        return repository.countByChainAndStatus(chain.name(), AddressPoolStatus.AVAILABLE.name());
    }

    @Override
    public int maxDerivationIndex(Chain chain) {
        return repository.maxDerivationIndex(chain.name());
    }

    @Override
    public List<AddressPoolEntry> findByStatus(AddressPoolStatus status) {
        return repository.findByStatus(status.name()).stream().map(this::toDomain).toList();
    }

    AddressPoolEntryEntity toEntity(AddressPoolEntry entry) {
        AddressPoolEntryEntity entity = new AddressPoolEntryEntity();
        entity.setId(entry.getId());
        entity.setChain(entry.getChain() != null ? entry.getChain().name() : null);
        entity.setAddress(entry.getAddress());
        entity.setEncryptedPrivateKey(entry.getEncryptedPrivateKey());
        entity.setDerivationPath(entry.getDerivationPath());
        entity.setDerivationIndex(entry.getDerivationIndex());
        entity.setStatus(entry.getStatus() != null ? entry.getStatus().name() : null);
        entity.setAssignedPaymentId(entry.getAssignedPaymentId());
        entity.setCreatedAt(entry.getCreatedAt());
        entity.setAssignedAt(entry.getAssignedAt());
        entity.setUpdatedAt(entry.getUpdatedAt());
        return entity;
    }

    AddressPoolEntry toDomain(AddressPoolEntryEntity entity) {
        return AddressPoolEntry.reconstitute()
                .id(entity.getId())
                .chain(entity.getChain() != null ? Chain.valueOf(entity.getChain()) : null)
                .address(entity.getAddress())
                .encryptedPrivateKey(entity.getEncryptedPrivateKey())
                .derivationPath(entity.getDerivationPath())
                .derivationIndex(entity.getDerivationIndex())
                .status(entity.getStatus() != null ? AddressPoolStatus.valueOf(entity.getStatus()) : null)
                .assignedPaymentId(entity.getAssignedPaymentId())
                .createdAt(entity.getCreatedAt())
                .assignedAt(entity.getAssignedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
