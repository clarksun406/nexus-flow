package com.nexusflow.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataChainScanCursorRepository extends JpaRepository<ChainScanCursorEntity, String> {
}
