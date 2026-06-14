package com.nexusflow.application;

import com.nexusflow.application.dto.CreatePaymentCommand;
import com.nexusflow.application.dto.PaymentResponse;
import com.nexusflow.common.ErrorCode;
import com.nexusflow.common.IdempotencyViolationException;
import com.nexusflow.common.NexusFlowException;
import com.nexusflow.common.PaymentNotFoundException;
import com.nexusflow.domain.event.DomainEvent;
import com.nexusflow.domain.event.DomainEventPublisher;
import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.payment.PaymentRepository;
import com.nexusflow.domain.payment.PaymentStatus;
import com.nexusflow.domain.shared.Chain;
import com.nexusflow.domain.shared.Money;
import com.nexusflow.domain.wallet.AddressPoolEntry;
import com.nexusflow.domain.wallet.AddressPoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Application service orchestrating payment use cases.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApplicationService {

    private final PaymentRepository paymentRepository;
    private final AddressPoolRepository addressPoolRepository;
    private final DomainEventPublisher eventPublisher;
    private final WebhookService webhookService;
    private final PaymentIdempotencyStore idempotencyStore;

    /**
     * Create a new payment and assign a receiving address.
     */
    @Transactional
    public PaymentResponse createPayment(CreatePaymentCommand command) {
        String idempotencyKey = normalize(command.getIdempotencyKey());
        if (idempotencyKey != null) {
            return createPaymentIdempotently(command, idempotencyKey);
        }
        return createPaymentOnce(command);
    }

    private PaymentResponse createPaymentIdempotently(CreatePaymentCommand command, String idempotencyKey) {
        String requestHash = requestHashFor(command);
        PaymentIdempotencyStore.StoredPaymentResponse existing = idempotencyStore.find(idempotencyKey).orElse(null);
        if (existing != null) {
            return replayOrReject(idempotencyKey, requestHash, existing);
        }

        Instant expiresAt = Instant.now().plus(Duration.ofHours(24));
        if (!idempotencyStore.reserve(idempotencyKey, requestHash, expiresAt)) {
            existing = idempotencyStore.find(idempotencyKey)
                    .orElseThrow(() -> new IdempotencyViolationException(idempotencyKey));
            return replayOrReject(idempotencyKey, requestHash, existing);
        }

        try {
            PaymentResponse response = createPaymentOnce(command);
            idempotencyStore.complete(idempotencyKey, response);
            return response;
        } catch (RuntimeException e) {
            idempotencyStore.delete(idempotencyKey);
            throw e;
        }
    }

    private PaymentResponse replayOrReject(String idempotencyKey,
                                           String requestHash,
                                           PaymentIdempotencyStore.StoredPaymentResponse existing) {
        if (!requestHash.equals(existing.requestHash())) {
            throw new IdempotencyViolationException(idempotencyKey);
        }
        if (!existing.isCompleted()) {
            throw new IdempotencyViolationException(idempotencyKey);
        }
        return existing.response();
    }

    private PaymentResponse createPaymentOnce(CreatePaymentCommand command) {
        // Idempotency check
        if (paymentRepository.existsByOrderId(command.getOrderId())) {
            throw new IdempotencyViolationException(command.getOrderId());
        }

        String paymentId = UUID.randomUUID().toString();
        String receivingAddress = allocateReceivingAddress(command.getCurrency(), paymentId);

        CryptoPayment payment = CryptoPayment.builder()
                .id(paymentId)
                .orderId(command.getOrderId())
                .expected(Money.of(command.getCurrency(), new BigDecimal(command.getAmount())))
                .receivingAddress(receivingAddress)
                .callbackUrl(command.getCallbackUrl())
                .build();

        payment.markPending();
        paymentRepository.save(payment);

        publishAndNotify(payment, payment.collectEvents());

        log.info("Payment created: orderId={}, paymentId={}, address={}",
                command.getOrderId(), payment.getId(), receivingAddress);

        return toResponse(payment);
    }

    static String requestHashFor(CreatePaymentCommand command) {
        String canonical = String.join("\n",
                hashPart(command.getOrderId()),
                hashPart(command.getCurrency()),
                hashPart(command.getAmount()),
                hashPart(command.getCallbackUrl()));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String normalize(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    private static String hashPart(String value) {
        String normalized = normalize(value);
        return normalized != null ? normalized : "";
    }

    /**
     * Handle payment detection from blockchain listener.
     * Matches an incoming on-chain transaction to a PENDING payment by receiving address,
     * validates currency, and transitions the payment to DETECTED.
     */
    @Transactional
    public void onPaymentDetected(String txHash, String toAddress, String amount, String currency) {
        onPaymentDetected(txHash, toAddress, amount, currency, null);
    }

    @Transactional
    public void onPaymentDetected(String txHash, String toAddress, String amount, String currency, Long blockNumber) {
        // Idempotency: skip transactions already linked to a payment
        if (paymentRepository.findByTxHash(txHash).isPresent()) {
            log.warn("Transaction already processed: txHash={}", txHash);
            return;
        }

        // Match the incoming tx to a payment awaiting funds at this address
        CryptoPayment payment = paymentRepository.findPendingByReceivingAddress(toAddress).orElse(null);
        if (payment == null) {
            log.warn("No PENDING payment found for incoming tx: toAddress={}, txHash={}", toAddress, txHash);
            return;
        }

        // Validate currency matches what the payment expects
        String expectedCurrency = payment.getExpected() != null ? payment.getExpected().getCurrency() : null;
        if (expectedCurrency != null && !expectedCurrency.equalsIgnoreCase(currency)) {
            log.warn("Currency mismatch for payment {}: expected={}, received={} (txHash={})",
                    payment.getId(), expectedCurrency, currency, txHash);
            return;
        }

        Money received = Money.of(currency, new BigDecimal(amount));
        if (payment.getExpected() != null) {
            int cmp = received.getAmount().compareTo(payment.getExpected().getAmount());
            if (cmp < 0) {
                // Reject dust payments (less than 10% of expected) to prevent address blocking
                BigDecimal minAcceptable = payment.getExpected().getAmount()
                        .multiply(new BigDecimal("0.1"));
                if (received.getAmount().compareTo(minAcceptable) < 0) {
                    log.warn("Dust payment rejected for {}: expected={}, received={}, txHash={} (below 10% minimum)",
                            payment.getId(), payment.getExpected().getAmount().toPlainString(),
                            received.getAmount().toPlainString(), txHash);
                    return;
                }
                log.warn("Underpayment detected for payment {}: expected={}, received={} (txHash={})",
                        payment.getId(), payment.getExpected().getAmount().toPlainString(),
                        received.getAmount().toPlainString(), txHash);
            }
        }

        payment.markDetected(txHash, received, blockNumber);
        paymentRepository.save(payment);
        publishAndNotify(payment, payment.collectEvents());

        log.info("Payment detected: paymentId={}, orderId={}, txHash={}, address={}",
                payment.getId(), payment.getOrderId(), txHash, toAddress);
    }

    /**
     * Confirm a payment by updating confirmation count.
     */
    @Transactional
    public void confirmPayment(String paymentId, int confirmations) {
        CryptoPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        boolean confirmed = payment.updateConfirmations(confirmations);
        paymentRepository.save(payment);

        publishAndNotify(payment, payment.collectEvents());

        if (confirmed) {
            log.info("Payment confirmed: paymentId={}, confirmations={}", paymentId, confirmations);
        }
    }

    @Transactional
    public void recordReconciliationFailure(String paymentId, String reason, int maxBackoffSeconds) {
        CryptoPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        int currentRetries = payment.getRetryCount() != null ? payment.getRetryCount() : 0;
        long delaySeconds = Math.min(maxBackoffSeconds, (long) Math.pow(2, currentRetries) * 5L);
        payment.recordRetryFailure(reason, Instant.now().plus(Duration.ofSeconds(delaySeconds)));
        paymentRepository.save(payment);
        log.warn("Payment reconciliation retry scheduled: paymentId={}, retryCount={}, nextRetryAt={}",
                paymentId, payment.getRetryCount(), payment.getNextRetryAt());
    }

    @Transactional
    public void rollbackPaymentsAfterReorg(Chain chain, long forkBlock) {
        List<CryptoPayment> candidates =
                paymentRepository.findByStatusIn(List.of(PaymentStatus.DETECTED, PaymentStatus.CONFIRMING));
        for (CryptoPayment payment : candidates) {
            if (payment.getExpected() == null) {
                continue;
            }
            Chain paymentChain = Chain.fromCurrency(payment.getExpected().getCurrency());
            Long detectedBlock = payment.getDetectedBlockNumber();
            if (paymentChain == chain && (detectedBlock == null || detectedBlock >= forkBlock)) {
                payment.rollbackAfterReorg();
                paymentRepository.save(payment);
                publishAndNotify(payment, payment.collectEvents());
                log.warn("Rolled payment back after chain reorg: paymentId={}, chain={}, forkBlock={}",
                        payment.getId(), chain, forkBlock);
            }
        }
    }

    /**
     * Mark an overdue payment as expired. Only valid while the payment is still PENDING;
     * the aggregate's state machine rejects expiry once funds have been detected.
     */
    @Transactional
    public void expirePayment(String paymentId) {
        CryptoPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        payment.markExpired();
        paymentRepository.save(payment);

        publishAndNotify(payment, payment.collectEvents());
        log.info("Payment expired: paymentId={}", paymentId);
    }

    /**
     * Mark payment as failed.
     */
    @Transactional
    public void failPayment(String paymentId, String reason) {
        CryptoPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        payment.markFailed(reason);
        paymentRepository.save(payment);

        publishAndNotify(payment, payment.collectEvents());
        log.info("Payment failed: paymentId={}, reason={}", paymentId, reason);
    }

    /**
     * Query payment by ID.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String paymentId) {
        CryptoPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        return toResponse(payment);
    }

    private String allocateReceivingAddress(String currency, String paymentId) {
        Chain chain = Chain.fromCurrency(currency);
        AddressPoolEntry entry = addressPoolRepository.findFirstAvailableByChain(chain)
                .orElseThrow(() -> new NexusFlowException(
                        ErrorCode.ADDRESS_NOT_AVAILABLE,
                        "No available address for chain: " + chain));
        entry.assignTo(paymentId);
        addressPoolRepository.save(entry);
        return entry.getAddress();
    }

    private void publishEvents(List<DomainEvent> events) {
        events.forEach(eventPublisher::publish);
    }

    private void publishAndNotify(CryptoPayment payment, List<DomainEvent> events) {
        publishEvents(events);
        webhookService.notifyCryptoPayment(payment, events);
    }

    private PaymentResponse toResponse(CryptoPayment p) {
        return PaymentResponse.builder()
                .paymentId(p.getId())
                .orderId(p.getOrderId())
                .currency(p.getExpected() != null ? p.getExpected().getCurrency() : null)
                .expectedAmount(p.getExpected() != null ? p.getExpected().getAmount().toPlainString() : null)
                .receivingAddress(p.getReceivingAddress())
                .status(p.getStatus().name())
                .txHash(p.getTxHash())
                .confirmations(p.getConfirmations())
                .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().toEpochMilli() : null)
                .detectedAt(p.getDetectedAt() != null ? p.getDetectedAt().toEpochMilli() : null)
                .confirmedAt(p.getConfirmedAt() != null ? p.getConfirmedAt().toEpochMilli() : null)
                .build();
    }
}
