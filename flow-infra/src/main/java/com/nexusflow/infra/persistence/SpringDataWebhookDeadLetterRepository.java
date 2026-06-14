package com.nexusflow.infra.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataWebhookDeadLetterRepository extends JpaRepository<WebhookDeadLetterEntity, String> {

    List<WebhookDeadLetterEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<WebhookDeadLetterEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
