package com.nexusflow.infra.wallet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.wallet.MpcSigningRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpMpcSignerTest {

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private HttpMpcSigner signer;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        objectMapper = new ObjectMapper();
        signer = new HttpMpcSigner("https://mpc.example/sign", "api-key-1", restTemplate, objectMapper);
    }

    @Test
    void postsSigningRequestAndParsesNestedResponse() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "data": {
                    "request_id": "req-1",
                    "mpc_wallet_id": "vault-wallet-1",
                    "signed_transaction": "0xsigned",
                    "provider_signature_id": "sig-1",
                    "signed_at": "2026-06-14T00:00:00Z"
                  }
                }
                """);
        when(restTemplate.exchange(eq("https://mpc.example/sign"), eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.<HttpEntity<JsonNode>>any(), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(response));

        var signature = signer.sign(MpcSigningRequest.builder()
                .requestId("req-1")
                .walletId("wallet-1")
                .mpcWalletId("vault-wallet-1")
                .chain(Chain.ETH)
                .unsignedTransaction("0xunsigned")
                .metadata("{\"refundOrderNo\":\"ref-1\"}")
                .build());

        assertEquals("req-1", signature.getRequestId());
        assertEquals("vault-wallet-1", signature.getMpcWalletId());
        assertEquals("0xsigned", signature.getSignedTransaction());
        assertEquals("sig-1", signature.getProviderSignatureId());
        assertEquals(Instant.parse("2026-06-14T00:00:00Z"), signature.getSignedAt());

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<HttpEntity<JsonNode>> captor = ArgumentCaptor.forClass((Class) HttpEntity.class);
        verify(restTemplate).exchange(eq("https://mpc.example/sign"), eq(HttpMethod.POST),
                captor.capture(), eq(JsonNode.class));
        HttpEntity<JsonNode> entity = captor.getValue();
        assertEquals("api-key-1", entity.getHeaders().getFirst("X-API-Key"));
        assertEquals("req-1", entity.getHeaders().getFirst("X-Request-Id"));
        assertEquals("req-1", entity.getBody().path("request_id").asText());
        assertEquals("wallet-1", entity.getBody().path("wallet_id").asText());
        assertEquals("ETH", entity.getBody().path("chain").asText());
        assertEquals("0xunsigned", entity.getBody().path("unsigned_transaction").asText());
    }

    @Test
    void rejectsResponseWithoutSignedTransaction() throws Exception {
        JsonNode response = objectMapper.readTree("{\"request_id\":\"req-1\"}");
        when(restTemplate.exchange(eq("https://mpc.example/sign"), eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.<HttpEntity<JsonNode>>any(), eq(JsonNode.class)))
                .thenReturn(ResponseEntity.ok(response));

        assertThrows(IllegalStateException.class, () -> signer.sign(MpcSigningRequest.builder()
                .requestId("req-1")
                .walletId("wallet-1")
                .mpcWalletId("vault-wallet-1")
                .chain(Chain.ETH)
                .unsignedTransaction("0xunsigned")
                .build()));
    }
}
