package com.nexusflow.infra.blockchain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * JDK-HttpClient-based {@link TronGridClient}.
 *
 * NOTE: the HTTP transport itself is not covered by offline unit tests; only the response parsing
 * in {@link TronAdapter} is. Verify against a real TronGrid endpoint before production use.
 */
@Slf4j
public class HttpTronGridClient implements TronGridClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpTronGridClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public JsonNode post(String path, Map<String, Object> body) {
        try {
            String json = objectMapper.writeValueAsString(body == null ? Map.of() : body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IOException("TronGrid HTTP " + response.statusCode() + " for " + path);
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("TronGrid request interrupted: " + path, e);
        } catch (IOException e) {
            throw new IllegalStateException("TronGrid request failed: " + path, e);
        }
    }
}
