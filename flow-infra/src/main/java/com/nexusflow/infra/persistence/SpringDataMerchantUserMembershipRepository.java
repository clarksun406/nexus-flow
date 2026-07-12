package com.nexusflow.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataMerchantUserMembershipRepository extends JpaRepository<MerchantUserMembershipEntity, Long> {

    List<MerchantUserMembershipEntity> findByUserId(String userId);

    List<MerchantUserMembershipEntity> findByMerchantId(String merchantId);
}
