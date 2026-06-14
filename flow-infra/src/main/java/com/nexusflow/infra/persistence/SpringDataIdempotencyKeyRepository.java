package com.nexusflow.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataIdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, String> {
}
