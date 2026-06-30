package com.nexusflow.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataMerchantProfileRepository extends JpaRepository<MerchantProfileEntity, String> {
}
