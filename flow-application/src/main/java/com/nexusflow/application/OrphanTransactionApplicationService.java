package com.nexusflow.application;

import com.nexusflow.application.dto.OrphanTransactionResponse;
import com.nexusflow.common.OrphanTransactionNotFoundException;
import com.nexusflow.domain.blockchain.OrphanTransaction;
import com.nexusflow.domain.blockchain.OrphanTransactionRepository;
import com.nexusflow.domain.blockchain.OrphanTransactionStatus;
import com.nexusflow.domain.shared.Chain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrphanTransactionApplicationService {

    private final OrphanTransactionRepository repository;
    private final PaymentApplicationService paymentApplicationService;

    @Transactional(readOnly = true)
    public List<OrphanTransactionResponse> list(OrphanTransactionStatus status) {
        return repository.findByStatus(status).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrphanTransactionResponse resolve(Chain chain, String txHash, String paymentId) {
        OrphanTransaction transaction = findRequired(chain, txHash);
        transaction.resolve(paymentId);
        repository.save(transaction);
        return toResponse(transaction);
    }

    @Transactional
    public OrphanTransactionResponse ignore(Chain chain, String txHash) {
        OrphanTransaction transaction = findRequired(chain, txHash);
        transaction.ignore();
        repository.save(transaction);
        return toResponse(transaction);
    }

    @Transactional
    public OrphanTransactionResponse compensate(Chain chain, String txHash) {
        OrphanTransaction transaction = findRequired(chain, txHash);
        paymentApplicationService.compensateOrphanTransaction(transaction);
        return toResponse(transaction);
    }

    private OrphanTransaction findRequired(Chain chain, String txHash) {
        return repository.findByChainAndTxHash(chain, txHash)
                .orElseThrow(() -> new OrphanTransactionNotFoundException(chain.name(), txHash));
    }

    private OrphanTransactionResponse toResponse(OrphanTransaction transaction) {
        return OrphanTransactionResponse.builder()
                .id(transaction.getId())
                .chain(transaction.getChain() != null ? transaction.getChain().name() : null)
                .txHash(transaction.getTxHash())
                .toAddress(transaction.getToAddress())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .blockNumber(transaction.getBlockNumber())
                .status(transaction.getStatus() != null ? transaction.getStatus().name() : null)
                .firstSeenAt(toEpochMillis(transaction.getFirstSeenAt()))
                .lastSeenAt(toEpochMillis(transaction.getLastSeenAt()))
                .seenCount(transaction.getSeenCount())
                .resolvedPaymentId(transaction.getResolvedPaymentId())
                .build();
    }

    private Long toEpochMillis(Instant instant) {
        return instant != null ? instant.toEpochMilli() : null;
    }
}
