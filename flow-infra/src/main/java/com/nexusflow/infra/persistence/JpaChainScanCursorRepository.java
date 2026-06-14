package com.nexusflow.infra.persistence;

import com.nexusflow.domain.blockchain.ChainScanCursor;
import com.nexusflow.domain.blockchain.ChainScanCursorRepository;
import com.nexusflow.domain.shared.Chain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaChainScanCursorRepository implements ChainScanCursorRepository {

    private final SpringDataChainScanCursorRepository repository;

    @Override
    public void save(ChainScanCursor cursor) {
        ChainScanCursorEntity entity = toEntity(cursor);
        repository.findById(cursor.getChain().name())
                .map(ChainScanCursorEntity::getVersion)
                .ifPresent(entity::setVersion);
        repository.save(entity);
    }

    @Override
    public Optional<ChainScanCursor> findByChain(Chain chain) {
        return repository.findById(chain.name()).map(this::toDomain);
    }

    ChainScanCursorEntity toEntity(ChainScanCursor cursor) {
        ChainScanCursorEntity entity = new ChainScanCursorEntity();
        entity.setChain(cursor.getChain().name());
        entity.setLastScannedBlock(cursor.getLastScannedBlock());
        entity.setLastScannedBlockHash(cursor.getLastScannedBlockHash());
        entity.setUpdatedAt(cursor.getUpdatedAt());
        return entity;
    }

    ChainScanCursor toDomain(ChainScanCursorEntity entity) {
        return ChainScanCursor.reconstitute()
                .chain(Chain.valueOf(entity.getChain()))
                .lastScannedBlock(entity.getLastScannedBlock() != null ? entity.getLastScannedBlock() : 0)
                .lastScannedBlockHash(entity.getLastScannedBlockHash())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
