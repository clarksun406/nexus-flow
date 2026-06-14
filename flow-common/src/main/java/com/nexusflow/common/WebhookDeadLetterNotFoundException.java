package com.nexusflow.common;

public class WebhookDeadLetterNotFoundException extends NexusFlowException {

    public WebhookDeadLetterNotFoundException(String id) {
        super(ErrorCode.WEBHOOK_DEAD_LETTER_NOT_FOUND,
                "Webhook dead letter not found: id=" + id);
    }
}
