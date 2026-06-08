package com.nexusflow.infra.persistence;

import com.nexusflow.domain.order.OrderRepository;
import com.nexusflow.domain.order.OrderStatus;
import com.nexusflow.domain.order.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
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

    @Override
    default List<PaymentOrder> findByStatusIn(Collection<OrderStatus> statuses) {
        List<String> names = statuses.stream().map(OrderStatus::name).toList();
        return findEntitiesByStatusIn(names).stream().map(this::toDomain).toList();
    }

    @Query("SELECT o FROM PaymentOrderEntity o WHERE o.status IN :statuses")
    List<PaymentOrderEntity> findEntitiesByStatusIn(@Param("statuses") Collection<String> statuses);

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
        return PaymentOrder.reconstitute()
                .paymentId(e.getPaymentId()).merchantId(e.getMerchantId())
                .merchantOrderNo(e.getMerchantOrderNo()).amountFiat(e.getAmountFiat())
                .currencyFiat(e.getCurrencyFiat()).amountCrypto(e.getAmountCrypto())
                .currencyCrypto(e.getCurrencyCrypto()).network(e.getNetwork())
                .exchangeRate(e.getExchangeRate()).channelId(e.getChannelId())
                .channelUserId(e.getChannelUserId()).channelOrderId(e.getChannelOrderId())
                .status(e.getStatus() != null ? OrderStatus.valueOf(e.getStatus()) : null)
                .payAddress(e.getPayAddress()).memo(e.getMemo())
                .paidAmountCrypto(e.getPaidAmountCrypto()).paidAmountFiat(e.getPaidAmountFiat())
                .txHash(e.getTxHash()).notifyUrl(e.getNotifyUrl()).returnUrl(e.getReturnUrl())
                .extendData(e.getExtendData()).expireTime(e.getExpireTime())
                .payTime(e.getPayTime()).confirmTime(e.getConfirmTime())
                .createTime(e.getCreateTime()).updateTime(e.getUpdateTime())
                .build();
    }
}