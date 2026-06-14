package com.nexusflow.infra.persistence;

import com.nexusflow.domain.payment.CryptoPayment;
import com.nexusflow.domain.payment.PaymentRepository;
import com.nexusflow.domain.payment.PaymentStatus;
import com.nexusflow.domain.shared.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nexusflow.execution.persistence", havingValue = "jpa", matchIfMissing = true)
public class JpaPaymentRepository implements PaymentRepository {

    private final SpringDataCryptoPaymentRepository repository;

    @Override
    public void save(CryptoPayment payment) {
        CryptoPaymentEntity entity = toEntity(payment);
        repository.findById(payment.getId())
                .map(CryptoPaymentEntity::getVersion)
                .ifPresent(entity::setVersion);
        repository.save(entity);
    }

    @Override
    public Optional<CryptoPayment> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<CryptoPayment> findByOrderId(String orderId) {
        return repository.findByOrderId(orderId).map(this::toDomain);
    }

    @Override
    public Optional<CryptoPayment> findByTxHash(String txHash) {
        return repository.findFirstByTxHash(txHash).map(this::toDomain);
    }

    @Override
    public Optional<CryptoPayment> findPendingByReceivingAddress(String receivingAddress) {
        if (receivingAddress == null) {
            return Optional.empty();
        }
        return repository.findFirstByReceivingAddressAndStatus(receivingAddress, PaymentStatus.PENDING.name())
                .map(this::toDomain);
    }

    @Override
    public List<CryptoPayment> findByStatusIn(Collection<PaymentStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        List<String> names = statuses.stream().map(PaymentStatus::name).toList();
        return repository.findByStatusIn(names).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByOrderId(String orderId) {
        return repository.existsByOrderId(orderId);
    }

    CryptoPaymentEntity toEntity(CryptoPayment payment) {
        CryptoPaymentEntity entity = new CryptoPaymentEntity();
        entity.setId(payment.getId());
        entity.setOrderId(payment.getOrderId());
        entity.setCurrency(payment.getExpected() != null ? payment.getExpected().getCurrency() : null);
        entity.setExpectedAmount(payment.getExpected() != null ? payment.getExpected().getAmount() : null);
        entity.setReceivedAmount(payment.getReceived() != null ? payment.getReceived().getAmount() : null);
        entity.setStatus(payment.getStatus() != null ? payment.getStatus().name() : null);
        entity.setReceivingAddress(payment.getReceivingAddress());
        entity.setTxHash(payment.getTxHash());
        entity.setDetectedBlockNumber(payment.getDetectedBlockNumber());
        entity.setConfirmations(payment.getConfirmations() != null ? payment.getConfirmations() : 0);
        entity.setRequiredConfirmations(payment.getRequiredConfirmations());
        entity.setRetryCount(payment.getRetryCount() != null ? payment.getRetryCount() : 0);
        entity.setNextRetryAt(payment.getNextRetryAt());
        entity.setLastFailureReason(payment.getLastFailureReason());
        entity.setCallbackUrl(payment.getCallbackUrl());
        entity.setCreatedAt(payment.getCreatedAt());
        entity.setDetectedAt(payment.getDetectedAt());
        entity.setConfirmedAt(payment.getConfirmedAt());
        entity.setExpiredAt(payment.getExpiredAt());
        return entity;
    }

    CryptoPayment toDomain(CryptoPaymentEntity entity) {
        return CryptoPayment.reconstitute()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .expected(money(entity.getCurrency(), entity.getExpectedAmount()))
                .received(money(entity.getCurrency(), entity.getReceivedAmount()))
                .status(entity.getStatus() != null ? PaymentStatus.valueOf(entity.getStatus()) : null)
                .receivingAddress(entity.getReceivingAddress())
                .txHash(entity.getTxHash())
                .detectedBlockNumber(entity.getDetectedBlockNumber())
                .confirmations(entity.getConfirmations())
                .requiredConfirmations(entity.getRequiredConfirmations())
                .retryCount(entity.getRetryCount())
                .nextRetryAt(entity.getNextRetryAt())
                .lastFailureReason(entity.getLastFailureReason())
                .callbackUrl(entity.getCallbackUrl())
                .createdAt(entity.getCreatedAt())
                .detectedAt(entity.getDetectedAt())
                .confirmedAt(entity.getConfirmedAt())
                .expiredAt(entity.getExpiredAt())
                .build();
    }

    private Money money(String currency, BigDecimal amount) {
        return amount != null ? Money.of(currency, amount) : null;
    }
}
