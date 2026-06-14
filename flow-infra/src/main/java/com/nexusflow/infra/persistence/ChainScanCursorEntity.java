package com.nexusflow.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "chain_scan_cursors")
public class ChainScanCursorEntity {

    @Id
    @Column(length = 20)
    private String chain;

    @Column(name = "last_scanned_block", nullable = false)
    private Long lastScannedBlock;

    @Column(name = "last_scanned_block_hash", length = 128)
    private String lastScannedBlockHash;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;
}
