package com.nexusflow.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusflow.domain.event.DomainEvent;
import com.nexusflow.domain.order.PaymentOrder;
import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookServiceTest {

    private ObjectMapper objectMapper;
    private WebhookClient webhookClient;
    private CapturingWebhookDeadLetterStore deadLetterStore;
    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        webhookClient = mock(WebhookClient.class);
        deadLetterStore = new CapturingWebhookDeadLetterStore();
        webhookService = new WebhookService(webhookClient, deadLetterStore, objectMapper);
    }

    @Test
    void sendsExecutionCallbackForPaymentStateChange() throws Exception {
        CryptoPayment payment = payment("https://8.8.8.8/callback");
        payment.markPending();
        payment.collectEvents();
        payment.markDetected("tx-1", Money.of("USDT_TRC20", new BigDecimal("100")), 123L);
        List<DomainEvent> events = payment.collectEvents();
        when(webhookClient.sendWithRetry(eq("https://8.8.8.8/callback"), anyString()))
                .thenReturn(WebhookDeliveryResult.succeeded(1));

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
        assertThat(deadLetterStore.findRecent(10)).isEmpty();
    }

    @Test
    void skipsInitialPendingEvent() {
        CryptoPayment payment = payment("https://8.8.8.8/callback");
        payment.markPending();

        webhookService.notifyCryptoPayment(payment, payment.collectEvents());

        verify(webhookClient, never()).sendWithRetry(eq("https://8.8.8.8/callback"), anyString());
        assertThat(deadLetterStore.findRecent(10)).isEmpty();
    }

    @Test
    void blocksUnsafeExecutionCallbackUrlAndRecordsDeadLetter() {
        CryptoPayment payment = payment("http://8.8.8.8/callback");
        payment.markPending();
        payment.collectEvents();
        payment.markDetected("tx-1", Money.of("USDT_TRC20", new BigDecimal("100")), 123L);

        webhookService.notifyCryptoPayment(payment, payment.collectEvents());

        verify(webhookClient, never()).sendWithRetry(anyString(), anyString());
        List<WebhookDeadLetter> deadLetters = deadLetterStore.findRecent(10);
        assertThat(deadLetters).hasSize(1);
        assertThat(deadLetters.get(0).getDeliveryType()).isEqualTo("CRYPTO_PAYMENT");
        assertThat(deadLetters.get(0).getFailureReason()).isEqualTo("Unsafe webhook URL");
        assertThat(deadLetters.get(0).getAttempts()).isZero();
        assertThat(deadLetters.get(0).getStatus()).isEqualTo(WebhookDeadLetterStatus.PENDING);
        assertThat(deadLetters.get(0).getEventType()).isEqualTo("crypto.payment.detected");
        assertThat(deadLetters.get(0).getPaymentId()).isEqualTo("pay-1");
    }

    @Test
    void recordsDeadLetterWhenExecutionCallbackRetriesAreExhausted() {
        CryptoPayment payment = payment("https://8.8.8.8/callback");
        payment.markPending();
        payment.collectEvents();
        payment.markDetected("tx-1", Money.of("USDT_TRC20", new BigDecimal("100")), 123L);
        when(webhookClient.sendWithRetry(eq("https://8.8.8.8/callback"), anyString()))
                .thenReturn(WebhookDeliveryResult.failed(4, "read timed out"));

        webhookService.notifyCryptoPayment(payment, payment.collectEvents());

        List<WebhookDeadLetter> deadLetters = deadLetterStore.findRecent(10);
        assertThat(deadLetters).hasSize(1);
        WebhookDeadLetter deadLetter = deadLetters.get(0);
        assertThat(deadLetter.getTargetUrl()).isEqualTo("https://8.8.8.8/callback");
        assertThat(deadLetter.getFailureReason()).isEqualTo("read timed out");
        assertThat(deadLetter.getAttempts()).isEqualTo(4);
        assertThat(deadLetter.getStatus()).isEqualTo(WebhookDeadLetterStatus.PENDING);
        assertThat(deadLetter.getPayload()).contains("\"payment_id\":\"pay-1\"");
    }

    @Test
    void recordsDeadLetterWhenMerchantWebhookRetriesAreExhausted() {
        PaymentOrder order = PaymentOrder.builder()
                .paymentId("pay-order-1")
                .merchantId("merchant-1")
                .merchantOrderNo("merchant-order-1")
                .amountFiat(new BigDecimal("50.00"))
                .currencyFiat("USD")
                .amountCrypto(new BigDecimal("50.00"))
                .currencyCrypto("USDT")
                .network("TRC20")
                .exchangeRate(BigDecimal.ONE)
                .channelId("STUB")
                .channelUserId("user-1")
                .notifyUrl("https://8.8.8.8/order")
                .build();
        order.markExpired();
        List<DomainEvent> events = order.collectEvents();
        when(webhookClient.sendWithRetry(eq("https://8.8.8.8/order"), anyString()))
                .thenReturn(WebhookDeliveryResult.failed(4, "503 Service Unavailable"));

        webhookService.notifyMerchant(order, events);

        List<WebhookDeadLetter> deadLetters = deadLetterStore.findRecent(10);
        assertThat(deadLetters).hasSize(1);
        WebhookDeadLetter deadLetter = deadLetters.get(0);
        assertThat(deadLetter.getDeliveryType()).isEqualTo("ORDER");
        assertThat(deadLetter.getTargetUrl()).isEqualTo("https://8.8.8.8/order");
        assertThat(deadLetter.getEventType()).isEqualTo("nexusflow.order.expired");
        assertThat(deadLetter.getPaymentId()).isEqualTo("pay-order-1");
        assertThat(deadLetter.getOrderId()).isEqualTo("merchant-order-1");
        assertThat(deadLetter.getFailureReason()).isEqualTo("503 Service Unavailable");
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

    private static class CapturingWebhookDeadLetterStore implements WebhookDeadLetterStore {
        private final List<WebhookDeadLetter> deadLetters = new ArrayList<>();

        @Override
        public void save(WebhookDeadLetter deadLetter) {
            deadLetters.add(0, deadLetter);
        }

        @Override
        public List<WebhookDeadLetter> findRecent(int limit) {
            return deadLetters.stream().limit(limit).toList();
        }

        @Override
        public List<WebhookDeadLetter> findByStatus(WebhookDeadLetterStatus status, int limit) {
            return deadLetters.stream()
                    .filter(deadLetter -> deadLetter.getStatus() == status)
                    .limit(limit)
                    .toList();
        }

        @Override
        public Optional<WebhookDeadLetter> findById(String id) {
            return deadLetters.stream()
                    .filter(deadLetter -> deadLetter.getId().equals(id))
                    .findFirst();
        }
    }
}
