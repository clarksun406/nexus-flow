package com.nexusflow.infra.persistence;

import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.wallet.MnemonicBackup;
import com.nexusflow.domain.wallet.MnemonicStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaMnemonicStore implements MnemonicStore {

    private final SpringDataMnemonicBackupRepository repository;

    @Override
    public void save(MnemonicBackup backup) {
        MnemonicBackupEntity entity = toEntity(backup);
        repository.findById(backup.getId())
                .map(MnemonicBackupEntity::getVersion)
                .ifPresent(entity::setVersion);
        repository.save(entity);
    }

    @Override
    public Optional<MnemonicBackup> findByWalletId(String walletId) {
        return repository.findByWalletId(walletId).map(this::toDomain);
    }

    MnemonicBackupEntity toEntity(MnemonicBackup backup) {
        MnemonicBackupEntity entity = new MnemonicBackupEntity();
        entity.setId(backup.getId());
        entity.setWalletId(backup.getWalletId());
        entity.setChain(backup.getChain() != null ? backup.getChain().name() : null);
        entity.setEncryptedMnemonic(backup.getEncryptedMnemonic());
        entity.setDerivationPath(backup.getDerivationPath());
        entity.setCreatedAt(backup.getCreatedAt());
        return entity;
    }

    MnemonicBackup toDomain(MnemonicBackupEntity entity) {
        return MnemonicBackup.reconstitute()
                .id(entity.getId())
                .walletId(entity.getWalletId())
                .chain(entity.getChain() != null ? Chain.valueOf(entity.getChain()) : null)
                .encryptedMnemonic(entity.getEncryptedMnemonic())
                .derivationPath(entity.getDerivationPath())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
