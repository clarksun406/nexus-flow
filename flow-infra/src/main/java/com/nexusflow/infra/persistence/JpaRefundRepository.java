package com.nexusflow.infra.persistence;

import com.nexusflow.domain.refund.RefundOrder;
import com.nexusflow.domain.refund.RefundRepository;
import com.nexusflow.domain.refund.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
        return findEntityByChannelRefundId(channelRefundId).map(this::toDomain);
    }

    @Query("SELECT r FROM RefundOrderEntity r WHERE r.channelRefundId = :id")
    Optional<RefundOrderEntity> findEntityByChannelRefundId(@Param("id") String channelRefundId);

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
        return RefundOrder.reconstitute()
                .refundOrderNo(e.getRefundOrderNo()).paymentId(e.getPaymentId())
                .channelRefundId(e.getChannelRefundId())
                .refundAmountFiat(e.getRefundAmountFiat()).refundAmountCrypto(e.getRefundAmountCrypto())
                .exchangeRate(e.getExchangeRate()).token(e.getToken()).network(e.getNetwork())
                .toAddress(e.getToAddress()).txHash(e.getTxHash()).notifyUrl(e.getNotifyUrl())
                .status(e.getStatus() != null ? RefundStatus.valueOf(e.getStatus()) : null)
                .createTime(e.getCreateTime()).confirmTime(e.getConfirmTime())
                .updateTime(e.getUpdateTime())
                .build();
    }
}
