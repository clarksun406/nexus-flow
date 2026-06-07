package com.nexusflow.infra.webhook;

import com.nexusflow.application.WebhookClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class HttpWebhookClient implements WebhookClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final int[] RETRY_SECONDS = {60, 300, 600, 1800, 3600, 7200};

    @Override
    public void sendWithRetry(String url, String payload) {
        for (int delay : RETRY_SECONDS) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(payload, headers);
                restTemplate.postForEntity(url, entity, String.class);
                log.info("Webhook sent to {}", url);
                return;
            } catch (Exception e) {
                log.warn("Webhook failed to {} (retry in {}s): {}", url, delay, e.getMessage());
                try { Thread.sleep(delay * 1000L); } catch (InterruptedException ignored) {}
            }
        }
        log.error("Webhook exhausted retries for {}", url);
    }
}