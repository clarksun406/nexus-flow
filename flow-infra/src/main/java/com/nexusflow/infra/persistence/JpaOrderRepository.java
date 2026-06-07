package com.nexusflow.infra.persistence;

import com.nexusflow.domain.order.OrderRepository;
import com.nexusflow.domain.order.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaOrderRepository extends OrderRepository, JpaRepository<PaymentOrderEntity, String> {

    @Override
    default void save(PaymentOrder order) {
        save(toEntity(order));
    }

    @Override
    default Optional<PaymentOrder> findByPaymentId(String paymentId) {
        return findById(paymentId).map(this::toDomain);
    }

    @Override
    default Optional<PaymentOrder> findByMerchantOrderNo(String merchantId, String merchantOrderNo) {
        return findByMerchantIdAndMerchantOrderNo(merchantId, merchantOrderNo).map(this::toDomain);
    }

    @Override
    default Optional<PaymentOrder> findByChannelOrderId(String channelId, String channelOrderId) {
        return findByChannelIdAndChannelOrderId(channelId, channelOrderId).map(this::toDomain);
    }

    @Override
    default boolean existsByMerchantOrderNo(String merchantId, String merchantOrderNo) {
        return existsByMerchantIdAndMerchantOrderNo(merchantId, merchantOrderNo);
    }

    Optional<PaymentOrderEntity> findByMerchantIdAndMerchantOrderNo(String merchantId, String merchantOrderNo);
    Optional<PaymentOrderEntity> findByChannelIdAndChannelOrderId(String channelId, String channelOrderId);
    boolean existsByMerchantIdAndMerchantOrderNo(String merchantId, String merchantOrderNo);

    default PaymentOrderEntity toEntity(PaymentOrder o) {
        PaymentOrderEntity e = new PaymentOrderEntity();
        e.setPaymentId(o.getPaymentId()); e.setMerchantId(o.getMerchantId());
        e.setMerchantOrderNo(o.getMerchantOrderNo()); e.setAmountFiat(o.getAmountFiat());
        e.setCurrencyFiat(o.getCurrencyFiat()); e.setAmountCrypto(o.getAmountCrypto());
        e.setCurrencyCrypto(o.getCurrencyCrypto()); e.setNetwork(o.getNetwork());
        e.setExchangeRate(o.getExchangeRate()); e.setChannelId(o.getChannelId());
        e.setChannelUserId(o.getChannelUserId()); e.setChannelOrderId(o.getChannelOrderId());
        e.setStatus(o.getStatus().name()); e.setPayAddress(o.getPayAddress()); e.setMemo(o.getMemo());
        e.setPaidAmountCrypto(o.getPaidAmountCrypto()); e.setPaidAmountFiat(o.getPaidAmountFiat());
        e.setTxHash(o.getTxHash()); e.setNotifyUrl(o.getNotifyUrl()); e.setReturnUrl(o.getReturnUrl());
        e.setExtendData(o.getExtendData()); e.setExpireTime(o.getExpireTime());
        e.setPayTime(o.getPayTime()); e.setConfirmTime(o.getConfirmTime());
        e.setCreateTime(o.getCreateTime()); e.setUpdateTime(o.getUpdateTime());
        return e;
    }

    default PaymentOrder toDomain(PaymentOrderEntity e) {
        return PaymentOrder.builder()
                .paymentId(e.getPaymentId()).merchantId(e.getMerchantId())
                .merchantOrderNo(e.getMerchantOrderNo()).amountFiat(e.getAmountFiat())
                .currencyFiat(e.getCurrencyFiat()).amountCrypto(e.getAmountCrypto())
                .currencyCrypto(e.getCurrencyCrypto()).network(e.getNetwork())
                .exchangeRate(e.getExchangeRate()).channelId(e.getChannelId())
                .channelUserId(e.getChannelUserId()).notifyUrl(e.getNotifyUrl())
                .returnUrl(e.getReturnUrl()).extendData(e.getExtendData())
                .expireTime(e.getExpireTime()).build();
    }
}