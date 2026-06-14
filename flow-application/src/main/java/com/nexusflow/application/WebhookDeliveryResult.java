package com.nexusflow.application;

public record WebhookDeliveryResult(boolean success, int attempts, String lastError) {

    public WebhookDeliveryResult {
        attempts = Math.max(0, attempts);
    }

    public static WebhookDeliveryResult succeeded(int attempts) {
        return new WebhookDeliveryResult(true, attempts, null);
    }

    public static WebhookDeliveryResult failed(int attempts, String lastError) {
        return new WebhookDeliveryResult(false, attempts, lastError);
    }
}
