package com.nexusflow.domain.blockchain;

import com.nexusflow.domain.shared.Chain;

import java.util.Optional;

public interface ChainScanCursorRepository {

    void save(ChainScanCursor cursor);

    Optional<ChainScanCursor> findByChain(Chain chain);
}
