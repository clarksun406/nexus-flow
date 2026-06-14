package com.nexusflow.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexusflow.domain.event.DomainEvent;
import com.nexusflow.domain.event.PaymentStateChangedEvent;
import com.nexusflow.domain.order.PaymentOrder;
import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookClient webhookClient;
    private final ObjectMapper objectMapper;

    @Async
    public void notifyMerchant(PaymentOrder order, List<DomainEvent> events) {
        String url = order.getNotifyUrl();
        if (url == null || url.isBlank()) return;

        if (!isSafeUrl(url)) {
            log.warn("Blocked webhook to unsafe URL: {}", url);
            return;
        }

        String payload = buildPayload(order);
        webhookClient.sendWithRetry(url, payload);
    }

    @Async
    public void notifyCryptoPayment(CryptoPayment payment, List<DomainEvent> events) {
        String url = payment.getCallbackUrl();
        if (url == null || url.isBlank()) {
            return;
        }
        if (!isSafeUrl(url)) {
            log.warn("Blocked execution callback to unsafe URL: {}", url);
            return;
        }

        for (DomainEvent event : events) {
            if (event instanceof PaymentStateChangedEvent stateChanged && shouldNotify(stateChanged)) {
                webhookClient.sendWithRetry(url, buildCryptoPaymentPayload(payment, stateChanged));
            }
        }
    }

    private String buildPayload(PaymentOrder order) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("payment_id", order.getPaymentId());
            node.put("merchant_order_no", order.getMerchantOrderNo());
            node.put("status", order.getStatus().name());
            node.put("amount", order.getAmountFiat().toPlainString());
            node.put("paid_amount", order.getPaidAmountFiat().toPlainString());
            node.put("paid_crypto_amount", order.getPaidAmountCrypto().toPlainString());
            node.put("tx_hash", order.getTxHash());
            node.put("currency", order.getCurrencyFiat());
            node.put("timestamp", System.currentTimeMillis());
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.error("Failed to build webhook payload: {}", e.getMessage());
            return "{}";
        }
    }

    private String buildCryptoPaymentPayload(CryptoPayment payment, PaymentStateChangedEvent event) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("event_id", event.getEventId());
            node.put("event_type", event.eventType());
            node.put("payment_id", payment.getId());
            node.put("order_id", payment.getOrderId());
            node.put("reference_order_no", payment.getOrderId());
            node.put("status", event.getNewStatus().name());
            node.put("previous_status", event.getPreviousStatus().name());
            node.put("currency", payment.getExpected() != null ? payment.getExpected().getCurrency() : null);
            node.put("expected_amount", payment.getExpected() != null
                    ? payment.getExpected().getAmount().toPlainString() : null);
            String receivedAmount = payment.getReceived() != null
                    ? payment.getReceived().getAmount().toPlainString() : null;
            node.put("received_amount", receivedAmount);
            node.put("amount", receivedAmount);
            node.put("cumulative_amount", receivedAmount);
            node.put("receiving_address", payment.getReceivingAddress());
            node.put("tx_hash", event.getTxHash());
            if (event.getConfirmations() != null) {
                node.put("confirmations", event.getConfirmations());
            } else if (payment.getConfirmations() != null) {
                node.put("confirmations", payment.getConfirmations());
            }
            if (payment.getDetectedBlockNumber() != null) {
                node.put("detected_block_number", payment.getDetectedBlockNumber());
            }
            node.put("timestamp", event.getOccurredAt().toEpochMilli());
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.error("Failed to build execution callback payload: {}", e.getMessage());
            return "{}";
        }
    }

    private boolean shouldNotify(PaymentStateChangedEvent event) {
        return !(event.getPreviousStatus() == PaymentStatus.CREATED
                && event.getNewStatus() == PaymentStatus.PENDING);
    }

    /**
     * SSRF protection: reject non-HTTPS URLs and private/internal IP addresses.
     */
    private boolean isSafeUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (!"https".equalsIgnoreCase(scheme)) {
                log.warn("Rejected non-HTTPS webhook URL: {}", url);
                return false;
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) return false;

            InetAddress addr = InetAddress.getByName(host);
            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress()) {
                log.warn("Rejected webhook to private IP: {} ({})", host, addr.getHostAddress());
                return false;
            }
            // Block AWS metadata endpoint
            if ("169.254.169.254".equals(addr.getHostAddress())) {
                log.warn("Rejected webhook to cloud metadata endpoint: {}", host);
                return false;
            }
            return true;
        } catch (UnknownHostException | IllegalArgumentException e) {
            log.warn("Rejected webhook with invalid URL: {} ({})", url, e.getMessage());
            return false;
        }
    }
}
