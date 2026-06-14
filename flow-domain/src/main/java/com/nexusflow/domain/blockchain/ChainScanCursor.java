package com.nexusflow.domain.blockchain;

import com.nexusflow.domain.shared.Chain;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
public class ChainScanCursor {

    private Chain chain;
    private long lastScannedBlock;
    private String lastScannedBlockHash;
    private Instant updatedAt;

    @Builder
    public ChainScanCursor(Chain chain, long lastScannedBlock, String lastScannedBlockHash) {
        this.chain = chain;
        this.lastScannedBlock = lastScannedBlock;
        this.lastScannedBlockHash = lastScannedBlockHash;
        this.updatedAt = Instant.now();
    }

    @Builder(builderMethodName = "reconstitute", builderClassName = "ChainScanCursorReconstituteBuilder")
    private ChainScanCursor(Chain chain, long lastScannedBlock, String lastScannedBlockHash, Instant updatedAt) {
        this.chain = chain;
        this.lastScannedBlock = lastScannedBlock;
        this.lastScannedBlockHash = lastScannedBlockHash;
        this.updatedAt = updatedAt;
    }

    public void advanceTo(long blockNumber, String blockHash) {
        this.lastScannedBlock = blockNumber;
        this.lastScannedBlockHash = blockHash;
        this.updatedAt = Instant.now();
    }

    public void rewindTo(long blockNumber, String blockHash) {
        this.lastScannedBlock = Math.max(0, blockNumber);
        this.lastScannedBlockHash = blockHash;
        this.updatedAt = Instant.now();
    }
}
