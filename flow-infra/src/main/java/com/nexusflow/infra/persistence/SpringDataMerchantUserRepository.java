package com.nexusflow.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataMerchantUserRepository extends JpaRepository<MerchantUserEntity, String> {

    Optional<MerchantUserEntity> findByEmail(String email);
}
