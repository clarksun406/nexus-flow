package com.nexusflow.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface SpringDataMerchantCredentialRepository extends JpaRepository<MerchantCredentialEntity, String> {

    /**
     * Loads the active credential for a key hash. Merchant profile lookup is done
     * separately via {@code MerchantProfileRepository}: Hibernate 6 tuple
     * (multi-entity select) results have driver-dependent array shapes, so a
     * plain single-entity query keeps behavior identical on PostgreSQL and H2.
     */
    @Query("""
            select credential
            from MerchantCredentialEntity credential
            where credential.keyHash = :keyHash
              and credential.active = true
              and (credential.expiresAt is null or credential.expiresAt > :now)
            """)
    Optional<MerchantCredentialEntity> findActiveByKeyHash(@Param("keyHash") String keyHash, @Param("now") Instant now);
}
