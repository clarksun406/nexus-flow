package com.nexusflow.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.domain.event.DomainEvent;
import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class WebhookServiceTest {

    private ObjectMapper objectMapper;
    private WebhookClient webhookClient;
    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        webhookClient = mock(WebhookClient.class);
        webhookService = new WebhookService(webhookClient, objectMapper);
    }

    @Test
    void sendsExecutionCallbackForPaymentStateChange() throws Exception {
        CryptoPayment payment = payment("https://8.8.8.8/callback");
        payment.markPending();
        payment.collectEvents();
        payment.markDetected("tx-1", Money.of("USDT_TRC20", new BigDecimal("100")), 123L);
        List<DomainEvent> events = payment.collectEvents();

        webhookService.notifyCryptoPayment(payment, events);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(webhookClient).sendWithRetry(eq("https://8.8.8.8/callback"), payload.capture());
        JsonNode json = objectMapper.readTree(payload.getValue());
        assertThat(json.get("event_type").asText()).isEqualTo("crypto.payment.detected");
        assertThat(json.get("payment_id").asText()).isEqualTo("pay-1");
        assertThat(json.get("order_id").asText()).isEqualTo("order-1");
        assertThat(json.get("reference_order_no").asText()).isEqualTo("order-1");
        assertThat(json.get("status").asText()).isEqualTo("DETECTED");
        assertThat(json.get("previous_status").asText()).isEqualTo("PENDING");
        assertThat(json.get("currency").asText()).isEqualTo("USDT_TRC20");
        assertThat(json.get("expected_amount").asText()).isEqualTo("100");
        assertThat(json.get("received_amount").asText()).isEqualTo("100");
        assertThat(json.get("amount").asText()).isEqualTo("100");
        assertThat(json.get("cumulative_amount").asText()).isEqualTo("100");
        assertThat(json.get("tx_hash").asText()).isEqualTo("tx-1");
        assertThat(json.get("detected_block_number").asLong()).isEqualTo(123L);
    }

    @Test
    void skipsInitialPendingEvent() {
        CryptoPayment payment = payment("https://8.8.8.8/callback");
        payment.markPending();

        webhookService.notifyCryptoPayment(payment, payment.collectEvents());

        verify(webhookClient, never()).sendWithRetry(eq("https://8.8.8.8/callback"), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void blocksUnsafeExecutionCallbackUrl() {
        CryptoPayment payment = payment("http://8.8.8.8/callback");
        payment.markPending();
        payment.collectEvents();
        payment.markDetected("tx-1", Money.of("USDT_TRC20", new BigDecimal("100")), 123L);

        webhookService.notifyCryptoPayment(payment, payment.collectEvents());

        verify(webhookClient, never()).sendWithRetry(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    private CryptoPayment payment(String callbackUrl) {
        return CryptoPayment.builder()
                .id("pay-1")
                .orderId("order-1")
                .expected(Money.of("USDT_TRC20", new BigDecimal("100")))
                .receivingAddress("TADDR")
                .callbackUrl(callbackUrl)
                .requiredConfirmations(3)
                .build();
    }
}
