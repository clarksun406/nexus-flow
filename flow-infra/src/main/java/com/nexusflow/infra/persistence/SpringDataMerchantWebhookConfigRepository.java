package com.nexusflow.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataMerchantWebhookConfigRepository extends JpaRepository<MerchantWebhookConfigEntity, String> {
}
