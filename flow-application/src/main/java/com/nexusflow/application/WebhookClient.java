package com.nexusflow.application;

/**
 * Port for sending webhook callbacks to merchants.
 * Implementation in flow-infra.
 */
public interface WebhookClient {
    void sendWithRetry(String url, String payload);
}