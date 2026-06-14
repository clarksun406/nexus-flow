package com.nexusflow.infra.webhook;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Opt-in outbound webhook smoke test. It is skipped by default and only runs
 * when LIVE_WEBHOOK_URL is configured.
 */
class LiveWebhookDeliveryTest {

    @Test
    void liveWebhookDeliverySmoke() {
        String url = requireEnv("LIVE_WEBHOOK_URL");
        String secret = System.getenv("LIVE_WEBHOOK_SIGNING_SECRET");
        HttpWebhookClient client = new HttpWebhookClient(restTemplate(), secret, new int[]{0});
        String payload = """
                {"event_type":"nexusflow.live.webhook.smoke","event_id":"%s","occurred_at":"%s"}
                """.formatted(UUID.randomUUID(), Instant.now()).trim();

        var result = client.sendWithRetry(url, payload);

        assertTrue(result.success(), () -> "Webhook live smoke failed: " + result.lastError());
        assertTrue(result.attempts() > 0);
    }

    private static RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(10_000);
        return new RestTemplate(requestFactory);
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        assumeTrue(hasText(value), "Set " + name + " to run this live webhook smoke test");
        return value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
