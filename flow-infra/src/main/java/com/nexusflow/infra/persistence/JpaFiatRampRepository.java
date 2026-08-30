package com.nexusflow.infra.persistence;

import com.nexusflow.domain.fiat.FiatRampDirection;
import com.nexusflow.domain.fiat.FiatRampOrder;
import com.nexusflow.domain.fiat.FiatRampRepository;
import com.nexusflow.domain.fiat.FiatRampStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nexusflow.execution.persistence", havingValue = "jpa", matchIfMissing = true)
public class JpaFiatRampRepository implements FiatRampRepository {

    private final SpringDataFiatRampOrderRepository repository;

    @Override
    public void save(FiatRampOrder order) {
        FiatRampOrderEntity entity = toEntity(order);
        repository.findById(order.getRampOrderId())
                .map(FiatRampOrderEntity::getVersion)
                .ifPresent(entity::setVersion);
        repository.save(entity);
    }

    @Override
    public Optional<FiatRampOrder> findByRampOrderId(String rampOrderId) {
        return repository.findById(rampOrderId).map(this::toDomain);
    }

    @Override
    public Optional<FiatRampOrder> findByMerchantOrderNo(String merchantId, String merchantOrderNo) {
        return repository.findByMerchantIdAndMerchantOrderNo(merchantId, merchantOrderNo).map(this::toDomain);
    }

    @Override
    public Optional<FiatRampOrder> findByProviderOrderId(String providerId, String providerOrderId) {
        return repository.findByProviderIdAndProviderOrderId(providerId, providerOrderId).map(this::toDomain);
    }

    @Override
    public Optional<FiatRampOrder> findByPaymentId(String paymentId) {
        return repository.findByPaymentId(paymentId).map(this::toDomain);
    }

    @Override
    public com.nexusflow.common.PageResult<FiatRampOrder> search(FiatRampStatus status, String merchantId,
                                                                 int page, int size) {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,
                                "createTime"));
        org.springframework.data.domain.Page<FiatRampOrderEntity> result;
        if (status != null && merchantId != null) {
            result = repository.findByMerchantIdAndStatus(merchantId, status.name(), pageable);
        } else if (status != null) {
            result = repository.findByStatus(status.name(), pageable);
        } else if (merchantId != null) {
            result = repository.findByMerchantId(merchantId, pageable);
        } else {
            result = repository.findAll(pageable);
        }
        java.util.List<FiatRampOrder> items = result.getContent().stream().map(this::toDomain).toList();
        return com.nexusflow.common.PageResult.of(items, page, size, result.getTotalElements());
    }

    FiatRampOrderEntity toEntity(FiatRampOrder order) {
        FiatRampOrderEntity entity = new FiatRampOrderEntity();
        entity.setRampOrderId(order.getRampOrderId());
        entity.setMerchantId(order.getMerchantId());
        entity.setMerchantOrderNo(order.getMerchantOrderNo());
        entity.setPaymentId(order.getPaymentId());
        entity.setDirection(order.getDirection() != null ? order.getDirection().name() : null);
        entity.setProviderId(order.getProviderId());
        entity.setProviderOrderId(order.getProviderOrderId());
        entity.setQuoteId(order.getQuoteId());
        entity.setFiatAmount(order.getFiatAmount());
        entity.setFiatCurrency(order.getFiatCurrency());
        entity.setCryptoAmount(order.getCryptoAmount());
        entity.setToken(order.getToken());
        entity.setNetwork(order.getNetwork());
        entity.setExchangeRate(order.getExchangeRate());
        entity.setFeeAmountFiat(order.getFeeAmountFiat());
        entity.setWalletAddress(order.getWalletAddress());
        entity.setCheckoutUrl(order.getCheckoutUrl());
        entity.setFiatTransferId(order.getFiatTransferId());
        entity.setCryptoTxHash(order.getCryptoTxHash());
        entity.setNotifyUrl(order.getNotifyUrl());
        entity.setReturnUrl(order.getReturnUrl());
        entity.setFailureReason(order.getFailureReason());
        entity.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        entity.setExpireTime(order.getExpireTime());
        entity.setCompleteTime(order.getCompleteTime());
        entity.setCreateTime(order.getCreateTime());
        entity.setUpdateTime(order.getUpdateTime());
        return entity;
    }

    FiatRampOrder toDomain(FiatRampOrderEntity entity) {
        return FiatRampOrder.reconstitute()
                .rampOrderId(entity.getRampOrderId())
                .merchantId(entity.getMerchantId())
                .merchantOrderNo(entity.getMerchantOrderNo())
                .paymentId(entity.getPaymentId())
                .direction(entity.getDirection() != null ? FiatRampDirection.valueOf(entity.getDirection()) : null)
                .providerId(entity.getProviderId())
                .providerOrderId(entity.getProviderOrderId())
                .quoteId(entity.getQuoteId())
                .fiatAmount(entity.getFiatAmount())
                .fiatCurrency(entity.getFiatCurrency())
                .cryptoAmount(entity.getCryptoAmount())
                .token(entity.getToken())
                .network(entity.getNetwork())
                .exchangeRate(entity.getExchangeRate())
                .feeAmountFiat(entity.getFeeAmountFiat())
                .walletAddress(entity.getWalletAddress())
                .checkoutUrl(entity.getCheckoutUrl())
                .fiatTransferId(entity.getFiatTransferId())
                .cryptoTxHash(entity.getCryptoTxHash())
                .notifyUrl(entity.getNotifyUrl())
                .returnUrl(entity.getReturnUrl())
                .failureReason(entity.getFailureReason())
                .status(entity.getStatus() != null ? FiatRampStatus.valueOf(entity.getStatus()) : null)
                .expireTime(entity.getExpireTime())
                .completeTime(entity.getCompleteTime())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
