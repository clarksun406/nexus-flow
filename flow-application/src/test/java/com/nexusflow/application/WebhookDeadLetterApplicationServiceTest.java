package com.nexusflow.application;

import com.nexusflow.common.InvalidStateTransitionException;
import com.nexusflow.common.WebhookDeadLetterNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookDeadLetterApplicationServiceTest {

    private WebhookDeadLetterStore deadLetterStore;
    private WebhookClient webhookClient;
    private WebhookDeadLetterApplicationService service;

    @BeforeEach
    void setUp() {
        deadLetterStore = mock(WebhookDeadLetterStore.class);
        webhookClient = mock(WebhookClient.class);
        service = new WebhookDeadLetterApplicationService(deadLetterStore, webhookClient);
    }

    @Test
    void listReturnsPendingDeadLetters() {
        when(deadLetterStore.findByStatus(WebhookDeadLetterStatus.PENDING, 25))
                .thenReturn(List.of(deadLetter(WebhookDeadLetterStatus.PENDING)));

        var responses = service.list(WebhookDeadLetterStatus.PENDING, 25);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo("PENDING");
        assertThat(responses.get(0).getTargetUrl()).isEqualTo("https://merchant.example/callback");
    }

    @Test
    void replaySuccessMarksDeadLetterReplayed() {
        when(deadLetterStore.findById("dlq-1"))
                .thenReturn(Optional.of(deadLetter(WebhookDeadLetterStatus.PENDING)));
        when(webhookClient.sendWithRetry("https://merchant.example/callback", "{\"ok\":true}"))
                .thenReturn(WebhookDeliveryResult.succeeded(1));

        var response = service.replay("dlq-1");

        ArgumentCaptor<WebhookDeadLetter> captor = ArgumentCaptor.forClass(WebhookDeadLetter.class);
        verify(deadLetterStore).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(WebhookDeadLetterStatus.REPLAYED);
        assertThat(captor.getValue().getAttempts()).isEqualTo(5);
        assertThat(captor.getValue().getResolvedAt()).isNotNull();
        assertThat(response.getStatus()).isEqualTo("REPLAYED");
    }

    @Test
    void replayFailureKeepsDeadLetterPendingAndUpdatesReason() {
        when(deadLetterStore.findById("dlq-1"))
                .thenReturn(Optional.of(deadLetter(WebhookDeadLetterStatus.PENDING)));
        when(webhookClient.sendWithRetry("https://merchant.example/callback", "{\"ok\":true}"))
                .thenReturn(WebhookDeliveryResult.failed(4, "timeout"));

        var response = service.replay("dlq-1");

        ArgumentCaptor<WebhookDeadLetter> captor = ArgumentCaptor.forClass(WebhookDeadLetter.class);
        verify(deadLetterStore).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(WebhookDeadLetterStatus.PENDING);
        assertThat(captor.getValue().getAttempts()).isEqualTo(8);
        assertThat(captor.getValue().getFailureReason()).isEqualTo("timeout");
        assertThat(captor.getValue().getResolvedAt()).isNull();
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void replayClientExceptionKeepsDeadLetterPending() {
        when(deadLetterStore.findById("dlq-1"))
                .thenReturn(Optional.of(deadLetter(WebhookDeadLetterStatus.PENDING)));
        when(webhookClient.sendWithRetry("https://merchant.example/callback", "{\"ok\":true}"))
                .thenThrow(new IllegalStateException("transport broken"));

        var response = service.replay("dlq-1");

        ArgumentCaptor<WebhookDeadLetter> captor = ArgumentCaptor.forClass(WebhookDeadLetter.class);
        verify(deadLetterStore).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(WebhookDeadLetterStatus.PENDING);
        assertThat(captor.getValue().getAttempts()).isEqualTo(4);
        assertThat(captor.getValue().getFailureReason()).isEqualTo("transport broken");
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void ignoreMarksDeadLetterIgnored() {
        when(deadLetterStore.findById("dlq-1"))
                .thenReturn(Optional.of(deadLetter(WebhookDeadLetterStatus.PENDING)));

        var response = service.ignore("dlq-1");

        ArgumentCaptor<WebhookDeadLetter> captor = ArgumentCaptor.forClass(WebhookDeadLetter.class);
        verify(deadLetterStore).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(WebhookDeadLetterStatus.IGNORED);
        assertThat(captor.getValue().getResolvedAt()).isNotNull();
        assertThat(response.getStatus()).isEqualTo("IGNORED");
    }

    @Test
    void replayThrowsWhenDeadLetterIsMissing() {
        when(deadLetterStore.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.replay("missing"))
                .isInstanceOf(WebhookDeadLetterNotFoundException.class);
    }

    @Test
    void replayRejectsClosedDeadLetter() {
        when(deadLetterStore.findById("dlq-1"))
                .thenReturn(Optional.of(deadLetter(WebhookDeadLetterStatus.REPLAYED)));

        assertThatThrownBy(() -> service.replay("dlq-1"))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    private WebhookDeadLetter deadLetter(WebhookDeadLetterStatus status) {
        return WebhookDeadLetter.builder()
                .id("dlq-1")
                .deliveryType("CRYPTO_PAYMENT")
                .targetUrl("https://merchant.example/callback")
                .payload("{\"ok\":true}")
                .eventId("event-1")
                .eventType("crypto.payment.detected")
                .paymentId("pay-1")
                .orderId("order-1")
                .failureReason("read timed out")
                .attempts(4)
                .status(status)
                .createdAt(Instant.parse("2026-06-14T00:00:00Z"))
                .resolvedAt(status == WebhookDeadLetterStatus.PENDING
                        ? null
                        : Instant.parse("2026-06-14T00:01:00Z"))
                .build();
    }
}
