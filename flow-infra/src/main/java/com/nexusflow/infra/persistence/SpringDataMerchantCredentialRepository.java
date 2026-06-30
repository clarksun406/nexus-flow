package com.nexusflow.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface SpringDataMerchantCredentialRepository extends JpaRepository<MerchantCredentialEntity, String> {

    @Query("""
            select credential, profile
            from MerchantCredentialEntity credential
            join MerchantProfileEntity profile on profile.merchantId = credential.merchantId
            where credential.keyHash = :keyHash
              and credential.active = true
              and (credential.expiresAt is null or credential.expiresAt > :now)
              and profile.status = 'ACTIVE'
            """)
    Optional<Object[]> findActiveCredentialWithMerchant(@Param("keyHash") String keyHash, @Param("now") Instant now);
}
