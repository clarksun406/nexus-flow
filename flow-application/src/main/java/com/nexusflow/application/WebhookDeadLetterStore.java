package com.nexusflow.application;

import java.util.List;
import java.util.Optional;

public interface WebhookDeadLetterStore {

    void save(WebhookDeadLetter deadLetter);

    List<WebhookDeadLetter> findRecent(int limit);

    List<WebhookDeadLetter> findByStatus(WebhookDeadLetterStatus status, int limit);

    Optional<WebhookDeadLetter> findById(String id);
}
