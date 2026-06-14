package com.nexusflow.application;

import java.util.List;

public interface WebhookDeadLetterStore {

    void save(WebhookDeadLetter deadLetter);

    List<WebhookDeadLetter> findRecent(int limit);
}
