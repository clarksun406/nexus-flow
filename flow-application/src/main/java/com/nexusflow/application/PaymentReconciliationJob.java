package com.nexusflow.application;

import com.nexusflow.domain.blockchain.BlockchainAdapter;
import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.payment.PaymentRepository;
import com.nexusflow.domain.payment.PaymentStatus;
import com.nexusflow.domain.shared.Chain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Closes the execution-layer payment lifecycle that on-chain detection alone cannot:
 *
 * <ul>
 *   <li><b>Reconciliation</b> — re-queries the chain for confirmations of payments that are
 *       DETECTED/CONFIRMING and drives them toward CONFIRMED.</li>
 *   <li><b>Expiry</b> — marks PENDING payments that were never funded as EXPIRED after a TTL.</li>
 * </ul>
 *
 * The per-payment state mutations are delegated to {@link PaymentApplicationService} so each runs
 * in its own transaction (via the Spring proxy); a single failing payment never aborts the batch.
 */
@Slf4j
@Component
public class PaymentReconciliationJob {

    private final PaymentRepository paymentRepository;
    private final PaymentApplicationService paymentService;
    private final Map<Chain, BlockchainAdapter> adaptersByChain;
    private final long expiryMinutes;

    public PaymentReconciliationJob(PaymentRepository paymentRepository,
                                    PaymentApplicationService paymentService,
                                    List<BlockchainAdapter> adapters,
                                    @Value("${nexusflow.payment.expiry-minutes:30}") long expiryMinutes) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.adaptersByChain = adapters.stream()
                .collect(Collectors.toMap(BlockchainAdapter::supportedChain, Function.identity(), (a, b) -> a));
        this.expiryMinutes = expiryMinutes;
    }

    /**
     * Re-check confirmations for in-flight payments and advance their state.
     */
    @Scheduled(fixedDelayString = "${nexusflow.reconciliation.interval-ms:30000}")
    public void reconcileConfirmations() {
        List<CryptoPayment> inFlight =
                paymentRepository.findByStatusIn(List.of(PaymentStatus.DETECTED, PaymentStatus.CONFIRMING));
        for (CryptoPayment payment : inFlight) {
            try {
                if (payment.getTxHash() == null) {
                    continue;
                }
                Chain chain = Chain.fromCurrency(payment.getExpected().getCurrency());
                BlockchainAdapter adapter = adaptersByChain.get(chain);
                if (adapter == null) {
                    log.warn("No blockchain adapter for chain {} (payment {})", chain, payment.getId());
                    continue;
                }
                int confirmations = adapter.getConfirmations(payment.getTxHash());
                paymentService.confirmPayment(payment.getId(), confirmations);
            } catch (Exception e) {
                log.error("Reconciliation failed for payment {}: {}", payment.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Expire PENDING payments older than the configured TTL.
     */
    @Scheduled(fixedDelayString = "${nexusflow.expiry.interval-ms:60000}")
    public void expireOverduePayments() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(expiryMinutes));
        List<CryptoPayment> pending = paymentRepository.findByStatusIn(List.of(PaymentStatus.PENDING));
        for (CryptoPayment payment : pending) {
            try {
                if (payment.getCreatedAt() != null && payment.getCreatedAt().isBefore(cutoff)) {
                    paymentService.expirePayment(payment.getId());
                }
            } catch (Exception e) {
                log.error("Expiry failed for payment {}: {}", payment.getId(), e.getMessage(), e);
            }
        }
    }
}
