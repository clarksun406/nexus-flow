package com.nexusflow.infra.webhook;

import com.nexusflow.application.WebhookClient;
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
    private static final int[] RETRY_SECONDS = {5, 15, 60, 300};

    public HttpWebhookClient(RestTemplate restTemplate, String signingSecret) {
        this.restTemplate = restTemplate;
        this.signingSecret = signingSecret;
    }

    @Override
    public void sendWithRetry(String url, String payload) {
        for (int i = 0; i < RETRY_SECONDS.length; i++) {
            int delay = RETRY_SECONDS[i];
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (signingSecret != null && !signingSecret.isBlank()) {
                    headers.set("X-Signature", hmacSha256(payload, signingSecret));
                }
                HttpEntity<String> entity = new HttpEntity<>(payload, headers);
                restTemplate.postForEntity(url, entity, String.class);
                log.info("Webhook sent to {}", url);
                return;
            } catch (Exception e) {
                log.warn("Webhook attempt {}/{} failed to {}: {}", i + 1, RETRY_SECONDS.length, url, e.getMessage());
                if (i < RETRY_SECONDS.length - 1) {
                    try {
                        Thread.sleep(delay * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Webhook retry interrupted for {}", url);
                        return;
                    }
                }
            }
        }
        log.error("Webhook exhausted all {} retries for {}", RETRY_SECONDS.length, url);
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
