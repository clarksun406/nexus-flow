package com.nexusflow.infra.webhook;

import com.nexusflow.application.WebhookClient;
import com.nexusflow.application.WebhookDeliveryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * HTTP webhook client with HMAC-SHA256 signing and bounded retry.
 */
@Slf4j
public class HttpWebhookClient implements WebhookClient {

    private final RestTemplate restTemplate;
    private final String signingSecret;
    private final int[] retrySeconds;
    private static final int[] DEFAULT_RETRY_SECONDS = {5, 15, 60, 300};

    public HttpWebhookClient(RestTemplate restTemplate, String signingSecret) {
        this(restTemplate, signingSecret, DEFAULT_RETRY_SECONDS);
    }

    HttpWebhookClient(RestTemplate restTemplate, String signingSecret, int[] retrySeconds) {
        this.restTemplate = restTemplate;
        this.signingSecret = signingSecret;
        this.retrySeconds = retrySeconds.clone();
    }

    @Override
    public WebhookDeliveryResult sendWithRetry(String url, String payload) {
        String lastError = null;
        int attempts = 0;
        for (int i = 0; i < retrySeconds.length; i++) {
            int delay = retrySeconds[i];
            attempts = i + 1;
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (signingSecret != null && !signingSecret.isBlank()) {
                    headers.set("X-Signature", hmacSha256(payload, signingSecret));
                }
                HttpEntity<String> entity = new HttpEntity<>(payload, headers);
                restTemplate.postForEntity(url, entity, String.class);
                log.info("Webhook sent to {}", url);
                return WebhookDeliveryResult.succeeded(attempts);
            } catch (Exception e) {
                lastError = e.getMessage();
                log.warn("Webhook attempt {}/{} failed to {}: {}", i + 1, retrySeconds.length, url, e.getMessage());
                if (i < retrySeconds.length - 1) {
                    try {
                        if (delay > 0) {
                            Thread.sleep(delay * 1000L);
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Webhook retry interrupted for {}", url);
                        return WebhookDeliveryResult.failed(attempts, "Webhook retry interrupted");
                    }
                }
            }
        }
        log.error("Webhook exhausted all {} retries for {}", retrySeconds.length, url);
        return WebhookDeliveryResult.failed(attempts, lastError);
    }

    private static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 not available", e);
        }
    }
}
