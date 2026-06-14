package com.nexusflow.infra.persistence;

import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.wallet.Wallet;
import com.nexusflow.domain.wallet.WalletRepository;
import com.nexusflow.domain.wallet.WalletType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nexusflow.execution.persistence", havingValue = "jpa", matchIfMissing = true)
public class JpaWalletRepository implements WalletRepository {

    private final SpringDataWalletRepository repository;

    @Override
    public void save(Wallet wallet) {
        WalletEntity entity = toEntity(wallet);
        repository.findById(wallet.getId())
                .map(WalletEntity::getVersion)
                .ifPresent(entity::setVersion);
        repository.save(entity);
    }

    @Override
    public Optional<Wallet> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Wallet> findActiveByChain(Chain chain) {
        if (chain == null) {
            return Optional.empty();
        }
        return repository.findFirstByChainAndActive(chain.name(), true).map(this::toDomain);
    }

    @Override
    public List<Wallet> findAllActive() {
        return repository.findByActive(true).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Wallet> findByAddress(String address) {
        return repository.findByAddress(address).map(this::toDomain);
    }

    WalletEntity toEntity(Wallet wallet) {
        WalletEntity entity = new WalletEntity();
        entity.setId(wallet.getId());
        entity.setName(wallet.getName());
        entity.setChain(wallet.getChain() != null ? wallet.getChain().name() : null);
        entity.setType(wallet.getType() != null ? wallet.getType().name() : null);
        entity.setAddress(wallet.getAddress());
        entity.setEncryptedPrivateKey(wallet.getEncryptedPrivateKey());
        entity.setKmsKeyId(wallet.getKmsKeyId());
        entity.setActive(wallet.isActive());
        entity.setCreatedAt(wallet.getCreatedAt());
        entity.setUpdatedAt(wallet.getUpdatedAt());
        return entity;
    }

    Wallet toDomain(WalletEntity entity) {
        return Wallet.reconstitute()
                .id(entity.getId())
                .name(entity.getName())
                .chain(entity.getChain() != null ? Chain.valueOf(entity.getChain()) : null)
                .type(entity.getType() != null ? WalletType.valueOf(entity.getType()) : null)
                .address(entity.getAddress())
                .encryptedPrivateKey(entity.getEncryptedPrivateKey())
                .kmsKeyId(entity.getKmsKeyId())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
