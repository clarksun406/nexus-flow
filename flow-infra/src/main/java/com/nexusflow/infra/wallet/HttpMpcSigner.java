package com.nexusflow.infra.wallet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexusflow.domain.wallet.MpcSignature;
import com.nexusflow.domain.wallet.MpcSigner;
import com.nexusflow.domain.wallet.MpcSigningRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

public class HttpMpcSigner implements MpcSigner {

    private final String signUrl;
    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HttpMpcSigner(String signUrl, String apiKey, ObjectMapper objectMapper) {
        this(signUrl, apiKey, new RestTemplate(), objectMapper);
    }

    HttpMpcSigner(String signUrl, String apiKey, RestTemplate restTemplate, ObjectMapper objectMapper) {
        if (!hasText(signUrl)) {
            throw new IllegalArgumentException("MPC signer URL is required");
        }
        this.signUrl = signUrl.trim();
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public MpcSignature sign(MpcSigningRequest request) {
        JsonNode response = post(signRequestBody(request), headers(request));
        JsonNode payload = response.has("data") ? response.path("data") : response;
        String signedTransaction = firstText(payload, "signed_transaction", "signedTransaction");
        if (!hasText(signedTransaction)) {
            throw new IllegalStateException("MPC signer response missing signed transaction");
        }
        return MpcSignature.builder()
                .requestId(firstTextOrDefault(payload, request.getRequestId(), "request_id", "requestId"))
                .mpcWalletId(firstTextOrDefault(payload, request.getMpcWalletId(), "mpc_wallet_id", "mpcWalletId"))
                .signedTransaction(signedTransaction)
                .providerSignatureId(firstText(payload, "provider_signature_id", "providerSignatureId"))
                .signedAt(parseInstant(firstText(payload, "signed_at", "signedAt")))
                .build();
    }

    private JsonNode post(JsonNode body, HttpHeaders headers) {
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                signUrl,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                JsonNode.class);
        JsonNode responseBody = response.getBody();
        if (responseBody == null) {
            throw new IllegalStateException("Empty MPC signer response");
        }
        return responseBody;
    }

    private ObjectNode signRequestBody(MpcSigningRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("MPC signing request is required");
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("request_id", request.getRequestId());
        body.put("wallet_id", request.getWalletId());
        body.put("mpc_wallet_id", request.getMpcWalletId());
        body.put("chain", request.getChain() != null ? request.getChain().name() : null);
        body.put("unsigned_transaction", request.getUnsignedTransaction());
        body.put("metadata", request.getMetadata());
        return body;
    }

    private HttpHeaders headers(MpcSigningRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (hasText(apiKey)) {
            headers.set("X-API-Key", apiKey.trim());
        }
        if (request != null && hasText(request.getRequestId())) {
            headers.set("X-Request-Id", request.getRequestId());
        }
        return headers;
    }

    private Instant parseInstant(String value) {
        return hasText(value) ? Instant.parse(value) : Instant.now();
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asText(null);
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String firstTextOrDefault(JsonNode node, String defaultValue, String... fields) {
        String value = firstText(node, fields);
        return hasText(value) ? value : defaultValue;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
