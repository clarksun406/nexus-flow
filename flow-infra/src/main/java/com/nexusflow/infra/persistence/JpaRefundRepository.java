package com.nexusflow.infra.persistence;

import com.nexusflow.domain.refund.RefundOrder;
import com.nexusflow.domain.refund.RefundRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaRefundRepository extends RefundRepository, JpaRepository<RefundOrderEntity, String> {

    @Override
    default void save(RefundOrder order) { save(toEntity(order)); }

    @Override
    default Optional<RefundOrder> findByRefundOrderNo(String refundOrderNo) {
        return findById(refundOrderNo).map(this::toDomain);
    }

    @Override
    default Optional<RefundOrder> findByChannelRefundId(String channelRefundId) {
        return findByChannelRefundIdCustom(channelRefundId).map(this::toDomain);
    }

    Optional<RefundOrderEntity> findByChannelRefundIdCustom(String channelRefundId);

    default RefundOrderEntity toEntity(RefundOrder r) {
        RefundOrderEntity e = new RefundOrderEntity();
        e.setRefundOrderNo(r.getRefundOrderNo()); e.setPaymentId(r.getPaymentId());
        e.setChannelRefundId(r.getChannelRefundId()); e.setRefundAmountFiat(r.getRefundAmountFiat());
        e.setRefundAmountCrypto(r.getRefundAmountCrypto()); e.setExchangeRate(r.getExchangeRate());
        e.setToken(r.getToken()); e.setNetwork(r.getNetwork()); e.setToAddress(r.getToAddress());
        e.setTxHash(r.getTxHash()); e.setNotifyUrl(r.getNotifyUrl());
        e.setStatus(r.getStatus().name()); e.setCreateTime(r.getCreateTime());
        e.setConfirmTime(r.getConfirmTime()); e.setUpdateTime(r.getUpdateTime());
        return e;
    }

    default RefundOrder toDomain(RefundOrderEntity e) {
        return RefundOrder.builder()
                .refundOrderNo(e.getRefundOrderNo()).paymentId(e.getPaymentId())
                .refundAmountFiat(e.getRefundAmountFiat()).refundAmountCrypto(e.getRefundAmountCrypto())
                .exchangeRate(e.getExchangeRate()).token(e.getToken()).network(e.getNetwork())
                .toAddress(e.getToAddress()).notifyUrl(e.getNotifyUrl()).build();
    }
}