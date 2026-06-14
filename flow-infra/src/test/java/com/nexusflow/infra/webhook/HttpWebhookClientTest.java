package com.nexusflow.infra.webhook;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpWebhookClientTest {

    @Test
    void sendsJsonPayloadWithHmacSignature() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        HttpWebhookClient client = new HttpWebhookClient(restTemplate, "secret", new int[]{0});
        when(restTemplate.postForEntity(eq("https://merchant.example/webhook"),
                org.mockito.ArgumentMatchers.any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        var result = client.sendWithRetry("https://merchant.example/webhook", "{\"event\":\"ok\"}");

        assertTrue(result.success());
        assertEquals(1, result.attempts());
        assertNull(result.lastError());

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://merchant.example/webhook"),
                entityCaptor.capture(), eq(String.class));
        assertEquals("application/json", entityCaptor.getValue().getHeaders().getContentType().toString());
        assertEquals(expectedHmac("{\"event\":\"ok\"}", "secret"),
                entityCaptor.getValue().getHeaders().getFirst("X-Signature"));
    }

    @Test
    void usesInjectedRetryDelaysForFastFailureTests() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        HttpWebhookClient client = new HttpWebhookClient(restTemplate, null, new int[]{0, 0});
        when(restTemplate.postForEntity(eq("https://merchant.example/webhook"),
                org.mockito.ArgumentMatchers.any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("down"));

        var result = client.sendWithRetry("https://merchant.example/webhook", "{}");

        assertFalse(result.success());
        assertEquals(2, result.attempts());
        assertEquals("down", result.lastError());
    }

    private static String expectedHmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
